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
import { randomUUID } from "node:crypto";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { recoverCredentials } from "../dist/adapter/auth/cleanup.js";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig } from "../dist/app/profile.js";
import { authStatus } from "../dist/app/auth.js";

test("slow orphan backlog yields a pending warning within a small recovery budget", async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-recovery-budget-"));
  const previousRoot = process.env.SEATA_AI_STATE_DIR;
  process.env.SEATA_AI_STATE_DIR = root;
  const originalDelete = CredentialStore.prototype.delete;
  let deletions = 0;
  CredentialStore.prototype.delete = async function (_ref, signal) {
    await delay(180);
    signal.throwIfAborted();
    deletions++;
  };
  t.after(async () => {
    CredentialStore.prototype.delete = originalDelete;
    if (previousRoot === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = previousRoot;
    await rm(root, { recursive: true, force: true });
  });
  const records = Array.from({ length: 4 }, () => ({ credential_ref: randomUUID(), flow_id: randomUUID(), provider: "protected_file" }));
  const calls = [];
  const store = { async call(op, args, signal) {
    signal?.throwIfAborted();
    calls.push({ op, args });
    if (op === "credential.cleanup.list") return { credentials: records, state_store_id: randomUUID() };
    if (op === "credential.cleanup.done") return {};
    throw new Error("unexpected store operation");
  } };
  const warnings = [];
  const started = performance.now();
  await recoverCredentials(store, AbortSignal.timeout(2000), warning => warnings.push(warning), 1000);
  const elapsed = performance.now() - started;
  assert.ok(elapsed < 500, `cleanup occupied ${elapsed} ms`);
  assert.ok(deletions < records.length);
  assert.deepEqual(warnings.map(w => w.code), ["credential_cleanup_pending"]);
  assert.deepEqual(calls[0], { op: "credential.cleanup.list", args: { reap_flows: true } });
});

test("auth status returns its local result while slow owned credentials remain queued", async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-auth-status-budget-"));
  const previousRoot = process.env.SEATA_AI_STATE_DIR;
  process.env.SEATA_AI_STATE_DIR = root;
  const store = new StateStore(root);
  const originalDelete = CredentialStore.prototype.delete;
  t.after(async () => {
    CredentialStore.prototype.delete = originalDelete;
    await store.close();
    if (previousRoot === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = previousRoot;
    await rm(root, { recursive: true, force: true });
  });
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: "http://127.0.0.1:9999", naming_endpoint: "http://127.0.0.1:9999",
      compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const stateId = (await store.call("info")).state_store_id;
  const secrets = new CredentialStore(root, "protected_file", stateId);
  for (let i = 0; i < 4; i++) {
    const { flow } = await store.call("flow.create", { name: "p" });
    await store.call("flow.submit", { flow_id: flow.flow_id });
    const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
    await secrets.put(staged.credential_ref, "Bearer SYNTHETIC_PENDING", AbortSignal.timeout(5000));
    await store.call("flow.fail", { flow_id: flow.flow_id, helper_instance_id: flow.helper_instance_id,
      reason: "helper_unavailable" });
  }
  CredentialStore.prototype.delete = async function (ref, signal) {
    await delay(250);
    signal.throwIfAborted();
    return originalDelete.call(this, ref, signal);
  };
  const warnings = [];
  const status = await authStatus({ context: { profile: "p" }, options: { timeout_ms: 1500 }, input: { verify: false } },
    AbortSignal.timeout(1500), warning => warnings.push(warning));
  assert.equal(status.authenticated, false);
  assert.deepEqual(warnings.map(w => w.code), ["credential_cleanup_pending"]);
  assert.ok((await store.call("credential.cleanup.list")).credentials.length > 0);
});
