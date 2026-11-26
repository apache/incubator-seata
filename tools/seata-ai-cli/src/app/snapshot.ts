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
import { randomUUID } from "node:crypto";
import type { Invocation } from "../command/registry.js";
import { type Obj, type Json, canonical, digest, object } from "../protocol/value.js";
import { CliError, PartialResult } from "../protocol/error.js";
import { readerFor } from "./read.js";
import { transactionDTO, branchDTO, lockDTO, safeIdentity } from "../adapter/management/read.js";
import { inspectLocal } from "./inspect.js";

export const snapshotImplemented = new Set(["diagnose.snapshot"]);
export const snapshotLimits = { bytes: 8 * 1024 * 1024, entities: 10000, milliseconds: 60000 };

/** Evidence accounting is shared across all sources. Oversized sources are
 * explicitly omitted, never relabelled as a successful empty observation. */
export class SnapshotCollector {
  readonly sources: Obj[] = [];
  private bytes = 0;
  private entities = 0;
  constructor(readonly signal: AbortSignal, private readonly limits = snapshotLimits) {}
  get usage(): Obj { return { bytes: this.bytes, entities: this.entities }; }
  get remainingEntities(): number { return Math.max(0, this.limits.entities - this.entities); }
  private entry(id: string, status: string, reason: string | null, data: Obj | null, bytes = 0, entities = 0): Obj {
    return { id, status, observed_at: new Date().toISOString(), bytes, entities, data, digest: data === null ? null : digest(data),
      truncation: { truncated: status === "truncated", reason_code: reason } };
  }
  skip(id: string, status: "not_implemented" | "not_requested", reason: string): void {
    this.sources.push(this.entry(id, status, reason, null));
  }
  async collect(id: string, action: () => Promise<{ data: Obj; entities: number; incomplete?: boolean; truncationReason?: string }>): Promise<void> {
    const cancelled = () => {
      if (this.signal.aborted && this.signal.reason instanceof CliError && this.signal.reason.type === "cancelled") throw this.signal.reason;
    };
    cancelled();
    if (this.signal.aborted) { this.sources.push(this.entry(id, "not_collected", "deadline_exceeded", null)); return; }
    if (this.bytes >= this.limits.bytes || this.entities >= this.limits.entities) {
      this.sources.push(this.entry(id, "not_collected", "snapshot_budget_exhausted", null)); return;
    }
    try {
      const result = await action(); cancelled();
      const bytes = Buffer.byteLength(canonical(result.data));
      if (!Number.isSafeInteger(result.entities) || result.entities < 0)
        throw new CliError("internal", "contract_violation", "Invalid snapshot entity accounting.");
      if (bytes + this.bytes > this.limits.bytes || result.entities + this.entities > this.limits.entities) {
        this.sources.push(this.entry(id, "truncated", bytes + this.bytes > this.limits.bytes ? "evidence_byte_limit" : "entity_limit", null)); return;
      }
      this.bytes += bytes; this.entities += result.entities;
      this.sources.push(this.entry(id, result.truncationReason ? "truncated" : result.incomplete ? "partial" : "observed",
        result.truncationReason ?? (result.incomplete ? "source_incomplete" : null), result.data, bytes, result.entities));
    } catch (error) {
      cancelled();
      this.sources.push(this.entry(id, "unavailable", this.signal.aborted ? "deadline_exceeded" : error instanceof CliError ? error.subtype : "collection_failed", null));
    }
  }
}

