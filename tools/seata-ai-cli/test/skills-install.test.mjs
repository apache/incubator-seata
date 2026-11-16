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
import { mkdtempSync, existsSync, readFileSync, writeFileSync, mkdirSync, lstatSync, readdirSync, cpSync, symlinkSync, unlinkSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { DatabaseSync } from "node:sqlite";
import { createHash } from "node:crypto";
import { ExportFile } from "../dist/adapter/export/file.js";
const cli = new URL("../dist/cli.js", import.meta.url).pathname;
function fixture() {
  const home = mkdtempSync(join(tmpdir(), "seata-direct-install-")), state = join(home, "state");
  const directory = join(home, ".agents", "skills");
  const run = (entrypoint = cli, nodeArgs = []) => {
    const r = spawnSync(process.execPath, [...nodeArgs, entrypoint, "skills", "install", "--global"], {
      env: { ...process.env, HOME: home, SEATA_AI_STATE_DIR: state }, encoding: "utf8", timeout: 30000 });
    return { code: r.status, signal: r.signal, data: r.stdout || r.stderr ? JSON.parse(r.stdout || r.stderr) : null, stderr: r.stderr };
  };
  return { home, state, directory, run };
}

function killAfterRecord(phase, artifact) {
  // Inject only at the test process boundary. The real worker transaction and
  // filesystem writes complete before its response kills the whole CLI.
  const bootstrap = `import { Worker } from 'node:worker_threads';
    const emit = Worker.prototype.emit, post = Worker.prototype.postMessage, pending = new WeakMap();
    Worker.prototype.postMessage = function(message, ...args) {
      if (!pending.has(this)) pending.set(this, new Map());
      pending.get(this).set(message.id, message.op);
      return post.call(this, message, ...args);
    };
    Worker.prototype.emit = function(event, ...args) {
      const record = args[0]?.value?.record;
      if (event === 'message' && record?.phase === ${JSON.stringify(phase)} &&
          pending.get(this)?.get(args[0]?.id) === ${JSON.stringify(artifact ? "artifact.record" : "skills.install.record")})
        process.kill(process.pid, 'SIGKILL');
      return emit.call(this, event, ...args);
    };`;
  return ["--import", "data:text/javascript," + encodeURIComponent(bootstrap)];
}

function installationRecord(f) {
  const db = new DatabaseSync(join(f.state, "state.db"));
  try { return JSON.parse(db.prepare("SELECT value FROM local_settings WHERE key='skills.install'").get().value); }
  finally { db.close(); }
}

// Build a second, self-consistent package in a temporary directory. Neither
// the working package nor the user's installed Skills are modified.
function revisedBundle(home, addResource = false, removeResource = false) {
  const source = new URL("../", import.meta.url).pathname, root = join(home, "revised-package");
  mkdirSync(root);
  for (const name of ["dist", "api", "schemas", "compatibility", "skills", "package.json"])
    cpSync(join(source, name), join(root, name), { recursive: true });
  symlinkSync(join(source, "node_modules"), join(root, "node_modules"), "dir");
  const relative = "skills/seata-shared/SKILL.md";
  const bytes = Buffer.concat([readFileSync(join(root, relative)), Buffer.from("\nRevision fixture.\n")]);
  writeFileSync(join(root, relative), bytes);
  const hash = bytes => createHash("sha256").update(bytes).digest("hex");
  const manifest = JSON.parse(readFileSync(join(root, "api/skills.json")));
  const skill = manifest.skills.find(skill => skill.skill_id === "seata-shared");
  const file = skill.files.find(file => file.path === "SKILL.md");
  file.bytes = bytes.length; file.digest = hash(bytes);
  skill.digest = hash(JSON.stringify(skill.files));
  // Change a later file too: killing after the first payload leaves genuinely
  // mixed old/new content, so recovery must consult the durable old baseline.
  const secondRelative = "skills/seata-shared/references/evidence.md";
  const secondBytes = Buffer.concat([readFileSync(join(root, secondRelative)), Buffer.from("\nLater revision fixture.\n")]);
  writeFileSync(join(root, secondRelative), secondBytes);
  const secondSkill = skill;
  const secondFile = secondSkill.files.find(file => file.path === "references/evidence.md");
  secondFile.bytes = secondBytes.length; secondFile.digest = hash(secondBytes);
  secondSkill.digest = hash(JSON.stringify(secondSkill.files));
  const addedRelative = "skills/seata-shared/references/new-evidence.md";
  if (addResource) {
    const addedBytes = Buffer.from("New bundled evidence instructions.\n");
    writeFileSync(join(root, addedRelative), addedBytes);
    skill.files.push({ path: "references/new-evidence.md", bytes: addedBytes.length, digest: hash(addedBytes) });
    skill.digest = hash(JSON.stringify(skill.files));
  }
  const removedRelative = secondRelative;
  if (removeResource) {
    unlinkSync(join(root, removedRelative));
    skill.files = skill.files.filter(file => file.path !== "references/evidence.md");
    skill.digest = hash(JSON.stringify(skill.files));
  }
  writeFileSync(join(root, "api/skills.json"), JSON.stringify(manifest));
  const resources = JSON.parse(readFileSync(join(root, "api/resources.json")));
  for (const name of [relative, ...(removeResource ? [] : [secondRelative]), "api/skills.json"]) resources[name] = hash(readFileSync(join(root, name)));
  if (addResource) resources[addedRelative] = hash(readFileSync(join(root, addedRelative)));
  if (removeResource) delete resources[removedRelative];
  writeFileSync(join(root, "api/resources.json"), JSON.stringify(resources));
  return { cli: join(root, "dist/cli.js"), bytes };
}

// A self-contained old-release inventory exercises seven-to-one migration
// without depending on deleted source files or a developer's installed Skills.
function legacyBundle(home) {
  const bundle = revisedBundle(home), root = join(home, "revised-package");
  const ids = ["seata-shared", "seata-onboarding", "seata-integration-check", "seata-transaction-analysis",
    "seata-incident-diagnosis", "seata-recovery-plan", "seata-controlled-recovery"];
  const hash = bytes => createHash("sha256").update(bytes).digest("hex");
  const resources = JSON.parse(readFileSync(join(root, "api/resources.json")));
  for (const name of Object.keys(resources)) if (name.startsWith("skills/")) delete resources[name];
  const skills = ids.map(id => {
    const files = ["SKILL.md", "references/evidence.md", "examples/scenario.md", "tests/acceptance.md"].map(path => {
      const relative = `skills/${id}/${path}`;
      mkdirSync(join(root, relative, ".."), { recursive: true });
      const bytes = Buffer.from(path === "SKILL.md"
        ? `---\nname: ${id}\ndescription: Legacy release fixture.\n---\n\nLegacy entrypoint.\n`
        : `Legacy ${id} ${path}.\n`);
      writeFileSync(join(root, relative), bytes);
      resources[relative] = hash(bytes);
      return { path, bytes: bytes.length, digest: hash(bytes) };
    });
    return { skill_id: id, version: "0.1.0-r11.0", description: "Legacy release fixture.", digest: hash(JSON.stringify(files)), files };
  });
  writeFileSync(join(root, "api/skills.json"), JSON.stringify({ version: "0.1.0-r11.0", skills }));
  resources["api/skills.json"] = hash(readFileSync(join(root, "api/skills.json")));
  writeFileSync(join(root, "api/resources.json"), JSON.stringify(resources));
  return { ...bundle, ids };
}

test("seven owned legacy Skills consolidate to one entrypoint without retaining old content", () => {
  const f = fixture(), legacy = legacyBundle(f.home), initial = f.run(legacy.cli);
  assert.equal(initial.code, 0, initial.stderr);
  assert.equal(initial.data.data.manifest.files.length, 28);
  const unrelated = join(f.directory, "user-owned", "SKILL.md");
  mkdirSync(join(f.directory, "user-owned"), { mode: 0o700 });
  writeFileSync(unrelated, "USER SKILL");
  const result = f.run(); assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.host_status.content_installed, true);
  assert.equal(result.data.data.manifest.files.length, 2);
  for (const file of initial.data.data.manifest.files) {
    const retained = result.data.data.manifest.files.some(current => current.relative_path === file.relative_path);
    assert.equal(existsSync(file.path), retained, file.relative_path);
  }
  assert.equal(readFileSync(unrelated, "utf8"), "USER SKILL");
  assert.equal(f.run().data.data.installation_state, "unchanged");
});

