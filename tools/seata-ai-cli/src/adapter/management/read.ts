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
import { readRequest, type ReadAttempt } from "../transport/http.js";
import { type Json, type Obj, isObject, digest } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";

export const adapterId = "seata-src-41694e14";
const lockFilterFields = { xid: "xid", branch_id: "branchId", transaction_id: "transactionId",
  resource_id: "resourceId", table_name: "tableName", pk: "pk" } as const;
const transactionFilterFields = { xid: "xid", status: "status", application_id: "applicationId",
  transaction_name: "transactionName" } as const;
const globalNames = ["UnKnown", "Begin", "Committing", "CommitRetrying", "Rollbacking",
  "RollbackRetrying", "TimeoutRollbacking", "TimeoutRollbackRetrying", "AsyncCommitting",
  "Committed", "CommitFailed", "Rollbacked", "RollbackFailed", "TimeoutRollbacked",
  "TimeoutRollbackFailed", "Finished", "CommitRetryTimeout", "RollbackRetryTimeout",
  "Deleting", "StopCommitOrCommitRetry", "StopRollbackOrRollbackRetry"];
const branchNames = ["Unknown", "Registered", "PhaseOne_Done", "PhaseOne_Failed",
  "PhaseOne_Timeout", "PhaseTwo_Committed", "PhaseTwo_CommitFailed_Retryable",
  "PhaseTwo_CommitFailed_Unretryable", "PhaseTwo_Rollbacked", "PhaseTwo_RollbackFailed_Retryable",
  "PhaseTwo_RollbackFailed_Unretryable", "PhaseTwo_CommitFailed_XAER_NOTA_Retryable",
  "PhaseTwo_RollbackFailed_XAER_NOTA_Retryable", "PhaseOne_RDONLY", "STOP_RETRY", "PhaseOne_PrepareFailed"];
