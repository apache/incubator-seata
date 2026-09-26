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
import { randomBytes } from "node:crypto";
import { mkdtemp, readFile, readdir } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { spawn } from "node:child_process";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig } from "../dist/app/profile.js";
import { startBridge } from "../dist/adapter/auth/bridge.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { login } from "../dist/adapter/auth/login.js";
import { DatabaseSync } from "node:sqlite";
import { PrivateDirectory } from "../dist/platform/linux.js";
import { closeSync } from "node:fs";

const token = "Bearer SYNTHETIC_CONSOLE_CREDENTIAL";
const password = "SYNTHETIC_PASSWORD";
async function request(url, method = "GET", headers = {}, body) {
  return await new Promise((resolve, reject) => {
    const req = http.request(url, { method, headers, agent: false }, res => {
      let text = ""; res.on("data", c => text += c); res.on("end", () => resolve({ status: res.statusCode, headers: res.headers, text }));
    });
    req.on("error", reject); req.setTimeout(10000, () => req.destroy(new Error("test timeout"))); req.end(body);
  });
}
async function fixture(t, respond, bridge = true) {
  const received = [];
  const console = http.createServer(async (req, res) => {
    let body = ""; for await (const c of req) body += c;
    received.push({ path: req.url, method: req.method, body: JSON.parse(body) });
    if (respond) await respond(req, res);
    else { res.writeHead(200, { authorization: token }); res.end(JSON.stringify({ code: "200", data: token })); }
  });
  console.listen(0, "127.0.0.1"); await once(console, "listening");
  const endpoint = `http://127.0.0.1:${console.address().port}/trusted-prefix`;
  const root = await mkdtemp(join(tmpdir(), "seata-auth-bridge-")), store = new StateStore(root);
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  let local, flow, url;
  if (bridge) {
    ({ flow } = await store.call("flow.create", { name: "p" }));
    const capability = randomBytes(32).toString("base64url");
    local = await startBridge(root, flow.flow_id, flow.helper_instance_id, capability);
    url = `http://127.0.0.1:${local.port}/login/${capability}`;
  }
  t.after(async () => { await local?.close(); await store.close(); await new Promise(resolve => console.close(resolve)); });
  return { root, store, flow, url, endpoint, received };
}
function form(page, url, overrides = {}) {
  const csrf = /name="csrf" value="([A-Za-z0-9_-]+)"/.exec(page.text)[1];
  return { url: url + "/submit", headers: { origin: new URL(url).origin, "content-type": "application/x-www-form-urlencoded",
    cookie: page.headers["set-cookie"][0].split(";")[0], "sec-fetch-site": "same-origin", ...overrides },
    body: new URLSearchParams({ csrf, username: "test", password }).toString() };
}
test("GET is non-consuming; CSRF/Origin/Host gates precede a single accepted password POST", async t => {
  const f = await fixture(t), page = await request(f.url), post = form(page, f.url);
  assert.equal(page.status, 200); assert.equal(f.received.length, 0);
  assert.equal(page.headers["cache-control"], "no-store");
  assert.equal(page.headers["referrer-policy"], "strict-origin");
  assert.match(page.headers["content-security-policy"], /frame-ancestors 'none'/);
  assert.match(page.headers["set-cookie"][0], /HttpOnly; SameSite=Strict; Path=\/login\//);
  for (const headers of [{ origin: "http://evil.example" }, { host: "evil.example" }, { "sec-fetch-site": "cross-site" }, { cookie: "seata_login=wrong" }])
    assert.equal((await request(post.url, "POST", { ...post.headers, ...headers }, post.body)).status, 403);
  const missingOrigin = { ...post.headers }; delete missingOrigin.origin;
  assert.equal((await request(post.url, "POST", missingOrigin, post.body)).status, 403);
  assert.equal((await request(post.url, "POST", post.headers, post.body.replace(/csrf=[^&]+/, "csrf=wrong"))).status, 403);
  assert.equal(f.received.length, 0);
  const responses = await Promise.allSettled([request(post.url, "POST", post.headers, post.body), request(post.url, "POST", post.headers, post.body)]);
  assert.ok(responses.some(r => r.status === "fulfilled" && r.value.status === 200));
  assert.equal(f.received.length, 1); assert.equal(f.received[0].method, "POST");
  assert.equal(f.received[0].path, "/trusted-prefix/api/v1/auth/login");
  assert.deepEqual(f.received[0].body, { username: "test", password });
  const status = await f.store.call("flow.status", { name: "p", flow_id: f.flow.flow_id });
  assert.equal(status.flow_state, "authenticated");
  const binding = await f.store.call("auth.binding", { name: "p" });
  assert.equal(await new CredentialStore(f.root, "protected_file", binding.state_store_id).get(binding.credential.credential_ref, AbortSignal.timeout(1000)), token);
  for (const name of ["state.db", "state.db-wal"]) {
    const bytes = await readFile(join(f.root, name));
    assert.equal(bytes.includes(Buffer.from(token)), false); assert.equal(bytes.includes(Buffer.from(password)), false);
    assert.equal(bytes.includes(Buffer.from(new URL(f.url).pathname)), false);
  }
});

test("cancel before form submission prevents Console login", async t => {
  const f = await fixture(t), page = await request(f.url), post = form(page, f.url);
  await f.store.call("flow.cancel", { name: "p", flow_id: f.flow.flow_id });
  assert.equal((await request(post.url, "POST", post.headers, post.body)).status, 410);
  assert.equal(f.received.length, 0);
});

test("public cancel removes an orphaned staged secret after its helper has gone", async t => {
  const f = await fixture(t, undefined, false);
  const { flow } = await f.store.call("flow.create", { name: "p" });
  await f.store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await f.store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const stateId = (await f.store.call("info")).state_store_id;
  const secrets = new CredentialStore(f.root, "protected_file", stateId);
  await secrets.put(staged.credential_ref, token, AbortSignal.timeout(5000));
  const cancelled = await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", flow.flow_id]);
  assert.equal(cancelled.code, 0, cancelled.stderr);
  assert.equal(JSON.parse(cancelled.stdout).data.flow_state, "cancelled");
  assert.equal((await f.store.call("auth.binding", { name: "p" })).credential, null);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
  assert.deepEqual((await f.store.call("credential.cleanup.list")).credentials, []);
});

test("public cancel warns when staged-secret cleanup is still pending", async t => {
  const f = await fixture(t, undefined, false);
  const { flow } = await f.store.call("flow.create", { name: "p" });
  await f.store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await f.store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const stateId = (await f.store.call("info")).state_store_id;
  const secrets = new CredentialStore(f.root, "protected_file", stateId);
  await secrets.put(staged.credential_ref, token, AbortSignal.timeout(5000));
  const directory = new PrivateDirectory(f.root);
  const held = directory.lock("auth-flow-" + flow.flow_id + ".lock");
  try {
    const cancelled = await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", flow.flow_id]);
    assert.equal(cancelled.code, 0, cancelled.stderr);
    const envelope = JSON.parse(cancelled.stdout);
    assert.deepEqual(Object.keys(envelope.data).sort(), ["changed", "flow_id", "flow_state"]);
    assert.equal(envelope.data.flow_state, "cancelled");
    assert.equal(envelope.data.changed, true);
    assert.equal(envelope.warnings.length, 1);
    assert.equal(envelope.warnings[0].code, "credential_cleanup_pending");
    assert.equal(await secrets.get(staged.credential_ref, AbortSignal.timeout(5000)), token);
  } finally { closeSync(held); directory.close(); }
  const retried = await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", flow.flow_id]);
  assert.equal(retried.code, 0, retried.stderr);
  assert.equal(JSON.parse(retried.stdout).data.changed, false);
  assert.deepEqual(JSON.parse(retried.stdout).warnings, []);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});

async function strandedCredential(f) {
  const { flow } = await f.store.call("flow.create", { name: "p" });
  await f.store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await f.store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const stateId = (await f.store.call("info")).state_store_id;
  const secrets = new CredentialStore(f.root, "protected_file", stateId);
  await secrets.put(staged.credential_ref, token, AbortSignal.timeout(5000));
  const db = new DatabaseSync(join(f.root, "state.db"));
  try {
    db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?")
      .run(new Date(Date.now() - 60000).toISOString(), flow.flow_id);
  } finally { db.close(); }
  return { flow, staged, secrets };
}

test("login status cleans a helper's stranded credential; local profile reads do not", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const local = await cli(f.root, ["profile", "list"]);
  assert.equal(local.code, 0, local.stderr);
  assert.deepEqual((await f.store.call("credential.cleanup.list")).credentials, []);
  assert.equal(await secrets.get(staged.credential_ref, AbortSignal.timeout(5000)), token);
  const status = await cli(f.root, ["auth", "login", "status", "--profile", "p", "--flow-id", flow.flow_id]);
  assert.equal(status.code, 0, status.stderr);
  assert.equal(JSON.parse(status.stdout).data.flow_state, "failed");
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
  assert.deepEqual((await f.store.call("credential.cleanup.list")).credentials, []);
});

