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
import { mkdtempSync, mkdirSync, writeFileSync, existsSync, symlinkSync, readdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { createServer } from "node:http";
import { validateOutput } from "../dist/command/registry.js";
import { ConfigDirectory } from "../dist/adapter/config/files.js";

const cli = new URL("../dist/cli.js", import.meta.url).pathname;
function fixture() {
  const base = mkdtempSync(join(tmpdir(), "seata-local-inspect-")), root = join(base, "parent", "project"), state = join(base, "state");
  mkdirSync(root, { recursive: true });
  const file = (path, content) => { writeFileSync(path, content); return path; };
  const run = async (command, input, env = {}, typed) => {
    const child = spawn(process.execPath, [cli, ...(typed ?? ["invoke"])], {
      cwd: root, env: { ...process.env, SEATA_AI_STATE_DIR: state, ...env }, stdio: ["pipe", "pipe", "pipe"],
    });
    let stdout = "", stderr = "";
    child.stdout.on("data", b => stdout += b); child.stderr.on("data", b => stderr += b);
    child.stdin.end(typed ? undefined : JSON.stringify({ protocol_version: "0.1", contract_revision: "R9", command, input, context: {}, options: { timeout_ms: 10000 } }));
    const [code] = await once(child, "close");
    const envelope = JSON.parse(stdout || stderr);
    if (code === 0 || code === 9) validateOutput(command, envelope.data);
    assert.equal(existsSync(state), false, "inspection must not create StateStore or credentials");
    return { code, stdout, stderr, envelope, data: envelope.data };
  };
  return { base, root, file, run };
}

test("setup candidates retain unverified source provenance and never contact discovered URLs", async t => {
  let requests = 0;
  const server = createServer((_req, res) => { requests++; res.end("{}"); });
  server.listen(0, "127.0.0.1"); await once(server, "listening");
  t.after(() => new Promise(resolve => server.close(resolve)));
  const f = fixture(), endpoint = `http://127.0.0.1:${server.address().port}/base`;
  f.file(join(f.root, "application.yml"), `management:\n  console_endpoint: ${endpoint}\n  naming_endpoint: https://naming.example/base\npassword: SECRET\n`);
  const result = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 }, { SEATA_AI_NAMING_ENDPOINT: "https://env.example/naming" });
  assert.equal(result.code, 0, result.stderr); assert.equal(requests, 0);
  assert.equal(result.data.candidates.length, 3);
  assert.ok(result.data.candidates.every(c => c.trust_state === "unverified" && c.confidence === "medium"));
  assert.ok(result.data.candidates.some(c => c.endpoints.console_endpoint === endpoint && c.endpoints.naming_endpoint === null));
  assert.ok(result.data.candidates.every(c => c.source.location && c.source.evidence_id));
  assert.equal(result.data.scan_scope.network_requests, 0); assert.doesNotMatch(result.stdout, /SECRET/);
});

test("setup respects explicit parent depth and known directories without scanning siblings or unrelated files", async () => {
  const f = fixture(), parent = join(f.base, "parent");
  f.file(join(parent, "application.properties"), "management.naming_endpoint=https://parent.example\n");
  mkdirSync(join(f.root, "config"));
  f.file(join(f.root, "config/application-dev.properties"), "management.console_endpoint=https://project.example\n");
  f.file(join(f.root, "unrelated.properties"), "management.console_endpoint=https://unrelated.example\n");
  mkdirSync(join(parent, "sibling"));
  f.file(join(parent, "sibling/application.yml"), "management:\n  console_endpoint: https://sibling.example\n");
  const only = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 });
  assert.equal(only.code, 0, only.stderr); assert.equal(only.data.candidates.length, 1);
  const parents = await f.run("setup.inspect", {}, {}, ["setup", "inspect", "--root", f.root, "--max-parent-depth", "1"]);
  assert.equal(parents.code, 0, parents.stderr); assert.equal(parents.data.candidates.length, 2);
  assert.deepEqual(parents.data.scan_scope.roots, [f.root, parent]);
  assert.doesNotMatch(parents.stdout, /sibling\.example|unrelated\.example/);
});

test("environment inspect reports local-only facts and explicitly allowlisted environment values", async () => {
  const f = fixture();
  f.file(join(f.root, "application.properties"), "seata.enabled=true\ndb.password=SECRET_FILE\n");
  const result = await f.run("environment.inspect", { root: f.root }, { SEATA_NAMESPACE: "test", SEATA_PASSWORD: "SECRET_ENV", UNRELATED_KEY: "SECRET_OTHER" });
  assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.scope.observed_process, "cli_only"); assert.equal(result.data.scope.remote_process, null);
  assert.equal(result.data.scan_scope.max_parent_depth, 0);
  assert.ok(result.data.environment_facts.some(f => f.key === "SEATA_NAMESPACE" && f.value === "test"));
  assert.ok(result.data.environment_facts.every(f => f.effective_status === "unknown"));
  assert.doesNotMatch(result.stdout, /SECRET_FILE|SECRET_ENV|SECRET_OTHER|SEATA_PASSWORD|UNRELATED_KEY/);
});

