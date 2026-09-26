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
import { type Obj, digest, isObject } from "../protocol/value.js";
import { knownManagementStatus } from "../adapter/management/read.js";

/** Compare only pinned management status fields from the locally recorded plan.
 * This neither establishes historical causality nor evaluates arbitrary plan
 * predicates. Unknown selectors/fields/statuses cannot become successful proof. */
export function compareHistoricalStatus(saved: Obj, context: Obj, input: Obj, facts: Obj, complete: Obj, observedAt: string): Obj {
  const unknown = (reason: string): Obj => ({ status: "not_evaluated", reason_code: reason, scope: "management_status_only",
    historical_source: "local_execution_plan", historical_plan_id: null, before_status: null, current_status: null,
    preconditions_currently_observed: null });
  if (complete.status !== "complete") return unknown("incomplete_current_evidence");
  const command = String(saved.command), branch = command.startsWith("branch.");
  if (!branch && !command.startsWith("transaction.")) return unknown("historical_comparison_scope_unsupported");
  const plan = context.plan;
  if (!isObject(plan) || context.plan_binding_valid !== true) return unknown("historical_plan_missing_or_unbound");
  if (context.target_digest !== saved.target_digest || plan.plan_id !== saved.plan_id || plan.plan_format_version !== 1 ||
      plan.command !== command || !isObject(plan.target) || !isObject(saved.target) || digest(plan.target) !== digest(saved.target))
    return unknown("historical_plan_binding_mismatch");
  if (!isObject(plan.normalized_input) || typeof input.xid !== "string" ||
      ["xid", "expected_direction", ...(branch ? ["branch_id"] : [])].some(key => (plan.normalized_input as Obj)[key] !== input[key]))
    return unknown("historical_selector_mismatch");
  const created = typeof plan.created_at === "string" ? Date.parse(plan.created_at) : NaN;
  const observed = Date.parse(observedAt), cutoff = typeof saved.send_intent_at === "string" ? Date.parse(saved.send_intent_at) : observed;
  if (!Number.isFinite(created) || !Number.isFinite(cutoff) || !Number.isFinite(observed) ||
      created > cutoff || cutoff > observed) return unknown("historical_timestamp_invalid");
  const field = branch ? "branch_status" : "global_status";
  const before = isObject(plan.before) ? knownManagementStatus(plan.before[field], branch) : null;
  if (!before) return unknown("historical_status_unknown");
  const row = branch ? (Array.isArray(facts.branches) && facts.branches.length === 1 ? facts.branches[0] : null) : facts.transaction;
  if (!isObject(row) || row.xid !== input.xid || (branch && row.branch_id !== input.branch_id))
    return unknown("current_identity_or_presence_unknown");
  const current = isObject(row.status) && row.status.known === true ? knownManagementStatus(row.status.code, branch) : null;
  if (!current) return unknown("current_status_unknown");
  const predicates = plan.preconditions;
  const knownPredicates = Array.isArray(predicates) && predicates.length > 0 && predicates.every(p =>
    isObject(p) && Object.keys(p).length === 1 && knownManagementStatus(p[field], branch)?.code === before.code);
  const preconditions = knownPredicates ? predicates.every(p => knownManagementStatus((p as Obj)[field], branch)!.code === current.code) : null;
  return { status: before.code === current.code ? "unchanged" : "changed", reason_code: "bound_management_status_compared",
    scope: "management_status_only", historical_source: "local_execution_plan", historical_plan_id: plan.plan_id!,
    before_status: before, current_status: current, preconditions_currently_observed: preconditions };
}
