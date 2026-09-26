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
import { resolve, join, parse } from "node:path";
import { createHash } from "node:crypto";
import type { Invocation } from "../command/registry.js";
import { type Obj, object, str, canonical } from "../protocol/value.js";
import { CliError, PartialResult } from "../protocol/error.js";
import { resource } from "../resources.js";
import { skillCatalog } from "../skillcontent/catalog.js";
import { ConfigDirectory, ConfigFiles } from "../adapter/config/files.js";
import { ExportFile } from "../adapter/export/file.js";
import { StateStore } from "../adapter/statestore/client.js";

const hash = (bytes: Buffer) => createHash("sha256").update(bytes).digest("hex");
export const skillFilesystem = new Set(["skills.export", "skills.validate", "skills.status"]);
const manifestName = "seata-skills-manifest.json";
function manifestFor(skills: Obj[]): Obj {
  return { manifest_version: "1", cli_version: "0.1.0-r11.0", skills,
    scope: "read_only", ownership: "not_established_by_manifest", host_discovered: "unknown", session_loaded: "unknown" };
}
export function validateSkills(input: Obj, signal: AbortSignal): Obj {
  signal.throwIfAborted();
  const catalog = skillCatalog(), findings: Obj[] = [], reader = new ConfigFiles();
  let selected = catalog;
  const root = input.root === undefined ? undefined : resolve(str(input.root));
  const add = (code: string, skill: string | null = null, path: string | null = null) =>
    findings.push({ code, skill_id: skill, relative_path: path });
  function names(path: string): string[] {
    const dir = new ConfigDirectory(path, signal);
    try {
      const result = dir.list(signal);
      if (result.truncated) add("directory_limit_exceeded");
      return result.names;
    } finally { dir.close(); }
  }
  if (root) {
    if (root === parse(root).root) throw new CliError("validation", "unsafe_skill_root", "Select a dedicated Skills directory, not a filesystem root.");
    const entries = names(root);
    selected = catalog.filter(s => entries.includes(str(s.skill_id)));
    if (!selected.length) add("skills_missing");
    if (entries.some(n => n !== manifestName && !catalog.some(s => s.skill_id === n))) add("unexpected_root_entries");
    if (entries.includes(manifestName)) {
      try {
        const bytes = reader.read(join(root, manifestName), signal).bytes;
        if (!bytes.equals(Buffer.from(canonical(manifestFor(selected)) + "\n"))) add("manifest_mismatch");
      } catch (error) {
        signal.throwIfAborted();
        if (!(error instanceof CliError)) throw error;
        add("manifest_unreadable");
      }
    }
  }
  let checked = 0;
  for (const skill of selected) {
    const id = str(skill.skill_id), files = (skill.files as Obj[]).map(object);
    if (root) {
      try {
        const entryNames = names(join(root, id));
        if (entryNames.some(n => !["SKILL.md", "references", "examples", "tests"].includes(n))) add("unexpected_skill_entries", id);
        for (const section of ["references", "examples", "tests"]) {
          const allowed = files.filter(f => str(f.path).startsWith(section + "/")).map(f => str(f.path).slice(section.length + 1));
          if (names(join(root, id, section)).some(n => !allowed.includes(n))) add("unexpected_section_entries", id, section);
        }
      } catch (error) {
        signal.throwIfAborted();
        if (!(error instanceof CliError)) throw error;
        add("skill_directory_unreadable", id);
      }
    }
    for (const file of files) {
      signal.throwIfAborted();
      const path = str(file.path);
      try {
        const bytes = root ? reader.read(join(root, id, path), signal).bytes : resource(`skills/${id}/${path}`);
        checked++;
        if (bytes.length !== file.bytes || hash(bytes) !== file.digest) add("content_mismatch", id, path);
      } catch (error) {
        signal.throwIfAborted();
        if (!(error instanceof CliError)) throw error;
        add("resource_unreadable", id, path);
      }
    }
  }
  const valid = findings.length === 0;
  return { valid, findings, validated_version: valid ? "0.1.0-r11.0" : null,
    expected_version: "0.1.0-r11.0", validation_scope: "bundled_content_integrity",
    source: root ? "custom_directory" : "package", skills_checked: selected.length, files_checked: checked,
    host_discovered: "unknown", session_loaded: "unknown" };
}

export async function exportSkills(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  const selected = skillCatalog().filter(s => inv.input.skill_id === undefined || s.skill_id === inv.input.skill_id);
  if (!selected.length) throw new CliError("not_found", "skill_not_found", "The requested Skill is not bundled.");
  const manifest = manifestFor(selected);
  const payloads: { path: string; bytes: Buffer }[] = selected.flatMap(s =>
    (s.files as Obj[]).map(f => ({ path: str(s.skill_id) + "/" + str(f.path),
      bytes: resource(`skills/${s.skill_id}/${f.path}`) })));
  payloads.push({ path: manifestName, bytes: Buffer.from(canonical(manifest) + "\n") });
  const targets: ExportFile[] = [], written: Obj[] = [];
  const store = new StateStore(undefined, Number(inv.options.timeout_ms ?? 30000));
  try {
    // Prepare every destination before modifying any payload. The manifest is
    // written last; it reports content, never authority to overwrite a file.
    for (const p of payloads) {
      signal.throwIfAborted();
      const target = new ExportFile(str(inv.input.root), str(inv.input.path) + "/" + p.path,
        inv.input.overwrite === true, undefined, Number(inv.options.timeout_ms ?? 30000), store);
      targets.push(target);
      await target.prepare(signal);
    }
    for (let i = 0; i < targets.length; i++) {
      const result = await targets[i]!.write(payloads[i]!.bytes, signal);
      written.push({ ...result, relative_path: payloads[i]!.path });
    }
    return { export_manifest: { ...manifest, complete: true }, files: written };
  } catch (error) {
    if (written.length) throw new PartialResult({ export_manifest: { ...manifest, complete: false }, files: written },
      "skills_export_partial", "Only the listed files completed export. No files were removed; do not treat this directory as a complete installation.");
    throw error;
  } finally {
    // One worker owns all per-file lifetime locks until the entire batch closes.
    await Promise.allSettled(targets.map(t => t.close()));
    await store.close();
  }
}
