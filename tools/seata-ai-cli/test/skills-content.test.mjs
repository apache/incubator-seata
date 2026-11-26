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
import { readFileSync, mkdtempSync, existsSync, mkdirSync, cpSync, writeFileSync, symlinkSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { skillCatalog, readSkill } from "../dist/skillcontent/catalog.js";
import { validateOutput } from "../dist/command/registry.js";
import { validateSkills, exportSkills } from "../dist/app/skills.js";
import { ExportFile } from "../dist/adapter/export/file.js";
import { skillsStatus } from "../dist/skillcontent/status.js";

const root = new URL("../", import.meta.url);
const hash = bytes => createHash("sha256").update(bytes).digest("hex");
test("one lightweight Skill has source-bound digests and opt-in investigation guidance", () => {
  const catalog = skillCatalog();
  assert.deepEqual(catalog.map(s => s.skill_id), ["seata-ai-cli"]);
  validateOutput("skills.list", { skills: catalog });
  let entryBytes = 0;
  for (const skill of catalog) {
    assert.equal(hash(JSON.stringify(skill.files)), skill.digest);
    assert.deepEqual(skill.files.map(f => f.path), ["SKILL.md", "references/evidence.md"]);
    for (const file of skill.files) {
      const source = readFileSync(new URL(`skills/${skill.skill_id}/${file.path}`, root));
      assert.equal(source.length, file.bytes); assert.equal(hash(source), file.digest);
    }
    for (const section of ["main", "references"]) {
      const result = readSkill({ skill_id: skill.skill_id, section });
      validateOutput("skills.read", result);
      assert.equal(hash(result.content), result.digest);
      if (section === "main") {
        // Authoring guard for our bundled entrypoints, not a user-Skill limit.
        // Detailed references stay opt-in instead of growing default context.
        const bytes = Buffer.byteLength(result.content, "utf8");
        entryBytes += bytes;
        assert.ok(result.content.split("\n").length <= 30, `${skill.skill_id}: keep the entrypoint short`);
        assert.ok(bytes <= 2500, `${skill.skill_id}: move optional detail out of the entrypoint`);
      }
    }
  }
  assert.ok(entryBytes <= 2500, "The entrypoint must remain lightweight");
  const registry = JSON.parse(readFileSync(new URL("api/protocol/registry.json", root)));
  for (const command of registry) {
    if (command.skills !== "—") assert.equal(command.skills, "seata-ai-cli", command.id);
  }
});

test("Skill reads reject traversal and unlisted resources without filesystem fallback", () => {
  for (const path of ["../SKILL.md", "/etc/passwd", "..\\SKILL.md", "a/../../SKILL.md", "./evidence.md", "a//b", "%2e%2e/passwd"]) {
    assert.throws(() => readSkill({ skill_id: "seata-ai-cli", section: "references", relative_path: path }), e => e.type === "validation");
  }
  assert.throws(() => readSkill({ skill_id: "seata-ai-cli", section: "references", relative_path: "missing.md" }), e => e.type === "not_found");
  assert.throws(() => readSkill({ skill_id: "unknown" }), e => e.type === "not_found");
  assert.throws(() => readSkill({ skill_id: "seata-ai-cli", section: "examples" }), e => e.subtype === "skill_resource_not_found");
  assert.throws(() => readSkill({ skill_id: "seata-ai-cli", section: "main", relative_path: "references/evidence.md" }), e => e.type === "validation");
});

test("typed and canonical Skill commands agree outside package cwd without StateStore writes", () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-")), state = join(temp, "state");
  const run = (args, input) => {
    const result = spawnSync(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, ...args], {
      cwd: temp, encoding: "utf8", input, env: { ...process.env, SEATA_AI_STATE_DIR: state }, timeout: 15000,
    });
    assert.equal(result.status, 0, result.stderr); assert.equal(result.stderr, "");
    assert.equal(existsSync(state), false);
    return JSON.parse(result.stdout).data;
  };
  assert.equal(run(["skills", "list"]).skills.length, 1);
  const typed = run(["skills", "read", "--skill-id", "seata-ai-cli", "--section", "references"]);
  const canonical = run(["invoke"], JSON.stringify({ protocol_version: "0.1", contract_revision: "R9", command: "skills.read",
    input: { skill_id: "seata-ai-cli", section: "references" }, context: {}, options: {} }));
  assert.deepEqual(typed, canonical);
});

