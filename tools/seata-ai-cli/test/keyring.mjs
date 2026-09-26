// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import { spawn, spawnSync } from "node:child_process";
import { mkdtemp, mkdir, access, rm } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { setTimeout as delay } from "node:timers/promises";
import { fileURLToPath } from "node:url";
import assert from "node:assert/strict";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { cleanupCredentials } from "../dist/adapter/auth/cleanup.js";
import { profileConfig } from "../dist/app/profile.js";
import { DatabaseSync } from "node:sqlite";

if (!process.argv.includes("--private-session")) {
  const root = await mkdtemp(join(tmpdir(), "seata-keyring-test-"));
  for (const name of ["data", "config", "runtime", "control"]) await mkdir(join(root, name), { mode: 0o700 });
  const child = spawn("/usr/bin/dbus-run-session", ["--", process.execPath, fileURLToPath(import.meta.url), "--private-session"], {
    env: { ...process.env, SEATA_AI_KEYRING_TEST_ROOT: root, XDG_DATA_HOME: join(root, "data"),
      XDG_CONFIG_HOME: join(root, "config"), XDG_RUNTIME_DIR: join(root, "runtime"), DISPLAY: "", WAYLAND_DISPLAY: "" },
    stdio: ["ignore", "inherit", "inherit"],
  });
  try {
    const [code] = await once(child, "close"); process.exitCode = code ?? 1;
  } finally { await rm(root, { recursive: true, force: true }); }
} else {
  const root = process.env.SEATA_AI_KEYRING_TEST_ROOT;
  assert.ok(root?.startsWith(join(tmpdir(), "seata-keyring-test-")) && process.env.DBUS_SESSION_BUS_ADDRESS);
  assert.equal(process.env.XDG_DATA_HOME, join(root, "data"));
  const daemon = spawn("/usr/bin/gnome-keyring-daemon", ["--foreground", "--components=secrets", "--control-directory", join(root, "control"), "--unlock"], {
    stdio: ["pipe", "ignore", "ignore"],
  });
  daemon.stdin.on("error", () => {});
  daemon.stdin.end("synthetic-keyring-test-password");
  const done = once(daemon, "close");
  try {
    const args = ["call", "--session", "--dest", "org.freedesktop.secrets", "--object-path", "/org/freedesktop/secrets"];
    let ready = false;
    for (let i = 0; i < 30; i++) {
      const result = spawnSync("/usr/bin/gdbus", ["call", "--session", "--dest", "org.freedesktop.DBus",
        "--object-path", "/org/freedesktop/DBus", "--method", "org.freedesktop.DBus.NameHasOwner", "org.freedesktop.secrets"], { timeout: 1000, encoding: "utf8" });
      if (result.status === 0 && result.stdout.includes("true")) { ready = true; break; }
      await delay(100);
    }
    assert.ok(ready, "Private Secret Service did not start");
    const store = new CredentialStore(root, "keyring_required", randomUUID()), ref = randomUUID();
    await store.put(ref, "Bearer SYNTHETIC_KEYRING_TEST", AbortSignal.timeout(6000));
    assert.equal(await store.get(ref, AbortSignal.timeout(6000)), "Bearer SYNTHETIC_KEYRING_TEST");
    await store.delete(ref, AbortSignal.timeout(6000));
    await assert.rejects(store.get(ref, AbortSignal.timeout(6000)));
    const stateRoot = join(root, "state"), state = new StateStore(stateRoot);
    let secrets, stagedRef, untrackedRef, controlRef, orphanRef;
    try {
      await state.call("profile.create", profileConfig({ name: "keyring-test", environment: "development",
        management: { console_endpoint: "http://127.0.0.1:9999", naming_endpoint: "http://127.0.0.1:9999",
          compatibility_id: "seata-src-41694e14" },
        target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "keyring_required" } }, true));
      const { flow } = await state.call("flow.create", { name: "keyring-test" });
      await state.call("flow.submit", { flow_id: flow.flow_id });
      const staged = await state.call("flow.stage", { flow_id: flow.flow_id, provider: "keyring" });
      stagedRef = staged.credential_ref;
      const stateId = (await state.call("info")).state_store_id;
      secrets = new CredentialStore(stateRoot, "keyring_required", stateId);
      const stagedToken = "Bearer SYNTHETIC_STAGED_KEYRING_TEST";
      await secrets.put(staged.credential_ref, stagedToken, AbortSignal.timeout(6000));
      assert.equal(await secrets.get(staged.credential_ref, AbortSignal.timeout(6000)), stagedToken);
      untrackedRef = randomUUID();
      const untrackedToken = "Bearer SYNTHETIC_UNTRACKED_KEYRING_TEST";
      await secrets.put(untrackedRef, untrackedToken, AbortSignal.timeout(6000));
      const db = new DatabaseSync(join(stateRoot, "state.db"));
      try {
        assert.equal((await state.call("flow.heartbeat", { flow_id: flow.flow_id,
          helper_instance_id: flow.helper_instance_id })).active, true);
        assert.equal((await state.call("flow.status", { name: "keyring-test", flow_id: flow.flow_id })).phase, "submitting");
        assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?")
          .get(staged.credential_ref).status, "staged");
        // The UPDATE succeeds, but this trigger creates a deferred FK violation
        // that makes the StateStore's subsequent COMMIT fail and roll back.
        db.exec("PRAGMA foreign_keys=ON");
        db.exec("CREATE TABLE test_commit_parent(id INTEGER PRIMARY KEY); " +
          "CREATE TABLE test_commit_child(credential_ref TEXT, parent_id INTEGER REFERENCES test_commit_parent(id) DEFERRABLE INITIALLY DEFERRED); " +
          "CREATE TRIGGER reject_keyring_commit AFTER UPDATE OF status ON credential_metadata WHEN NEW.status='active' " +
          "BEGIN INSERT INTO test_commit_child VALUES(NEW.credential_ref, 1); END");
        db.exec("BEGIN IMMEDIATE");
        try {
          db.prepare("UPDATE credential_metadata SET status='active' WHERE credential_ref=?").run(staged.credential_ref);
          assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?")
            .get(staged.credential_ref).status, "active");
          assert.equal(db.prepare("SELECT count(*) AS n FROM test_commit_child").get().n, 1);
          let commitError;
          try { db.exec("COMMIT"); } catch (error) { commitError = error; }
          assert.match(String(commitError?.message), /FOREIGN KEY constraint failed/);
        } finally { try { db.exec("ROLLBACK"); } catch { /* COMMIT may have closed the probe transaction. */ } }
        assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?")
          .get(staged.credential_ref).status, "staged");
        assert.equal(db.prepare("SELECT count(*) AS n FROM test_commit_child").get().n, 0);
        assert.equal((await state.call("flow.heartbeat", { flow_id: flow.flow_id,
          helper_instance_id: flow.helper_instance_id })).active, true);
        await assert.rejects(state.call("flow.activate", { flow_id: flow.flow_id,
          credential_ref: staged.credential_ref }), { type: "internal", subtype: "unexpected" });
        assert.equal((await state.call("flow.status", { name: "keyring-test", flow_id: flow.flow_id })).phase, "submitting");
        assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?")
          .get(staged.credential_ref).status, "staged");
        assert.equal(db.prepare("SELECT count(*) AS n FROM test_commit_child").get().n, 0);
      } finally {
        try { db.exec("DROP TRIGGER IF EXISTS reject_keyring_commit; DROP TABLE IF EXISTS test_commit_child; DROP TABLE IF EXISTS test_commit_parent"); }
        finally { db.close(); }
      }
      assert.equal((await state.call("auth.binding", { name: "keyring-test" })).credential, null);
      await state.call("flow.fail", { flow_id: flow.flow_id, helper_instance_id: flow.helper_instance_id,
        reason: "login_transport_failed" });
      assert.equal((await state.call("flow.status", { name: "keyring-test", flow_id: flow.flow_id })).flow_state, "failed");
      assert.ok((await state.call("credential.cleanup.list")).credentials
        .some(item => item.credential_ref === staged.credential_ref));
      assert.equal(await cleanupCredentials(state, AbortSignal.timeout(6000), stateRoot), "complete");
      await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(6000)));
      assert.equal(await secrets.get(untrackedRef, AbortSignal.timeout(6000)), untrackedToken);
      const { flow: control } = await state.call("flow.create", { name: "keyring-test" });
      await state.call("flow.submit", { flow_id: control.flow_id });
      const controlStage = await state.call("flow.stage", { flow_id: control.flow_id, provider: "keyring" });
      controlRef = controlStage.credential_ref;
      await secrets.put(controlRef, "Bearer SYNTHETIC_CONTROL_KEYRING_TEST", AbortSignal.timeout(6000));
      assert.equal((await state.call("flow.heartbeat", { flow_id: control.flow_id,
        helper_instance_id: control.helper_instance_id })).active, true);
      await state.call("flow.activate", { flow_id: control.flow_id, credential_ref: controlRef });
      assert.equal((await state.call("auth.binding", { name: "keyring-test" })).credential.credential_ref, controlRef);
      const { flow: orphan } = await state.call("flow.create", { name: "keyring-test" });
      await state.call("flow.submit", { flow_id: orphan.flow_id });
      const orphanStage = await state.call("flow.stage", { flow_id: orphan.flow_id, provider: "keyring" });
      orphanRef = orphanStage.credential_ref;
      await secrets.put(orphanRef, "Bearer SYNTHETIC_ORPHAN_KEYRING_TEST", AbortSignal.timeout(6000));
      const aged = new DatabaseSync(join(stateRoot, "state.db"));
      try {
        aged.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?")
          .run(new Date(Date.now() - 60000).toISOString(), orphan.flow_id);
      } finally { aged.close(); }
      const recovered = spawnSync(process.execPath, [fileURLToPath(new URL("../dist/cli.js", import.meta.url)),
        "auth", "status", "--profile", "keyring-test"], {
        env: { ...process.env, SEATA_AI_STATE_DIR: stateRoot }, encoding: "utf8", timeout: 15000,
      });
      assert.equal(recovered.status, 0, recovered.stderr);
      assert.equal(JSON.parse(recovered.stdout).data.authenticated, true);
      assert.deepEqual(JSON.parse(recovered.stdout).warnings, []);
      await assert.rejects(secrets.get(orphanRef, AbortSignal.timeout(6000)));
      assert.equal(await secrets.get(controlRef, AbortSignal.timeout(6000)), "Bearer SYNTHETIC_CONTROL_KEYRING_TEST");
      assert.equal(await secrets.get(untrackedRef, AbortSignal.timeout(6000)), untrackedToken);
      await state.call("auth.logout", { name: "keyring-test" });
      assert.equal(await cleanupCredentials(state, AbortSignal.timeout(6000), stateRoot), "complete");
      await assert.rejects(secrets.get(controlRef, AbortSignal.timeout(6000)));
    } finally {
      if (secrets)
        for (const ownedRef of [stagedRef, untrackedRef, controlRef, orphanRef])
          if (ownedRef) await secrets.delete(ownedRef, AbortSignal.timeout(6000)).catch(() => {});
      await state.close();
    }
    const lockedRef = randomUUID();
    await store.put(lockedRef, "Bearer SYNTHETIC_LOCKED_TEST", AbortSignal.timeout(6000));
    const alias = spawnSync("/usr/bin/gdbus", [...args, "--method", "org.freedesktop.Secret.Service.ReadAlias", "default"], { timeout: 1000, encoding: "utf8" });
    const path = /objectpath '(\/org\/freedesktop\/secrets\/collection\/[A-Za-z0-9_]+)'/.exec(alias.stdout)?.[1];
    assert.ok(path);
    const lock = spawnSync("/usr/bin/gdbus", [...args, "--method", "org.freedesktop.Secret.Service.Lock", "['" + path + "']"], { timeout: 1000, encoding: "utf8" });
    assert.equal(lock.status, 0);
    await assert.rejects(store.get(lockedRef, AbortSignal.timeout(6000)), { subtype: "credential_store_unavailable" });
    await assert.rejects(access(join(root, "credentials")));
    console.log(JSON.stringify({ result: "passed", provider: "gnome-keyring", isolated_dbus: true,
      checks: ["store", "lookup", "delete", "staged_commit_failure_cleanup", "auth_status_orphan_recovery", "locked_rejection", "no_file_fallback"] }));
  } finally {
    daemon.kill("SIGTERM");
    const timer = setTimeout(() => daemon.kill("SIGKILL"), 2000);
    await done; clearTimeout(timer);
  }
}
