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
	"bytes"
	"encoding/json"
	"strings"
	"testing"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

func TestDiscoveryOutputContracts(t *testing.T) {
	r, err := New()
	if err != nil {
		t.Fatal(err)
	}
	cases := []Invocation{NewInvocation("system.version"), NewInvocation("system.help"), NewInvocation("system.capabilities"), NewInvocation("system.schema")}
	cases[3].Input["command_id"] = "system.version"
	for _, inv := range cases {
		t.Run(inv.Command, func(t *testing.T) {
			e := r.Dispatch(inv, time.Now())
			schema := r.Get(inv.Command).OutputDataSchema
			if schema == nil {
				t.Fatal("implemented handler missing output contract")
			}
			validator, err := protocol.Compile("https://seata.invalid/test-output/"+inv.Command, schema)
			if err != nil {
				t.Fatal(err)
			}
			raw, _ := json.Marshal(e.Data)
			v, err := protocol.Decode(bytes.NewReader(raw))
			if err != nil {
				t.Fatal(err)
			}
			if err = validator.Validate(v); err != nil {
				t.Fatal(err)
			}
			obj := v.(map[string]any)
			for _, required := range schema["required"].([]string) {
				copy := map[string]any{}
				for k, v := range obj {
					copy[k] = v
				}
				delete(copy, required)
				if err = validator.Validate(copy); err == nil {
					t.Fatalf("missing %s accepted", required)
				}
			}
			obj["future_optional_field"] = true
			if err = validator.Validate(obj); err != nil {
				t.Fatal("optional output extension rejected", err)
			}
		})
	}
}
func TestCapabilitiesUseTypedDependenciesAndHonestObservations(t *testing.T) {
	r, err := New()
	if err != nil {
		t.Fatal(err)
	}
	data := r.Capabilities("")
	if len(data.Commands) != 57 {
		t.Fatal(len(data.Commands))
	}
	for _, c := range data.Commands {
		if c.SchemaVersion != "R9" {
			t.Fatal(c)
		}
		for _, id := range append(append([]string{}, c.RequiredBackends...), c.OptionalBackends...) {
			if strings.ContainsAny(id, " ,/") {
				t.Fatal("unparsed backend", id)
			}
		}
		if c.ManagementReachable != "not_evaluated" || c.Authenticated != "not_evaluated" {
			t.Fatal("invented environment evidence", c)
		}
		if c.Risk == "high_risk_write" && c.Availability != "unavailable" {
			t.Fatal("uncertified write advertised", c)
		}
	}
}
func TestCatalogIsDeterministicAndComplete(t *testing.T) {
	r, err := New()
	if err != nil {
		t.Fatal(err)
	}
	a, err := json.Marshal(r.Catalog())
	if err != nil {
		t.Fatal(err)
	}
	b, err := json.Marshal(r.Catalog())
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(a, b) {
		t.Fatal("unstable catalog")
	}
	catalog := r.Catalog()
	if len(catalog.Affordances) != 13 || len(catalog.SkillIDs) != 7 {
		t.Fatal("discovery domains or skills missing")
	}
	seen := map[string]bool{}
	for _, domain := range catalog.Affordances {
		for _, id := range domain.EntryCommands {
			if r.Get(id) == nil {
				t.Fatal(id)
			}
			seen[id] = true
		}
	}
	if len(seen) != 57 {
		t.Fatal("unmapped command", len(seen))
	}
}
