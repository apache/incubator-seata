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
import { createHash } from "node:crypto";
import { resource } from "../resources.js";
import { object, parseJson, str, type Obj } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";

export function skillCatalog(): Obj[] {
  const manifest = object(parseJson(resource("api/skills.json")));
  if (!Array.isArray(manifest.skills)) throw new CliError("internal", "contract_violation", "Invalid Skill manifest.");
  return manifest.skills.map(object);
}

export function readSkill(input: Obj): Obj {
  const id = str(input.skill_id), skill = skillCatalog().find(s => s.skill_id === id);
  if (!skill) throw new CliError("not_found", "skill_not_found", "The requested Skill is not bundled.");
  const section = input.section === undefined ? "main" : str(input.section);
  const path = input.relative_path === undefined ?
    (section === "main" ? "SKILL.md" : section === "references" ? "evidence.md" : "scenario.md") : str(input.relative_path);
  if (!/^[a-zA-Z0-9_.-]+(?:\/[a-zA-Z0-9_.-]+)*$/.test(path) || path.split("/").some(p => p === "." || p === "..") ||
      !["main", "references", "examples"].includes(section) || (section === "main" && path !== "SKILL.md"))
    throw new CliError("validation", "invalid_skill_path", "Use a listed path relative to the selected Skill section.");
  const relative = section === "main" ? path : `${section}/${path}`;
  const files = Array.isArray(skill.files) ? skill.files.map(object) : [];
  if (!files.some(f => f.path === relative)) throw new CliError("not_found", "skill_resource_not_found", "The requested Skill resource is not bundled.");
  const bytes = resource(`skills/${id}/${relative}`);
  return { skill_id: id, version: skill.version!, section, relative_path: path,
    content: bytes.toString("utf8"), digest: createHash("sha256").update(bytes).digest("hex") };
}
