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
          scope: { const: "local" },
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
