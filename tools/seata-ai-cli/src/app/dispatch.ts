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

import { registry, specFor, outputSchemas, type Invocation } from "../command/registry.js";
import { type Obj, str } from "../protocol/value.js";
import { renderHelp } from "../help/render.js";
import { CliError } from "../protocol/error.js";

export function dispatch(inv: Invocation, signal: AbortSignal): Obj {
  signal.throwIfAborted();
  switch (inv.command) {
    case "system.version": {
      return {
        versions: {
          cli: "0.1.0-r11.0",
          protocol: "0.1",
          node: process.versions.node,
          implementation: "typescript",
        },
        contract_revision: "R9",
        state_store_id: null,
      };
    }
    case "system.help": {
      const path = Array.isArray(inv.input.path) ? inv.input.path.map(str) : [];
      return {
        help_text: renderHelp(
          path,
          typeof inv.input.field === "string" ? inv.input.field : undefined,
        ),
        resolved_path: path,
        format: "text",
      };
    }
    case "system.schema": {
      const spec = specFor(str(inv.input.command_id));
      return {
        command_id: spec.id,
        schema_version: spec.schema_version,
        invocation_schema: spec.invocation,
        output_schema: outputSchemas[spec.id] ?? {},
        output_schema_status:
          outputSchemas[spec.id] !== undefined
            ? "implemented"
            : "not_implemented",
      };
    }
    case "system.capabilities": {
      const list = inv.input.command_id ? [specFor(str(inv.input.command_id))] : registry;
      const observedAt = new Date().toISOString();
      return {
        commands: list.map(spec => ({
          command_id: spec.id,
          risk: spec.risk,
          confirmation: spec.confirmation,
          schema_version: spec.schema_version,
          backends: spec.backends,
          status: "available",
          reason_code: "local_implementation",
          observed_at: observedAt,
          scope: "local",
          source: "command_registry",
        })),
        runtime: {
          authenticated: "not_evaluated",
          management_reachable: "not_evaluated",
          storage_safe: "unavailable",
          compatibility_evidence: "not_evaluated",
          production_writes: false,
          server_mutations: "disabled",
          release_scope: "read_only",
          optional_sources: {
            logs: "not_implemented",
            metrics: "not_implemented",
            database: "not_implemented",
            config_center: "not_implemented",
          },
        },
        assurance: {
          server_cas: false,
          server_idempotency: false,
          dedup_scope: "not_available",
          causality: "unproven",
        },
      };
    }
    default:
      throw new CliError("validation", "unknown_command", "Unknown executable command.");
  }
}
