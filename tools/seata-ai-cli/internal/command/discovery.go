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
	"strings"
)

type Capability struct {
	ID                    string   `json:"id"`
	Path                  string   `json:"path"`
	Risk                  string   `json:"risk"`
	Confirmation          string   `json:"confirmation"`
	SchemaVersion         string   `json:"schema_version"`
	SchemaRef             string   `json:"schema_ref"`
	RequiredBackends      []string `json:"required_backends"`
	OptionalBackends      []string `json:"optional_backends"`
	MissingSources        []string `json:"missing_sources"`
	Availability          string   `json:"availability"`
	ReasonCode            string   `json:"reason_code"`
	Authenticated         string   `json:"authenticated"`
	ManagementReachable   string   `json:"management_reachable"`
	CompatibilityEvidence string   `json:"compatibility_evidence"`
	StorageSafe           string   `json:"storage_safe"`
	RouteAssurance        string   `json:"route_assurance"`
	ObservationScope      string   `json:"observation_scope"`
	EvidenceSource        string   `json:"evidence_source"`
	ObservedAt            *string  `json:"observed_at"`
}
type CapabilityData struct {
	Commands  []Capability   `json:"commands"`
	Runtime   map[string]any `json:"runtime"`
	Assurance map[string]any `json:"assurance"`
}

var backendIDs = map[string]bool{"local": true, "local_auth": true, "console_auth": true, "naming_management": true, "credential_store": true, "execution_store": true, "filesystem": true, "environment": true, "rules": true, "embedded_skills": true}

func splitIDs(s string) []string {
	out := []string{}
	for _, v := range strings.Split(s, ",") {
		v = strings.TrimSpace(v)
		if v != "" && v != "—" {
			out = append(out, v)
		}
	}
	return out
}
func (s *Spec) Capability() Capability {
	parts := strings.SplitN(s.Backends, "/", 2)
	c := Capability{ID: s.ID, Path: s.Path, Risk: s.Risk, Confirmation: s.Confirmation, SchemaVersion: "R9", SchemaRef: s.ID, RequiredBackends: splitIDs(parts[0]), OptionalBackends: []string{}, MissingSources: []string{}, Availability: "unavailable", ReasonCode: "not_implemented", Authenticated: "not_evaluated", ManagementReachable: "not_evaluated", CompatibilityEvidence: "not_evaluated", StorageSafe: "not_evaluated", RouteAssurance: "not_evaluated", ObservationScope: "static_only", EvidenceSource: "compiled_registry"}
	if len(parts) == 2 {
		for _, id := range splitIDs(parts[1]) {
			if backendIDs[id] {
				c.OptionalBackends = append(c.OptionalBackends, id)
			} else {
				c.MissingSources = append(c.MissingSources, id)
			}
		}
	}
	if s.Available() {
		c.Availability = "available"
		c.ReasonCode = "local_discovery"
	}
	return c
}
func (r *Registry) Capabilities(id string) CapabilityData {
	data := CapabilityData{Commands: []Capability{}, Runtime: map[string]any{"network_probed": false, "stage": "M1", "platform_support": "experimental"}, Assurance: map[string]any{
		"authentication": "console_bearer", "routing": "namingserver_managed", "write_precondition": "client_fresh_read_plus_server_domain_validation",
		"outcome_tracking": "local_transactional_journal", "dedup_scope": "single_state_store", "server_cas": false, "server_idempotency": false, "route_assurance": "not_evaluated", "causality": "unproven", "production_write_enabled": false, "certified_combinations": []any{},
	}}
	for _, s := range r.Specs {
		if id == "" || s.ID == id {
			data.Commands = append(data.Commands, s.Capability())
		}
	}
	return data
}

type Affordance struct {
	DomainID          string   `json:"domain_id"`
	CLIDomains        []string `json:"cli_domains"`
	Goal              string   `json:"goal"`
	EntryCommands     []string `json:"entry_commands"`
	ReadOnlyDiscovery []string `json:"read_only_discovery"`
	RequiredContext   []string `json:"required_context"`
	RiskSummary       []string `json:"risk_summary"`
	SkillRefs         []string `json:"skill_refs"`
	ExamplesRefs      []string `json:"examples_refs"`
}
type CatalogCommand struct {
	Capability        Capability     `json:"capability"`
	InvocationSchema  map[string]any `json:"invocation_schema"`
	OutputDataSchema  map[string]any `json:"output_data_schema"`
	OutputSchemaScope string         `json:"output_schema_scope"`
}
type ProtocolCatalog struct {
	ProtocolVersion  string           `json:"protocol_version"`
	ContractRevision string           `json:"contract_revision"`
	Commands         []CatalogCommand `json:"commands"`
	Affordances      []Affordance     `json:"affordances"`
	SkillIDs         []string         `json:"skill_ids"`
}

