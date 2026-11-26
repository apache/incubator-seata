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
import test from "node:test";
import assert from "node:assert/strict";
import { currentRecoveryCondition, observeLockDeletionScope } from "../dist/app/reconcile.js";
import { compareHistoricalStatus } from "../dist/app/reconcile-before.js";
const complete = { status: "complete" };
function check(command, code, direction, known = true, completeness = complete) {
  const row = { status: { code, known } };
  return currentRecoveryCondition(command, direction ? { expected_direction: direction } : {},
    command.startsWith("branch.") ? { branches: [row] } : { transaction: row }, completeness);
}
test("retry predicates use exact pinned status codes without accepting terminal states as retrying", () => {
  const cases = [
    ["transaction.retry.pause", "commit", [19]], ["transaction.retry.pause", "rollback", [20]],
    ["transaction.retry.pause", undefined, [19, 20]],
    ["transaction.retry.resume", "commit", [3]], ["transaction.retry.resume", "rollback", [5]],
    ["branch.retry.pause", undefined, [14]], ["branch.retry.resume", undefined, [1]],
  ];
  for (const [command, direction, accepted] of cases) {
    for (let code = 0; code <= 20; code++) {
      const result = check(command, code, direction);
      assert.equal(result.observed, accepted.includes(code), command + "/" + direction + "/" + code);
      assert.equal(result.business_completion, "not_claimed");
      assert.equal(result.causality, "unproven");
    }
  }
});
test("dispatch and recovery observe terminal management state without proving RM effects", () => {
  for (const command of ["transaction.dispatch", "transaction.status.recover"]) {
    for (let code = 0; code <= 20; code++) {
      assert.equal(check(command, code, "commit").observed, code === 9);
      assert.equal(check(command, code, "rollback").observed, code === 11 || code === 13);
    }
  }
});
test("missing direction, unknown statuses, incomplete evidence and unproven absence remain unknown", () => {
  for (const command of ["transaction.retry.resume", "transaction.dispatch", "transaction.status.recover"])
    assert.equal(check(command, 9).observed, null);
  for (const command of ["transaction.retry.pause", "branch.retry.pause"]) {
    assert.equal(check(command, 222, undefined, false).observed, null);
    assert.equal(check(command, 19, undefined, true, { status: "truncated" }).observed, null);
    assert.equal(check(command, 19, undefined, true, { status: "partial" }).observed, null);
  }
  for (const command of ["transaction.delete", "branch.delete", "lock.delete"])
    assert.equal(currentRecoveryCondition(command, {}, {}, complete).observed, null);
  assert.equal(currentRecoveryCondition("branch.retry.resume", {}, { branches: [] }, complete).observed, null);
  assert.equal(currentRecoveryCondition("branch.retry.resume", {}, { branches: [{}, {}] }, complete).observed, null);
});

test("lock scope observations distinguish DB row keys from branch-wide release without inferring backend mode", () => {
  const input = { xid: "x", branch_id: "9007199254740993", resource_id: "jdbc:SECRET", table_name: "orders", pk: "42" };
  const rows = [
    { xid: "x", branchId: "9007199254740994", rowKey: "jdbc:SECRET^^^orders^^^42" },
    { xid: "x", branchId: "9007199254740993", rowKey: "jdbc:SECRET^^^orders^^^99" },
    { xid: "x", branchId: "9007199254740993", rowKey: "jdbc:SECRET^^^other^^^1" },
  ];
  const result = observeLockDeletionScope(input, rows, complete);
  assert.equal(result.selected_scope, "unknown");
  assert.equal(result.candidates[0].observed_matches, 1);
  assert.equal(result.candidates[0].branch_id_restricts_delete, false);
  assert.equal(result.candidates[1].observed_matches, 2);
  assert.equal(result.candidates[1].single_row_delete_proven, false);
  assert.doesNotMatch(JSON.stringify(result), /jdbc:SECRET/);
  assert.equal(observeLockDeletionScope(input, [], complete).candidates[0].absence_observed, true);
  assert.equal(observeLockDeletionScope(input, [], { status: "truncated" }).candidates[0].absence_observed, null);
  const missing = observeLockDeletionScope(input, [{ xid: "x" }], complete);
  assert.ok(missing.candidates.every(c => c.absence_observed === null));
  assert.throws(() => observeLockDeletionScope({ xid: "x" }, [], complete), e => e.subtype === "execution_evidence_insufficient");
  assert.equal(currentRecoveryCondition("lock.delete", input, { deletion_scope: result }, complete).observed, null);
});

