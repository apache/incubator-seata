// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import { mkdtemp, mkdir, readFile, access } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";

const root = fileURLToPath(new URL("../", import.meta.url));
const temp = await mkdtemp(join(tmpdir(), "seata-r11-install-"));
const prefix = join(temp, "安装 prefix with spaces");
await mkdir(prefix);
const run = (command, args, options = {}) => {
  const r = spawnSync(command, args, {
    cwd: root,
    encoding: "utf8",
    timeout: 120000,
    env: { ...process.env, SEATA_AI_STATE_DIR: join(temp, "state") },
    ...options,
  });
  assert.equal(r.status, 0, r.stderr + r.stdout);
  return r;
};
const pack = JSON.parse(
  run("npm", ["pack", "--ignore-scripts", "--json", "--pack-destination", temp])
    .stdout,
)[0];
const packed = new Set(pack.files.map((f) => f.path));
for (const name of [
  "dist/cli.js",
  "api/protocol/registry.json",
  "api/protocol/outputs.json",
  "api/store/schema.sql",
  "api/resources.json",
  "compatibility/manifest.json",
  "LICENSE",
  "NOTICE",
  "npm-shrinkwrap.json",
])
  assert.ok(packed.has(name), name);
for (const file of packed)
  assert.equal(
    /(^|\/)(src|test|node_modules|\.git|state\.db)(\/|$)/.test(file),
    false,
    file,
  );
run("npm", [
  "install",
  "--prefix",
  prefix,
  "--ignore-scripts",
  "--omit=dev",
  "--no-audit",
  "--no-fund",
  join(temp, pack.filename),
]);
const installed = join(prefix, "node_modules/seata-ai-cli-development");
const metadata = JSON.parse(
  await readFile(join(installed, "package.json"), "utf8"),
);
for (const hook of [
  "preinstall",
  "install",
  "postinstall",
  "uninstall",
  "postuninstall",
])
  assert.equal(metadata.scripts[hook], undefined);
const manifest = JSON.parse(
  await readFile(join(installed, "api/resources.json"), "utf8"),
);
for (const [path, hash] of Object.entries(manifest))
  assert.equal(
    createHash("sha256")
      .update(await readFile(join(installed, path)))
      .digest("hex"),
    hash,
    path,
  );
const lock = JSON.parse(
  await readFile(join(installed, "npm-shrinkwrap.json"), "utf8"),
);
for (const [path, entry] of Object.entries(lock.packages))
  if (path && !entry.dev) {
    const dependency = JSON.parse(
      await readFile(join(installed, path, "package.json"), "utf8").catch(() =>
        readFile(join(prefix, path, "package.json"), "utf8"),
      ),
    );
    assert.equal(dependency.version, entry.version, path);
    assert.notEqual(entry.hasInstallScript, true, path);
  }
const shim = join(prefix, "node_modules/.bin/seata-ai");
await access(shim);
const version = run(shim, ["version"], { cwd: temp });
assert.equal(version.stderr, "");
assert.equal(JSON.parse(version.stdout).data.contract_revision, "R9");
const schema = run(shim, ["schema", "transaction.retry.pause"], { cwd: "/" });
assert.equal(schema.stderr, "");
assert.equal(
  JSON.parse(schema.stdout).data.invocation_schema.properties.command.const,
  "transaction.retry.pause",
);
const profile = {
  name: "dev",
  environment: "development",
  management: {
    console_endpoint: "http://127.0.0.1:8080",
    naming_endpoint: "http://127.0.0.1:8081",
    compatibility_id: "seata-src-41694e14",
  },
  target: { namespace: "public", cluster: "default" },
};
const created = run(
  shim,
  ["profile", "create", "--config", JSON.stringify(profile), "--accept-target"],
  { cwd: temp },
);
assert.equal(created.stderr, "");
assert.equal(JSON.parse(created.stdout).data.profile.revision, 1);
const versionAfter = JSON.parse(run(shim, ["version"], { cwd: temp }).stdout);
assert.equal(typeof versionAfter.data.state_store_id, "string");
console.log(
  JSON.stringify({
    result: "passed",
    tarball: join(temp, pack.filename),
    sha256: createHash("sha256")
      .update(await readFile(join(temp, pack.filename)))
      .digest("hex"),
    prefix,
    package_version: metadata.version,
    installed_runtime_dependencies:
      Object.values(lock.packages).filter((e) => !e.dev).length - 1,
  }),
);
