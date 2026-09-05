/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package command

import (
	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
	"github.com/santhosh-tekuri/jsonschema/v6"
)

func textSchema() map[string]any          { return map[string]any{"type": "string"} }
func boolSchema() map[string]any          { return map[string]any{"type": "boolean"} }
func constant(v any) map[string]any       { return map[string]any{"const": v} }
func listSchema(items any) map[string]any { return map[string]any{"type": "array", "items": items} }
func outputObject(properties map[string]any, required ...string) map[string]any {
	return map[string]any{"type": "object", "properties": properties, "required": required, "additionalProperties": true}
}
func oneOfStrings(values ...string) map[string]any {
	return map[string]any{"type": "string", "enum": values}
}
func outputDataSchemas() map[string]map[string]any {
	capability := outputObject(map[string]any{
		"id": textSchema(), "path": textSchema(), "risk": oneOfStrings("read", "sensitive_read", "diagnostic_read", "local_write", "delegated", "controlled_write", "high_risk_write"),
		"confirmation": oneOfStrings("none", "agent_judgment", "delegated", "hard_gate"), "schema_version": constant("R9"), "schema_ref": textSchema(),
		"required_backends": listSchema(textSchema()), "optional_backends": listSchema(textSchema()), "missing_sources": listSchema(textSchema()),
		"availability": oneOfStrings("available", "degraded", "unavailable", "not_evaluated"), "reason_code": textSchema(),
		"authenticated": oneOfStrings("not_evaluated", "authenticated", "unauthenticated"), "management_reachable": oneOfStrings("not_evaluated", "reachable", "unreachable"),
		"compatibility_evidence": oneOfStrings("not_evaluated", "source_inspected", "contract_tested", "integration_verified"), "storage_safe": oneOfStrings("not_evaluated", "safe", "unsafe"),
		"route_assurance": oneOfStrings("not_evaluated", "selector_only", "resolved_identity"), "observation_scope": textSchema(), "evidence_source": textSchema(), "observed_at": map[string]any{"type": []string{"string", "null"}, "format": "date-time"},
	}, "id", "path", "risk", "confirmation", "schema_version", "schema_ref", "required_backends", "optional_backends", "missing_sources", "availability", "reason_code", "authenticated", "management_reachable", "compatibility_evidence", "storage_safe", "route_assurance", "observation_scope", "evidence_source", "observed_at")
	return map[string]map[string]any{
		"system.version": outputObject(map[string]any{"versions": outputObject(map[string]any{"cli": textSchema(), "protocol": constant("0.1"), "go": textSchema()}, "cli", "protocol", "go"), "contract_revision": constant("R9"), "state_store_id": map[string]any{"type": []string{"string", "null"}}}, "versions", "contract_revision", "state_store_id"),
		"system.help":    outputObject(map[string]any{"help_text": textSchema(), "resolved_path": listSchema(textSchema()), "format": constant("text")}, "help_text", "resolved_path", "format"),
		"system.schema":  outputObject(map[string]any{"command_id": textSchema(), "schema_version": constant("0.1"), "invocation_schema": map[string]any{"type": "object"}, "output_schema": map[string]any{"type": "object"}, "output_schema_scope": oneOfStrings("command", "common_envelope_only")}, "command_id", "schema_version", "invocation_schema", "output_schema", "output_schema_scope"),
		"system.capabilities": outputObject(map[string]any{
			"commands": listSchema(capability), "runtime": outputObject(map[string]any{"network_probed": boolSchema(), "stage": textSchema(), "platform_support": oneOfStrings("experimental", "verified")}, "network_probed", "stage", "platform_support"),
			"assurance": outputObject(map[string]any{"authentication": constant("console_bearer"), "routing": constant("namingserver_managed"), "write_precondition": constant("client_fresh_read_plus_server_domain_validation"), "outcome_tracking": constant("local_transactional_journal"), "dedup_scope": constant("single_state_store"), "server_cas": constant(false), "server_idempotency": constant(false), "route_assurance": oneOfStrings("not_evaluated", "selector_only", "resolved_identity"), "causality": constant("unproven"), "production_write_enabled": boolSchema(), "certified_combinations": listSchema(map[string]any{"type": "object"})}, "authentication", "routing", "write_precondition", "outcome_tracking", "dedup_scope", "server_cas", "server_idempotency", "route_assurance", "causality", "production_write_enabled", "certified_combinations"),
		}, "commands", "runtime", "assurance"),
	}
}
func (r *Registry) OutputValidators() map[string]*jsonschema.Schema {
	out := map[string]*jsonschema.Schema{}
	for _, s := range r.Specs {
		if s.outputValidator != nil {
			out[s.ID] = s.outputValidator
		}
	}
	return out
}
func envelopeForCommand(id string) map[string]any {
	doc := protocol.EnvelopeSchema()
	clauses, _ := doc["allOf"].([]any)
	doc["allOf"] = append(clauses, map[string]any{"type": "object", "properties": map[string]any{"command": constant(id)}, "required": []string{"command"}})
	return doc
}
