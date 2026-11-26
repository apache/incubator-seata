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
import { dirname, resolve } from "node:path";
import type { Invocation } from "../command/registry.js";
import { type Obj, str, digest } from "../protocol/value.js";
import { CliError, PartialResult } from "../protocol/error.js";
import { ConfigDirectory, ConfigFiles } from "../adapter/config/files.js";
import { environmentFacts, environmentKeys, loadConfigFacts, label } from "./config.js";

export const inspectImplemented = new Set(["setup.inspect", "environment.inspect"]);
const filename = /^(?:application|bootstrap|file|registry|seata|seata-ai|seata-ai-profile)(?:-[A-Za-z0-9_.-]+)?\.(?:properties|ya?ml)$/;
const relativeDirectories = [".", "config", "src/main/resources"];
const limitations = ["local_observation_not_remote_JVM_configuration", "configuration_precedence_not_verified",
  "known_configuration_filenames_only", "no_network_requests", "no_target_trust_granted"];

/** Explicit root plus bounded parent roots, never recursively scanning siblings
 * or loading project code. Missing optional folders are not failed sources.
 */
export function inspectLocal(rootInput: string, parentDepth: number, signal: AbortSignal): Obj {
  const root = resolve(rootInput);
  if (root === "/") throw new CliError("config", "unsafe_config_path", "The filesystem root is not an inspection root.");
  // Validate even an empty root before returning environment-only evidence.
  const anchor = new ConfigDirectory(root, signal); anchor.close();
  const roots = [root];
  for (let depth = 0; depth < parentDepth; depth++) {
    const parent = dirname(roots.at(-1)!);
    if (parent === "/" || parent === roots.at(-1)) break;
    roots.push(parent);
  }
  const files = new ConfigFiles(), facts = environmentFacts(), sources: Obj[] = [], visited = new Set<string>();
  let evidenceBytes = Buffer.byteLength(JSON.stringify(facts)), truncated = false, failed = false, limitReason: string | null = null;
  const directories: string[] = [];
  outer: for (const current of roots) {
    for (const relative of relativeDirectories) {
      signal.throwIfAborted();
      const path = resolve(current, relative);
      if (visited.has(path)) continue;
      visited.add(path); directories.push(label(path));
      let directory: ConfigDirectory;
      try { directory = new ConfigDirectory(path, signal); }
      catch (error) {
        if (signal.aborted) signal.throwIfAborted();
        if (error instanceof CliError && error.subtype === "config_directory_missing") continue;
        failed = true; sources.push({ source: label(path), status: "unavailable", reason_code: error instanceof CliError ? error.subtype : "config_source_unavailable", bytes: 0 });
        continue;
      }
      let entries: ReturnType<ConfigDirectory["list"]>;
      try { entries = directory.list(signal); }
      catch {
        signal.throwIfAborted(); failed = true;
        sources.push({ source: label(path), status: "unavailable", reason_code: "config_directory_unavailable", bytes: 0 });
        continue;
      }
      finally { directory.close(); }
      if (entries.truncated) { truncated = true; limitReason = "directory_entry_limit"; }
      for (const name of entries.names.filter(name => filename.test(name))) {
        if (files.usage.files >= 200) { truncated = true; limitReason = "file_count_limit"; break outer; }
        const source = resolve(path, name), before = files.usage.bytes;
        try {
          const observed = loadConfigFacts({ path: source }, files, signal), bytes = Buffer.byteLength(JSON.stringify(observed));
          if (facts.length + observed.length > 10000 || evidenceBytes + bytes > 8 * 1024 * 1024) {
            truncated = true; limitReason = "evidence_limit";
            sources.push({ source: label(source), status: "truncated", reason_code: "evidence_limit", bytes: files.usage.bytes - before });
            break outer;
          }
          facts.push(...observed); evidenceBytes += bytes;
          sources.push({ source: label(source), status: "observed", reason_code: "local_file_read", bytes: files.usage.bytes - before });
        } catch (error) {
          if (signal.aborted) signal.throwIfAborted();
          failed = true;
          const reason = error instanceof CliError ? error.subtype : "config_source_unavailable";
          sources.push({ source: label(source), status: "unavailable", reason_code: reason, bytes: files.usage.bytes - before });
          if (reason === "config_limit_exceeded") { truncated = true; limitReason = reason; break outer; }
        }
      }
    }
  }
  return { facts, sources, scan_scope: { root: label(root), roots: roots.map(label), directories, max_parent_depth: parentDepth,
    filename_pattern: filename.source, environment_allowlist: [...environmentKeys], network_requests: 0, ...files.usage },
    completeness: { status: truncated ? "truncated" : failed ? "partial" : "complete", scope: "declared_local_scan_only",
      limit_reason: limitReason, remote_effective_configuration: "unknown" }, limitations };
}