export function knownManagementStatus(value: Json | undefined, branch = false): Obj | null {
  const names = branch ? branchNames : globalNames;
  const code = typeof value === "string" ? names.indexOf(value) : value;
  if (typeof code !== "number" || !Number.isSafeInteger(code) || code <= 0 || code >= names.length) return null;
  return { code, name: names[code]! };
}
function invalid(): never {
  throw new CliError("api", "response_contract_invalid", "Management response does not match the pinned adapter contract.");
}
function record(value: Json | undefined): Obj {
  return isObject(value) ? value : invalid();
}
function text(value: Json | undefined): string {
  if (typeof value !== "string" || value.length > 4096) return invalid();
  return value;
}
function count(value: Json | undefined): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 0) return invalid();
  return value;
}
function id(value: Json | undefined): string {
  const result = typeof value === "number" && Number.isSafeInteger(value) ? String(value) : text(value);
  if (!/^[0-9]{1,19}$/.test(result) || BigInt(result) > 9223372036854775807n) return invalid();
  return result;
}
function list(value: Json | undefined): Json[] {
  if (!Array.isArray(value) || value.length > 10000) return invalid();
  return value;
}
// Opaque operational strings may contain embedded credentials. Do not try to
// preserve a partially redacted identity for subsequent calls.
export function safeIdentity(value: Json | undefined): Json {
  if (value === undefined || value === null) return null;
  const raw = text(value);
  if (/bearer\s|[\x00-\x1f\x7f]|(?:password|passwd|token|secret|authorization|cookie)\s*[:=]|\/\/[^/\s]*@/i.test(raw))
    return { digest: digest(raw), redacted: true };
  return raw;
}
function status(value: Json | undefined, branch = false): Obj {
  const code = count(value), names = branch ? branchNames : globalNames;
  const name = names[code];
  const known = code !== 0 && name !== undefined;
  const terminal = branch ? [3, 4, 5, 7, 8, 10, 13, 15].includes(code) : [9, 10, 11, 12, 13, 14, 15, 16, 17].includes(code);
  const abnormal = branch ? [3, 4, 6, 7, 9, 10, 11, 12, 14, 15].includes(code) : [3, 5, 7, 10, 12, 14, 16, 17, 19, 20].includes(code);
  return { code, name: name ?? "unknown", known,
    phase: !known ? "unknown" : terminal ? "terminal" : code === 1 ? "active" : "processing",
    terminal: known ? terminal : null, abnormal: known ? abnormal : null };
}
export function transactionDTO(value: Json, observed: string): Obj {
  const row = record(value);
  return { xid: safeIdentity(text(row.xid)), transaction_id: id(row.transactionId),
    status: status(row.status), transaction_mode: "unknown", branch_mode: "unknown",
    application_id: safeIdentity(row.applicationId), transaction_name: safeIdentity(row.transactionName),
    begin_time: row.beginTime === null || row.beginTime === undefined ? null : count(row.beginTime),
    observed_at: observed, redacted: true, omitted_fields: ["applicationData"],
    limitations: ["transaction_mode_not_exposed", "best_effort_observation"] };
}
export function branchDTO(value: Json, xid: string, observed: string): Obj {
  const row = record(value);
  if (row.xid !== xid) return invalid();
  return { xid: safeIdentity(xid), branch_id: id(row.branchId), transaction_id: id(row.transactionId),
    branch_type: safeIdentity(row.branchType), status: status(row.status, true),
    resource_identity: safeIdentity(row.resourceId), observed_at: observed,
    redacted: true, omitted_fields: ["applicationData", "clientId"] };
}
export function lockDTO(value: Json, observed: string): Obj {
  const row = record(value);
  return { xid: safeIdentity(text(row.xid)), transaction_id: id(row.transactionId), branch_id: id(row.branchId),
    resource_id: safeIdentity(row.resourceId), table_name: safeIdentity(row.tableName),
    pk: safeIdentity(row.pk), row_key: safeIdentity(row.rowKey), observed_at: observed, redacted: true };
}
export function completeness(from: string, pages = 1, state = "complete", reason: string | null = null, scope = "single_page",
  readAttempts: ReadAttempt[] = []): Obj {
  return { scope, status: state, consistency: "best_effort", pages_read: pages,
    limit_reason: reason, observed_from: from, observed_to: new Date().toISOString(),
    ...(readAttempts.length ? { read_attempts: readAttempts } : {}) };
}
export function evidence(command: string, subject: Json, facts: Obj, complete: Obj): Obj {
  return { id: digest({ command, subject, facts, complete }), source: { command, adapter: adapterId },
    observed_at: new Date().toISOString(), subject, facts, completeness: complete,
    redacted: true, limitations: ["not_a_consistent_snapshot", "selector_only_route"] };
}
export interface ReadTarget { endpoint: string; consoleEndpoint?: string; namespace: string; cluster?: string; vgroup?: string; }
/** Remaining source-query budget, optionally shared by a composed read. */
export interface CollectionBudget { pages: number; bytes: number; }
export class ManagementReader {
  private readonly attemptHistory: ReadAttempt[] = [];
  get readAttempts(): ReadAttempt[] { return this.attemptHistory.map(attempt => ({ ...attempt })); }
  private readonly recordAttempt = (attempt: ReadAttempt): void => {
    // Composed reads allow at most 100 pages, each with at most three attempts.
    if (this.attemptHistory.length < 300) this.attemptHistory.push({ ...attempt });
  };
  description(): Obj {
    return { naming_endpoint: this.target.endpoint, console_endpoint: this.target.consoleEndpoint ?? null,
      namespace: safeIdentity(this.target.namespace), cluster: safeIdentity(this.target.cluster), vgroup: safeIdentity(this.target.vgroup),
      adapter: "seata-src-41694e14", route_assurance: "selector_only" };
  }
  get credentialPresent(): boolean { return this.credential !== undefined; }
  constructor(private readonly target: ReadTarget, private readonly signal: AbortSignal,
    private readonly credential?: string, private readonly beforeAttempt?: () => Promise<void>, private readonly ca?: string,
    private readonly proxyEndpoint?: string) {
    if ((!target.cluster && !target.vgroup) || (target.cluster && target.vgroup))
      throw new CliError("config", "invalid_route", "Specify exactly one cluster or vgroup.");
    for (const value of [target.namespace, target.cluster, target.vgroup])
      if (value !== undefined && /[\x00-\x1f\x7f-\uffff]/.test(value))
        throw new CliError("validation", "invalid_route", "Route is not representable in management HTTP headers.");
  }
  async get(suffix: string, query: Record<string, string> = {}, routed = false): Promise<{ body: Json; readAttempts: ReadAttempt[] }> {
    const headers: Record<string, string> = {};
    if (this.credential) headers.authorization = this.credential;
    if (routed) {
      headers["x-seata-namespace"] = this.target.namespace;
      // ConsoleRemotingFilter reads lowercase vgroup. The separate discovery
      // controller uses camel-case vGroup; these are not interchangeable.
      if (this.target.vgroup) query = { ...query, vgroup: this.target.vgroup };
      else headers["x-seata-cluster"] = this.target.cluster!;
    }
    const result = await readRequest({ endpoint: this.target.endpoint, suffix, method: "GET", query, headers,
      ...(this.proxyEndpoint === undefined ? {} : { proxyEndpoint: this.proxyEndpoint }),
      ...(this.ca === undefined ? {} : { ca: this.ca }) }, this.signal, this.beforeAttempt, this.recordAttempt);
    if (result.status === 401) throw new CliError("authentication", "authentication_required", "Management authentication is required or expired.");
    if (result.status === 403) throw new CliError("authorization", "permission_denied", "Management access was denied.");
    if (result.status !== 200) throw new CliError("api", "management_http_error", "Management endpoint returned an unsuccessful HTTP status.");
    return { body: result.body, readAttempts: result.readAttempts ?? [] };
  }
  async single(suffix: string, query: Record<string, string>, routed = false): Promise<{ data: Json; readAttempts: ReadAttempt[] }> {
    const response = await this.get(suffix, query, routed), result = record(response.body);
    if (result.code !== "200") throw new CliError("api", "management_rejected", "Management endpoint reported a business failure.");
    if (result.data === undefined) return invalid();
    return { data: result.data, readAttempts: response.readAttempts };
  }
  async consoleProbe(): Promise<Obj> {
    const from = new Date().toISOString();
    if (!this.target.consoleEndpoint) throw new CliError("config", "console_endpoint_missing", "The Console endpoint is not configured.");
    const response = await readRequest({ endpoint: this.target.consoleEndpoint, suffix: "/api/v1/overview/getData", method: "GET",
      ...(this.proxyEndpoint === undefined ? {} : { proxyEndpoint: this.proxyEndpoint }),
      ...(this.ca === undefined ? {} : { ca: this.ca }),
      ...(this.credential ? { headers: { authorization: this.credential } } : {}) }, this.signal, this.beforeAttempt, this.recordAttempt);
    if (response.status === 401) throw new CliError("authentication", "authentication_required", "Console authentication is required or expired.");
    if (response.status === 403) throw new CliError("authorization", "permission_denied", "Console read access was denied.");
    if (response.status !== 200) throw new CliError("api", "management_http_error", "Console returned an unsuccessful status.");
    const body = record(response.body);
    if (body.code !== "200") throw new CliError("api", "management_rejected", "Console reported a business failure.");
    list(body.data);
    // OverviewController returns sample overview records, not a version or
    // deployment descriptor. Its response proves only this read path works.
    return { completeness: completeness(from, 1, "complete", null, "single_page", response.readAttempts ?? []) };
  }
  async namespaces(): Promise<Obj> {
    const from = new Date().toISOString();
    const response = await this.single("/api/v1/naming/namespace", {}), data = record(response.data);
    return { namespaces: Object.entries(data).map(([name, value]) => {
      const row = record(value);
      return { namespace: safeIdentity(name), clusters: list(row.clusters).map(safeIdentity), vgroups: list(row.vgroups).map(safeIdentity) };
    }), completeness: completeness(from, 1, "complete", null, "target_set", response.readAttempts) };
  }
  async clusters(): Promise<Obj> {
    const from = new Date().toISOString();
    const response = await this.get("/api/v1/naming/clusters", { namespace: this.target.namespace }), data = list(response.body);
    return { clusters: data.map(value => {
      const row = record(value);
      return { name: safeIdentity(text(row.clusterName)), cluster_type: safeIdentity(row.clusterType),
        vgroups: list(row.vgroupMapping ?? row.vGroupMapping).map(safeIdentity) };
    }), completeness: completeness(from, 1, "complete", null, "target_set", response.readAttempts) };
  }
  async servers(): Promise<Obj> {
    const from = new Date().toISOString();
    let clusters: Obj[], readAttempts: ReadAttempt[];
    if (this.target.vgroup) {
      const read = await this.get("/api/v1/naming/discovery", {
        namespace: this.target.namespace, vGroup: this.target.vgroup,
      }), response = record(read.body);
      readAttempts = read.readAttempts;
      clusters = list(response.clusterList).map(record);
    } else {
      const read = await this.single("/api/v1/naming/clusterData", {
        namespace: this.target.namespace, clusterName: this.target.cluster!,
      }), response = record(read.data);
      readAttempts = read.readAttempts;
      if (response.clusterName !== this.target.cluster) return invalid();
      clusters = [{ ...response, unitData: Object.values(record(response.unitData)) }];
    }
    const servers: Obj[] = [], versions = new Set<string>();
    const nodeEndpoint = (value: Json | undefined): Json => {
      if (value === null || value === undefined) return null;
      const address = record(value), port = count(address.port);
      if (port < 1 || port > 65535) return invalid();
      return { host: safeIdentity(text(address.host)), port };
    };
    for (const cluster of clusters) for (const unitValue of list(cluster.unitData)) {
      const unit = record(unitValue);
      for (const nodeValue of list(unit.namingInstanceList)) {
        const node = record(nodeValue), version = safeIdentity(node.version);
        if (typeof version === "string") versions.add(version);
        if (typeof node.healthy !== "boolean") return invalid();
        servers.push({ namespace: safeIdentity(this.target.namespace), cluster: safeIdentity(text(cluster.clusterName)),
          cluster_type: safeIdentity(cluster.clusterType), unit: safeIdentity(text(unit.unitName)),
          role: safeIdentity(node.role), version, healthy: node.healthy,
          control_endpoint: nodeEndpoint(node.control), transaction_endpoint: nodeEndpoint(node.transaction),
          observed_at: from, redacted: true, omitted_fields: ["metadata", "internal"],
          limitations: ["discovery_not_node_liveness", "addresses_are_not_connection_targets"] });
        if (servers.length > 10000) return invalid();
      }
    }
    return { servers, observed_versions: [...versions], completeness: completeness(from, 1, "complete", null, "route_discovery", readAttempts) };
  }
  async page(kind: "transaction" | "lock", input: Obj, withBranch = false, budget?: CollectionBudget): Promise<{ rows: Obj[]; pagination: Obj; completeness: Obj }> {
    if (budget !== undefined || input.time_range !== undefined || Object.keys(kind === "lock" ? lockFilterFields : transactionFilterFields)
      .some(key => input[key] !== undefined))
      return this.timePage(kind, input, withBranch, budget);
    return this.wirePage(kind, input, withBranch);
  }
  private async wirePage(kind: "transaction" | "lock", input: Obj, withBranch: boolean): Promise<{ rows: Obj[]; pagination: Obj; completeness: Obj }> {
    const from = new Date().toISOString(), page = Number(input.page ?? 1);
    const size = Math.min(Number(input.page_size ?? 20), Number(input.limit ?? 10000));
    const query: Record<string, string> = { pageNum: String(page), pageSize: String(size) };
    if (kind === "transaction") {
      // These server filters narrow to a superset of our exact match across
      // DB, File and Redis. Redis's combined XID+status path can return data
      // with total=0 on a mismatch, so omit status when XID is present.
      for (const [key, wire] of Object.entries(transactionFilterFields))
        if (input[key] !== undefined && (key !== "status" || input.xid === undefined))
          query[wire] = String(input[key]);
      query.withBranch = String(withBranch);
    } else if (input.xid !== undefined) {
      // Redis requires an XID or a complete row identity for lock reads.
      // DB/File may apply fewer fields, so every returned row is filtered
      // locally before the result can be called complete.
      query.xid = String(input.xid);
    } else if (input.resource_id !== undefined && input.table_name !== undefined && input.pk !== undefined) {
      query.resourceId = String(input.resource_id);
      query.tableName = String(input.table_name);
      query.pk = String(input.pk);
    }
    const response = await this.get(kind === "transaction" ? "/api/v1/console/globalSession/query" : "/api/v1/console/globalLock/query", query, true),
      result = record(response.body);
    if (result.code !== "200") throw new CliError("api", "management_rejected", "Management endpoint reported a business failure.");
    const rows = list(result.data).map(record), total = count(result.total);
    if (count(result.pageNum) !== page || count(result.pageSize) !== size) return invalid();
    // Redis's scoped lock implementation returns the complete XID/row set
    // even when it exceeds pageSize. Accept only an internally consistent
    // first page; public pagination still happens after bounded collection.
    const unpagedScopedLocks = kind === "lock" && page === 1 && (query.xid !== undefined || query.resourceId !== undefined) &&
      rows.length > size && rows.length === total;
    if (rows.length > size && !unpagedScopedLocks) return invalid();
    // Count and data are separate reads on the server. An inconsistent count
    // cannot be turned into reliable absence or a false has_more=false.
    const expectedRows = unpagedScopedLocks ? rows.length : Math.min(size, Math.max(0, total - (page - 1) * size));
    const inconsistent = rows.length !== expectedRows;
    return { rows, pagination: { page, page_size: size, returned: rows.length, total,
      has_more: inconsistent ? null : unpagedScopedLocks ? false : page * size < total },
      completeness: completeness(from, 1, inconsistent ? "unknown" : "complete", inconsistent ? "count_data_drift" : null,
        "single_page", response.readAttempts) };
  }
  private async timePage(kind: "transaction" | "lock", input: Obj, withBranch: boolean, budget?: CollectionBudget): Promise<{ rows: Obj[]; pagination: Obj; completeness: Obj }> {
    const result = await this.collect(kind, input, withBranch, undefined, budget);
    const page = Number(input.page ?? 1), size = Math.min(Number(input.page_size ?? 20), Number(input.limit ?? 10000));
    const rows = result.rows.slice((page - 1) * size, page * size);
    const complete = result.completeness.status === "complete";
    return { rows, pagination: { page, page_size: size, returned: rows.length, total: complete ? result.rows.length : null,
      has_more: result.rows.length > page * size ? true : complete ? false : null }, completeness: result.completeness };
  }
  async collect(kind: "transaction" | "lock", input: Obj, withBranch = false,
    predicate: (row: Obj) => boolean | null = () => true,
    budget: CollectionBudget = { pages: 100, bytes: 8 * 1024 * 1024 }): Promise<{ rows: Obj[]; completeness: Obj }> {
    // Store implementations differ in time precision and inclusive upper
    // bounds. Do not assume DB-specific filtering semantics for file/Redis.
    // Read bounded pages and enforce the public [start,end) contract locally.
    const from = new Date().toISOString(), range = input.time_range === undefined ? undefined : record(input.time_range);
    const start = range ? Date.parse(text(range.start)) : 0, end = range ? Date.parse(text(range.end)) : 0;
    const localFilters = Object.entries(kind === "lock" ? lockFilterFields : transactionFilterFields)
      .filter(([key]) => input[key] !== undefined);
    const selected: Obj[] = [], seen = new Set<string>(), readAttempts: ReadAttempt[] = [];
    let pages = 0, reason: string | null = "page_limit", lastTotal: Json | undefined;
    let drift = false, missingFilter = false;
    const limit = Number(input.limit ?? 10000);
    scan: for (let page = 1; page <= 100; page++) {
      if (budget.pages <= 0) break;
      if (budget.bytes <= 0) { reason = "evidence_byte_limit"; break; }
      // Pinned server filters may be exact, contains, or ignored by store
      // mode. They only narrow candidates; exact filtering remains local.
      budget.pages--;
      const result = await this.wirePage(kind, { ...input, page, page_size: 100, limit: 10000 }, withBranch);
      pages++;
      if (Array.isArray(result.completeness.read_attempts))
        readAttempts.push(...result.completeness.read_attempts as ReadAttempt[]);
      if (lastTotal !== undefined && lastTotal !== result.pagination.total) drift = true;
      lastTotal = result.pagination.total;
      if (result.completeness.status !== "complete") drift = true;
      for (const [index, row] of result.rows.entries()) {
        budget.bytes -= Buffer.byteLength(JSON.stringify(row));
        if (budget.bytes < 0) { reason = "evidence_byte_limit"; break scan; }
        const identity = kind === "transaction" ? text(row.xid) : digest([row.xid ?? null, row.branchId ?? null,
          row.resourceId ?? null, row.tableName ?? null, row.pk ?? null]);
        if (seen.has(identity)) { drift = true; continue; }
        seen.add(identity);
        if (range) {
          const timestamp = row[kind === "transaction" ? "beginTime" : "gmtCreate"];
          if (timestamp === null || timestamp === undefined) { reason = "missing_time_evidence"; break scan; }
          const time = count(timestamp);
          if (time < start || time >= end) continue;
        }
        let localMatch: boolean | null = true;
        for (const [key, wire] of localFilters) {
          const actual = row[wire];
          if (kind === "transaction" ? key === "status" ? typeof actual !== "number" : typeof actual !== "string"
            : typeof actual !== "string" && typeof actual !== "number") { localMatch = null; break; }
          if (String(actual) !== String(input[key])) { localMatch = false; break; }
        }
        if (localMatch === null) { missingFilter = true; continue; }
        if (localMatch === false) continue;
        const match = predicate(row);
        if (match === null) missingFilter = true;
        if (match) selected.push(row);
        if (selected.length >= limit && (index < result.rows.length - 1 || result.pagination.has_more !== false)) {
          reason = "entity_limit"; break scan;
        }
      }
      if (result.completeness.status !== "complete") { reason = "count_or_identity_drift"; break; }
      if (result.pagination.has_more === false) { reason = missingFilter ? "missing_filter_evidence" : drift ? "count_or_identity_drift" : null; break; }
      if (result.rows.length === 0) { reason = "empty_page_before_end"; break; }
    }
    return { rows: selected,
      completeness: completeness(from, pages, reason === null ? "complete" : "truncated", reason, "bounded_filtered_query", readAttempts) };
  }
  async observeTransaction(xid: string, withBranch = false, budget?: CollectionBudget): Promise<{ row: Obj | null; completeness: Obj }> {
    const result = await this.page("transaction", { xid, page: 1, page_size: 2 }, withBranch, budget);
    if (result.rows.some(row => row.xid !== xid)) return invalid();
    if (result.rows.length === 0 && result.pagination.total === 0 && result.completeness.status === "complete")
      return { row: null, completeness: { ...result.completeness, scope: "exact_identity", status: "unknown",
        limit_reason: "empty_exact_query_not_absence_proof" } };
    if (result.rows.length !== 1 || result.pagination.total !== 1 || result.completeness.status !== "complete")
      throw new CliError("api", "lookup_incomplete", "The exact transaction lookup did not establish one unambiguous current object; absence is not proof of completion.");
    return { row: result.rows[0]!, completeness: { ...result.completeness, scope: "exact_identity" } };
  }
  async transaction(xid: string, withBranch = false, budget?: CollectionBudget): Promise<{ row: Obj; completeness: Obj }> {
    const result = await this.observeTransaction(xid, withBranch, budget);
    if (result.row === null)
      throw new CliError("api", "lookup_incomplete", "The exact transaction lookup did not establish one unambiguous current object; absence is not proof of completion.");
    return { row: result.row, completeness: result.completeness };
  }
  async observeBranches(xid: string): Promise<{ rows: Obj[] | null; completeness: Obj }> {
    const result = await this.observeTransaction(xid, true);
    if (result.row === null) return { rows: null, completeness: result.completeness };
    const rows = list(result.row.branchSessionVOs).map(record);
    const ids = new Set<string>();
    for (const row of rows) {
      if (row.xid !== xid || ids.has(id(row.branchId))) return invalid();
      ids.add(id(row.branchId));
    }
    return { rows, completeness: { ...result.completeness, scope: "embedded_branch_set" } };
  }
  async branches(xid: string): Promise<{ rows: Obj[]; completeness: Obj }> {
    const result = await this.observeBranches(xid);
    if (result.rows === null)
      throw new CliError("api", "lookup_incomplete", "The parent transaction was not observed in the exact current-route query; absence is not proof of completion.");
    return { rows: result.rows, completeness: result.completeness };
  }
  async checkLock(xid: string, branchId: string): Promise<Obj> {
    const from = new Date().toISOString();
    const response = await this.single("/api/v1/console/globalLock/check", { xid, branchId }, true), exists = response.data;
    if (typeof exists !== "boolean") return invalid();
    return { owner_session_exists: exists, evidence: [evidence("lock.check", { xid: safeIdentity(xid), branch_id: branchId },
      { owner_session_exists: exists }, completeness(from, 1, "complete", null, "single_page", response.readAttempts))] };
  }
}
