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
export function outputs(registry, execution) {
  const text = { type: "string" },
    bool = { type: "boolean" },
    integer = { type: "integer", minimum: 0 };
  const strings = { type: "array", items: text };
  const obj = (properties) => ({
    type: "object",
    properties,
    required: Object.keys(properties),
    additionalProperties: true,
  });
  const nullableText = { type: ["string", "null"] };
  const array = (items) => ({ type: "array", items });
  const record = { type: "object" };
  const profileInput = registry.find((s) => s.id === "profile.create")
    .invocation.properties.input.properties.config;
  const profile = {
    ...profileInput,
    additionalProperties: true,
    properties: {
      ...profileInput.properties,
      profile_id: text,
      revision: { type: "integer", minimum: 1 },
      auth_generation: integer,
      trust: obj({
        state: { enum: ["accepted", "unverified"] },
        accepted_target_digest: nullableText,
      }),
    },
    required: [
      ...profileInput.required,
      "profile_id",
      "revision",
      "auth_generation",
      "trust",
    ],
  };
  const status = {
    enum: ["available", "degraded", "unavailable", "not_evaluated"],
  };
  const schemas = {
    "skills.install": obj({ manifest: record, installation_state: { enum: ["complete", "unchanged"] }, host_status: record }),
    "skills.status": obj({ content_installed: bool, exposure_valid: bool, host_discovered: { const: "unknown" }, session_loaded: { const: "unknown" },
      versions: obj({ cli: text, bundled_skills: text, installed_skills: nullableText }),
      host: obj({ id: { const: "generic_agent_skills" }, version: { type: "null" }, support: { const: "directory_only_unverified_host" } }),
      ownership: { const: "not_verified" }, installation_path: text, installation_mode: { const: "direct" },
      last_verified_at: { type: "string", format: "date-time" }, findings: array(obj({ code: text })), limitations: strings }),
    "execution.reconcile": obj({ execution, observation: { enum: ["unknown", "postcondition_observed", "precondition_still_observed"] }, evidence: array(record), current_evidence: array(record),
      desired_condition_currently_observed: obj({ observed: { type: ["boolean", "null"] }, reason_code: text,
        scope: { const: "current_management_state_only" }, business_completion: { const: "not_claimed" }, causality: { const: "unproven" } }),
      comparison_to_before: obj({ status: { enum: ["not_evaluated", "unchanged", "changed"] }, reason_code: text,
        scope: { const: "management_status_only" }, historical_source: { const: "local_execution_plan" }, historical_plan_id: nullableText,
        before_status: { type: ["object", "null"] }, current_status: { type: ["object", "null"] },
        preconditions_currently_observed: { type: ["boolean", "null"] } }), completeness: record,
      route_assurance: { const: "selector_only" }, causality: { const: "unproven" }, next_action: { const: "manual_diagnosis" },
      replay_allowed: { const: false } }),
    "skills.validate": obj({ valid: bool, findings: array(obj({ code: text, skill_id: nullableText, relative_path: nullableText })),
      validated_version: nullableText, expected_version: text, validation_scope: { const: "bundled_content_integrity" },
      source: { enum: ["package", "custom_directory"] }, skills_checked: integer, files_checked: integer,
      host_discovered: { const: "unknown" }, session_loaded: { const: "unknown" } }),
    "skills.export": obj({ export_manifest: obj({ manifest_version: { const: "1" }, cli_version: text, skills: array(record),
      scope: { const: "read_only" }, ownership: { const: "not_established_by_manifest" },
      host_discovered: { const: "unknown" }, session_loaded: { const: "unknown" }, complete: bool }),
      files: array(obj({ path: text, relative_path: text, bytes: integer, digest: text,
        permissions: { const: "0600" }, ownership: { const: "state_store_verified" } })) }),
    "skills.list": obj({ skills: array(obj({ skill_id: text, version: text, description: text, digest: text,
      files: array(obj({ path: text, bytes: integer, digest: text })) })) }),
    "skills.read": obj({ skill_id: text, version: text, content: text, digest: text,
      section: { enum: ["main", "references", "examples"] }, relative_path: text }),
    "system.version": obj({
      versions: obj({
        cli: text,
        protocol: { const: "0.1" },
        node: text,
        implementation: { const: "typescript" },
      }),
      contract_revision: { const: "R9" },
      state_store_id: nullableText,
    }),
    "system.help": obj({
      help_text: text,
      resolved_path: strings,
      format: { const: "text" },
    }),
    "system.schema": obj({
      command_id: text,
      schema_version: text,
      invocation_schema: record,
      output_schema: record,
      output_schema_status: { enum: ["implemented", "not_implemented"] },
    }),
    "system.capabilities": obj({
      commands: array(
        obj({
          command_id: text,
          risk: text,
          confirmation: text,
          schema_version: text,
          backends: text,
          status,
          reason_code: text,
          observed_at: { type: "string", format: "date-time" },
          scope: { enum: ["local", "profile"] },
          source: text,
        }),
      ),
      runtime: obj({
        authenticated: text,
        management_reachable: text,
        storage_safe: status,
        compatibility_evidence: text,
        production_writes: { const: false },
        optional_sources: obj(
          Object.fromEntries(
            ["logs", "metrics", "database", "config_center"].map((k) => [
              k,
              { const: "not_implemented" },
            ]),
          ),
        ),
      }),
      assurance: obj({
        server_cas: { const: false },
        server_idempotency: { const: false },
        dedup_scope: { const: "single_state_store" },
        causality: { const: "unproven" },
      }),
    }),
    "profile.list": obj({
      profiles: array(profile),
      current_profile: nullableText,
    }),
    "profile.get": obj({ profile }),
    "profile.create": obj({ profile, trust: profile.properties.trust }),
    "setup.initialize": obj({
      profile,
      trust: profile.properties.trust,
      next_steps: strings,
    }),
    "profile.update": obj({
      profile,
      invalidated_plans: bool,
      auth_invalidated: bool,
    }),
    "profile.use": obj({ current_profile: text }),
    "profile.delete": obj({
      deleted_profile_id: text,
      credential_cleanup: text,
      retained_execution_count: integer,
    }),
    "auth.status": obj({
      authenticated: bool,
      generation: integer,
      storage: text,
      verification: text,
      expiry_hint: nullableText,
    }),
    "auth.logout": obj({
      authenticated: { const: false },
      auth_generation: integer,
      credential_cleanup: text,
    }),
    "auth.login.cancel": obj({
      flow_id: text,
      flow_state: {
        enum: ["pending", "authenticated", "failed", "expired", "cancelled"],
      },
      changed: bool,
    }),
    "auth.login.status": obj({ flow_id: text,
      flow_state: { enum: ["pending", "authenticated", "failed", "expired", "cancelled"] },
      phase: { enum: ["pending", "submitting", "authenticated", "failed", "expired", "cancelled"] },
      reason: { type: ["object", "null"] }, expires_at: { type: "string", format: "date-time" },
      terminal_at: nullableText }),
    "auth.login": { ...obj({ flow_id: text, flow_state: { const: "pending" }, expires_at: { type: "string", format: "date-time" },
      assurance: { enum: ["local_user_file", "os_keyring"] }, host_support: { const: "not_evaluated" } }),
      properties: { flow_id: text, flow_state: { const: "pending" }, expires_at: { type: "string", format: "date-time" },
        assurance: { enum: ["local_user_file", "os_keyring"] }, host_support: { const: "not_evaluated" },
        verification_url: { type: "string", format: "uri" }, verification_url_available: { const: false } } },
    "execution.get": obj({
      execution,
      events: array(
        obj({
          sequence: { type: "integer", minimum: 1 },
          kind: text,
          data: record,
          created_at: { type: "string", format: "date-time" },
        }),
      ),
      state_store_id: text,
      recovery: obj({ next_action: text, replay_allowed: { const: false } }),
    }),
  };
  const completeness = obj({
    scope: text, status: { enum: ["complete", "truncated", "partial", "unknown"] },
    consistency: { const: "best_effort" }, pages_read: integer,
    limit_reason: nullableText, observed_from: { type: "string", format: "date-time" },
    observed_to: { type: "string", format: "date-time" },
  });
  completeness.properties.read_attempts = { type: "array", maxItems: 300, items: {
    ...obj({ attempt: { type: "integer", minimum: 1, maximum: 3 },
      path: { type: "string", pattern: "^/api/v1/[A-Za-z0-9/_-]+$" },
      selector_digest: { type: "string", pattern: "^sha256:[a-f0-9]{64}$" },
      started_at: { type: "string", format: "date-time" }, observed_at: { type: "string", format: "date-time" },
      outcome: { enum: ["http_response", "connection_error"] },
      http_status: { type: ["integer", "null"], minimum: 100, maximum: 599 } }),
    additionalProperties: false,
  } };
  const identity = { anyOf: [nullableText, obj({ digest: text, redacted: { const: true } })] };
  const domainStatus = obj({ code: integer, name: text, known: bool, phase: text,
    terminal: { type: ["boolean", "null"] }, abnormal: { type: ["boolean", "null"] } });
  const transaction = obj({ xid: identity, transaction_id: text, status: domainStatus,
    transaction_mode: text, branch_mode: text, application_id: identity,
    transaction_name: identity, begin_time: { type: ["integer", "null"], minimum: 0 },
    observed_at: { type: "string", format: "date-time" }, redacted: bool,
    omitted_fields: strings, limitations: strings });
  const branch = obj({ xid: identity, transaction_id: text, branch_id: text,
    branch_type: identity, status: domainStatus, resource_identity: identity,
    observed_at: { type: "string", format: "date-time" }, redacted: bool, omitted_fields: strings });
  const lock = obj({ xid: identity, transaction_id: text, branch_id: text,
    resource_id: identity, table_name: identity, pk: identity, row_key: identity,
    observed_at: { type: "string", format: "date-time" }, redacted: bool });
  const evidence = array(obj({ id: text, source: obj({ command: text, adapter: text }),
    observed_at: { type: "string", format: "date-time" }, subject: {}, facts: record,
    completeness, redacted: bool, limitations: strings }));
  const pagination = obj({ page: { type: "integer", minimum: 1 }, page_size: { type: "integer", minimum: 1, maximum: 100 },
    returned: integer, total: { type: ["integer", "null"], minimum: 0 }, has_more: { type: ["boolean", "null"] } });
  const configFact = obj({ id: text, key: text, value: nullableText, value_kind: text, source: text, location: text,
    scope: { enum: ["local_project", "local_cli_process"] }, target_process: { type: "null" }, evidence_kind: { enum: ["observed_file", "observed_environment"] },
    precedence: { type: "null" }, effective_status: { const: "unknown" }, version: { type: "null" }, document: { type: ["integer", "null"], minimum: 0 },
    sensitive: bool, redacted: bool, unresolved_reason: nullableText, limitations: strings });
  const configCompleteness = obj({ status: { const: "complete" }, scope: { const: "selected_local_sources_only" }, remote_effective_configuration: { const: "unknown" } });
  const comparison = { enum: ["equal", "different", "unknown", "absent"] };
  const scanScope = obj({ root: text, roots: strings, directories: strings, max_parent_depth: { type: "integer", minimum: 0, maximum: 3 },
    filename_pattern: text, environment_allowlist: strings, network_requests: { const: 0 }, files: integer, bytes: integer });
  const scanCompleteness = obj({ status: { enum: ["complete", "partial", "truncated"] }, scope: { const: "declared_local_scan_only" },
    limit_reason: nullableText, remote_effective_configuration: { const: "unknown" } });
  const scanSources = array(obj({ source: text, status: { enum: ["observed", "unavailable", "truncated"] }, reason_code: text, bytes: integer }));
  const snapshotCompleteness = obj({ status: { enum: ["complete", "partial"] }, scope: text, consistency: { const: "best_effort" },
    observed_from: { type: "string", format: "date-time" }, observed_to: { type: "string", format: "date-time" }, duration_ms: integer,
    limits: obj({ bytes: { const: 8388608 }, entities: { const: 10000 }, milliseconds: { type: "integer", minimum: 1, maximum: 60000 } }),
    usage: obj({ bytes: { type: "integer", minimum: 0, maximum: 8388608 }, entities: { type: "integer", minimum: 0, maximum: 10000 } }), unsupported_sources: strings });
  Object.assign(schemas, {
    "diagnose.snapshot.export": obj({ export_manifest: obj({ version: { const: "1" }, snapshot_id: { type: "string", format: "uuid" },
      redaction_version: { const: "seata-redaction-v1" }, encoding: { const: "utf-8" }, canonicalization: text,
      entries: array(obj({ path: { const: "snapshot.json" }, bytes: integer, digest: text, redaction_version: { const: "seata-redaction-v1" } })),
      completeness: snapshotCompleteness, manifest_digest_scope: text,
      artifact: obj({ path: text, bytes: integer, digest: text, permissions: { const: "0600" }, ownership: { const: "state_store_verified" }, format: { enum: ["json", "zip"] } }) }),
      completeness: snapshotCompleteness }),
    "diagnose.snapshot": obj({ snapshot: obj({ version: { const: "1" }, snapshot_id: { type: "string", format: "uuid" },
      created_at: { type: "string", format: "date-time" }, target: obj({ naming_endpoint: text, console_endpoint: nullableText,
        namespace: identity, cluster: identity, vgroup: identity, adapter: text, route_assurance: { const: "selector_only" } }), requested_xid: identity,
      sources: array(obj({ id: text, status: { enum: ["observed", "partial", "unavailable", "truncated", "not_collected", "not_requested", "not_implemented"] },
        observed_at: { type: "string", format: "date-time" }, bytes: integer, entities: integer, data: { type: ["object", "null"] }, digest: nullableText,
        truncation: obj({ truncated: bool, reason_code: nullableText }) })),
      findings: array(obj({ rule_id: text, source_ids: strings, status: text, inference: text })),
      redaction: obj({ version: { const: "seata-redaction-v1" }, applied: { const: true }, omitted: strings }),
      completeness: snapshotCompleteness }) }),
    "setup.inspect": obj({ candidates: array(obj({ candidate_id: text, endpoints: obj({ console_endpoint: nullableText, naming_endpoint: nullableText }),
      source: obj({ evidence_id: text, location: text, key: text }), confidence: { enum: ["medium", "low"] }, trust_state: { const: "unverified" },
      reason_code: text, limitations: strings })), scan_scope: scanScope, completeness: scanCompleteness, sources: scanSources, limitations: strings }),
    "environment.inspect": obj({ environment_facts: array(configFact), scope: obj({ local_project_root: text, observed_process: { const: "cli_only" }, remote_process: { type: "null" } }),
      scan_scope: scanScope, sources: scanSources, completeness: scanCompleteness, limitations: strings }),
    "config.inspect": obj({ observed: array(configFact), inferred_effective: array(configFact), shadowed: array(configFact), unresolved: array(configFact),
      limitations: strings, scan_scope: obj({ sources: strings, environment_allowlist: strings, include_environment: bool, files: integer, bytes: integer }), completeness: configCompleteness }),
    "config.validate": obj({ findings: array(obj({ rule_id: text, status: { enum: ["passed", "failed", "unknown", "not_evaluated"] }, evidence_ids: strings, reason_code: text })),
      evidence: array(configFact), ruleset_version: text, completeness: configCompleteness, limitations: strings }),
    "config.diff": obj({ changes: array(obj({ key: text, document: { type: ["integer", "null"], minimum: 0 },
      left: { anyOf: [configFact, { type: "null" }] }, right: { anyOf: [configFact, { type: "null" }] }, value: comparison, source: comparison, scope: comparison,
      version: comparison, evidence_level: comparison, effective_comparison: { const: "unknown" } })), limitations: strings }),
    "diagnose.rules.list": obj({ rules: array(obj({ rule_id: text, scope: { enum: ["environment", "transaction"] }, description: text,
      required_evidence: strings, limitations: strings })), ruleset_version: text }),
    "diagnose.rules.get": obj({ rule: obj({ rule_id: text, scope: { enum: ["environment", "transaction"] }, description: text,
      required_evidence: strings, limitations: strings }), required_evidence: strings, limitations: strings }),
    "diagnose.run": obj({ findings: array(obj({ rule_id: text, evidence_ids: strings, observed_fact: record,
      inference: text, confidence: { enum: ["high", "unknown"] }, counter_evidence: array(record), recommendation: text })), evidence,
      not_evaluated: array(obj({ rule_id: text, missing_evidence: strings, reason_code: text })), completeness, ruleset_version: text }),
    "transaction.abnormal": obj({ transactions: array(transaction), evidence, findings: array(obj({ rule_id: text, ruleset_version: text,
      evidence_ids: strings, observed_fact: record, inference: text, confidence: { enum: ["high", "unknown"] },
      counter_evidence: array(record), recommendation: text })), pagination, completeness }),
    "transaction.trace": obj({ transaction, branches: array(branch), locks: array(lock),
      timeline: array(obj({ timestamp: { type: "string", format: "date-time" }, event: text, source: text, evidence_ids: strings })),
      evidence, missing_sources: array(obj({ source: text, reason_code: text })), completeness }),
    "lock.conflicts": obj({ holders: array(lock), pagination,
      potential_conflicts: array(obj({ resource_digest: text, holders: array(lock), classification: { const: "potential_conflict" },
        confirmed_waiter: { const: false }, limitations: strings })), waiter_evidence: array(record), completeness }),
    "connectivity.check": obj({ checks: array(obj({ id: text, status,
      reason_code: text, observed_at: { type: "string", format: "date-time" }, source: text })), overall: status,
      assurance: obj({ compatibility_evidence: { const: "source_inspected" }, route_assurance: { const: "selector_only" },
        authentication: text, consistency: { const: "best_effort" }, server_mutations: { const: false }, limitations: strings }) }),
    "server.list": obj({ servers: array(obj({ namespace: identity, cluster: identity,
      cluster_type: identity, unit: identity, role: identity, version: identity, healthy: bool,
      control_endpoint: { anyOf: [{ type: "null" }, obj({ host: identity, port: { type: "integer", minimum: 1, maximum: 65535 } })] },
      transaction_endpoint: { anyOf: [{ type: "null" }, obj({ host: identity, port: { type: "integer", minimum: 1, maximum: 65535 } })] },
      observed_at: { type: "string", format: "date-time" }, redacted: bool, omitted_fields: strings, limitations: strings })),
      observed_versions: strings, completeness }),
    "namespace.list": obj({ namespaces: array(obj({ namespace: identity, clusters: array(identity), vgroups: array(identity) })), completeness }),
    "cluster.list": obj({ clusters: array(obj({ name: identity, cluster_type: identity, vgroups: array(identity) })), completeness }),
    "transaction.list": obj({ transactions: array(transaction), pagination, completeness }),
    "transaction.get": obj({ transaction, evidence, completeness }),
    "branch.list": obj({ branches: array(branch), pagination, completeness }),
    "branch.get": obj({ branch, evidence, completeness }),
    "lock.list": obj({ locks: array(lock), pagination, completeness }),
    "lock.check": obj({ owner_session_exists: bool, evidence }),
  });
  return Object.fromEntries(
    registry.map((spec) => [
      spec.id,
      schemas[spec.id] ?? {
        not: {},
        description:
          "No enabled success handler; this command returns a failed Envelope.",
      },
    ]),
  );
}
