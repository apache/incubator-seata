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
import { type Obj, object } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { readerFor } from "./read.js";
import { transactionDTO, branchDTO, completeness, type ManagementReader } from "../adapter/management/read.js";

export async function connectivity(inv: Invocation, signal: AbortSignal,
  createReader: () => Promise<ManagementReader> = () => readerFor(inv, signal)): Promise<Obj> {
  const checks: Obj[] = [];
  const failed = (id: string, error: unknown): Obj => ({ id, status: "unavailable",
    reason_code: error instanceof CliError ? error.subtype : "probe_failed",
    error_type: error instanceof CliError ? error.type : "internal", observed_at: new Date().toISOString(), source: "bounded_read_probe" });
  const assurance: Obj = { compatibility_evidence: "source_inspected", route_assurance: "selector_only",
    authentication: "not_evaluated", consistency: "best_effort", server_mutations: false,
    limitations: ["probe_does_not_certify_server_version", "discovery_is_not_node_liveness", "no_direct_tc_connection",
      "token_validity_not_proven_by_read"] };
  let reader;
  try { reader = await createReader(); }
  catch (error) {
    signal.throwIfAborted();
    return { checks: [failed("profile", error)], overall: "unavailable", assurance };
  }
  const probe = async (id: string, action: () => Promise<Obj>) => {
    const from = new Date().toISOString(), start = reader.readAttempts.length;
    try {
      const result = await action();
      checks.push({ id, status: "available", reason_code: "read_succeeded", observed_at: new Date().toISOString(),
        source: "bounded_read_probe", completeness: result.completeness ?? null });
    } catch (error) {
      signal.throwIfAborted();
      const attemptHistory = reader.readAttempts.slice(start), check = failed(id, error);
      // Count the reserved probe page independently of the attempt ledger:
      // e.g. malformed JSON rejects before a decoded HTTP result is recorded.
      check.completeness = completeness(from, 1, "partial", String(check.reason_code),
        "bounded_read_probe", attemptHistory);
      checks.push(check);
    }
  };
  await probe("console_read", () => reader.consoleProbe());
  await probe("naming_namespace", () => reader.namespaces());
  await probe("naming_clusters", () => reader.clusters());
  await probe("route_discovery", async () => {
    const result = await reader.servers();
    if (!Array.isArray(result.servers) || result.servers.length === 0)
      throw new CliError("not_found", "route_not_found", "No servers were observed for the selected route.");
    return result;
  });
  await probe("transaction_read", async () => {
    const result = await reader.page("transaction", { page: 1, page_size: 1 }, true);
    for (const row of result.rows) {
      transactionDTO(row, new Date().toISOString());
      if (!Array.isArray(row.branchSessionVOs)) throw new CliError("api", "response_contract_invalid", "The branch set was not exposed by the transaction read.");
      for (const branch of row.branchSessionVOs) branchDTO(branch, String(row.xid), new Date().toISOString());
    }
    if (result.completeness.status !== "complete") throw new CliError("api", "lookup_incomplete", "The read probe returned inconsistent evidence.");
    return { completeness: result.completeness };
  });
  const naming = checks.find(c => c.id === "naming_namespace")!;
  assurance.authentication = naming.status === "available" ? "credential_attached_read_succeeded"
    : naming.error_type === "authentication" ? "rejected" : "not_evaluated";
  return { checks, overall: checks.every(c => c.status === "available") ? "available"
    : checks.some(c => c.status === "available") ? "degraded" : "unavailable", assurance };
}

export function capabilityProbe(command: string, report: Obj): Obj {
  const checks = Array.isArray(report.checks) ? report.checks.map(object) : [];
  const id = command === "namespace.list" ? "naming_namespace" : command === "cluster.list" ? "naming_clusters"
    : command === "server.list" ? "route_discovery" : command.startsWith("transaction.") || command.startsWith("branch.") ? "transaction_read" : null;
  const profile = checks.find(c => c.id === "profile");
  if (profile) return { status: "unavailable", reason_code: profile.reason_code ?? "profile_unavailable" };
  if (command === "connectivity.check") return report.overall === "available"
    ? { status: "degraded", reason_code: "read_paths_probed_not_certified" }
    : report.overall === "degraded"
      ? { status: "degraded", reason_code: "some_read_paths_unavailable" }
      : { status: "unavailable", reason_code: "required_read_paths_unavailable" };
  const route = checks.find(c => c.id === "route_discovery");
  if ((command.startsWith("transaction.") || command.startsWith("branch.") || command.startsWith("lock.")) && route?.status !== "available")
    return { status: "unavailable", reason_code: route?.reason_code ?? "route_not_evaluated" };
  const found = checks.find(c => c.id === id);
  // Success of a related read is not proof of all command-specific shapes or
  // a certified deployment. Lock APIs are not probed with invented identities.
  return found ? { status: found.status === "available" ? "degraded" : "unavailable",
    reason_code: found.status === "available" ? "read_family_probed_not_certified" : found.reason_code ?? "probe_failed" }
    : { status: "not_evaluated", reason_code: "command_probe_unavailable" };
}
