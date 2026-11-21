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
import { deferredWrite, enforceReadOnly } from "../command/scope.js";
import { readCommand, readImplemented } from "./read.js";
import { authLogin, authStatus, localBrowser } from "./auth.js";
import { connectivity, capabilityProbe } from "./connectivity.js";
import { analysisCommand, analysisImplemented } from "./analysis.js";
import { diagnoseCommand, diagnosticLocal, diagnosticRemote } from "./diagnose.js";
import { configCommand, configImplemented } from "./config.js";
import { inspectCommand, inspectImplemented } from "./inspect.js";
import { snapshotCommand, snapshotImplemented } from "./snapshot.js";
import { exportSnapshot } from "./export.js";
import { skillCatalog, readSkill } from "../skillcontent/catalog.js";
import { skillsStatus } from "../skillcontent/status.js";
import { installSkills } from "../skillcontent/install.js";
import { reconcileExecution } from "./reconcile.js";
import { validateSkills, exportSkills, skillFilesystem } from "./skills.js";

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
  "auth.login.status",
  "auth.login",
  "execution.get",
  "diagnose.snapshot.export",
  "skills.install",
]);
export const implemented = new Set([
  "system.version",
  "system.help",
  "system.schema",
  "system.capabilities",
  "system.invoke",
  "skills.list",
  "skills.read",
  "skills.status",
  "execution.reconcile",
  ...skillFilesystem,
  ...localImplemented,
  ...readImplemented,
  ...analysisImplemented,
  ...diagnosticLocal,
  ...diagnosticRemote,
  ...configImplemented,
  ...inspectImplemented,
  ...snapshotImplemented,
  "connectivity.check",
]);
export async function dispatch(
  inv: Invocation,
  signal: AbortSignal,
  warn?: (warning: Obj) => void,
): Promise<Obj> {
  signal.throwIfAborted();
  enforceReadOnly(inv.command);
  if (inv.command === "skills.install") return installSkills(inv, signal);
  if (inv.command === "skills.status") return skillsStatus(inv.input, signal);
  if (inv.command === "execution.reconcile") return reconcileExecution(inv, signal);
  if (inv.command === "skills.validate") return validateSkills(inv.input, signal);
  if (inv.command === "skills.export") return exportSkills(inv, signal);
  if (inv.command === "skills.list") return { skills: skillCatalog() };
  if (inv.command === "skills.read") return readSkill(inv.input);
  if (inv.command === "diagnose.snapshot.export") return exportSnapshot(inv, signal);
  if (snapshotImplemented.has(inv.command)) return snapshotCommand(inv, signal);
  if (inspectImplemented.has(inv.command)) return inspectCommand(inv, signal);
  if (configImplemented.has(inv.command)) return configCommand(inv, signal);
  if (inv.command === "connectivity.check") return connectivity(inv, signal);
  if (analysisImplemented.has(inv.command)) return analysisCommand(inv, signal);
  if (diagnosticLocal.has(inv.command) || diagnosticRemote.has(inv.command)) return diagnoseCommand(inv, signal);
  if (inv.command === "auth.login") return authLogin(inv, signal, warn);
  if (inv.command === "auth.status") return authStatus(inv, signal, warn);
  if (readImplemented.has(inv.command)) return readCommand(inv, signal);
  if (localImplemented.has(inv.command)) return localCommand(inv, signal, warn);
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
      const report = inv.context.profile ? await connectivity(inv, signal) : undefined;
      return {
        commands: list.map((s) => {
          const local = localImplemented.has(s.id), remote = s.id === "connectivity.check" || s.id === "execution.reconcile" || readImplemented.has(s.id) || analysisImplemented.has(s.id) || diagnosticRemote.has(s.id) || snapshotImplemented.has(s.id),
            exists = implemented.has(s.id);
          const filesystemUnavailable = (configImplemented.has(s.id) || inspectImplemented.has(s.id) || skillFilesystem.has(s.id)) &&
            !["linux", "darwin"].includes(process.platform);
          const status = deferredWrite(s.id) || !exists || filesystemUnavailable || (s.id === "auth.login" && !localBrowser())
            ? "unavailable"
            : remote
              ? "not_evaluated"
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
            reason_code: deferredWrite(s.id)
              ? "deferred_read_only_scope"
              : filesystemUnavailable
                ? "filesystem_adapter_unavailable"
              : s.id === "auth.login" && !localBrowser()
                ? "local_auth_unavailable"
              : !exists
              ? "not_implemented"
              : remote
                ? "management_not_probed"
                : !local
                ? "local_implementation"
                : status === "degraded"
                  ? "local_status_only"
                  : "storage_" + storage,
            observed_at: new Date().toISOString(),
            scope: report && remote ? "profile" : "local",
            source: report && remote ? "bounded_read_probe" : "handler_and_storage_check",
            ...(report && remote ? capabilityProbe(s.id, report) : {}),
          };
        }),
        runtime: {
          authenticated: report ? (report.assurance as Obj).authentication ?? "not_evaluated" : "not_evaluated",
          management_reachable: report?.overall ?? "not_evaluated",
          ...(report ? { checks: report.checks ?? [] } : {}),
          storage_safe: storage,
          compatibility_evidence: "source_inspected",
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
