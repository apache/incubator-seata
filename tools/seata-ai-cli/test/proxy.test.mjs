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
import https from "node:https";
import net from "node:net";
import { once } from "node:events";
import { mkdtempSync, readFileSync, writeFileSync, chmodSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { execFileSync, spawn } from "node:child_process";
import { readRequest, requestOnce } from "../dist/adapter/transport/http.js";
import { CliError } from "../dist/protocol/error.js";
import { login } from "../dist/adapter/auth/login.js";
import { profileConfig, resolveSetupEndpoints } from "../dist/app/profile.js";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { startBridge } from "../dist/adapter/auth/bridge.js";
import { randomBytes } from "node:crypto";

function certificate() {
  const root = mkdtempSync(join(tmpdir(), "seata-proxy-")), key = join(root, "key.pem"), cert = join(root, "ca.pem"), config = join(root, "openssl.cnf");
  writeFileSync(config, "[req]\ndistinguished_name=dn\n[dn]\n", { mode: 0o600 });
  execFileSync("openssl", ["req", "-config", config, "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", key, "-out", cert,
    "-days", "1", "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost", "-addext", "basicConstraints=critical,CA:TRUE"], { stdio: "ignore" });
  chmodSync(cert, 0o600);
  return { root, certPath: cert, key: readFileSync(key), cert: readFileSync(cert), ca: readFileSync(cert, "utf8") };
}
async function listen(t, server, secure = false) {
  const sockets = new Set(); server.on("connection", socket => { sockets.add(socket); socket.on("close", () => sockets.delete(socket)); });
  server.listen(0); await once(server, "listening");
  t.after(() => { for (const socket of sockets) socket.destroy(); server.close(); });
  return `${secure ? "https" : "http"}://localhost:${server.address().port}`;
}
async function proxy(t, tls, mode = "forward") {
  const requests = [], connects = [], upstreams = new Set();
  let notifyConnect;
  const connected = new Promise(resolve => { notifyConnect = resolve; });
  const handler = (req, res) => {
    requests.push({ method: req.method, url: req.url, headers: req.headers });
    const target = new URL(req.url);
    const upstream = http.request(target, { method: req.method, headers: req.headers }, response => { res.writeHead(response.statusCode, response.headers); response.pipe(res); });
    upstream.on("error", () => { res.writeHead(502); res.end(); }); req.pipe(upstream);
  };
  const server = tls ? https.createServer(tls, handler) : http.createServer(handler);
  server.on("connect", (req, socket, head) => {
    connects.push({ url: req.url, headers: req.headers, head: head.length });
    notifyConnect();
    if (mode === "hang") return;
    if (mode !== "forward") { socket.end(`HTTP/1.1 ${mode} Denied\r\nLocation: http://localhost:1/\r\nContent-Length: 0\r\n\r\n`); return; }
    const target = new URL("http://" + req.url), upstream = net.connect(Number(target.port), "127.0.0.1"); upstreams.add(upstream);
    upstream.on("close", () => upstreams.delete(upstream)); upstream.on("error", () => socket.destroy()); socket.on("error", () => upstream.destroy());
    upstream.once("connect", () => { socket.write("HTTP/1.1 200 Connection Established\r\n\r\n"); if (head.length) upstream.write(head); socket.pipe(upstream); upstream.pipe(socket); });
    socket.on("close", () => upstream.destroy());
  });
  t.after(() => { for (const socket of upstreams) socket.destroy(); });
  return { endpoint: await listen(t, server, Boolean(tls)), requests, connects, connected };
}

for (const secureProxy of [false, true]) test(`HTTP forwarding through ${secureProxy ? "HTTPS" : "HTTP"} proxy preserves the target base path without direct fallback`, async t => {
  const seen = [], cert = secureProxy ? certificate() : undefined;
  const endpoint = (await listen(t, http.createServer((req, res) => { seen.push({ url: req.url, host: req.headers.host }); res.end('{"ok":true}'); }))).replace("localhost", "127.0.0.1");
  const p = await proxy(t, cert);
  const response = await requestOnce({ endpoint: endpoint + "/base", suffix: "/api/v1/test", method: "GET", query: { q: "a b" }, proxyEndpoint: p.endpoint, ca: cert?.ca }, AbortSignal.timeout(5000));
  assert.equal(response.status, 200); assert.equal(p.requests.length, 1);
  assert.equal(p.requests[0].url, endpoint + "/base/api/v1/test?q=a+b");
  assert.deepEqual(seen, [{ url: "/base/api/v1/test?q=a+b", host: new URL(endpoint).host }]);
});

for (const secureProxy of [false, true]) test(`HTTPS login uses an authenticated TLS tunnel through a ${secureProxy ? "HTTPS" : "HTTP"} proxy`, async t => {
  const cert = certificate(), seen = [];
  const endpoint = await listen(t, https.createServer(cert, async (req, res) => {
    let body = ""; for await (const chunk of req) body += chunk;
    seen.push({ method: req.method, path: req.url, body }); res.setHeader("authorization", "Bearer PROXY_TEST"); res.end('{"code":"200","data":"Bearer PROXY_TEST"}');
  }), true);
  const p = await proxy(t, secureProxy ? cert : undefined);
  assert.equal(await login(endpoint + "/base", "synthetic", "SECRET_PASSWORD", AbortSignal.timeout(5000), cert.ca, p.endpoint), "Bearer PROXY_TEST");
  assert.equal(p.connects.length, 1); assert.equal(p.requests.length, 0);
  assert.equal(p.connects[0].url, new URL(endpoint).host); assert.equal(p.connects[0].head, 0);
  assert.equal(p.connects[0].headers.authorization, undefined); assert.doesNotMatch(JSON.stringify(p.connects), /SECRET_PASSWORD/);
  assert.deepEqual(seen, [{ method: "POST", path: "/base/api/v1/auth/login", body: '{"username":"synthetic","password":"SECRET_PASSWORD"}' }]);
});

test("proxy rejection never falls back to direct login or follows proxy redirects", async t => {
  const cert = certificate(); let sent = 0;
  const endpoint = await listen(t, https.createServer(cert, (_req, res) => { sent++; res.end("{}"); }), true);
  for (const status of ["302", "407", "502"]) {
    const p = await proxy(t, undefined, status);
    await assert.rejects(login(endpoint, "synthetic", "SECRET_PASSWORD", AbortSignal.timeout(5000), cert.ca, p.endpoint), e => e.subtype === "proxy_rejected");
    assert.equal(p.connects.length, 1); assert.equal(sent, 0);
  }
});

test("proxy and origin TLS hostname checks cannot be bypassed by the tunnel", async t => {
  const cert = certificate(); let sent = 0;
  const endpoint = await listen(t, https.createServer(cert, (_req, res) => { sent++; res.end("{}"); }), true);
  const p = await proxy(t, cert), input = { endpoint, suffix: "/api/v1/test", method: "GET", ca: cert.ca, proxyEndpoint: p.endpoint };
  await assert.rejects(requestOnce({ ...input, proxyEndpoint: p.endpoint.replace("localhost", "127.0.0.1") }, AbortSignal.timeout(5000)));
  assert.equal(p.connects.length, 0);
  await assert.rejects(requestOnce({ ...input, endpoint: endpoint.replace("localhost", "127.0.0.1") }, AbortSignal.timeout(5000)));
  assert.equal(p.connects.length, 1); assert.equal(sent, 0);
});

test("proxied management reads never retry a TLS hostname verification failure", async t => {
  const cert = certificate();
  const endpoint = await listen(t, https.createServer(cert, (_req, res) => res.end("{}")), true);
  for (const mismatch of ["proxy", "origin"]) {
    const p = await proxy(t, mismatch === "proxy" ? cert : undefined);
    const request = { endpoint: mismatch === "origin" ? endpoint.replace("localhost", "127.0.0.1") : endpoint,
      suffix: "/api/v1/test", method: "GET", ca: cert.ca,
      proxyEndpoint: mismatch === "proxy" ? p.endpoint.replace("localhost", "127.0.0.1") : p.endpoint };
    await assert.rejects(readRequest(request, AbortSignal.timeout(5000)), e => e.subtype === "transport_failure");
    assert.equal(p.connects.length, mismatch === "proxy" ? 0 : 1);
  }
});

test("invocation deadline during CONNECT retains timeout classification", async t => {
  const cert = certificate(), endpoint = await listen(t, https.createServer(cert, (_req, res) => res.end("{}")), true);
  const p = await proxy(t, undefined, "hang"), controller = new AbortController();
  const pending = readRequest({ endpoint, suffix: "/api/v1/test", method: "GET", ca: cert.ca,
    proxyEndpoint: p.endpoint }, controller.signal);
  await p.connected;
  controller.abort(new CliError("network", "timeout", "Invocation deadline exceeded."));
  await assert.rejects(pending, e => e.type === "network" && e.subtype === "timeout");
  assert.equal(p.connects.length, 1);
});

test("cancelling a pending CONNECT closes it without sending the login body", async t => {
  const cert = certificate(), endpoint = await listen(t, https.createServer(cert, (_req, res) => res.end("{}")), true);
  const p = await proxy(t, undefined, "hang");
  await assert.rejects(login(endpoint, "synthetic", "SECRET_PASSWORD", AbortSignal.timeout(500), cert.ca, p.endpoint), e => e.type === "cancelled");
  assert.equal(p.connects.length, 1); assert.doesNotMatch(JSON.stringify(p.connects), /SECRET_PASSWORD/);
});

test("invalid explicit proxy URLs fail before target networking", async t => {
  let sent = 0; const endpoint = await listen(t, http.createServer((_req, res) => { sent++; res.end("{}"); }));
  for (const proxyEndpoint of ["http://user:pass@localhost", "http://localhost/path", "http://localhost/?q=1", "socks://localhost", "http://localhost/#fragment"])
    await assert.rejects(requestOnce({ endpoint, suffix: "/api/v1/test", method: "GET", proxyEndpoint }, AbortSignal.timeout(5000)), e => e.subtype === "invalid_proxy_endpoint");
  assert.equal(sent, 0);
});

for (const secureProxy of [false, true]) test(`accepted ${secureProxy ? "HTTPS" : "HTTP"} Profile proxy drives production HTTPS setup and public reads`, async t => {
  const cert = certificate(), seen = [];
  const endpoint = await listen(t, https.createServer(cert, (req, res) => {
    seen.push({ path: req.url, authorization: req.headers.authorization });
    res.end(JSON.stringify({ code: "200", data: req.url.endsWith("/overview/getData") ? [] : { public: { clusters: [], vgroups: [] } } }));
  }), true);
  const p = await proxy(t, secureProxy ? cert : undefined);
  const config = { name: "proxied", environment: "production", management: { console_endpoint: endpoint + "/base",
    compatibility_id: "seata-src-41694e14", proxy_mode: "explicit", proxy_endpoint: p.endpoint, ca_ref: cert.certPath },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } };
  const resolved = await resolveSetupEndpoints(config, true, AbortSignal.timeout(10000));
  assert.equal(resolved.management.naming_endpoint, endpoint + "/base");
  assert.equal(p.connects.length, 2);
  const state = join(cert.root, "state"), store = new StateStore(state);
  try {
    await store.call("profile.create", profileConfig(resolved, true));
    const { flow } = await store.call("flow.create", { name: "proxied" });
    await store.call("flow.submit", { flow_id: flow.flow_id });
    const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
    const binding = await store.call("auth.binding", { name: "proxied" });
    await new CredentialStore(state, "protected_file", binding.state_store_id)
      .put(staged.credential_ref, "Bearer PROXY_READ_TEST", AbortSignal.timeout(5000));
    await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  } finally { await store.close(); }
  const child = spawn(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, "topology", "namespace", "list", "--profile", "proxied"], {
    env: { ...process.env, SEATA_AI_STATE_DIR: state }, stdio: ["ignore", "pipe", "pipe"] });
  let stdout = "", stderr = ""; child.stdout.on("data", chunk => stdout += chunk); child.stderr.on("data", chunk => stderr += chunk);
  const [code] = await once(child, "close"); assert.equal(code, 0, stderr);
  assert.equal(JSON.parse(stdout).ok, true); assert.equal(p.connects.length, 3);
  assert.ok(seen.every(row => row.path.startsWith("/base/api/v1/")));
  assert.deepEqual(seen.map(row => row.authorization), [undefined, undefined, "Bearer PROXY_READ_TEST"]);
  assert.throws(() => profileConfig({ ...config, management: { ...config.management, proxy_endpoint: p.endpoint + "/path" } }, true), e => e.subtype === "invalid_proxy_endpoint");
});

