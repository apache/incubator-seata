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

export function outputs() {
  const text = { type: "string" };
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
  const status = { enum: ["available", "unavailable", "degraded", "not_evaluated"] };
  return {
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
        server_mutations: { const: "disabled" },
        release_scope: { const: "read_only" },
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
        dedup_scope: { const: "not_available" },
        causality: { const: "unproven" },
      }),
    }),
  };
}