test("inspection retains safe facts with partial exit 9 when another file is malformed or unsafe", async () => {
  const f = fixture();
  f.file(join(f.root, "application.properties"), "management.console_endpoint=https://good.example\n");
  f.file(join(f.root, "application-bad.yml"), "x: SECRET\nx: duplicate\n");
  symlinkSync(join(f.root, "application.properties"), join(f.root, "application-link.properties"));
  const result = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 });
  assert.equal(result.code, 9); assert.equal(result.stderr, ""); assert.equal(result.envelope.state, "partial");
  assert.equal(result.data.candidates[0].endpoints.console_endpoint, "https://good.example");
  assert.ok(result.data.sources.some(s => s.reason_code === "config_duplicate_key"));
  assert.ok(result.data.sources.some(s => s.reason_code === "config_source_unavailable"));
  assert.doesNotMatch(result.stdout, /SECRET/);
  const outside = join(f.base, "outside"); mkdirSync(outside);
  f.file(join(outside, "application.yml"), "management:\n  naming_endpoint: https://outside.example\n");
  symlinkSync(outside, join(f.root, "config"));
  const escaped = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 });
  assert.equal(escaped.code, 9); assert.doesNotMatch(escaped.stdout, /outside\.example/);
  const rootLink = join(f.base, "link"); symlinkSync(f.root, rootLink);
  const denied = await f.run("environment.inspect", { root: rootLink });
  assert.equal(denied.envelope.error.subtype, "unsafe_config_path");
});

test("missing schemes are not guessed, unsafe addresses are redacted and non-Seata registry URLs are not management candidates", async () => {
  const f = fixture();
  f.file(join(f.root, "application.properties"), "seata.registry.seata.server-addr=127.0.0.1:8081\nseata.registry.nacos.server-addr=https://nacos.example\nmanagement.console_endpoint=https://user:SECRET@console.example\n");
  const result = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 });
  assert.equal(result.code, 0, result.stderr); assert.equal(result.data.candidates.length, 2);
  assert.ok(result.data.candidates.every(c => c.endpoints.console_endpoint === null && c.endpoints.naming_endpoint === null));
  assert.ok(result.data.candidates.some(c => c.reason_code === "explicit_unambiguous_http_url_required"));
  assert.doesNotMatch(result.stdout, /SECRET|nacos\.example/);
  const secret = await f.run("environment.inspect", { root: f.root }, { SEATA_AI_NAMING_ENDPOINT: "https://naming.example/?q=HIDDEN_TOKEN" });
  assert.doesNotMatch(secret.stdout, /HIDDEN_TOKEN/);
});

test("scan file budget returns collected facts as truncated and directory handles are closed", async () => {
  const f = fixture();
  for (let i = 0; i < 201; i++) f.file(join(f.root, `application-${String(i).padStart(3, "0")}.properties`), "seata.enabled=true\n");
  const result = await f.run("environment.inspect", { root: f.root });
  assert.equal(result.code, 9); assert.equal(result.data.scan_scope.files, 200);
  assert.equal(result.data.completeness.status, "truncated"); assert.equal(result.data.completeness.limit_reason, "file_count_limit");
  const count = readdirSync("/proc/self/fd").length;
  for (let i = 0; i < 5; i++) {
    const directory = new ConfigDirectory(f.root, new AbortController().signal);
    assert.equal(directory.list(new AbortController().signal).names.length, 201); directory.close(); directory.close();
  }
  assert.equal(readdirSync("/proc/self/fd").length, count);
});

test("directory entry budgets and invalid roots cannot be reported as complete empty scans", async () => {
  const f = fixture();
  for (let i = 0; i < 2049; i++) f.file(join(f.root, `irrelevant-${i}.txt`), "");
  const bounded = await f.run("setup.inspect", { root: f.root, max_parent_depth: 0 });
  assert.equal(bounded.code, 9); assert.equal(bounded.data.completeness.limit_reason, "directory_entry_limit");
  assert.equal(bounded.data.scan_scope.files, 0);
  const missing = await f.run("environment.inspect", { root: join(f.base, "missing") });
  assert.equal(missing.envelope.error.subtype, "config_directory_missing");
  const global = await f.run("setup.inspect", { root: "/" });
  assert.equal(global.envelope.error.subtype, "unsafe_config_path");
  const tooMany = await f.run("setup.inspect", { root: f.root, max_parent_depth: 4 });
  assert.equal(tooMany.code, 2);
});
