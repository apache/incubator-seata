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
import { mkdtempSync, readFileSync, chmodSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { execFileSync } from "node:child_process";
import https from "node:https";
import { once } from "node:events";
import { loadCa } from "../dist/adapter/transport/ca.js";
import { requestOnce, readRequest } from "../dist/adapter/transport/http.js";
import { login } from "../dist/adapter/auth/login.js";
import { resolveSetupEndpoints, profileConfig } from "../dist/app/profile.js";
import { randomBytes } from "node:crypto";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { startBridge } from "../dist/adapter/auth/bridge.js";

function certificate() {
  const root = mkdtempSync(join(tmpdir(), "seata-ca-")), cert = join(root, "ca.pem"), key = join(root, "key.pem");
  const config = join(root, "openssl.cnf");
  writeFileSync(config, "[req]\ndistinguished_name=dn\n[dn]\n", { mode: 0o600 });
  execFileSync("openssl", ["req", "-config", config, "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", key, "-out", cert,
    "-days", "1", "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost",
    "-addext", "basicConstraints=critical,CA:TRUE"], { stdio: "ignore" });
  chmodSync(cert, 0o600);
  return { root, cert, key };
}
test("custom CA reads only bounded protected certificate files and refuses links, exposed permissions and non-PEM data", () => {
  const f = certificate(), signal = AbortSignal.timeout(5000);
  assert.equal(loadCa("system", signal), undefined);
  assert.match(loadCa(f.cert, signal), /BEGIN CERTIFICATE/);
  assert.throws(() => loadCa("relative.pem", signal), e => e.subtype === "unsafe_ca_file");
  const link = join(f.root, "link.pem"); symlinkSync(f.cert, link);
  assert.throws(() => loadCa(link, signal), e => e.subtype === "unsafe_ca_file");
  chmodSync(f.cert, 0o644);
  assert.throws(() => loadCa(f.cert, signal), e => e.subtype === "unsafe_ca_file");
  chmodSync(f.cert, 0o600);
  writeFileSync(f.cert, "PRIVATE-INVALID-CONTENT");
  assert.throws(() => loadCa(f.cert, signal), e => e.subtype === "unsafe_ca_file" && !e.message.includes("PRIVATE"));
});
test("real TLS uses the explicit CA while preserving hostname verification and system trust isolation", async t => {
  const f = certificate(), other = certificate();
  const server = https.createServer({ key: readFileSync(f.key), cert: readFileSync(f.cert) }, (_req, res) => res.end('{"code":"200","data":[]}'));
  server.listen(0); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const signal = AbortSignal.timeout(10000), ca = loadCa(f.cert, signal);
  const request = { endpoint: `https://localhost:${server.address().port}`, suffix: "/api/v1/test", method: "GET" };
  await assert.rejects(requestOnce(request, signal));
  assert.equal((await requestOnce({ ...request, ca }, signal)).status, 200);
  await assert.rejects(requestOnce({ ...request, ca: loadCa(other.cert, signal) }, signal));
  await assert.rejects(requestOnce({ ...request, endpoint: `https://127.0.0.1:${server.address().port}`, ca }, signal));
});
test("management reads do not retry a TLS hostname verification failure", async t => {
  const f = certificate();
  let connections = 0;
  const server = https.createServer({ key: readFileSync(f.key), cert: readFileSync(f.cert) }, (_req, res) => res.end("{}"));
  server.on("connection", () => connections++);
  server.listen(0); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const signal = AbortSignal.timeout(5000);
  await assert.rejects(readRequest({ endpoint: `https://127.0.0.1:${server.address().port}`,
    suffix: "/api/v1/test", method: "GET", ca: loadCa(f.cert, signal) }, signal), error => error.type === "network");
  assert.equal(connections, 1);
});

test("login and single-endpoint discovery use explicit CA without password retries or probe credentials", async t => {
  const f = certificate(), requests = [];
  const server = https.createServer({ key: readFileSync(f.key), cert: readFileSync(f.cert) }, (req, res) => {
    requests.push({ path: req.url, method: req.method, authorization: req.headers.authorization });
    if (req.url.endsWith("/auth/login")) {
      res.setHeader("authorization", "Bearer TLS_TEST");
      res.end(JSON.stringify({ code: "200", data: "Bearer TLS_TEST" }));
    } else res.end(JSON.stringify({ code: "200", data: req.url.endsWith("/overview/getData") ? [] : { public: { clusters: [], vgroups: [] } } }));
  });
  server.listen(0); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `https://localhost:${server.address().port}/prefix`, signal = AbortSignal.timeout(10000);
  await assert.rejects(login(base, "synthetic", "synthetic", signal));
  assert.equal(requests.length, 0);
  assert.equal(await login(base, "synthetic", "synthetic", signal, loadCa(f.cert, signal)), "Bearer TLS_TEST");
  assert.equal(requests.length, 1); assert.equal(requests[0].method, "POST");
  const config = { name: "tls", environment: "production", management: { console_endpoint: base,
    compatibility_id: "seata-src-41694e14", ca_ref: f.cert }, target: { namespace: "public", cluster: "default" } };
  assert.equal((await resolveSetupEndpoints(config, true, signal)).management.naming_endpoint, base);
  assert.equal(requests.length, 3);
  assert.ok(requests.slice(1).every(r => r.method === "GET" && r.authorization === undefined && r.path.startsWith("/prefix/api/")));
});

test("full login bridge accepts the pinned CA and stops before password transmission if the file changes", async t => {
  for (const changed of [false, true]) {
    const f = certificate(), replacement = certificate(), requests = [];
    const server = https.createServer({ key: readFileSync(f.key), cert: readFileSync(f.cert) }, async (req, res) => {
      let body = ""; for await (const chunk of req) body += chunk;
      requests.push({ path: req.url, body: JSON.parse(body) });
      res.setHeader("authorization", "Bearer BRIDGE_CA_TEST");
      res.end(JSON.stringify({ code: "200", data: "Bearer BRIDGE_CA_TEST" }));
    });
    server.listen(0); await once(server, "listening");
    const endpoint = `https://localhost:${server.address().port}/console`, store = new StateStore(f.root);
    let bridge;
    t.after(async () => { await bridge?.close(); await store.close(); await new Promise(resolve => server.close(resolve)); });
    await store.call("profile.create", profileConfig({ name: "p", environment: "production",
      management: { console_endpoint: endpoint, naming_endpoint: endpoint, ca_ref: f.cert, compatibility_id: "seata-src-41694e14" },
      target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
    const { flow } = await store.call("flow.create", { name: "p" }), capability = randomBytes(32).toString("base64url");
    bridge = await startBridge(f.root, flow.flow_id, flow.helper_instance_id, capability);
    const url = `http://127.0.0.1:${bridge.port}/login/${capability}`;
    const page = await fetch(url), html = await page.text();
    assert.equal(page.status, 200);
    const csrf = /name="csrf" value="([A-Za-z0-9_-]+)"/.exec(html)[1];
    if (changed) writeFileSync(f.cert, readFileSync(replacement.cert));
    const response = await fetch(url + "/submit", { method: "POST", headers: {
      origin: new URL(url).origin, "content-type": "application/x-www-form-urlencoded",
      cookie: page.headers.getSetCookie()[0].split(";")[0],
    }, body: new URLSearchParams({ csrf, username: "test", password: "SYNTHETIC_CA_PASSWORD" }) });
    await response.text();
    const state = await store.call("flow.status", { name: "p", flow_id: flow.flow_id });
    assert.equal(state.flow_state, changed ? "failed" : "authenticated");
    assert.equal(requests.length, changed ? 0 : 1);
    if (!changed) assert.deepEqual(requests[0], { path: "/console/api/v1/auth/login", body: { username: "test", password: "SYNTHETIC_CA_PASSWORD" } });
    else {
      assert.equal((await store.call("auth.binding", { name: "p" })).credential, null);
      assert.match(JSON.stringify(state.reason), /ca_changed/);
    }
  }
});