function endpointCandidate(f: Obj): Obj | undefined {
  const key = str(f.key);
  const explicitConsole = /^(?:management\.console_endpoint|seata\.ai\.console-endpoint|SEATA_AI_CONSOLE_ENDPOINT)$/.test(key);
  const explicitNaming = /^(?:management\.naming_endpoint|seata\.ai\.naming-endpoint|SEATA_AI_NAMING_ENDPOINT)$/.test(key);
  const registryNaming = /^(?:seata\.)?registry\.seata\.server-?addr$/i.test(key) || key === "SEATA_REGISTRY_SEATA_SERVER_ADDR";
  if (!explicitConsole && !explicitNaming && !registryNaming) return undefined;
  let address: string | null = null, reason: string | null = null;
  if (f.redacted || typeof f.value !== "string") reason = "redacted_or_unresolved_address";
  else {
    const raw = f.value;
    try {
      if (/[\u0000-\u0020\\?#]/.test(raw) || /%(?:2f|5c|2e|00|25)/i.test(raw) || /(?:^|\/)\.{1,2}(?:\/|$)/.test(raw)) throw new Error();
      const url = new URL(raw);
      if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.search || url.hash) throw new Error();
      address = url.href.replace(/\/$/, "");
    } catch { reason = "explicit_unambiguous_http_url_required"; }
  }
  return { candidate_id: digest([f.id!, explicitConsole ? "console" : "naming"]),
    endpoints: { console_endpoint: explicitConsole ? address : null, naming_endpoint: explicitConsole ? null : address },
    source: { evidence_id: f.id!, location: f.location!, key }, confidence: address ? "medium" : "low", trust_state: "unverified",
    reason_code: reason ?? "endpoint_literal_observed", limitations: ["not_contacted", "transport_policy_not_evaluated",
      "other_endpoint_not_inferred", ...(registryNaming ? ["registry_configuration_activation_not_verified"] : [])] };
}

export async function inspectCommand(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  const scan = inspectLocal(typeof inv.input.root === "string" ? inv.input.root : process.cwd(),
    inv.command === "setup.inspect" ? Number(inv.input.max_parent_depth ?? 3) : 0, signal);
  const facts = scan.facts as Obj[];
  const result: Obj = inv.command === "setup.inspect" ? {
    candidates: facts.flatMap(f => { const candidate = endpointCandidate(f); return candidate ? [candidate] : []; }),
    scan_scope: scan.scan_scope!, completeness: scan.completeness!, sources: scan.sources!, limitations,
  } : { environment_facts: facts, scope: { local_project_root: (scan.scan_scope as Obj).root!,
    observed_process: "cli_only", remote_process: null }, scan_scope: scan.scan_scope!, sources: scan.sources!, completeness: scan.completeness!, limitations };
  if ((scan.completeness as Obj).status !== "complete")
    throw new PartialResult(result, "local_inspection_incomplete", "Some local sources were unavailable or exceeded inspection limits; collected observations are retained.");
  return result;
}