test("bundled and custom Skill validation checks exact contents without executing or exposing them", () => {
  const signal = new AbortController().signal;
  const bundled = validateSkills({}, signal);
  validateOutput("skills.validate", bundled);
  assert.equal(bundled.valid, true); assert.equal(bundled.files_checked, 2);
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-validation-"));
  cpSync(new URL("skills/seata-ai-cli", root), join(temp, "seata-ai-cli"), { recursive: true });
  const original = validateSkills({ root: temp }, signal);
  assert.equal(original.valid, true); assert.equal(original.skills_checked, 1);
  const path = join(temp, "seata-ai-cli", "SKILL.md");
  writeFileSync(path, "SECRET-MARKER: execute arbitrary shell\n");
  const changed = validateSkills({ root: temp }, signal);
  assert.equal(changed.valid, false); assert.equal(changed.validated_version, null);
  assert.ok(changed.findings.some(f => f.code === "content_mismatch"));
  assert.doesNotMatch(JSON.stringify(changed), /SECRET-MARKER|arbitrary shell/);
  assert.equal(readFileSync(path, "utf8"), "SECRET-MARKER: execute arbitrary shell\n");
  writeFileSync(join(temp, "seata-ai-cli", "unexpected"), "private");
  assert.ok(validateSkills({ root: temp }, signal).findings.some(f => f.code === "unexpected_skill_entries"));
});

test("custom validation refuses symlinks and bounds inspection instead of following extra paths", () => {
  const signal = new AbortController().signal, temp = mkdtempSync(join(tmpdir(), "seata-skills-links-"));
  symlinkSync(new URL("skills/seata-ai-cli", root).pathname, join(temp, "seata-ai-cli"));
  const result = validateSkills({ root: temp }, signal);
  assert.equal(result.valid, false);
  assert.ok(result.findings.some(f => f.code === "skill_directory_unreadable"));
  assert.equal(result.files_checked, 0);
  assert.throws(() => validateSkills({ root: "/" }, signal), e => e.subtype === "unsafe_skill_root");
  const abort = new AbortController(); abort.abort();
  assert.throws(() => validateSkills({}, abort.signal), e => e.name === "AbortError");
});

test("Skill export round-trips all resources and requires unchanged owned files for overwrite", async () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-export-")), state = join(temp, "state");
  const oldState = process.env.SEATA_AI_STATE_DIR;
  process.env.SEATA_AI_STATE_DIR = state;
  const signal = new AbortController().signal;
  const inv = { command: "skills.export", input: { root: temp, path: "exported" }, context: {}, options: { timeout_ms: 120000 } };
  try {
    const result = await exportSkills(inv, signal);
    validateOutput("skills.export", result);
    assert.equal(result.files.length, 3); assert.equal(result.export_manifest.complete, true);
    assert.equal(validateSkills({ root: join(temp, "exported") }, signal).valid, true);
    assert.equal(result.export_manifest.host_discovered, "unknown");
    await assert.rejects(exportSkills(inv, signal), e => e.subtype === "export_exists");
    inv.input.overwrite = true;
    assert.equal((await exportSkills(inv, signal)).files.length, 3);
    const changed = join(temp, "exported", "seata-ai-cli", "SKILL.md");
    writeFileSync(changed, "USER-EDIT");
    await assert.rejects(exportSkills(inv, signal), e => e.type === "policy");
    assert.equal(readFileSync(changed, "utf8"), "USER-EDIT");
  } finally {
    if (oldState === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = oldState;
  }
});

test("export preflight leaves earlier payloads unwritten when a later unowned file conflicts", async () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-conflict-")), state = join(temp, "state");
  const oldState = process.env.SEATA_AI_STATE_DIR; process.env.SEATA_AI_STATE_DIR = state;
  const output = join(temp, "exported"), skill = join(output, "seata-ai-cli");
  mkdirSync(join(skill, "references"), { recursive: true, mode: 0o700 });
  writeFileSync(join(skill, "references", "evidence.md"), "USER-FILE", { mode: 0o600 });
  const inv = { command: "skills.export", input: { root: temp, path: "exported", skill_id: "seata-ai-cli", overwrite: true }, context: {}, options: { timeout_ms: 30000 } };
  try {
    await assert.rejects(exportSkills(inv, new AbortController().signal), e => e.subtype === "export_not_owned");
    assert.equal(existsSync(join(skill, "SKILL.md")), false);
    assert.equal(readFileSync(join(skill, "references", "evidence.md"), "utf8"), "USER-FILE");
    assert.equal(existsSync(join(output, "seata-skills-manifest.json")), false);
    inv.input.path = "../outside";
    await assert.rejects(exportSkills(inv, new AbortController().signal), e => e.subtype === "unsafe_export_path");
  } finally {
    if (oldState === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = oldState;
  }
});