test("login status still cleans a stranded credential when flow history is no longer queryable", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const db = new DatabaseSync(join(f.root, "state.db"));
  try {
    db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?")
      .run(new Date(Date.now() - 25 * 60 * 60 * 1000).toISOString(), flow.flow_id);
  } finally { db.close(); }
  const status = await cli(f.root, ["auth", "login", "status", "--profile", "p", "--flow-id", flow.flow_id]);
  assert.notEqual(status.code, 0);
  assert.equal(JSON.parse(status.stderr).error.subtype, "auth_flow_not_found");
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
  assert.deepEqual((await f.store.call("credential.cleanup.list")).credentials, []);
});

test("expired-history status preserves a pending cleanup warning on its error envelope", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const db = new DatabaseSync(join(f.root, "state.db"));
  try {
    db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?")
      .run(new Date(Date.now() - 25 * 60 * 60 * 1000).toISOString(), flow.flow_id);
  } finally { db.close(); }
  const directory = new PrivateDirectory(f.root);
  const held = directory.lock("auth-flow-" + flow.flow_id + ".lock");
  try {
    const status = await cli(f.root, ["auth", "login", "status", "--profile", "p", "--flow-id", flow.flow_id]);
    assert.notEqual(status.code, 0);
    const envelope = JSON.parse(status.stderr);
    assert.equal(envelope.error.subtype, "auth_flow_not_found");
    assert.deepEqual(envelope.warnings.map(w => w.code), ["credential_cleanup_pending"]);
    assert.equal(await secrets.get(staged.credential_ref, AbortSignal.timeout(5000)), token);
  } finally { closeSync(held); directory.close(); }
  const retried = await cli(f.root, ["auth", "status", "--profile", "p"]);
  assert.equal(retried.code, 0, retried.stderr);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});

