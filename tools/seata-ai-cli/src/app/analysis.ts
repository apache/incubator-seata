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
import { type Invocation } from "../command/registry.js";
import { type Obj, object, str, digest, canonical } from "../protocol/value.js";
import { CliError, PartialResult } from "../protocol/error.js";
import { readerFor } from "./read.js";
import { transactionDTO, branchDTO, lockDTO, evidence, type CollectionBudget, type ManagementReader } from "../adapter/management/read.js";

export const analysisImplemented = new Set(["transaction.abnormal", "transaction.trace", "lock.conflicts"]);
export const ruleset = "seata_readonly_v1";
export function requireRuleset(value: unknown): void {
  if (value !== undefined && value !== ruleset) throw new CliError("validation", "ruleset_not_found", "The requested ruleset is not bundled with this CLI.");
}
function pagination(input: Obj, count: number, complete: boolean): Obj {
  const page = Number(input.page ?? 1), size = Math.min(Number(input.page_size ?? 20), Number(input.limit ?? 10000));
  return { page, page_size: size, returned: Math.max(0, Math.min(size, count - (page - 1) * size)), total: complete ? count : null,
    has_more: count > page * size ? true : complete ? false : null };
}
export async function analysisCommand(inv: Invocation, signal: AbortSignal, sharedTraceBudget?: CollectionBudget,
  boundReader?: ManagementReader): Promise<Obj> {
  requireRuleset(inv.input.ruleset_id);
  const client = boundReader ?? await readerFor(inv, signal), observed = new Date().toISOString();
  const attemptStart = client.readAttempts.length;
  if (inv.command === "transaction.abnormal") {
    const now = Date.now();
    const result = await client.collect("transaction", inv.input, false, row => {
      const dto = transactionDTO(row, observed), status = object(dto.status);
      if (inv.input.min_age_ms !== undefined) {
        if (dto.begin_time === null) return null;
        if (now - Number(dto.begin_time) < Number(inv.input.min_age_ms)) return false;
      }
      return status.abnormal === true || status.known === false || status.code === 0;
    });
    const page = pagination(inv.input, result.rows.length, result.completeness.status === "complete");
    const start = (Number(page.page) - 1) * Number(page.page_size);
    const transactions = result.rows.slice(start, start + Number(page.page_size)).map(row => transactionDTO(row, observed));
    const records = transactions.map(t => evidence(inv.command, t.xid ?? null, t, result.completeness));
    const findings = transactions.map((t, i) => ({ rule_id: "transaction_abnormal_status", ruleset_version: ruleset,
      evidence_ids: [records[i]!.id!], observed_fact: { xid: t.xid ?? null, status: t.status ?? null },
      inference: "The observed management status warrants investigation; it does not establish the business outcome or cause.",
      confidence: object(t.status).known === true ? "high" : "unknown", counter_evidence: [],
      recommendation: "Inspect transaction trace and application-side evidence. Do not infer a safe recovery action from status alone." }));
    return { transactions, findings, evidence: records, pagination: page, completeness: result.completeness };
  }
  if (inv.command === "lock.conflicts") {
    const result = await client.collect("lock", inv.input), groups = new Map<string, Obj[]>();
    for (const row of result.rows) {
      if (typeof row.resourceId !== "string" || typeof row.tableName !== "string" || typeof row.pk !== "string")
        throw new CliError("api", "response_contract_invalid", "Lock conflict analysis requires complete resource identities.");
      const identity = digest([row.resourceId, row.tableName, row.pk]);
      const owners = groups.get(identity) ?? []; owners.push(lockDTO(row, observed)); groups.set(identity, owners);
    }
    const all = result.rows.map(row => lockDTO(row, observed)), page = pagination(inv.input, all.length, result.completeness.status === "complete");
    const start = (Number(page.page) - 1) * Number(page.page_size), holders = all.slice(start, start + Number(page.page_size));
    return { holders, pagination: page, potential_conflicts: [...groups.entries()].filter(([, owners]) =>
      new Set(owners.map(o => digest([o.xid ?? null, o.branch_id ?? null]))).size > 1).map(([identity, owners]) => ({
        resource_digest: identity, holders: owners, classification: "potential_conflict", confirmed_waiter: false,
        limitations: ["holder_records_only", "concurrent_page_movement_possible", "no_waiter_or_deadlock_evidence"] })),
      waiter_evidence: [], completeness: result.completeness };
  }
  if (inv.command === "transaction.trace") {
    const budget = sharedTraceBudget ?? { pages: 100, bytes: 8 * 1024 * 1024 };
    const initialPages = budget.pages;
    const xid = str(inv.input.xid), result = await client.transaction(xid, true, budget);
    const transactionObserved = new Date().toISOString(), transaction = transactionDTO(result.row, transactionObserved);
    const hasBranches = Array.isArray(result.row.branchSessionVOs);
    const rawBranches = Array.isArray(result.row.branchSessionVOs) ? result.row.branchSessionVOs : [];
    const branchTruncated = rawBranches.length > 9999;
    const branches = rawBranches.slice(0, 9999).map(row => branchDTO(row, xid, transactionObserved));
    if (new Set(branches.map(branch => branch.branch_id)).size !== branches.length)
      throw new CliError("api", "response_contract_invalid", "Duplicate branch identities in transaction trace.");
    const missing: Obj[] = [{ source: "logs", reason_code: "not_implemented" }, { source: "rm_business_outcome", reason_code: "not_observed" }];
    const branchReason = !hasBranches ? "branch_metadata_missing" : branchTruncated ? "entity_limit" : null;
    const transactionCompleteness: Obj = { ...result.completeness,
      status: branchReason ? branchTruncated ? "truncated" : "partial" : result.completeness.status!,
      limit_reason: branchReason ?? result.completeness.limit_reason! };
    const sources = [transactionCompleteness];
    const records = [evidence(inv.command, transaction.xid ?? null, { transaction, branches }, transactionCompleteness)];
    let locks: Obj[] = [], partial = branchReason !== null, limitReason = branchReason;
    if (branchReason) missing.push({ source: "branches", reason_code: branchReason });
    const remainingEntities = 10000 - 1 - branches.length;
    if (remainingEntities === 0) {
      partial = true; limitReason = "entity_limit";
      missing.push({ source: "locks", reason_code: "entity_limit" });
    } else try {
      const found = await client.collect("lock", { xid, limit: remainingEntities }, false, undefined, budget);
      const lockObserved = new Date().toISOString();
      locks = found.rows.map(row => lockDTO(row, lockObserved));
      sources.push(found.completeness);
      records.push(evidence("lock.list", transaction.xid ?? null, { locks }, found.completeness));
      if (found.completeness.status !== "complete") {
        partial = true; limitReason ??= String(found.completeness.limit_reason);
        missing.push({ source: "locks", reason_code: String(found.completeness.limit_reason) });
      }
    } catch (error) {
      signal.throwIfAborted(); partial = true;
      limitReason ??= "required_management_evidence_missing";
      missing.push({ source: "locks", reason_code: error instanceof CliError ? error.subtype : "read_failed" });
    }
    // Charge both evidence and its top-level fact copies: DTO expansion and
    // duplicated facts must not escape the retained evidence budget.
    const evidenceLimit = 8 * 1024 * 1024;
    const retainedBytes = () => Buffer.byteLength(canonical({ transaction, branches, locks, evidence: records }));
    for (const entry of [
      { index: 1, rows: locks, source: "locks", command: "lock.list", facts: { locks } },
      { index: 0, rows: branches, source: "branches", command: inv.command, facts: { transaction, branches } },
    ]) {
      if (retainedBytes() <= evidenceLimit) break;
      if (!records[entry.index] || entry.rows.length === 0) continue;
      partial = true; limitReason = "evidence_byte_limit";
      missing.push({ source: entry.source, reason_code: limitReason });
      const complete = sources[entry.index]!;
      complete.status = "truncated"; complete.limit_reason = limitReason;
      records[entry.index] = evidence(entry.command, transaction.xid ?? null, entry.facts, complete);
      let bytes = retainedBytes();
      while (bytes > evidenceLimit && entry.rows.length) {
        const removed = entry.rows.pop()!;
        bytes -= 2 * (Buffer.byteLength(canonical(removed)) + (entry.rows.length ? 1 : 0));
      }
      records[entry.index] = evidence(entry.command, transaction.xid ?? null, entry.facts, complete);
    }
    if (retainedBytes() > evidenceLimit)
      throw new CliError("api", "evidence_byte_limit", "Transaction evidence cannot fit within the retained evidence budget.");
    const traceObserved = new Date().toISOString();
    const begin = transaction.begin_time, timeline: Obj[] = [];
    if (typeof begin === "number" && begin <= 8640000000000000)
      timeline.push({ timestamp: new Date(begin).toISOString(), event: "transaction_begin", source: "server_begin_time", evidence_ids: [records[0]!.id!] });
    timeline.push({ timestamp: traceObserved, event: "management_observation", source: "cli_clock", evidence_ids: records.map(r => r.id!) });
    const data = { transaction, branches, locks, timeline, evidence: records, missing_sources: missing,
      completeness: { ...result.completeness, scope: "transaction_trace", status: partial ? "partial" : "complete",
        pages_read: initialPages - budget.pages,
        read_attempts: client.readAttempts.slice(attemptStart),
        observed_to: traceObserved, limit_reason: limitReason } };
    if (partial) throw new PartialResult(data, "trace_incomplete", "Some management evidence could not be collected.");
    return data;
  }
  throw new CliError("internal", "contract_violation", "Unknown analysis command.");
}
