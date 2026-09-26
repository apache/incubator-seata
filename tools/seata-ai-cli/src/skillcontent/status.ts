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
import { homedir } from "node:os";
import { join, resolve } from "node:path";
import { createHash } from "node:crypto";
import { type Obj, str } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { ConfigDirectory, ConfigFiles } from "../adapter/config/files.js";
import { skillCatalog } from "./catalog.js";

export const skillVersion = "0.1.0-r11.0";
export function skillsStatus(input: Obj, signal: AbortSignal,
  paths = { directory: join(homedir(), ".agents", "skills") }): Obj {
  signal.throwIfAborted();
  if (input.host_id !== undefined && input.host_id !== "generic_agent_skills")
    throw new CliError("policy", "host_adapter_unavailable", "Only the declared generic_agent_skills directory adapter is available.");
  const findings: Obj[] = [], catalog = skillCatalog(), reader = new ConfigFiles();
  for (const skill of catalog) {
    const id = str(skill.skill_id), files = skill.files as Obj[];
    try {
      // Inspect only our named directories; unrelated user Skills are neither
      // validated nor traversed. Reject extra content inside a managed Skill.
      for (const section of ["", "references", "examples", "tests"]) {
        const dir = new ConfigDirectory(join(paths.directory, id, section), signal);
        try {
          const listed = dir.list(signal);
          const allowed = section === "" ? ["SKILL.md", "references", "examples", "tests"] :
            files.filter(f => str(f.path).startsWith(section + "/")).map(f => str(f.path).slice(section.length + 1));
          if (listed.truncated || listed.names.some(name => !allowed.includes(name))) throw new Error("extra content");
        } finally { dir.close(); }
      }
      for (const file of files) {
        const bytes = reader.read(join(paths.directory, id, str(file.path)), signal).bytes;
        if (bytes.length !== file.bytes || createHash("sha256").update(bytes).digest("hex") !== file.digest)
          throw new Error("changed content");
      }
    } catch {
      signal.throwIfAborted();
      findings.push({ code: "skill_missing_modified_or_unsafe", skill_id: id });
    }
  }
  const content = findings.length === 0;
  return { content_installed: content, exposure_valid: content,
    host_discovered: "unknown", session_loaded: "unknown",
    versions: { cli: skillVersion, bundled_skills: skillVersion, installed_skills: content ? skillVersion : null },
    host: { id: "generic_agent_skills", version: null, support: "directory_only_unverified_host" },
    ownership: "not_verified", installation_path: resolve(paths.directory), installation_mode: "direct",
    last_verified_at: new Date().toISOString(), findings,
    limitations: ["content_checks_only", "no_installation_ownership_claim", "host_discovery_not_observable", "session_loading_not_observable"] };
}
