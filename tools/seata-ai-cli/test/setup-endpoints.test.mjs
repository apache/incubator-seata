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
import { spawn } from "node:child_process";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { resolveSetupEndpoints } from "../dist/app/profile.js";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { validateOutput } from "../dist/command/registry.js";

async function fixture(t) {
  const requests = [], mode = { failure: false, malformed: false, redirect: false };
  const app = http.createServer((req, res) => {
    requests.push({ path: req.url, method: req.method, authorization: req.headers.authorization });
    if (mode.redirect) { res.writeHead(302, { location: "/elsewhere" }); res.end("{}"); return; }
    if (mode.failure) { res.writeHead(401); res.end("PRIVATE_ERROR"); return; }
    const body = mode.malformed ? { code: "200", data: "not a namespace map" } :
      req.url.endsWith("/overview/getData") ? { code: "200", data: [] } :
      { code: "200", data: { public: { clusters: ["default"], vgroups: [] } } };
    res.end(JSON.stringify(body));
  });
  app.listen(0, "127.0.0.1"); await once(app, "listening");
  t.after(() => new Promise(resolve => app.close(resolve)));
  const config = { name: "setup-test", environment: "development",
    management: { console_endpoint: `http://127.0.0.1:${app.address().port}/accepted/prefix`, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" } };
  return { requests, mode, config };
}
test("single endpoint setup probes only accepted anonymous pinned GETs and preserves the base path", async t => {
  const f = await fixture(t);
  const result = await resolveSetupEndpoints(f.config, true, AbortSignal.timeout(5000));
  assert.equal(result.management.naming_endpoint, f.config.management.console_endpoint);
  assert.equal(result.management.deployment, undefined, "Interface availability does not prove deployment topology");
  assert.deepEqual(f.requests.map(r => r.path), ["/accepted/prefix/api/v1/overview/getData", "/accepted/prefix/api/v1/naming/namespace"]);
  assert.ok(f.requests.every(r => r.method === "GET" && r.authorization === undefined));
});
test("unaccepted, unsupported and standalone targets are refused before networking", async t => {
  const f = await fixture(t);
  for (const [config, accepted] of [
    [f.config, false],
    [{ ...f.config, management: { ...f.config.management, compatibility_id: "unknown" } }, true],
    [{ ...f.config, management: { ...f.config.management, deployment: "standalone_console" } }, true],
    [{ ...f.config, management: { ...f.config.management, ca_ref: "/custom-ca" } }, true],
  ]) await assert.rejects(resolveSetupEndpoints(config, accepted, AbortSignal.timeout(5000)));
  assert.equal(f.requests.length, 0);
  const explicit = { ...f.config, management: { ...f.config.management, naming_endpoint: f.config.management.console_endpoint + "/explicit" } };
  assert.equal((await resolveSetupEndpoints(explicit, true, AbortSignal.timeout(5000))).management.naming_endpoint, explicit.management.naming_endpoint);
  assert.equal(f.requests.length, 0);
});
test("unauthenticated denial, wrong DTO and redirects never establish a second endpoint", async t => {
  const f = await fixture(t);
  for (const key of ["failure", "malformed", "redirect"]) {
    f.mode[key] = true;
    await assert.rejects(resolveSetupEndpoints(f.config, true, AbortSignal.timeout(5000)), error => {
      assert.equal(error.subtype, "management_endpoint_missing");
      assert.doesNotMatch(error.message, /PRIVATE_ERROR/); return true;
    });
    f.mode[key] = false;
  }
  assert.ok(f.requests.every(r => r.path.startsWith("/accepted/prefix/api/")));
});
test("public setup stores the resolved endpoint under the complete accepted target digest", async t => {
  const f = await fixture(t), directory = await mkdtemp(join(tmpdir(), "seata-setup-probe-"));
  const child = spawn(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, "invoke"], {
    env: { ...process.env, SEATA_AI_STATE_DIR: directory }, stdio: ["pipe", "pipe", "pipe"],
  });
  let stdout = "", stderr = "";
  child.stdout.on("data", b => stdout += b); child.stderr.on("data", b => stderr += b);
  child.stdin.end(JSON.stringify({ protocol_version: "0.1", contract_revision: "R9", command: "setup.initialize",
    input: { config: f.config, accept_target: true }, context: {}, options: {} }));
  const [code] = await once(child, "close");
  assert.equal(code, 0, stderr); assert.equal(stderr, "");
  const result = JSON.parse(stdout).data; validateOutput("setup.initialize", result);
  const store = new StateStore(directory);
  try {
    const binding = await store.call("auth.binding", { name: f.config.name });
    assert.equal(binding.profile.management.naming_endpoint, f.config.management.console_endpoint);
    assert.equal(binding.profile.trust.accepted_target_digest, binding.target_digest);
    assert.equal(binding.credential, null);
  } finally { await store.close(); }
});
