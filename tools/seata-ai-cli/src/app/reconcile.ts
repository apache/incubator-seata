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
import type { Invocation } from "../command/registry.js";
import { validateOutput } from "../command/registry.js";
import { type Obj, object, str, digest, parseJson, isObject } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { resource } from "../resources.js";
import { StateStore } from "../adapter/statestore/client.js";
import { adapterId, transactionDTO, branchDTO, lockDTO, evidence } from "../adapter/management/read.js";
import { readerFor } from "./read.js";
import { compareHistoricalStatus } from "./reconcile-before.js";

export const reconciliationAdapterDigest = digest(parseJson(resource("compatibility/manifest.json")));
export function observeLockDeletionScope(input: Obj, rows: Obj[], complete: Obj): Obj {
  for (const key of ["xid", "branch_id", "resource_id", "table_name", "pk"])
    if (typeof input[key] !== "string" || !input[key])
      throw new CliError("policy", "execution_evidence_insufficient", "Historical lock selectors are incomplete.");
  const rowKey = `${input.resource_id}^^^${input.table_name}^^^${input.pk}`;
  const dbRows = rows.filter(row => row.xid === input.xid && row.rowKey === rowKey);
  const branchRows = rows.filter(row => row.xid === input.xid && String(row.branchId) === input.branch_id);
  const dbKnown = complete.status === "complete" && rows.every(row => typeof row.rowKey === "string");
  const branchKnown = complete.status === "complete" && rows.every(row => row.branchId !== undefined && row.branchId !== null);
  return { selected_scope: "unknown", reason_code: "historical_lock_mode_not_verified",
    selector_digest: digest(input), candidates: [
      { backend: "db", predicate: "xid_and_row_key", observed_matches: dbRows.length,
        absence_observed: dbKnown ? dbRows.length === 0 : null, branch_id_restricts_delete: false },
      { backend: "redis_or_file", predicate: "branch_lock_set", observed_matches: branchRows.length,
        absence_observed: branchKnown ? branchRows.length === 0 : null, single_row_delete_proven: false },
    ], limitations: ["backend_mode_not_observed", "best_effort_query", "absence_does_not_prove_historical_deletion", "rm_business_completion_not_claimed"] };
}
/** Current management-state predicates, not proof that this execution caused
 * the state or that resource-manager business effects completed. Mappings are
 * pinned to AbstractGlobalService / AbstractBranchService at 41694e14. */
export function currentRecoveryCondition(command: string, input: Obj, facts: Obj, complete: Obj): Obj {
  const result = (observed: boolean | null, reason: string): Obj => ({ observed, reason_code: reason,
    scope: "current_management_state_only", business_completion: "not_claimed", causality: "unproven" });
  if (complete.status !== "complete") return result(null, "incomplete_current_evidence");
  const branch = command.startsWith("branch.");
  const row = branch ? (Array.isArray(facts.branches) && facts.branches.length === 1 ? facts.branches[0] : null) : facts.transaction;
  if (command.endsWith(".delete")) return result(null, "deletion_scope_and_absence_not_proven");
  if (!isObject(row) || !isObject(row.status) || row.status.known !== true || typeof row.status.code !== "number")
    return result(null, "current_status_unknown");
  const code = row.status.code, direction = input.expected_direction;
  if (command === "branch.retry.pause") return result(code === 14, "branch_stop_retry_predicate");
  if (command === "branch.retry.resume") return result(code === 1, "branch_registered_predicate");
  if (command === "transaction.retry.pause") {
    return result(direction === "commit" ? code === 19 : direction === "rollback" ? code === 20 : code === 19 || code === 20,
      "transaction_paused_predicate");
  }
  if (direction !== "commit" && direction !== "rollback") return result(null, "historical_direction_missing");
  if (command === "transaction.retry.resume") return result(code === (direction === "commit" ? 3 : 5), "transaction_retrying_predicate");
  if (command === "transaction.dispatch" || command === "transaction.status.recover")
    return result(direction === "commit" ? code === 9 : code === 11 || code === 13, "transaction_terminal_status_predicate");
  return result(null, "command_predicate_unavailable");
}
export function verifyReconciliationTarget(saved: Obj, binding: Obj, context: Obj): void {
  const target = object(saved.target), profile = object(binding.profile), management = object(profile.management), route = object(profile.target);
  const actual: Obj = {
    console_base_endpoint: management.console_endpoint!, naming_base_endpoint: management.naming_endpoint ?? null,
    namespace: context.namespace ?? route.namespace!,
    trust_digest: binding.trust_digest!, adapter_id: management.compatibility_id!,
    adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1", route_assurance: "selector_only",
    ...(context.cluster !== undefined ? { cluster: context.cluster } : context.vgroup !== undefined ? { vgroup: context.vgroup }
      : route.vgroup !== undefined ? { vgroup: route.vgroup } : { cluster: route.cluster! }),
  };
  // Profile identity/revision can differ for an explicitly selected replacement
  // Profile, but management endpoints, trust, selectors and adapter cannot.
  const keys = ["console_base_endpoint", "naming_base_endpoint", "namespace", "cluster", "vgroup",
    "trust_digest", "adapter_id", "adapter_digest", "command_schema_version", "route_assurance"];
  if (saved.target_digest !== binding.target_digest || target.adapter_id !== adapterId ||
      keys.some(key => (target[key] ?? null) !== (actual[key] ?? null)))
    throw new CliError("policy", "execution_target_mismatch", "The selected Profile does not match the original management target and adapter.");
}

