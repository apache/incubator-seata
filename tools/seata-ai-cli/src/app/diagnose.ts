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
import { type Obj, object, isObject } from "../protocol/value.js";
import { CliError, PartialResult } from "../protocol/error.js";
import { analysisCommand, requireRuleset, ruleset } from "./analysis.js";
import { connectivity } from "./connectivity.js";
import { completeness, evidence, safeIdentity, type ManagementReader } from "../adapter/management/read.js";
import { configCommand } from "./config.js";
import { readerFor } from "./read.js";

export function compareVgroupFacts(facts: Obj[], clusters: Obj[]): Obj[] {
  const groups = facts.filter(f => typeof f.key === "string" &&
    /(?:^|\.)tx[-.]?service[-.]?group$/i.test(f.key) && typeof f.value === "string" && f.redacted === false);
  return groups.map(group => {
    const mappings = facts.filter(f => typeof f.key === "string" &&
      /(?:^|\.)service\.vgroup-?Mapping\.(.+)$/i.exec(f.key)?.[1] === group.value &&
      f.document === group.document && f.source === group.source && typeof f.value === "string" && f.redacted === false);
    const local = [...new Set(mappings.map(f => f.value!))];
    const remote = [...new Set(clusters.filter(c => Array.isArray(c.vgroups) && c.vgroups.includes(group.value!)).map(c => c.name ?? null))];
    const evaluable = !group.unresolved_reason && mappings.every(f => !f.unresolved_reason) &&
      local.length === 1 && remote.length === 1 && typeof remote[0] === "string";
    return { vgroup: group.value!, local_clusters: local, observed_management_clusters: remote,
      status: evaluable ? local[0] === remote[0] ? "passed" : "failed" : "unknown",
      reason_code: evaluable ? "observed_mapping_comparison" : "mapping_evidence_ambiguous_or_missing",
      local_evidence_ids: [group.id!, ...mappings.map(f => f.id!)], remote_effective_configuration: "unknown" };
  });
}

export const diagnosticLocal = new Set(["diagnose.rules.list", "diagnose.rules.get"]);
export const diagnosticRemote = new Set(["diagnose.run"]);
const rules: Obj[] = [
  { rule_id: "management_connectivity", scope: "environment", description: "Observe Console, NamingServer and routed TC read availability.",
    required_evidence: ["connectivity.check"], limitations: ["not_a_version_certification", "failure_is_not_root_cause"] },
  { rule_id: "transaction_abnormal_status", scope: "transaction", description: "Flag observed retry/failure/paused or unknown global status; min_age_ms is an optional additional age filter.",
    required_evidence: ["transaction_status"], limitations: ["status_does_not_prove_rm_outcome", "unknown_status_has_unknown_semantics"] },
  { rule_id: "branch_abnormal_status", scope: "transaction", description: "Summarize abnormal or unknown branch states in the observed branch set.",
    required_evidence: ["branch_statuses"], limitations: ["branch_set_is_a_best_effort_observation"] },
  { rule_id: "lock_potential_conflict", scope: "transaction", description: "Multiple observed holders for a resource are potential conflicts, not demonstrated waiters or deadlocks.",
    required_evidence: ["lock_holder_records"], limitations: ["waiter_evidence_not_exposed", "concurrent_page_movement_possible"] },
  { rule_id: "vgroup_config_consistency", scope: "environment", description: "Compare observed local vGroup configuration with management route evidence.",
    required_evidence: ["local_config", "route_discovery"], limitations: ["local_files_are_not_server_effective_configuration"] },
  { rule_id: "undo_log", scope: "transaction", description: "Do not infer AT undo_log correctness without database evidence.",
    required_evidence: ["database_undo_log"], limitations: ["database_provider_not_implemented"] },
  { rule_id: "database_availability", scope: "environment", description: "Do not infer database health from a successful management read.",
    required_evidence: ["database_health"], limitations: ["database_provider_not_implemented"] },
  { rule_id: "root_cause", scope: "transaction", description: "Keep causal diagnosis unevaluated without application, RM and server evidence.",
    required_evidence: ["application_logs", "rm_outcome", "server_logs"], limitations: ["logs_provider_not_implemented", "causality_unproven"] },
];