for (const modification of ["owned-file", "extra-file"]) test(`legacy consolidation preserves ${modification} and refuses before payload changes`, () => {
  const f = fixture(), legacy = legacyBundle(f.home), initial = f.run(legacy.cli);
  assert.equal(initial.code, 0, initial.stderr);
  const first = join(f.directory, "seata-shared", "SKILL.md"), before = readFileSync(first);
  const user = join(f.directory, "seata-onboarding", modification === "owned-file" ? "SKILL.md" : "user-notes.md");
  writeFileSync(user, "USER CONTENT");
  const result = f.run(); assert.equal(result.code, 8, result.stderr);
  assert.equal(readFileSync(user, "utf8"), "USER CONTENT");
  assert.deepEqual(readFileSync(first), before);
  for (const file of initial.data.data.manifest.files) assert.equal(existsSync(file.path), true, file.relative_path);
  assert.deepEqual(installationRecord(f), initial.data.data.manifest);
});

test("unowned retired entrypoints are not adopted or deleted by a fresh installation", () => {
  const f = fixture(), user = join(f.directory, "seata-onboarding", "SKILL.md");
  mkdirSync(join(f.directory, "seata-onboarding"), { recursive: true, mode: 0o700 });
  writeFileSync(user, "USER SKILL");
  const result = f.run(); assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.manifest.files.length, 2);
  assert.equal(readFileSync(user, "utf8"), "USER SKILL");
});

