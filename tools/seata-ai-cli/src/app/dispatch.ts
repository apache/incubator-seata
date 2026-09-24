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
import {
  registry,
  specFor,
  outputSchemas,
  type Invocation,
} from "../command/registry.js";
import { type Obj, str } from "../protocol/value.js";
import { renderHelp } from "../help/render.js";
import { CliError } from "../protocol/error.js";
import { localCommand } from "./profile.js";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { StateStore, stateDirectory } from "../adapter/statestore/client.js";
import { storageCapability } from "../capability/storage.js";

export const localImplemented = new Set([
  "profile.list",
  "profile.get",
  "profile.create",
  "profile.update",
  "profile.use",
  "profile.delete",
  "setup.initialize",
  "auth.status",
  "auth.logout",
  "auth.login.cancel",
  "execution.get",
]);
export const implemented = new Set([
  "system.version",
  "system.help",
  "system.schema",
  "system.capabilities",
  "system.invoke",
  ...localImplemented,
]);
export async function dispatch(
  inv: Invocation,
  signal: AbortSignal,
): Promise<Obj> {
  signal.throwIfAborted();
  if (localImplemented.has(inv.command)) return localCommand(inv, signal);
  switch (inv.command) {
    case "system.version": {
      let id = null;
      if (existsSync(join(stateDirectory(), "state.db"))) {
        const store = new StateStore();
        try {
          id = (await store.call("info", {}, signal)).state_store_id ?? null;
        } finally {
          await store.close();
        }
      }
      return {
        versions: {
          cli: "0.1.0-r11.0",
          protocol: "0.1",
          node: process.versions.node,
          implementation: "typescript",
        },
        contract_revision: "R9",
        state_store_id: id,
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
          implemented.has(spec.id) && spec.id !== "system.invoke"
            ? "implemented"
            : "not_implemented",
      };
    }
    case "system.capabilities": {
      const list = inv.input.command_id
        ? [specFor(str(inv.input.command_id))]
        : registry;
      const storage = await storageCapability(signal);
      return {
        commands: list.map((s) => {
          const local = localImplemented.has(s.id),
            exists = implemented.has(s.id);
          const status = !exists
            ? "unavailable"
            : !local
              ? "available"
              : storage === "available" && s.id === "auth.status"
                ? "degraded"
                : storage;
          return {
            command_id: s.id,
            risk: s.risk,
            confirmation: s.confirmation,
            schema_version: s.schema_version,
            backends: s.backends,
            status,
            reason_code: !exists
              ? "not_implemented"
              : !local
                ? "local_implementation"
                : status === "degraded"
                  ? "local_status_only"
                  : "storage_" + storage,
            observed_at: new Date().toISOString(),
            scope: "local",
            source: "handler_and_storage_check",
          };
        }),
        runtime: {
          authenticated: "not_evaluated",
          management_reachable: "not_evaluated",
          storage_safe: storage,
          compatibility_evidence: "source_inspected",
          production_writes: false,
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
          dedup_scope: "single_state_store",
          causality: "unproven",
        },
      };
    }
    default:
      throw new CliError(
        "policy",
        "command_unavailable",
        "This command has no enabled handler in this development build.",
      );
  }
}