test("accepting an HTTP proxy does not relax HTTPS requirements for production management targets", () => {
  const input = { name: "p", environment: "production", management: { console_endpoint: "https://console.example",
    naming_endpoint: "https://naming.example", proxy_mode: "explicit", proxy_endpoint: "http://proxy.example:3128",
    compatibility_id: "seata-src-41694e14" }, target: { namespace: "public", cluster: "default" } };
  assert.equal(profileConfig(input, true).config.management.proxy_endpoint, "http://proxy.example:3128");
  assert.equal(profileConfig(input, false).config.trust.state, "unverified");
  assert.throws(() => profileConfig({ ...input, management: { ...input.management, console_endpoint: "http://console.example" } }, true), e => e.subtype === "trust_required");
});

test("development loopback HTTP traffic cannot escape through a remote plaintext proxy", async () => {
  const config = { name: "p", environment: "development", management: { console_endpoint: "http://localhost:7091",
    proxy_mode: "explicit", proxy_endpoint: "http://proxy.example:3128", compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" } };
  assert.throws(() => profileConfig(config, true), e => e.subtype === "trust_required");
  await assert.rejects(requestOnce({ endpoint: config.management.console_endpoint, proxyEndpoint: config.management.proxy_endpoint,
    suffix: "/api/v1/test", method: "GET" }, AbortSignal.timeout(1000)), e => e.subtype === "trust_required");
  assert.equal(profileConfig({ ...config, management: { ...config.management, proxy_endpoint: "https://proxy.example" } }, true).config.management.proxy_mode, "explicit");
});

test("browser login uses the bound proxy and changing that proxy invalidates its credential", async t => {
  const cert = certificate(), requests = [];
  const endpoint = await listen(t, https.createServer(cert, async (req, res) => {
    let body = ""; for await (const chunk of req) body += chunk;
    requests.push(JSON.parse(body)); res.setHeader("authorization", "Bearer BRIDGE_PROXY"); res.end('{"code":"200","data":"Bearer BRIDGE_PROXY"}');
  }), true);
  const p = await proxy(t, cert), replacement = await proxy(t, cert), state = join(cert.root, "state"), store = new StateStore(state);
  let bridge; t.after(async () => { await bridge?.close(); await store.close(); });
  const input = { name: "p", environment: "production", management: { console_endpoint: endpoint, naming_endpoint: endpoint,
    compatibility_id: "seata-src-41694e14", proxy_mode: "explicit", proxy_endpoint: p.endpoint, ca_ref: cert.certPath },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } };
  await store.call("profile.create", profileConfig(input, true));
  const { flow } = await store.call("flow.create", { name: "p" }), capability = randomBytes(32).toString("base64url");
  bridge = await startBridge(state, flow.flow_id, flow.helper_instance_id, capability);
  const url = `http://127.0.0.1:${bridge.port}/login/${capability}`, page = await fetch(url), html = await page.text();
  const csrf = /name="csrf" value="([A-Za-z0-9_-]+)"/.exec(html)[1];
  assert.ok(html.includes(p.endpoint));
  const response = await fetch(url + "/submit", { method: "POST", headers: { origin: new URL(url).origin,
    "content-type": "application/x-www-form-urlencoded", cookie: page.headers.getSetCookie()[0].split(";")[0] },
    body: new URLSearchParams({ csrf, username: "synthetic", password: "BRIDGE_PASSWORD" }) });
  await response.text();
  assert.equal((await store.call("flow.status", { name: "p", flow_id: flow.flow_id })).flow_state, "authenticated");
  assert.equal(p.connects.length, 1); assert.equal(replacement.connects.length, 0);
  assert.deepEqual(requests, [{ username: "synthetic", password: "BRIDGE_PASSWORD" }]);
  const before = await store.call("auth.binding", { name: "p" }); assert.notEqual(before.credential, null);
  await store.call("profile.update", { ...profileConfig({ ...input, management: { ...input.management, proxy_endpoint: replacement.endpoint } }, true),
    name: "p", expected_revision: before.profile.revision });
  const after = await store.call("auth.binding", { name: "p" });
  assert.equal(after.credential, null); assert.ok(after.profile.auth_generation > before.profile.auth_generation);
});

test("direct transport ignores ambient proxy variables", async t => {
  const p = await proxy(t), endpoint = await listen(t, http.createServer((_req, res) => res.end("{}")));
  const names = ["HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"], old = new Map(names.map(name => [name, process.env[name]]));
  try {
    for (const name of names) process.env[name] = p.endpoint;
    assert.equal((await requestOnce({ endpoint, suffix: "/api/v1/test", method: "GET" }, AbortSignal.timeout(5000))).status, 200);
    assert.equal(p.requests.length, 0); assert.equal(p.connects.length, 0);
  } finally { for (const name of names) { if (old.get(name) === undefined) delete process.env[name]; else process.env[name] = old.get(name); } }
});