// Catalog is deterministic build data. It performs no profile lookup or probe.
func (r *Registry) Catalog() ProtocolCatalog {
	catalog := ProtocolCatalog{ProtocolVersion: "0.1", ContractRevision: "R9", Commands: []CatalogCommand{}, Affordances: []Affordance{}, SkillIDs: []string{"seata-shared", "seata-onboarding", "seata-integration-check", "seata-transaction-analysis", "seata-incident-diagnosis", "seata-recovery-plan", "seata-controlled-recovery"}}
	domains := []struct{ id, cli, goal string }{
		{"framework", "", "Discover and validate the CLI protocol"}, {"onboarding", "setup", "Inspect and initialize a management environment"}, {"profiles", "profile", "Manage local environment profiles"}, {"authentication", "auth", "Authenticate through a local browser"}, {"skill_distribution", "skills", "Discover and install versioned Seata skills"}, {"environment", "environment", "Inspect environment and management connectivity"}, {"topology", "topology", "Discover namespaces, clusters and servers"}, {"configuration", "config", "Inspect and compare configuration evidence"}, {"transactions", "transaction", "Inspect and manage a transaction"}, {"branches", "branch", "Inspect and manage a transaction branch"}, {"locks", "lock", "Inspect lock ownership and conflicts"}, {"diagnosis", "diagnose", "Collect bounded diagnostic evidence"}, {"execution", "execution", "Recover and reconcile execution records"},
	}
	for _, s := range r.Specs {
		scope := "common_envelope_only"
		if s.OutputDataSchema != nil {
			scope = "command"
		}
		if s.ID == "system.invoke" {
			scope = "delegated"
		}
		catalog.Commands = append(catalog.Commands, CatalogCommand{s.Capability(), s.InvocationSchema, s.OutputDataSchema, scope})
	}
	for _, d := range domains {
		a := Affordance{DomainID: d.id, CLIDomains: []string{}, Goal: d.goal, EntryCommands: []string{}, ReadOnlyDiscovery: []string{"system.help", "system.schema", "system.capabilities"}, RequiredContext: []string{}, RiskSummary: []string{}, SkillRefs: []string{}, ExamplesRefs: []string{}}
		if d.cli != "" {
			a.CLIDomains = append(a.CLIDomains, d.cli)
		}
		for _, s := range r.Specs {
			top := strings.Fields(s.Path)[1]
			if (d.cli == "" && !strings.HasPrefix(s.ID, "system.")) || (d.cli != "" && top != d.cli) {
				continue
			}
			a.EntryCommands = append(a.EntryCommands, s.ID)
			a.RiskSummary = uniqueAppend(a.RiskSummary, s.Risk)
			if s.Remote || strings.HasPrefix(s.ID, "auth.") {
				a.RequiredContext = uniqueAppend(a.RequiredContext, "profile")
			}
			for _, skill := range splitIDs(s.Skills) {
				a.SkillRefs = uniqueAppend(a.SkillRefs, skill)
			}
			if s.ID != "system.invoke" {
				a.ExamplesRefs = append(a.ExamplesRefs, "examples/invocations/"+s.ID+".json")
			}
		}
		catalog.Affordances = append(catalog.Affordances, a)
	}
	return catalog
}
func uniqueAppend(a []string, s string) []string {
	for _, v := range a {
		if v == s {
			return a
		}
	}
	return append(a, s)
}

// Schema returns a self-contained envelope schema; unimplemented data shapes
// remain explicitly incomplete rather than inventing output contracts.
func (s *Spec) Schema() map[string]any {
	doc := envelopeForCommand(s.ID)
	if s.OutputDataSchema != nil {
		doc["allOf"] = append(doc["allOf"].([]any), map[string]any{
			"if":   map[string]any{"type": "object", "properties": map[string]any{"state": map[string]any{"const": "completed"}}, "required": []string{"state"}},
			"then": map[string]any{"type": "object", "properties": map[string]any{"data": s.OutputDataSchema}, "required": []string{"data"}},
		})
	}
	return doc
}