function historical(branch = false) {
  const input = branch ? { xid: "x", branch_id: "9007199254740993" } : { xid: "x", expected_direction: "rollback" };
  const saved = { command: branch ? "branch.retry.resume" : "transaction.retry.resume", plan_id: "plan-1",
    target_digest: "bound-target", target: { naming_base_endpoint: "https://example.test/base" }, send_intent_at: "2026-09-20T00:00:01Z" };
  const field = branch ? "branch_status" : "global_status", status = branch ? "STOP_RETRY" : "StopRollbackOrRollbackRetry";
  const context = { target_digest: saved.target_digest, plan_binding_valid: true, plan: { plan_id: saved.plan_id,
    plan_format_version: 1, command: saved.command, target: structuredClone(saved.target), normalized_input: input,
    before: { [field]: status }, preconditions: [{ [field]: status }], created_at: "2026-09-20T00:00:00Z" } };
  const row = { ...input, status: { known: true, code: branch ? 14 : 20 } };
  const facts = branch ? { branches: [row] } : { transaction: row };
  return { saved, context, input, facts, run: () => compareHistoricalStatus(saved, context, input, facts, complete, "2026-09-25T00:00:00Z") };
}

test("historical status comparison handles exact branch identity and evaluates only declared status predicates", () => {
  const f = historical(true);
  assert.equal(f.run().status, "unchanged"); assert.equal(f.run().preconditions_currently_observed, true);
  f.facts.branches[0].status.code = 1;
  assert.equal(f.run().status, "changed"); assert.equal(f.run().preconditions_currently_observed, false);
  f.facts.branches[0].branch_id = "9007199254740994";
  assert.equal(f.run().status, "not_evaluated");
  const g = historical();
  g.context.plan.preconditions.push({ lock_count: 0 });
  assert.equal(g.run().status, "unchanged"); assert.equal(g.run().preconditions_currently_observed, null);
  g.context.plan.preconditions = [{ global_status: "RollbackRetrying" }];
  g.facts.transaction.status.code = 5;
  assert.equal(g.run().preconditions_currently_observed, null, "A predicate not established in before cannot be reported as still observed");
});

test("historical comparison refuses unbound, future, unknown or incomplete evidence without exposing plan content", () => {
  const changes = [
    f => { f.context.plan_binding_valid = false; },
    f => { f.context.target_digest = "different"; },
    f => { f.context.plan.target.naming_base_endpoint = "https://other.test"; },
    f => { f.context.plan.plan_id = "different"; },
    f => { f.context.plan.normalized_input = { xid: "different" }; },
    f => { f.context.plan.created_at = "2026-10-01T00:00:00Z"; },
    f => { f.context.plan.created_at = "2026-10-01T00:00:00Z"; f.saved.send_intent_at = "2026-10-02T00:00:00Z"; },
    f => { f.context.plan.before.global_status = "SECRET_UNKNOWN_STATUS"; },
    f => { f.facts.transaction.status.known = false; },
    f => { f.facts.transaction.xid = "different"; },
    f => { f.context.plan.plan_format_version = 2; },
  ];
  for (const change of changes) {
    const f = historical(); change(f);
    const result = f.run(); assert.equal(result.status, "not_evaluated");
    assert.equal(result.before_status, null); assert.equal(result.preconditions_currently_observed, null);
    assert.doesNotMatch(JSON.stringify(result), /SECRET_UNKNOWN_STATUS/);
  }
  const f = historical();
  assert.equal(compareHistoricalStatus(f.saved, f.context, f.input, f.facts, { status: "truncated" }, "2026-09-25T00:00:00Z").status, "not_evaluated");
});