test("consolidation resumes after a retired entrypoint is captured but before its deletion completes", () => {
  const f = fixture(), legacy = legacyBundle(f.home), initial = f.run(legacy.cli);
  assert.equal(initial.code, 0, initial.stderr);
  // Linux removal uses a descriptor-anchored path, not the full host pathname.
  // The retained shared entrypoint is updated, so every removed SKILL.md here
  // belongs to a retired Skill.
  const killed = f.run(cli, removalRenameFault("kill", "/SKILL.md"));
  assert.equal(killed.signal, "SIGKILL", killed.stderr);
  const interrupted = installationRecord(f); assert.equal(interrupted.phase, "writing");
  const result = f.run(); assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.manifest.operation_id, interrupted.operation_id);
  assert.equal(result.data.data.host_status.content_installed, true);
  for (const id of legacy.ids.filter(id => id !== "seata-shared"))
    assert.equal(existsSync(join(f.directory, id, "SKILL.md")), false);
  assert.equal(f.run().data.data.installation_state, "unchanged");
});

test("direct Skills installation creates ordinary directories, verifies content and is idempotent", () => {
  const f = fixture();
  const unrelated = join(f.directory, "user-owned", "SKILL.md");
  mkdirSync(join(f.directory, "user-owned"), { recursive: true, mode: 0o700 });
  writeFileSync(unrelated, "UNRELATED USER SKILL");
  const first = f.run();
  assert.equal(first.code, 0, first.stderr);
  assert.equal(first.data.data.installation_state, "complete");
  assert.equal(first.data.data.host_status.content_installed, true);
  assert.equal(first.data.data.host_status.session_loaded, "unknown");
  assert.deepEqual(readdirSync(f.directory).filter(name => name.startsWith("seata-")), ["seata-shared"]);
  const file = join(f.directory, "seata-shared", "SKILL.md");
  assert.equal(lstatSync(join(f.directory, "seata-shared")).isSymbolicLink(), false);
  assert.equal(existsSync(join(f.state, "skills")), false);
  const before = lstatSync(file);
  const again = f.run(); assert.equal(again.code, 0, again.stderr);
  assert.equal(again.data.data.installation_state, "unchanged");
  assert.equal(lstatSync(file).mtimeMs, before.mtimeMs);
  writeFileSync(file, "USER EDIT");
  const conflict = f.run(); assert.equal(conflict.code, 8);
  assert.equal(readFileSync(file, "utf8"), "USER EDIT");
  assert.equal(readFileSync(unrelated, "utf8"), "UNRELATED USER SKILL");
});
test("same-name user Skill conflicts before payload writes and unrelated Skills remain untouched", () => {
  const f = fixture();
  mkdirSync(join(f.directory, "seata-shared"), { recursive: true, mode: 0o700 });
  writeFileSync(join(f.directory, "seata-shared", "SKILL.md"), "USER SKILL");
  const result = f.run(); assert.equal(result.code, 8, result.stderr);
  assert.equal(existsSync(join(f.directory, "seata-onboarding")), false);
  assert.equal(readFileSync(join(f.directory, "seata-shared", "SKILL.md"), "utf8"), "USER SKILL");
});