export async function reconcileExecution(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  if (!inv.context.profile) throw new CliError("validation", "profile_required", "Read-only reconciliation requires an explicit Profile.");
  const store = new StateStore(undefined, Number(inv.options.timeout_ms ?? 30000));
  try {
    const original = await store.call("execution.get", inv.input, signal);
    validateOutput("execution.get", original);
    const saved = object(original.execution);
    const privateContext = await store.call("execution.readContext", inv.input, signal);
    const intent = object(privateContext.intent);
    const input = intent.input === undefined ? intent : object(intent.input);
    if (typeof input.xid !== "string" || !input.xid)
      throw new CliError("policy", "execution_evidence_insufficient", "The historical record has no usable original transaction selector.");
    const client = await readerFor(inv, signal, binding => verifyReconciliationTarget(saved, binding, inv.context));
    const observed = new Date().toISOString(), command = str(saved.command);
    let facts: Obj, complete: Obj, source: string;
    if (command.startsWith("branch.")) {
      if (typeof input.branch_id !== "string")
        throw new CliError("policy", "execution_evidence_insufficient", "The historical record has no exact branch selector.");
      const result = await client.observeBranches(input.xid);
      const rows = (result.rows ?? []).map(row => branchDTO(row, str(input.xid), observed));
      facts = { branches: rows.filter(row => row.branch_id === input.branch_id),
        absence_meaning: result.rows === null ? "not_observed_in_current_route" : "not_observed_in_current_parent_branch_set" };
      complete = result.completeness; source = "branch.list";
    } else if (command === "lock.delete") {
      observeLockDeletionScope(input, [], { status: "unknown" });
      // Query the XID set without a branch filter: the pinned DB deletion does
      // not include branch_id in its SQL predicate, unlike branch-wide modes.
      const result = await client.collect("lock", { xid: input.xid });
      facts = { locks: result.rows.map(row => lockDTO(row, observed)),
        deletion_scope: observeLockDeletionScope(input, result.rows, result.completeness),
        comparison_limit: "historical_backend_mode_not_proven" };
      complete = result.completeness; source = "lock.list";
    } else {
      const result = await client.observeTransaction(input.xid, false);
      facts = result.row === null ? { transaction: null, absence_meaning: "not_observed_in_current_route" }
        : { transaction: transactionDTO(result.row, observed) };
      complete = result.completeness; source = "transaction.get";
    }
    const current = [evidence(source, null, facts, complete)];
    const condition = currentRecoveryCondition(command, input, facts, complete);
    const comparison = compareHistoricalStatus(saved, privateContext, input, facts, complete, observed);
    // A current condition can be observed even when the original response was
    // lost. Preserve historical outcome and separately report this predicate.
    const observation: Obj = { observation: condition.observed === true ? "postcondition_observed" :
      comparison.preconditions_currently_observed === true ? "precondition_still_observed" : "unknown", current_evidence: current,
      desired_condition_currently_observed: condition,
      comparison_to_before: comparison,
      completeness: complete, route_assurance: "selector_only", causality: "unproven",
      next_action: "manual_diagnosis", replay_allowed: false };
    // Reconciliation is an ephemeral read. Keep the historical execution and
    // its events unchanged; evidence is retained only by explicit export.
    return { execution: saved, observation: observation.observation!, evidence: current,
      desired_condition_currently_observed: condition,
      current_evidence: current, comparison_to_before: observation.comparison_to_before!,
      completeness: complete, route_assurance: "selector_only", causality: "unproven",
      next_action: "manual_diagnosis", replay_allowed: false };
  } finally { await store.close(); }
}