test("interrupted multi-file export reports completed files without a complete manifest or rollback claim", async () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-partial-"));
  const oldState = process.env.SEATA_AI_STATE_DIR; process.env.SEATA_AI_STATE_DIR = join(temp, "state");
  const original = ExportFile.prototype.write;
  let writes = 0;
  ExportFile.prototype.write = async function(bytes, signal) {
    if (++writes === 2) throw new Error("synthetic write interruption");
    return original.call(this, bytes, signal);
  };
  const inv = { command: "skills.export", input: { root: temp, path: "exported", skill_id: "seata-ai-cli" }, context: {}, options: { timeout_ms: 30000 } };
  try {
    await assert.rejects(exportSkills(inv, new AbortController().signal), error => {
      assert.equal(error.type, "partial_failure"); assert.equal(error.exitCode, 9);
      validateOutput("skills.export", error.data);
      assert.equal(error.data.files.length, 1); assert.equal(error.data.export_manifest.complete, false);
      for (const file of error.data.files) assert.equal("sha256:" + hash(readFileSync(file.path)), file.digest);
      return true;
    });
    assert.equal(existsSync(join(temp, "exported", "seata-skills-manifest.json")), false);
    assert.equal(validateSkills({ root: join(temp, "exported") }, new AbortController().signal).valid, false);
  } finally {
    ExportFile.prototype.write = original;
    if (oldState === undefined) delete process.env.SEATA_AI_STATE_DIR;
    else process.env.SEATA_AI_STATE_DIR = oldState;
  }
});

test("public CLI exports a selected Skill and validates it in typed and canonical forms", () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skills-public-")), state = join(temp, "state");
  const run = (args, input) => {
    const r = spawnSync(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, ...args], {
      cwd: temp, encoding: "utf8", input, env: { ...process.env, SEATA_AI_STATE_DIR: state }, timeout: 20000,
    });
    assert.equal(r.status, 0, r.stderr); assert.equal(r.stderr, "");
    return JSON.parse(r.stdout).data;
  };
  assert.equal(run(["skills", "validate"]).valid, true);
  assert.equal(existsSync(state), false);
  const result = run(["skills", "export", "--skill-id", "seata-ai-cli", "--root", temp, "--path", "out"]);
  validateOutput("skills.export", result); assert.equal(result.files.length, 3);
  const typed = run(["skills", "validate", "--root", join(temp, "out")]);
  const canonical = run(["invoke"], JSON.stringify({ protocol_version: "0.1", contract_revision: "R9",
    command: "skills.validate", input: { root: join(temp, "out") }, context: {}, options: {} }));
  assert.deepEqual(typed, canonical); assert.equal(typed.valid, true);
});

test("Skill status is read-only and separates installed bytes and valid exposure from host discovery", () => {
  const temp = mkdtempSync(join(tmpdir(), "seata-skill-status-")), signal = new AbortController().signal;
  const paths = { directory: join(temp, "user-skills") };
  const absent = skillsStatus({}, signal, paths);
  validateOutput("skills.status", absent);
  assert.equal(absent.content_installed, false); assert.equal(absent.exposure_valid, false);
  assert.equal(existsSync(paths.directory), false);
  cpSync(new URL("skills/", root), paths.directory, { recursive: true });
  mkdirSync(join(paths.directory, "unrelated-user-skill"));
  writeFileSync(join(paths.directory, "unrelated-user-skill", "SKILL.md"), "USER CONTENT");
  const exposed = skillsStatus({}, signal, paths);
  validateOutput("skills.status", exposed);
  assert.equal(exposed.content_installed, true); assert.equal(exposed.exposure_valid, true);
  assert.equal(exposed.ownership, "not_verified");
  assert.equal(exposed.host_discovered, "unknown"); assert.equal(exposed.session_loaded, "unknown");
  assert.equal(exposed.installation_path, paths.directory);
  assert.equal(exposed.installation_mode, "direct");
  writeFileSync(join(paths.directory, "seata-ai-cli", "SKILL.md"), "USER CHANGED CONTENT");
  const changed = skillsStatus({}, signal, paths);
  assert.equal(changed.content_installed, false); assert.equal(changed.exposure_valid, false);
  assert.equal(changed.versions.installed_skills, null);
  assert.equal(readFileSync(join(paths.directory, "seata-ai-cli", "SKILL.md"), "utf8"), "USER CHANGED CONTENT");
  assert.throws(() => skillsStatus({ host_id: "unregistered_host" }, signal, paths), e => e.subtype === "host_adapter_unavailable");
});
