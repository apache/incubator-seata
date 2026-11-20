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
import { mkdtemp, mkdir, readFile, access, rename } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";

const root = fileURLToPath(new URL("../", import.meta.url));
const developmentLock = JSON.parse(await readFile(join(root, "package-lock.json"), "utf8"));
const publishedLock = JSON.parse(await readFile(join(root, "npm-shrinkwrap.json"), "utf8"));
assert.deepEqual(developmentLock.packages, publishedLock.packages,
  "The CI cache lock and published shrinkwrap must pin the same dependency tree");
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
  "api/resources.json",
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
  "preuninstall",
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
const sbom = JSON.parse(run("npm", ["sbom", "--omit=dev", "--sbom-format=cyclonedx", "--package-lock-only"],
  { cwd: installed }).stdout);
assert.equal(sbom.bomFormat, "CycloneDX");
assert.equal(sbom.metadata.component.name, metadata.name);
assert.equal(sbom.metadata.component.version, metadata.version);
assert.equal(sbom.metadata.component.purl, `pkg:npm/${metadata.name}@${metadata.version}`);
const expectedComponents = Object.entries(lock.packages).filter(([path, entry]) => path && !entry.dev)
  .map(([path, entry]) => `${path.slice("node_modules/".length)}@${entry.version}`).sort();
assert.deepEqual(sbom.components.map(component => `${component.name}@${component.version}`).sort(), expectedComponents);
const shim = join(prefix, "node_modules/.bin/seata-ai");
await access(shim);

const version = JSON.parse(run(shim, ["version"], { cwd: temp }).stdout);
assert.equal(version.ok, true);
assert.equal(version.data.contract_revision, "R9");
assert.equal(version.data.state_store_id, null);
const schema = JSON.parse(run(shim, ["schema", "system.invoke"], { cwd: temp }).stdout);
assert.equal(schema.data.command_id, "system.invoke");
assert.ok(schema.data.invocation_schema.properties.input.properties.invocation.oneOf.length > 0);

// Verify the installed entrypoint fails closed if package resources are missing.
const target = join(installed, "api/protocol/registry.json");
const saved = target + ".backup";
await rename(target, saved);
try {
  const damaged = spawnSync(shim, ["version"], { cwd: temp, encoding: "utf8", timeout: 10000 });
  assert.equal(damaged.status, 5);
  assert.equal(damaged.stdout, "");
  assert.equal(JSON.parse(damaged.stderr).error.subtype, "startup_failure");
  assert.equal(damaged.stderr.includes(installed), false);
} finally {
  await rename(saved, target);
}
assert.equal(JSON.parse(run(shim, ["version"], { cwd: temp }).stdout).ok, true);
console.log("Installed tarball, pinned runtime dependencies, and resource checks passed.");