export async function snapshotCommand(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  const started = performance.now(), from = new Date().toISOString();
  const duration = Math.min(snapshotLimits.milliseconds, Number(inv.options.timeout_ms ?? snapshotLimits.milliseconds));
  const bounded = AbortSignal.any([signal, AbortSignal.timeout(duration)]);
  // Bind once. Subsequent sources cannot silently select a newly edited Profile.
  const reader = await readerFor(inv, bounded), collector = new SnapshotCollector(bounded);
  const queryBudget = { pages: 100, bytes: snapshotLimits.bytes };
  await collector.collect("console", async () => {
    // Each discovery/probe operation below is one page, including a failed
    // attempt; the remaining pages are shared by transaction and lock reads.
    queryBudget.pages--;
    const probe = await reader.consoleProbe();
    return { data: { read_path_available: true, completeness: object(probe.completeness) }, entities: 0 };
  });
  for (const [id, field, action] of [
    ["namespaces", "namespaces", () => reader.namespaces()],
    ["clusters", "clusters", () => reader.clusters()],
    ["servers", "servers", () => reader.servers()],
  ] as const) {
    await collector.collect(id, async () => {
      queryBudget.pages--;
      const data = await action();
      return { data, entities: Array.isArray(data[field]) ? data[field].length : 0,
        incomplete: object(data.completeness).status !== "complete" || (id === "servers" && Array.isArray(data.servers) && data.servers.length === 0) };
    });
  }
  await collector.collect("transactions", async () => {
    const observed = new Date().toISOString();
    if (typeof inv.input.xid === "string") {
      const result = await reader.transaction(inv.input.xid, true, queryBudget), transaction = transactionDTO(result.row, observed);
      const hasBranches = Array.isArray(result.row.branchSessionVOs);
      const branches = hasBranches ? (result.row.branchSessionVOs as Json[]).map(b => branchDTO(b, inv.input.xid as string, observed)) : [];
      if (new Set(branches.map(b => b.branch_id)).size !== branches.length)
        throw new CliError("api", "response_contract_invalid", "Duplicate branch identities in snapshot source.");
      return { data: { transactions: [transaction], branches, completeness: result.completeness,
        missing_evidence: hasBranches ? [] : ["branch_metadata"] }, entities: 1 + branches.length, incomplete: !hasBranches };
    }
    const result = await reader.collect("transaction", { limit: collector.remainingEntities }, false, undefined, queryBudget);
    return { data: { transactions: result.rows.map(row => transactionDTO(row, observed)), branches: [],
      completeness: result.completeness, missing_evidence: ["branches_not_requested_without_xid"] }, entities: result.rows.length,
      incomplete: result.completeness.status !== "complete",
      ...(result.completeness.status === "truncated" ? { truncationReason: String(result.completeness.limit_reason) } : {}) };
  });
  await collector.collect("locks", async () => {
    const result = await reader.collect("lock", { ...(typeof inv.input.xid === "string" ? { xid: inv.input.xid } : {}), limit: collector.remainingEntities },
      false, undefined, queryBudget);
    return { data: { locks: result.rows.map(row => lockDTO(row, new Date().toISOString())), completeness: result.completeness },
      entities: result.rows.length, incomplete: result.completeness.status !== "complete",
      ...(result.completeness.status === "truncated" ? { truncationReason: String(result.completeness.limit_reason) } : {}) };
  });
  if (inv.input.include_config === true) {
    await collector.collect("local_config", async () => {
      const data = inspectLocal(process.cwd(), 0, bounded);
      return { data, entities: Array.isArray(data.facts) ? data.facts.length : 0, incomplete: object(data.completeness).status !== "complete",
        ...(object(data.completeness).status === "truncated" ? { truncationReason: String(object(data.completeness).limit_reason) } : {}) };
    });
  } else collector.skip("local_config", "not_requested", "include_config_not_enabled");
  for (const id of ["logs", "metrics", "database", "config_center"]) collector.skip(id, "not_implemented", "provider_not_implemented");
  const findings: Obj[] = [];
  for (const source of collector.sources) {
    if (!["observed", "not_requested", "not_implemented"].includes(String(source.status)))
      findings.push({ rule_id: "snapshot_source_incomplete", source_ids: [source.id!], status: "unknown", inference: "This source cannot establish a complete observation." });
    if (source.id === "transactions" && source.data) {
      const transactions = object(source.data).transactions;
      if (Array.isArray(transactions)) {
        const flagged = transactions.map(object).filter(t => object(t.status).abnormal !== false || object(t.status).code === 0);
        findings.push({ rule_id: "transaction_abnormal_status", source_ids: [source.id!], status: flagged.length ? "observed_abnormal_or_unknown" : "no_abnormal_status_observed",
          observed_abnormal_or_unknown_count: flagged.length,
          inference: "Management status alone does not establish root cause or business completion; inspect the referenced source for identities." });
      }
    }
  }
  const partial = collector.sources.some(s => ["partial", "unavailable", "truncated", "not_collected"].includes(String(s.status)));
  const readAttempts = reader.readAttempts;
  const snapshot: Obj = { version: "1", snapshot_id: randomUUID(), created_at: from,
    target: reader.description(), requested_xid: safeIdentity(inv.input.xid), sources: collector.sources, findings,
    redaction: { version: "seata-redaction-v1", applied: true, omitted: ["credentials", "applicationData", "unclassified_config_values"] },
    completeness: { status: partial ? "partial" : "complete", scope: typeof inv.input.xid === "string" ? "transaction_snapshot" : "selected_route_snapshot",
      consistency: "best_effort", observed_from: from, observed_to: new Date().toISOString(), duration_ms: Math.ceil(performance.now() - started),
      ...(readAttempts.length ? { read_attempts: readAttempts } : {}),
      limits: { ...snapshotLimits, milliseconds: duration }, usage: collector.usage, unsupported_sources: ["logs", "metrics", "database", "config_center"] } };
  const result = { snapshot };
  if (partial) throw new PartialResult(result, "snapshot_incomplete", "Some snapshot sources were unavailable or exceeded collection limits.");
  return result;
}
