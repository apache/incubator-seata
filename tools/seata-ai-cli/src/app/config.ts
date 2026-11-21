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
import { lstatSync } from "node:fs";
import { extname, resolve } from "node:path";
import type { Invocation } from "../command/registry.js";
import { type Obj, object, str, digest, canonical } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { ConfigFiles } from "../adapter/config/files.js";
import { parseProperties, type ConfigEntry } from "../adapter/config/properties.js";
import { parseYaml } from "../adapter/config/yaml.js";
import { requireRuleset, ruleset } from "./analysis.js";

export const configImplemented = new Set(["config.inspect", "config.validate", "config.diff"]);
function bounded(result: Obj): Obj {
  if (Buffer.byteLength(JSON.stringify(result)) > 8 * 1024 * 1024)
    throw new CliError("config", "config_limit_exceeded", "Configuration output exceeds its inspection budget.");
  return result;
}
const limitations = ["local_observation_not_remote_JVM_configuration", "configuration_precedence_not_verified", "remote_process_overrides_not_observed"];
export const environmentKeys = ["SEATA_ENABLED", "SEATA_APPLICATION_ID", "SEATA_TX_SERVICE_GROUP", "SEATA_REGISTRY_TYPE",
  "SEATA_CONFIG_TYPE", "SEATA_SERVICE_GROUPLIST", "SEATA_NAMESPACE", "SEATA_CLUSTER", "SEATA_REGISTRY_SEATA_SERVER_ADDR",
  "SEATA_AI_CONSOLE_ENDPOINT", "SEATA_AI_NAMING_ENDPOINT"] as const;
