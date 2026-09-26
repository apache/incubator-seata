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
import { mkdtemp, mkdir, readFile, access, lstat, readdir, rename, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { spawnSync } from "node:child_process";
import assert from "node:assert/strict";
import { createHash, randomUUID } from "node:crypto";

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
  "api/store/schema.sql",
  "api/resources.json",
  "api/skills.json",
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
const bundledSkills = JSON.parse(await readFile(join(installed, "api/skills.json"), "utf8"));
assert.equal(bundledSkills.skills.length, 7);
// Exercise the installed entrypoint, not just the manifest validator. Damaged
// package resources must fail before any user state or Skill deployment exists.
const missingResource = [...Object.keys(manifest)].find(path => path.startsWith("schemas/"));
assert.ok(missingResource);
const skillResource = `skills/${bundledSkills.skills[0].skill_id}/SKILL.md`;
for (const [kind, relative] of [["missing_schema", missingResource], ["changed_skill", skillResource]]) {
  const target = join(installed, relative), saved = `${target}.installation-test-backup`;
  const original = await readFile(target);
  const damagedState = join(temp, `damaged-${kind}-state`);
  const damagedHome = join(temp, `damaged-${kind}-home`);
  await mkdir(damagedHome, { mode: 0o700 });
  await rename(target, saved);
  try {
    if (kind === "changed_skill") await writeFile(target, "PACKAGE_TEST_SECRET_DO_NOT_EXPOSE\n", { flag: "wx" });
    const failed = spawnSync(shim, ["version"], { cwd: temp, encoding: "utf8", timeout: 10000,
      env: { ...process.env, HOME: damagedHome, SEATA_AI_STATE_DIR: damagedState } });
    assert.equal(failed.status, 5, `${kind}: ${failed.stderr}`);
    assert.equal(failed.stdout, "", kind);
    const error = JSON.parse(failed.stderr);
    assert.equal(error.ok, false);
    assert.equal(error.error.type, "internal");
    assert.equal(error.error.subtype, "startup_failure");
    assert.equal(error.error.retry, "never");
    assert.doesNotMatch(failed.stderr, /PACKAGE_TEST_SECRET_DO_NOT_EXPOSE|ENOENT|node:fs|installation-test-backup/);
    assert.equal(failed.stderr.includes(temp), false, "Internal installation paths must not leak");
    await assert.rejects(lstat(damagedState), { code: "ENOENT" });
    assert.deepEqual(await readdir(damagedHome), []);
  } finally {
    await rename(saved, target);
  }
  assert.deepEqual(await readFile(target), original);
  assert.equal(JSON.parse(run(shim, ["version"], { cwd: temp }).stdout).ok, true);
}
for (const skill of bundledSkills.skills) {
  for (const file of skill.files) {
    const path = `skills/${skill.skill_id}/${file.path}`;
    assert.ok(packed.has(path), path);
    assert.deepEqual(await readFile(join(installed, path)), await readFile(join(root, path)), path);
  }
  const result = JSON.parse(run(shim, ["skills", "read", "--skill-id", skill.skill_id], { cwd: temp }).stdout).data;
  assert.equal(result.digest, skill.files.find(f => f.path === "SKILL.md").digest);
}
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
  policy: { storage_policy: "protected_file" },
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
const testHome = join(temp, "isolated skill home");
await mkdir(testHome, { mode: 0o700 });
const skillEnv = { ...process.env, HOME: testHome, SEATA_AI_STATE_DIR: join(temp, "state") };
const skillInstall = JSON.parse(run(shim, ["skills", "install", "--global"], { cwd: temp, env: skillEnv }).stdout).data;
assert.equal(skillInstall.installation_state, "complete");
assert.equal(skillInstall.host_status.content_installed, true);
const skillStatus = JSON.parse(run(shim, ["skills", "status"], { cwd: temp, env: skillEnv }).stdout).data;
assert.equal(skillStatus.content_installed, true);
assert.equal(skillStatus.exposure_valid, true);
assert.equal(skillStatus.installation_mode, "direct");
assert.equal(skillStatus.session_loaded, "unknown");
const skillDirectory = join(testHome, ".agents", "skills");
assert.equal((await readdir(skillDirectory)).filter(name => name.startsWith("seata-")).length, 7);
for (const skill of bundledSkills.skills) {
  const skillRoot = join(skillDirectory, skill.skill_id);
  assert.equal((await lstat(skillRoot)).isSymbolicLink(), false);
  for (const file of skill.files)
    assert.deepEqual(await readFile(join(skillRoot, file.path)),
      await readFile(join(installed, "skills", skill.skill_id, file.path)));
}

// Seed only isolated CLI-local state. No Console login or Seata write is sent.
const { StateStore } = await import(pathToFileURL(join(installed, "dist/adapter/statestore/client.js")).href);
const { CredentialStore } = await import(pathToFileURL(join(installed, "dist/adapter/auth/credentials.js")).href);
const { digest } = await import(pathToFileURL(join(installed, "dist/protocol/value.js")).href);
const stateRoot = join(temp, "state"), requestId = randomUUID();
const store = new StateStore(stateRoot);
let credentialRef, stateStoreId;
try {
  const binding = await store.call("auth.binding", { name: "dev" }), p = binding.profile;
  stateStoreId = binding.state_store_id;
  const { flow } = await store.call("flow.create", { name: "dev" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  credentialRef = staged.credential_ref;
  await new CredentialStore(stateRoot, "protected_file", stateStoreId)
    .put(credentialRef, "Bearer INSTALL_REINSTALL_TEST", AbortSignal.timeout(5000));
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: credentialRef });
  const intent = { xid: "synthetic-install-test" };
  await store.call("execution.claim", { client_request_id: requestId, command: "transaction.retry.pause",
    intent, intent_digest: digest(intent), profile_id: p.profile_id, target_digest: binding.target_digest,
    target: { profile_id: p.profile_id, profile_revision: p.revision,
      console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
      namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
      adapter_id: "seata-src-41694e14", adapter_digest: "synthetic-install-test", command_schema_version: "0.1" } });
} finally { await store.close(); }
const historyBefore = JSON.parse(run(shim, ["execution", "get", "--client-request-id", requestId], { cwd: temp }).stdout).data;
async function inventory(directory) {
  const files = {};
  const visit = async (path, relative = "") => {
    for (const entry of await readdir(path, { withFileTypes: true })) {
      const name = join(relative, entry.name), child = join(path, entry.name);
      assert.equal(entry.isSymbolicLink(), false, name);
      if (entry.isDirectory()) await visit(child, name);
      else {
        assert.equal(entry.isFile(), true, name);
        files[name] = createHash("sha256").update(await readFile(child)).digest("hex");
      }
    }
  };
  await visit(directory);
  return files;
}
const stateBefore = await inventory(stateRoot), skillsBefore = await inventory(skillDirectory);
run("npm", ["uninstall", "--prefix", prefix, "--no-audit", "--no-fund", metadata.name]);
await assert.rejects(lstat(installed), { code: "ENOENT" });
await assert.rejects(lstat(shim), { code: "ENOENT" });
assert.deepEqual(await inventory(stateRoot), stateBefore);
assert.deepEqual(await inventory(skillDirectory), skillsBefore);
run("npm", ["install", "--prefix", prefix, "--omit=dev", "--no-audit", "--no-fund", join(temp, pack.filename)]);
assert.deepEqual(await inventory(stateRoot), stateBefore);
assert.deepEqual(await inventory(skillDirectory), skillsBefore);
const versionReinstalled = JSON.parse(run(shim, ["version"], { cwd: temp }).stdout).data;
assert.equal(versionReinstalled.state_store_id, stateStoreId);
const historyAfter = JSON.parse(run(shim, ["execution", "get", "--client-request-id", requestId], { cwd: temp }).stdout).data;
assert.deepEqual(historyAfter, historyBefore);
assert.equal(historyAfter.recovery.replay_allowed, false);
const authAfter = JSON.parse(run(shim, ["auth", "status", "--profile", "dev"], { cwd: temp }).stdout).data;
assert.equal(authAfter.authenticated, true);
assert.equal(await new CredentialStore(stateRoot, "protected_file", stateStoreId)
  .get(credentialRef, AbortSignal.timeout(5000)), "Bearer INSTALL_REINSTALL_TEST");
const skillsAfter = JSON.parse(run(shim, ["skills", "status"], { cwd: temp, env: skillEnv }).stdout).data;
assert.equal(skillsAfter.content_installed, true);
assert.equal(skillsAfter.exposure_valid, true);
const logout = JSON.parse(run(shim, ["auth", "logout", "--profile", "dev"], { cwd: temp }).stdout).data;
assert.equal(logout.credential_cleanup, "complete");
await assert.rejects(new CredentialStore(stateRoot, "protected_file", stateStoreId)
  .get(credentialRef, AbortSignal.timeout(5000)), { subtype: "credential_store_unavailable" });
console.log(
  JSON.stringify({
    result: "passed",
    tarball: join(temp, pack.filename),
    sha256: createHash("sha256")
      .update(await readFile(join(temp, pack.filename)))
      .digest("hex"),
    prefix,
    package_version: metadata.version,
    installed_skills_verified: bundledSkills.skills.length,
    damaged_package_startup_cases: 2,
    sbom_runtime_components_verified: sbom.components.length,
    uninstall_reinstall_preserved_user_state: true,
    installed_runtime_dependencies:
      Object.values(lock.packages).filter((e) => !e.dev).length - 1,
  }),
);
