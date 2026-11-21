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
import http from "node:http";
import { once } from "node:events";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { profileConfig } from "../dist/app/profile.js";
import { authStatus } from "../dist/app/auth.js";

async function activeProfile(endpoint = "http://127.0.0.1:1") {
  const root = await mkdtemp(join(tmpdir(), "seata-auth-status-race-"));
  const store = new StateStore(root);
  try {
    await store.call("profile.create", profileConfig({ name: "p", environment: "development",
      management: { console_endpoint: endpoint, naming_endpoint: endpoint,
        compatibility_id: "seata-src-41694e14" },
      target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
    const { flow } = await store.call("flow.create", { name: "p" });
    await store.call("flow.submit", { flow_id: flow.flow_id });
    const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
    const binding = await store.call("auth.binding", { name: "p" });
    await new CredentialStore(root, "protected_file", binding.state_store_id)
      .put(staged.credential_ref, "Bearer RACE_TEST", AbortSignal.timeout(5000));
    await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  } finally { await store.close(); }
  return root;
}

async function verifyIn(root, action) {
  const previous = process.env.SEATA_AI_STATE_DIR;
  process.env.SEATA_AI_STATE_DIR = root;
  try {
    return await action({ command: "auth.status", context: { profile: "p" }, input: { verify: true },
      options: { timeout_ms: 5000 } });
  } finally {
    if (previous === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = previous;
  }
}

test("logout between local status and credential binding cannot leave authenticated true", async () => {
  const root = await activeProfile(), original = StateStore.prototype.call;
  let injected = false;
  StateStore.prototype.call = async function (method, args, signal) {
    const result = await original.call(this, method, args, signal);
    if (method === "auth.status" && !injected) {
      injected = true;
      await original.call(this, "auth.logout", { name: "p" }, signal);
    }
    return result;
  };
  try {
    const result = await verifyIn(root, invocation => authStatus(invocation, AbortSignal.timeout(5000)));
    assert.equal(injected, true);
    assert.equal(result.authenticated, false);
    assert.equal(result.verification, "not_authenticated");
  } finally { StateStore.prototype.call = original; }
});

test("logout during credential loading cannot return stale authenticated status", async () => {
  const root = await activeProfile(), original = CredentialStore.prototype.get;
  let injected = false;
  CredentialStore.prototype.get = async function (...args) {
    const token = await original.apply(this, args);
    if (!injected) {
      injected = true;
      const store = new StateStore(root);
      try { await store.call("auth.logout", { name: "p" }); }
      finally { await store.close(); }
    }
    return token;
  };
  try {
    const result = await verifyIn(root, invocation => authStatus(invocation, AbortSignal.timeout(5000)));
    assert.equal(injected, true);
    assert.equal(result.authenticated, false);
    assert.equal(result.verification, "credential_changed");
  } finally { CredentialStore.prototype.get = original; }
});

test("login activation between local status and binding is reflected after a successful read", async t => {
  const requests = [];
  const server = http.createServer((req, res) => {
    requests.push({ path: req.url, authorization: req.headers.authorization });
    res.end(JSON.stringify({ code: "200", data: { public: { clusters: [], vgroups: [] } } }));
  });
  server.listen(0, "127.0.0.1"); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const root = await mkdtemp(join(tmpdir(), "seata-auth-status-login-race-"));
  const endpoint = `http://127.0.0.1:${server.address().port}`;
  const setup = new StateStore(root);
  try {
    await setup.call("profile.create", profileConfig({ name: "p", environment: "development",
      management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
      target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  } finally { await setup.close(); }
  const original = StateStore.prototype.call;
  let injected = false;
  StateStore.prototype.call = async function (method, args, signal) {
    const result = await original.call(this, method, args, signal);
    if (method === "auth.status" && !injected) {
      injected = true;
      const { flow } = await original.call(this, "flow.create", { name: "p" }, signal);
      await original.call(this, "flow.submit", { flow_id: flow.flow_id }, signal);
      const staged = await original.call(this, "flow.stage", { flow_id: flow.flow_id, provider: "protected_file" }, signal);
      const binding = await original.call(this, "auth.binding", { name: "p" }, signal);
      await new CredentialStore(root, "protected_file", binding.state_store_id)
        .put(staged.credential_ref, "Bearer LOGIN_RACE_TEST", AbortSignal.timeout(5000));
      await original.call(this, "flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref }, signal);
    }
    return result;
  };
  try {
    const result = await verifyIn(root, invocation => authStatus(invocation, AbortSignal.timeout(5000)));
    assert.equal(injected, true);
    assert.equal(result.authenticated, true);
    assert.equal(result.verification, "unavailable");
    assert.equal(result.probe_result, "read_succeeded");
    assert.deepEqual(requests, [{ path: "/api/v1/naming/namespace", authorization: "Bearer LOGIN_RACE_TEST" }]);
  } finally { StateStore.prototype.call = original; }
});

test("successful probe status stays bound to the credential generation that was read", async t => {
  const requests = [];
  const server = http.createServer((req, res) => {
    requests.push({ path: req.url, authorization: req.headers.authorization });
    res.end(JSON.stringify({ code: "200", data: { public: { clusters: [], vgroups: [] } } }));
  });
  server.listen(0, "127.0.0.1"); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const root = await activeProfile(`http://127.0.0.1:${server.address().port}`);
  const original = StateStore.prototype.call;
  let rotated = false;
  StateStore.prototype.call = async function (method, args, signal) {
    const result = await original.call(this, method, args, signal);
    if (method === "auth.binding" && requests.length === 1 && !rotated) {
      rotated = true;
      await original.call(this, "auth.logout", { name: "p" }, signal);
      const { flow } = await original.call(this, "flow.create", { name: "p" }, signal);
      await original.call(this, "flow.submit", { flow_id: flow.flow_id }, signal);
      const staged = await original.call(this, "flow.stage", { flow_id: flow.flow_id, provider: "protected_file" }, signal);
      const binding = await original.call(this, "auth.binding", { name: "p" }, signal);
      await new CredentialStore(root, "protected_file", binding.state_store_id)
        .put(staged.credential_ref, "Bearer REPLACEMENT_TEST", AbortSignal.timeout(5000));
      await original.call(this, "flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref }, signal);
    }
    return result;
  };
  try {
    const result = await verifyIn(root, invocation => authStatus(invocation, AbortSignal.timeout(5000)));
    assert.equal(rotated, true);
    assert.equal(result.authenticated, true);
    assert.equal(result.generation, 0, "Probe evidence must not be joined to a later credential generation");
    assert.equal(result.probe_result, "read_succeeded");
    assert.deepEqual(requests, [{ path: "/api/v1/naming/namespace", authorization: "Bearer RACE_TEST" }]);
  } finally { StateStore.prototype.call = original; }
  const store = new StateStore(root);
  try { assert.equal((await store.call("auth.status", { name: "p" })).generation, 1); }
  finally { await store.close(); }
});