test("cancel still cleans a stranded credential when flow history has expired", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const db = new DatabaseSync(join(f.root, "state.db"));
  try {
    db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?")
      .run(new Date(Date.now() - 25 * 60 * 60 * 1000).toISOString(), flow.flow_id);
  } finally { db.close(); }
  const cancelled = await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", flow.flow_id]);
  assert.notEqual(cancelled.code, 0);
  assert.equal(JSON.parse(cancelled.stderr).error.subtype, "auth_flow_not_found");
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
  assert.deepEqual((await f.store.call("credential.cleanup.list")).credentials, []);
});

test("auth status recovers a stranded credential without changing authentication", async t => {
  const f = await fixture(t, undefined, false);
  const { staged, secrets } = await strandedCredential(f);
  const status = await cli(f.root, ["auth", "status", "--profile", "p"]);
  assert.equal(status.code, 0, status.stderr);
  assert.equal(JSON.parse(status.stdout).data.authenticated, false);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});

test("new auth login recovers a prior helper's stranded credential", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const result = await cli(f.root, ["auth", "login", "--profile", "p"]);
  assert.equal(result.code, 0, result.stderr);
  const pending = JSON.parse(result.stdout);
  assert.equal(pending.state, "pending");
  assert.notEqual(pending.data.flow_id, flow.flow_id);
  t.after(async () => { await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", pending.data.flow_id]); });
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});

test("auth status reports delayed stranded-credential cleanup in generic warnings", async t => {
  const f = await fixture(t, undefined, false);
  const { flow, staged, secrets } = await strandedCredential(f);
  const directory = new PrivateDirectory(f.root);
  const held = directory.lock("auth-flow-" + flow.flow_id + ".lock");
  try {
    const result = await cli(f.root, ["auth", "status", "--profile", "p"]);
    assert.equal(result.code, 0, result.stderr);
    const envelope = JSON.parse(result.stdout);
    assert.equal(envelope.data.authenticated, false);
    assert.deepEqual(envelope.warnings.map(w => w.code), ["credential_cleanup_pending"]);
    assert.equal(await secrets.get(staged.credential_ref, AbortSignal.timeout(5000)), token);
  } finally { closeSync(held); directory.close(); }
  const retried = await cli(f.root, ["auth", "status", "--profile", "p"]);
  assert.equal(retried.code, 0, retried.stderr);
  assert.deepEqual(JSON.parse(retried.stdout).warnings, []);
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
});

test("logout while the password response is in flight fences late activation", async t => {
  let release, arrived;
  const arrival = new Promise(resolve => arrived = resolve), gate = new Promise(resolve => release = resolve);
  const f = await fixture(t, async (_req, res) => {
    arrived(); await gate; res.writeHead(200, { authorization: token }); res.end(JSON.stringify({ code: "200", data: token }));
  });
  const page = await request(f.url), post = form(page, f.url);
  const response = request(post.url, "POST", post.headers, post.body).catch(() => null);
  await arrival;
  await f.store.call("auth.logout", { name: "p" }); release(); await response;
  assert.equal((await f.store.call("auth.binding", { name: "p" })).credential, null);
  assert.equal((await f.store.call("flow.status", { name: "p", flow_id: f.flow.flow_id })).flow_state, "cancelled");
  assert.equal(f.received.length, 1);
});

test("business failure terminates the flow without upstream body leakage or retry", async t => {
  const f = await fixture(t, (_req, res) => res.end(JSON.stringify({ code: "401", message: password })));
  const page = await request(f.url), post = form(page, f.url);
  const response = await request(post.url, "POST", post.headers, post.body);
  assert.equal(response.status, 400); assert.doesNotMatch(response.text, /SYNTHETIC/);
  const status = await f.store.call("flow.status", { name: "p", flow_id: f.flow.flow_id });
  assert.equal(status.flow_state, "failed"); assert.equal(status.reason.subtype, "login_rejected");
  assert.equal(f.received.length, 1);
});

test("activation database failure leaves no active binding or staged secret", async t => {
  const f = await fixture(t);
  const db = new DatabaseSync(join(f.root, "state.db"));
  t.after(() => db.close());
  const put = CredentialStore.prototype.put;
  let secretWasWritten = false;
  CredentialStore.prototype.put = async function (ref, value, signal) {
    await put.call(this, ref, value, signal);
    secretWasWritten = true;
  };
  t.after(() => { CredentialStore.prototype.put = put; });
  // The live Bridge has already opened its StateStore. Reject only the
  // activation update, after the protected credential file has been written.
  db.exec("CREATE TRIGGER reject_test_activation BEFORE UPDATE OF status ON credential_metadata WHEN NEW.status='active' BEGIN SELECT RAISE(ABORT, 'injected activation failure'); END");
  const page = await request(f.url), post = form(page, f.url);
  const response = await request(post.url, "POST", post.headers, post.body);
  assert.equal(response.status, 400);
  assert.equal(secretWasWritten, true);
  assert.equal(f.received.length, 1);
  const status = await f.store.call("flow.status", { name: "p", flow_id: f.flow.flow_id });
  assert.equal(status.flow_state, "failed");
  assert.equal((await f.store.call("auth.binding", { name: "p" })).credential, null);
  const stateId = (await f.store.call("info")).state_store_id;
  const deadline = Date.now() + 5000;
  while (db.prepare("SELECT count(*) AS n FROM credential_metadata WHERE flow_id=?").get(f.flow.flow_id).n !== 0 && Date.now() < deadline)
    await new Promise(resolve => setTimeout(resolve, 50));
  assert.equal(db.prepare("SELECT count(*) AS n FROM credential_metadata WHERE flow_id=?").get(f.flow.flow_id).n, 0);
  assert.deepEqual(await readdir(join(f.root, "credentials", stateId)), []);
});

test("Console header/data mismatch and redirects cannot create a credential or replay password", async t => {
  for (const kind of ["mismatch", "redirect"]) {
    const f = await fixture(t, (_req, res) => {
      if (kind === "redirect") { res.writeHead(307, { location: "/another-login" }); res.end(); }
      else { res.writeHead(200, { authorization: "Bearer OTHER" }); res.end(JSON.stringify({ code: "200", data: token })); }
    }, false);
    await assert.rejects(login(f.endpoint, "test", password, AbortSignal.timeout(2000)));
    assert.equal(f.received.length, 1);
    assert.equal((await f.store.call("auth.binding", { name: "p" })).credential, null);
  }
});

async function cli(root, args, env = {}) {
  const child = spawn(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, ...args], {
    env: { ...process.env, SEATA_AI_STATE_DIR: root, SEATA_AI_LOCAL_AUTH: "shared_loopback", ...env }, stdio: ["ignore", "pipe", "pipe"],
  });
  let stdout = "", stderr = "";
  child.stdout.on("data", chunk => stdout += chunk); child.stderr.on("data", chunk => stderr += chunk);
  const [code] = await once(child, "close"); return { code, stdout, stderr };
}
test("public CLI exits after pending and its private helper survives, authenticates and exposes no secret", async t => {
  const f = await fixture(t, undefined, false);
  const first = await cli(f.root, ["auth", "login", "--profile", "p"]);
  assert.equal(first.code, 0, first.stderr); assert.equal(first.stderr, "");
  const pending = JSON.parse(first.stdout); assert.equal(pending.state, "pending");
  const flowId = pending.data.flow_id;
  t.after(async () => { await cli(f.root, ["auth", "login", "cancel", "--profile", "p", "--flow-id", flowId]); });
  const second = await cli(f.root, ["auth", "login", "--profile", "p"]);
  assert.equal(JSON.parse(second.stdout).data.flow_id, flowId);
  assert.equal(JSON.parse(second.stdout).data.verification_url, undefined);
  const url = pending.data.verification_url, page = await request(url), post = form(page, url);
  assert.equal((await request(post.url, "POST", post.headers, post.body)).status, 200);
  const completed = await cli(f.root, ["auth", "login", "status", "--profile", "p", "--flow-id", flowId]);
  assert.equal(completed.code, 0, completed.stderr);
  assert.equal(JSON.parse(completed.stdout).data.flow_state, "authenticated");
  assert.doesNotMatch(completed.stdout, /SYNTHETIC|credential_ref|verification_url/);
  assert.equal(f.received.length, 1);
});

test("headless hosts fail explicitly instead of advertising an inaccessible login URL", async t => {
  const f = await fixture(t, undefined, false);
  const result = await cli(f.root, ["auth", "login", "--profile", "p"], {
    SEATA_AI_LOCAL_AUTH: "", DISPLAY: "", WAYLAND_DISPLAY: "", SSH_CONNECTION: "synthetic-remote-session",
  });
  assert.equal(result.code, 3); assert.equal(result.stdout, "");
  assert.equal(JSON.parse(result.stderr).error.subtype, "local_auth_unavailable");
  assert.equal(f.received.length, 0);
});

test("login pages and request bodies are bounded without consuming the upstream login", async t => {
  const f = await fixture(t), page = await request(f.url), post = form(page, f.url);
  for (let i = 0; i < 3; i++) assert.equal((await request(f.url)).status, 200);
  assert.equal((await request(f.url)).status, 429);
  const oversized = await request(post.url, "POST", post.headers, post.body + "&padding=" + "x".repeat(20000)).catch(() => ({ status: 413 }));
  assert.equal(oversized.status, 413);
  assert.equal(f.received.length, 0);
  assert.equal((await f.store.call("flow.status", { name: "p", flow_id: f.flow.flow_id })).flow_state, "pending");
});