const secretPattern = /password|passwd|secret|credential|authorization|bearer\s|cookie|private.?key|[^\s/]+:[^\s/@]+@|https?:\/\/[^\s]*[?#]|-----BEGIN/i;
export function label(value: string): string {
  const embeddedSecret = /(?:password|secret|token|authorization)[=:]|bearer\s|(?:https?|jdbc:[a-z0-9]+):\/\/[^/\s]*@|-----BEGIN/i;
  return value.length > 4096 || /[\x00-\x1f\x7f]/.test(value) || embeddedSecret.test(value) ? `[redacted:${digest(value)}]` : value;
}
function fact(entry: ConfigEntry, source: string, environment = false): Obj {
  const key = label(entry.key), placeholder = /\$\{|#\{/.test(entry.value);
  const include = /(?:^|\.)(?:import|include|location|additional-location)(?:\[\d+\])?$/i.test(entry.key);
  if (include && /(?:https?|ftp):\/\//i.test(entry.value))
    throw new CliError("config", "config_network_include_forbidden", "Network configuration includes are not permitted.");
  const knownSafe = environment || /(?:^|\.)(?:enabled|type|mode|application[-.]?id|tx[-.]?service[-.]?group|namespace|cluster|clusterName|timeout|port|host|serverAddr|server-addr|console[-_]endpoint|naming[-_]endpoint)$|(?:^|\.)service\.(?:vgroup-?Mapping|grouplist)\./i.test(entry.key);
  const sensitive = secretPattern.test(entry.key) || secretPattern.test(entry.value) || placeholder || !knownSafe ||
    /[\x00-\x1f\x7f]/.test(entry.value) || entry.value.length > 4096;
  const unresolved = placeholder ? "expression_not_evaluated" : include ? "include_not_loaded" :
    entry.document !== undefined ? "yaml_document_activation_not_verified" : null;
  const safeSource = label(source), location = environment ? safeSource : `${safeSource}:${entry.line}`;
  return { id: digest([safeSource, key, entry.line, entry.document ?? null]), key, value: sensitive || entry.value_kind === "null" ? null : entry.value,
    value_kind: entry.value_kind ?? "string",
    source: safeSource, location, scope: environment ? "local_cli_process" : "local_project", target_process: null,
    evidence_kind: environment ? "observed_environment" : "observed_file", precedence: null, effective_status: "unknown",
    version: null, document: entry.document ?? null, sensitive, redacted: sensitive, unresolved_reason: unresolved,
    limitations: [...limitations, ...(unresolved ? [unresolved] : []), ...(!knownSafe ? ["unclassified_value_redacted"] : [])] };
}
export function environmentFacts(): Obj[] {
  return environmentKeys.flatMap(key => {
    const value = process.env[key];
    return value === undefined ? [] : [fact({ key, value, line: 0 }, key, true)];
  });
}
function defaults(): Obj[] {
  const result: Obj[] = [];
  for (const directory of [".", "config", "src/main/resources"]) {
    for (const name of ["application.yml", "application.yaml", "application.properties", "file.properties", "registry.properties"]) {
      const path = resolve(directory, name);
      try { lstatSync(path); result.push({ path }); }
      catch (error) { if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw new CliError("config", "config_source_unavailable", "A default configuration source cannot be inspected."); }
    }
  }
  return result;
}
export function loadConfigFacts(descriptor: Obj, files: ConfigFiles, signal: AbortSignal): Obj[] {
  const path = str(descriptor.path), extension = extname(path).toLowerCase();
  const format = descriptor.format ?? (extension === ".properties" ? "properties" : [".yaml", ".yml"].includes(extension) ? "yaml" : "unsupported");
  if (format !== "properties" && format !== "yaml") throw new CliError("config", "format_unsupported", "Only YAML and Java properties data are supported.");
  const file = files.read(path, signal);
  let source: string;
  try { source = new TextDecoder("utf-8", { fatal: true }).decode(file.bytes); }
  catch { throw new CliError("config", "config_encoding_unsupported", "Configuration files must use UTF-8; legacy Java encodings require an explicit conversion."); }
  const entries = format === "properties" ? parseProperties(source) : parseYaml(source);
  return entries.map(entry => fact(entry, file.path));
}
function inspect(input: Obj, signal: AbortSignal): Obj {
  const files = new ConfigFiles(), descriptors = Array.isArray(input.sources) ? input.sources.map(object) : defaults();
  const observed = descriptors.flatMap(d => loadConfigFacts(d, files, signal));
  if (input.include_environment !== false) observed.push(...environmentFacts());
  if (observed.length > 10000 || Buffer.byteLength(JSON.stringify(observed)) > 8 * 1024 * 1024)
    throw new CliError("config", "config_limit_exceeded", "Configuration fact output exceeds its inspection budget.");
  return { observed, inferred_effective: [], shadowed: [], unresolved: observed.filter(f => f.unresolved_reason !== null),
    limitations, scan_scope: { sources: descriptors.map(d => label(resolve(str(d.path)))), environment_allowlist: [...environmentKeys],
      include_environment: input.include_environment !== false, ...files.usage },
    completeness: { status: "complete", scope: "selected_local_sources_only", remote_effective_configuration: "unknown" } };
}
export async function configCommand(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  signal.throwIfAborted();
  if (inv.command === "config.diff") {
    const files = new ConfigFiles(), left = loadConfigFacts(object(inv.input.left), files, signal), right = loadConfigFacts(object(inv.input.right), files, signal);
    const index = (facts: Obj[]) => new Map(facts.map(f => [canonical([f.key ?? null, f.document ?? null]), f]));
    const a = index(left), b = index(right);
    const changes = [...new Set([...a.keys(), ...b.keys()])].sort().map(id => {
      const l = a.get(id), r = b.get(id);
      const compare = (field: string): string => !l || !r ? "absent" : l[field] === null || r[field] === null ? "unknown" :
        canonical(l[field] ?? null) === canonical(r[field] ?? null) ? "equal" : "different";
      return { key: (l ?? r)!.key!, document: (l ?? r)!.document!, left: l ?? null, right: r ?? null,
        value: !l || !r ? "absent" : l.redacted || r.redacted || l.unresolved_reason === "expression_not_evaluated" || r.unresolved_reason === "expression_not_evaluated" ? "unknown" :
          compare("value") === "equal" && compare("value_kind") !== "equal" ? "different" : compare("value"),
        source: compare("source"), scope: compare("scope"), version: compare("version"), evidence_level: compare("evidence_kind"),
        effective_comparison: "unknown" };
    });
    if (changes.length > 10000) throw new CliError("config", "config_limit_exceeded", "Configuration comparison exceeds its entity budget.");
    return bounded({ changes, limitations });
  }
  if (inv.command === "config.validate") requireRuleset(inv.input.ruleset_id);
  const result = inspect({ ...inv.input, ...(inv.command === "config.validate" ? { include_environment: false } : {}) }, signal);
  if (inv.command === "config.inspect") return bounded(result);
  const facts = result.observed as Obj[], findings: Obj[] = [];
  for (const f of facts) {
    if (f.unresolved_reason) findings.push({ rule_id: "config_resolution", status: "unknown", evidence_ids: [f.id!], reason_code: f.unresolved_reason });
    if (/(?:^|\.)(?:enabled)$/i.test(str(f.key)) && f.redacted === false && typeof f.value === "string")
      findings.push({ rule_id: "config_boolean_literal", status: ["true", "false"].includes(str(f.value).toLowerCase()) ? "passed" : "failed",
        evidence_ids: [f.id!], reason_code: "local_literal_check_only" });
  }
  const groups = facts.filter(f => /(?:^|\.)tx[-.]?service[-.]?group$/i.test(str(f.key)) && typeof f.value === "string" && !f.redacted);
  for (const group of groups) {
    const mappings = facts.filter(f => {
      const match = /(?:^|\.)service\.vgroup-?Mapping\.(.+)$/i.exec(str(f.key));
      return match?.[1] === group.value && f.document === group.document && typeof f.value === "string" && !f.redacted;
    });
    const values = new Set(mappings.map(f => f.value));
    findings.push({ rule_id: "local_vgroup_mapping", status: values.size === 1 ? "passed" : "unknown",
      evidence_ids: [group.id!, ...mappings.map(f => f.id!)], reason_code: values.size === 1 ? "local_mapping_observed_remote_route_not_checked" :
        values.size === 0 ? "mapping_not_observed_in_selected_sources" : "conflicting_observations_precedence_unknown" });
  }
  findings.push({ rule_id: "remote_effective_configuration", status: "not_evaluated", evidence_ids: [], reason_code: "remote_runtime_configuration_not_observed" });
  return bounded({ findings, evidence: facts, ruleset_version: ruleset, completeness: result.completeness!, limitations });
}
