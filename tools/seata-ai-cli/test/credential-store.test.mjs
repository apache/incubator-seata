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
import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, stat, chmod, symlink, readFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomUUID } from "node:crypto";
import { CredentialStore, bearer } from "../dist/adapter/auth/credentials.js";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig } from "../dist/app/profile.js";
import { invocationEnvelope } from "../dist/output/emitter.js";
import { validate } from "../dist/command/registry.js";
import { cleanupCredentials } from "../dist/adapter/auth/cleanup.js";
import { PrivateDirectory } from "../dist/platform/linux.js";
import { closeSync } from "node:fs";

test("explicit protected files are exclusive, private, isolated by state identity and deletable", async () => {
  const root = await mkdtemp(join(tmpdir(), "seata-credentials-")), state = randomUUID(), ref = randomUUID();
  const store = new CredentialStore(root, "protected_file", state), signal = AbortSignal.timeout(5000);
  const token = "Bearer TEST.secret.VALUE";
  await store.put(ref, token, signal);
  assert.equal(store.assurance, "local_user_file");
  assert.equal(await store.get(ref, signal), token);
  assert.equal((await stat(join(root, "credentials", state, ref))).mode & 0o777, 0o600);
  await assert.rejects(store.put(ref, "Bearer replacement", signal));
  assert.equal(await store.get(ref, signal), token);
  await assert.rejects(new CredentialStore(root, "protected_file", randomUUID()).get(ref, signal));
  await chmod(join(root, "credentials", state, ref), 0o644);
  await assert.rejects(store.get(ref, signal));
  await chmod(join(root, "credentials", state, ref), 0o600);
  const linkRef = randomUUID();
  await symlink(join(root, "credentials", state, ref), join(root, "credentials", state, linkRef));
  await assert.rejects(store.get(linkRef, signal));
  await assert.rejects(store.delete(linkRef, signal));
  assert.equal(await store.get(ref, signal), token);
  await store.delete(ref, signal); await store.delete(ref, signal);
  await assert.rejects(store.get(ref, signal));
  assert.throws(() => bearer("Bearer abc\r\nInjected: value"));
  assert.throws(() => new CredentialStore(root, "automatic", state));
});

test("auth flow status exposes a safe continuation and terminal history without credential references", async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-flow-status-")), store = new StateStore(root);
  t.after(() => store.close());
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: "http://127.0.0.1:9999", naming_endpoint: "http://127.0.0.1:9999", compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  const args = { name: "p", flow_id: flow.flow_id };
  const pending = await store.call("flow.status", args);
  assert.equal(pending.flow_state, "pending");
  assert.equal(pending.helper_instance_id, undefined);
  const invocation = validate({ protocol_version: "0.1", contract_revision: "R9", command: "auth.login.status",
    context: { profile: "p" }, input: { flow_id: flow.flow_id }, options: {} });
  const result = invocationEnvelope(invocation, performance.now(), pending);
  assert.equal(result.envelope.state, "pending");
  assert.equal(validate(result.envelope.continuation.invocation).command, "auth.login.status");
  await store.call("flow.submit", { flow_id: flow.flow_id });
  assert.equal((await store.call("flow.status", args)).phase, "submitting");
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  assert.equal(binding.credential, null);
  const secret = new CredentialStore(root, "protected_file", binding.state_store_id);
  await secret.put(staged.credential_ref, "Bearer NEVER_SQLITE", AbortSignal.timeout(5000));
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  const terminal = await store.call("flow.status", args);
  assert.equal(terminal.flow_state, "authenticated");
  assert.equal(invocationEnvelope(invocation, performance.now(), terminal).envelope.state, "completed");
  await store.call("auth.logout", { name: "p" });
  assert.equal((await store.call("auth.binding", { name: "p" })).credential, null);
  assert.equal((await store.call("flow.status", args)).flow_state, "authenticated");
  for (const file of ["state.db", "state.db-wal"])
    assert.equal((await readFile(join(root, file))).includes(Buffer.from("NEVER_SQLITE")), false);
});

test("cleanup retains ownership metadata until a cancelled helper's late secret write has stopped", async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-secret-cleanup-race-")), store = new StateStore(root);
  t.after(() => store.close());
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: "http://127.0.0.1:9999", naming_endpoint: "http://127.0.0.1:9999", compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  const secrets = new CredentialStore(root, "protected_file", binding.state_store_id);
  const directory = new PrivateDirectory(root), owner = directory.lock("auth-flow-" + flow.flow_id + ".lock");
  try {
    await store.call("flow.cancel", { name: "p", flow_id: flow.flow_id });
    assert.equal(await cleanupCredentials(store, AbortSignal.timeout(5000), root), "pending");
    assert.equal((await store.call("credential.cleanup.list")).credentials.length, 1);
    // A previously issued provider write completes after cancellation.
    await secrets.put(staged.credential_ref, "Bearer LATE_SYNTHETIC", AbortSignal.timeout(5000));
    await assert.rejects(store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref }));
  } finally { closeSync(owner); directory.close(); }
  assert.equal(await cleanupCredentials(store, AbortSignal.timeout(5000), root), "complete");
  assert.equal((await store.call("credential.cleanup.list")).credentials.length, 0);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});