test("repeat installation rejects incomplete or malformed ownership inventories without rewriting Skills", () => {
  const f = fixture(), installed = f.run();
  assert.equal(installed.code, 0, installed.stderr);
  const file = join(f.directory, "seata-shared", "SKILL.md"), before = lstatSync(file);
  const db = new DatabaseSync(join(f.state, "state.db"));
  const original = JSON.parse(db.prepare("SELECT value FROM local_settings WHERE key='skills.install'").get().value);
  db.close();
  const cases = [
    ["missing inventory", record => { record.files = []; }],
    ["duplicate entry", record => { record.files[1] = record.files[0]; }],
    ["non-array inventory", record => { record.files = {}; }],
    ["null entry", record => { record.files[0] = null; }],
    ["traversal", record => { record.files[0].relative_path = "../outside"; }],
    ["incorrect digest", record => { record.files[0].digest = "sha256:" + "0".repeat(64); }],
    ["incorrect length", record => { record.files[0].bytes += 1; }],
    ["incorrect absolute path", record => { record.files[0].path = join(f.home, "outside"); }],
    ["incorrect version", record => { record.version = "other"; }],
  ];
  for (const [name, corrupt] of cases) {
    const record = structuredClone(original); corrupt(record);
    const writer = new DatabaseSync(join(f.state, "state.db"));
    writer.prepare("UPDATE local_settings SET value=? WHERE key='skills.install'").run(JSON.stringify(record));
    writer.close();
    const result = f.run();
    assert.equal(result.code, 8, name + ": " + JSON.stringify(result.data));
    assert.equal(lstatSync(file).mtimeMs, before.mtimeMs, name);
  }
});

test("a changed bundled release replaces unchanged owned Skills in place and verifies the latest content", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const file = join(f.directory, "seata-shared", "SKILL.md"), before = lstatSync(file);
  const revised = revisedBundle(f.home), result = f.run(revised.cli);
  assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.installation_state, "complete");
  assert.equal(result.data.data.host_status.content_installed, true);
  assert.deepEqual(readFileSync(file), revised.bytes);
  assert.equal(lstatSync(file).ino, before.ino);
  assert.notEqual(result.data.data.manifest.content_digest, initial.data.data.manifest.content_digest);
  assert.equal(f.run(revised.cli).data.data.installation_state, "unchanged");
  assert.deepEqual(readdirSync(f.directory).sort(), [".seata-ai-install.lock", "seata-shared"]);
});

test("upgrade preflight preserves all old files and journal when any installed Skill was edited", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const first = join(f.directory, "seata-shared", "SKILL.md"), before = readFileSync(first);
  const edited = join(f.directory, "seata-shared", "references", "evidence.md"); writeFileSync(edited, "USER EDIT");
  const revised = revisedBundle(f.home), result = f.run(revised.cli);
  assert.equal(result.code, 8, result.stderr);
  assert.deepEqual(readFileSync(first), before);
  assert.equal(readFileSync(edited, "utf8"), "USER EDIT");
  const db = new DatabaseSync(join(f.state, "state.db"));
  const saved = JSON.parse(db.prepare("SELECT value FROM local_settings WHERE key='skills.install'").get().value); db.close();
  assert.deepEqual(saved, initial.data.data.manifest);
});

test("an interrupted final verification resumes by checking owned content without rewriting it", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const file = join(f.directory, "seata-shared", "SKILL.md"), before = lstatSync(file);
  const record = structuredClone(initial.data.data.manifest); record.phase = "verifying";
  const db = new DatabaseSync(join(f.state, "state.db"));
  db.prepare("UPDATE local_settings SET value=? WHERE key='skills.install'").run(JSON.stringify(record)); db.close();
  const result = f.run(); assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.installation_state, "complete");
  assert.equal(result.data.data.manifest.operation_id, record.operation_id);
  assert.equal(result.data.data.manifest.revision, record.revision + 1);
  assert.equal(result.data.data.manifest.phase, "complete");
  assert.equal(lstatSync(file).mtimeMs, before.mtimeMs);
  assert.equal(f.run().data.data.installation_state, "unchanged");
});

