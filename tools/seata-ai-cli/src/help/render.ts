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
import { registry } from "../command/registry.js";
import { object } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { deferredWrite } from "../command/scope.js";

export function renderHelp(path: string[], field?: string): string {
  if (path.length === 0)
    return (
      [
        "Seata AI CLI — R11 development snapshot (protocol 0.1 / R9)",
        "",
        "Usage: seata-ai <command> [options]",
        "",
        "Framework: version, help, schema, capabilities, invoke",
        "Domains:",
        ...new Set(
          registry
            .filter((s) => !s.id.startsWith("system."))
            .map((s) => "  " + s.path[0]),
        ),
        "",
        "Start with command --help; request exact fields with schema <command-id>.",
        "JSON invocation: seata-ai invoke < invocation.json",
        "Use an explicit --profile for management commands.",
        "Runtime: Node.js >=24.19.0 <25. Read-only release: all server mutations are deferred.",
        "Read availability from capabilities; catalog entries alone are not implemented capabilities.",
      ].join("\n") + "\n"
    );
  const matches = registry.filter((s) => path.every((p, i) => s.path[i] === p));
  if (!matches.length)
    throw new CliError("validation", "unknown_command", "Unknown help path.");
  const exact = matches.find((s) => s.path.length === path.length);
  if (!exact) {
    if (field)
      throw new CliError(
        "validation",
        "invalid_argument",
        "Field help requires a leaf command.",
      );
    return (
      [
        "seata-ai " + path.join(" "),
        ...matches.map(
          (s) =>
            "  " + s.path.slice(path.length).join(" ") + " [" + s.risk + "]" +
            (deferredWrite(s.id) ? " (deferred; unavailable)" : ""),
        ),
      ].join("\n") + "\n"
    );
  }
  const props = object(exact.invocation.properties);
  if (field) {
    for (const group of ["input", "context", "options"]) {
      const fields = object(object(props[group]).properties);
      if (fields[field])
        return (
          [
            exact.id + " / " + group + "." + field,
            JSON.stringify(fields[field]),
            "Use the complete invocation schema for cross-field conditions.",
          ].join("\n") + "\n"
        );
    }
    throw new CliError(
      "validation",
      "invalid_argument",
      "Unknown command field.",
    );
  }
  const lines = [
    "seata-ai " + exact.path.join(" "),
    "Command: " + exact.id,
    "Risk: " + exact.risk + "; confirmation: " + exact.confirmation,
    "Required capabilities: " + exact.backends,
    "Fields:",
  ];
  for (const group of ["context", "input", "options"])
    for (const [key, val] of Object.entries(
      object(object(props[group]).properties),
    )) {
      const def = object(val);
      lines.push(
        "  --" +
          (key === "confirm_high_risk" ? "yes" : key.replaceAll("_", "-")) +
          (def.type === "boolean"
            ? " / --no-" + (key === "confirm_high_risk" ? "yes" : key.replaceAll("_", "-"))
            : " <" + String(def.type ?? "JSON") + ">") +
          " (" +
          group +
          ")",
      );
    }
  lines.push(
    "Output: " + exact.output_contract,
    "Objects and arrays are passed as a single JSON argument.",
    "Exact contract: seata-ai schema " + exact.id,
  );
  if (deferredWrite(exact.id))
    lines.push(
      "Unavailable: deferred_read_only_scope. This release does not execute server mutations.",
      "--dry-run and confirmation flags do not enable this command.",
    );
  return lines.join("\n") + "\n";
}