export async function diagnoseCommand(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  if (inv.command === "diagnose.rules.list") return { rules, ruleset_version: ruleset };
  if (inv.command === "diagnose.rules.get") {
    const rule = rules.find(r => r.rule_id === inv.input.rule_id);
    if (!rule) throw new CliError("not_found", "rule_not_found", "No bundled diagnostic rule has this ID.");
    return { rule, required_evidence: rule.required_evidence ?? [], limitations: rule.limitations ?? [] };
  }
  requireRuleset(inv.input.ruleset_id);
  const from = new Date().toISOString(), findings: Obj[] = [], records: Obj[] = [];
  const notEvaluated: Obj[] = [
    { rule_id: "database_availability", missing_evidence: ["database_health"], reason_code: "provider_not_implemented" },
    { rule_id: "undo_log", missing_evidence: ["database_undo_log"], reason_code: "provider_not_implemented" },
    { rule_id: "root_cause", missing_evidence: ["application_logs", "rm_outcome", "server_logs"], reason_code: "provider_not_implemented" },
  ];
  // Pin one binding for the whole invocation, including failed initial binding.
  // Later phases must not silently switch targets after a concurrent edit/login.
  let readerPromise: Promise<ManagementReader> | undefined;
  const boundReader = () => readerPromise ??= readerFor(inv, signal);
  const report = await connectivity(inv, signal, boundReader), checks = Array.isArray(report.checks) ? report.checks.map(object) : [];
  // A local Profile failure sends no probe. Every other check reserves one
  // logical page, even when it fails; trace must use the remaining budget.
  const probePages = checks.filter(check => check.id !== "profile").length;
  const queryBudget = { pages: 100 - probePages, bytes: 8 * 1024 * 1024 };
  const readAttempts = checks.flatMap(check => isObject(check.completeness) && Array.isArray(check.completeness.read_attempts)
    ? check.completeness.read_attempts : []);
  let partial = report.overall !== "available";
  const observed = completeness(from, probePages, partial ? "partial" : "complete", null, "management_read_paths");
  const record = evidence("connectivity.check", { profile: safeIdentity(inv.context.profile) }, report, observed);
  records.push(record);
  findings.push({ rule_id: "management_connectivity", evidence_ids: [record.id!],
    observed_fact: { checks }, confidence: "high", counter_evidence: [],
    inference: partial ? "Some required read paths were not established. The failure alone does not identify a root cause." : "The probed read paths completed; this does not certify the deployment or database health.",
    recommendation: partial ? "Inspect the failed check's typed reason and verify configuration, permissions and routing." : "No repair action is inferred from these observations." });
  if (inv.input.scope === "environment") {
    let client: ManagementReader | undefined;
    let attemptStart = 0;
    try {
      const local = await configCommand({ ...inv, command: "config.inspect", input: { include_environment: false } }, signal);
      client = await boundReader();
      attemptStart = client.readAttempts.length;
      queryBudget.pages--;
      const remote = await client.clusters();
      const comparisons = compareVgroupFacts((local.observed as Obj[]).map(object), (remote.clusters as Obj[]).map(object));
      const localRecord = evidence("config.inspect", null, local, completeness(from, 0, "complete", null, "selected_local_sources_only"));
      const remoteRecord = evidence("cluster.list", null, remote, object(remote.completeness));
      records.push(localRecord, remoteRecord);
      if (!comparisons.length) notEvaluated.push({ rule_id: "vgroup_config_consistency", missing_evidence: ["local_vgroup"], reason_code: "local_vgroup_not_observed" });
      else findings.push({ rule_id: "vgroup_config_consistency", evidence_ids: [localRecord.id!, remoteRecord.id!],
        observed_fact: { comparisons }, confidence: comparisons.every(c => c.status !== "unknown") ? "high" : "unknown",
        counter_evidence: [], inference: "This compares local file mappings with management observations, not effective TC configuration or configuration precedence.",
        recommendation: "Verify the active application configuration and selected namespace before making any configuration change." });
    } catch (error) {
      signal.throwIfAborted(); partial = true;
      notEvaluated.push({ rule_id: "vgroup_config_consistency", missing_evidence: ["local_config_or_route_discovery"],
        reason_code: error instanceof CliError ? error.subtype : "read_failed" });
    } finally {
      if (client) readAttempts.push(...client.readAttempts.slice(attemptStart));
    }
  } else notEvaluated.push({ rule_id: "vgroup_config_consistency", missing_evidence: ["local_config"], reason_code: "outside_transaction_scope" });
  if (inv.input.scope === "transaction") {
    let trace: Obj | undefined;
    let client: ManagementReader | undefined;
    let attemptStart = 0;
    try {
      // Own the bound reader so attempts remain available even if trace cannot
      // return a result. Reuse its existing authentication and target checks.
      client = await boundReader();
      attemptStart = client.readAttempts.length;
      trace = await analysisCommand({ ...inv, command: "transaction.trace", input: { xid: inv.input.xid ?? null } }, signal, queryBudget, client);
    }
    catch (error) {
      signal.throwIfAborted(); partial = true;
      if (error instanceof PartialResult) trace = error.data;
      else {
        const reason = error instanceof CliError ? error.subtype : "read_failed";
        notEvaluated.push(
          { rule_id: "transaction_abnormal_status", missing_evidence: ["transaction_status"], reason_code: reason },
          { rule_id: "branch_abnormal_status", missing_evidence: ["branch_statuses"], reason_code: reason },
          { rule_id: "lock_potential_conflict", missing_evidence: ["other_transaction_holders", "waiter_evidence"], reason_code: reason },
        );
      }
    } finally {
      if (client) readAttempts.push(...client.readAttempts.slice(attemptStart));
    }
    if (trace) {
      if (Array.isArray(trace.evidence)) records.push(...trace.evidence.map(object));
      const transaction = object(trace.transaction), status = object(transaction.status);
      const ids = Array.isArray(trace.evidence) ? trace.evidence.map(e => object(e).id!) : [];
      findings.push({ rule_id: "transaction_abnormal_status", evidence_ids: ids,
        observed_fact: { xid: transaction.xid ?? null, status }, confidence: status.known === true ? "high" : "unknown", counter_evidence: [],
        inference: status.known !== true ? "The observed status is unknown or ambiguous; business completion and cause remain unproven."
          : status.abnormal === true ? "The observed status warrants investigation; business completion remains unproven."
            : "No known abnormal status was established; this is not proof of business success.",
        recommendation: "Correlate the observed state with application and RM evidence; this CLI does not execute recovery actions." });
      if (Array.isArray(trace.branches) && !(Array.isArray(trace.missing_sources) && trace.missing_sources.some(s => object(s).source === "branches"))) {
        const branches = trace.branches.map(object), flagged = branches.filter(b => object(b.status).abnormal !== false);
        findings.push({ rule_id: "branch_abnormal_status", evidence_ids: ids,
          observed_fact: { observed_branch_count: branches.length, flagged_branch_ids: flagged.map(b => b.branch_id!) },
          inference: "This is an observed branch-state summary, not an RM outcome or root-cause determination.",
          confidence: "high", counter_evidence: [], recommendation: "Inspect flagged branches and obtain RM-side evidence before deciding on recovery." });
      } else notEvaluated.push({ rule_id: "branch_abnormal_status", missing_evidence: ["branch_statuses"], reason_code: "branch_metadata_missing" });
      notEvaluated.push({ rule_id: "lock_potential_conflict", missing_evidence: ["other_transaction_holders", "waiter_evidence"], reason_code: "trace_is_transaction_scoped" });
    }
  }
  const totalCompleteness = completeness(from, 100 - queryBudget.pages, partial ? "partial" : "complete",
    partial ? "required_management_evidence_missing" : null, "diagnostic_scope");
  if (readAttempts.length) totalCompleteness.read_attempts = readAttempts;
  const result = { findings, evidence: records, not_evaluated: notEvaluated,
    completeness: totalCompleteness, ruleset_version: ruleset };
  if (partial) throw new PartialResult(result, "diagnosis_incomplete", "Some required management evidence could not be collected.");
  return result;
}