for (const [phase, artifact] of [["preparing", false], ["complete", true]]) {
  test(`SIGKILL after ${artifact ? "first complete payload" : "preparing journal"} resumes the same installation`, () => {
    const f = fixture(), killed = f.run(cli, killAfterRecord(phase, artifact));
    assert.equal(killed.signal, "SIGKILL");
    const interrupted = installationRecord(f);
    assert.equal(interrupted.phase, artifact ? "writing" : "preparing");
    assert.equal(existsSync(join(f.directory, "seata-shared", "references", "evidence.md")), false);
    const resumed = f.run(); assert.equal(resumed.code, 0, resumed.stderr);
    assert.equal(resumed.data.data.manifest.operation_id, interrupted.operation_id);
    assert.equal(resumed.data.data.manifest.phase, "complete");
    assert.equal(resumed.data.data.host_status.content_installed, true);
    assert.equal(f.run().data.data.installation_state, "unchanged");
  });
}

test("recovery refuses an uncertain payload intent and does not overwrite its bytes", () => {
  const f = fixture(), killed = f.run(cli, killAfterRecord("writing", true));
  assert.equal(killed.signal, "SIGKILL");
  const file = join(f.directory, "seata-shared", "SKILL.md"), before = readFileSync(file);
  const interrupted = installationRecord(f), resumed = f.run();
  assert.equal(resumed.code, 8, resumed.stderr);
  assert.equal(resumed.data.error.subtype, "skills_install_incomplete");
  assert.deepEqual(readFileSync(file), before);
  assert.deepEqual(installationRecord(f), interrupted);
});

for (const addResource of [false, true]) test(`SIGKILL during an upgrade resumes mixed old/new payloads (added resource: ${addResource})`, () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const revised = revisedBundle(f.home, addResource), killed = f.run(revised.cli, killAfterRecord("complete", true));
  assert.equal(killed.signal, "SIGKILL");
  const interrupted = installationRecord(f);
  assert.equal(interrupted.phase, "writing");
  assert.notEqual(interrupted.operation_id, initial.data.data.manifest.operation_id);
  const result = f.run(revised.cli); assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.manifest.operation_id, interrupted.operation_id);
  assert.equal(result.data.data.host_status.content_installed, true);
  assert.deepEqual(readFileSync(join(f.directory, "seata-shared", "SKILL.md")), revised.bytes);
  assert.equal(existsSync(join(f.directory, "seata-shared", "references", "new-evidence.md")), addResource);
});

test("recovery preserves user edits made after the interrupted file completed", () => {
  const f = fixture(), killed = f.run(cli, killAfterRecord("complete", true));
  assert.equal(killed.signal, "SIGKILL");
  const file = join(f.directory, "seata-shared", "SKILL.md"); writeFileSync(file, "EDIT AFTER CRASH");
  const interrupted = installationRecord(f), resumed = f.run();
  assert.equal(resumed.code, 8, resumed.stderr);
  assert.equal(resumed.data.error.subtype, "skills_install_incomplete");
  assert.equal(readFileSync(file, "utf8"), "EDIT AFTER CRASH");
  assert.equal(existsSync(join(f.directory, "seata-shared", "references", "evidence.md")), false);
  assert.deepEqual(installationRecord(f), interrupted);
});

test("recovery does not adopt content replaced by a separately owned export after the crash", async () => {
  const f = fixture(), killed = f.run(cli, killAfterRecord("complete", true));
  assert.equal(killed.signal, "SIGKILL");
  const target = new ExportFile(f.directory, "seata-shared/SKILL.md", true, f.state);
  try {
    const signal = new AbortController().signal;
    await target.prepare(signal); await target.write(Buffer.from("SEPARATE EXPORT"), signal);
  } finally { await target.close(); }
  const interrupted = installationRecord(f), resumed = f.run();
  assert.equal(resumed.code, 8, resumed.stderr);
  assert.equal(readFileSync(join(f.directory, "seata-shared", "SKILL.md"), "utf8"), "SEPARATE EXPORT");
  assert.deepEqual(installationRecord(f), interrupted);
});

test("upgrade installs added resources and remains idempotent without version directories", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const revised = revisedBundle(f.home, true), result = f.run(revised.cli);
  assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.host_status.content_installed, true);
  assert.equal(readFileSync(join(f.directory, "seata-shared", "references", "new-evidence.md"), "utf8"), "New bundled evidence instructions.\n");
  assert.equal(result.data.data.manifest.files.length, initial.data.data.manifest.files.length + 1);
  assert.equal(f.run(revised.cli).data.data.installation_state, "unchanged");
});

test("a user file at an added resource path prevents every upgrade payload write", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const first = join(f.directory, "seata-shared", "SKILL.md"), before = readFileSync(first);
  const user = join(f.directory, "seata-shared", "references", "new-evidence.md");
  writeFileSync(user, "USER RESOURCE", { mode: 0o600 });
  const revised = revisedBundle(f.home, true), result = f.run(revised.cli);
  assert.equal(result.code, 8, result.stderr);
  assert.equal(readFileSync(user, "utf8"), "USER RESOURCE");
  assert.deepEqual(readFileSync(first), before);
  assert.deepEqual(installationRecord(f), initial.data.data.manifest);
});

test("upgrade removes only unchanged owned obsolete resources and leaves no old-version or staging directory", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const revised = revisedBundle(f.home, false, true), result = f.run(revised.cli);
  assert.equal(result.code, 0, result.stderr);
  assert.equal(result.data.data.host_status.content_installed, true);
  assert.equal(result.data.data.manifest.files.length, initial.data.data.manifest.files.length - 1);
  assert.deepEqual(readdirSync(join(f.directory, "seata-shared", "references")), []);
  assert.equal(f.run(revised.cli).data.data.installation_state, "unchanged");
});

test("a modified obsolete resource blocks the upgrade before writes or deletions", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const first = join(f.directory, "seata-shared", "SKILL.md"), before = readFileSync(first);
  const obsolete = join(f.directory, "seata-shared", "references", "evidence.md"); writeFileSync(obsolete, "USER EDIT");
  const revised = revisedBundle(f.home, false, true), result = f.run(revised.cli);
  assert.equal(result.code, 8, result.stderr);
  assert.equal(readFileSync(obsolete, "utf8"), "USER EDIT");
  assert.deepEqual(readFileSync(first), before);
  assert.deepEqual(installationRecord(f), initial.data.data.manifest);
});

function removalRenameFault(mode, suffix = "/evidence.md") {
  const bootstrap = `import fs from 'node:fs'; import { syncBuiltinESMExports } from 'node:module';
    const rename = fs.renameSync;
    fs.renameSync = function(source, destination) {
      const removal = String(destination).endsWith('/payload') && String(source).endsWith(${JSON.stringify(suffix)});
      if (removal && ${JSON.stringify(mode)} === 'replace') {
        rename(source, source + '.user-moved');
        fs.writeFileSync(source, 'USER REPLACEMENT', {mode: 0o600});
      }
      const result = rename(source, destination);
      if (removal && ${JSON.stringify(mode)} === 'kill') process.kill(process.pid, 'SIGKILL');
      return result;
    }; syncBuiltinESMExports();`;
  return ["--import", "data:text/javascript," + encodeURIComponent(bootstrap)];
}

for (const point of ["removing", "captured", "removed"]) test(`obsolete resource removal resumes after SIGKILL at ${point}`, () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const revised = revisedBundle(f.home, false, true);
  const killed = f.run(revised.cli, point === "captured" ? removalRenameFault("kill") : killAfterRecord(point, true));
  assert.equal(killed.signal, "SIGKILL");
  const interrupted = installationRecord(f); assert.equal(interrupted.phase, "writing");
  const resumed = f.run(revised.cli); assert.equal(resumed.code, 0, resumed.stderr);
  assert.equal(resumed.data.data.manifest.operation_id, interrupted.operation_id);
  assert.equal(resumed.data.data.host_status.content_installed, true);
  assert.deepEqual(readdirSync(join(f.directory, "seata-shared", "references")), []);
});

test("a replacement raced into the removal source is restored rather than deleted", () => {
  const f = fixture(), initial = f.run(); assert.equal(initial.code, 0, initial.stderr);
  const revised = revisedBundle(f.home, false, true), result = f.run(revised.cli, removalRenameFault("replace"));
  assert.equal(result.code, 8, result.stderr);
  const source = join(f.directory, "seata-shared", "references", "evidence.md");
  assert.equal(readFileSync(source, "utf8"), "USER REPLACEMENT");
  assert.equal(existsSync(source + ".user-moved"), true);
  assert.equal(installationRecord(f).phase, "writing");
});
