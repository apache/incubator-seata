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
	_ "embed"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
	"github.com/santhosh-tekuri/jsonschema/v6"
)

//go:embed registry.json
var registryJSON []byte

type Spec struct {
	ID               string         `json:"id"`
	Path             string         `json:"path"`
	Risk             string         `json:"risk"`
	Confirmation     string         `json:"confirmation"`
	Remote           bool           `json:"remote"`
	Backends         string         `json:"backends"`
	Skills           string         `json:"skills"`
	InvocationSchema map[string]any `json:"invocation_schema"`
	validator        *jsonschema.Schema
}

func (s *Spec) Available() bool {
	switch s.ID {
	case "system.version", "system.help", "system.schema", "system.capabilities", "system.invoke":
		return true
	}
	return false
}
func (s *Spec) InputProperties() map[string]any {
	if s.InvocationSchema == nil {
		return nil
	}
	return s.InvocationSchema["properties"].(map[string]any)["input"].(map[string]any)["properties"].(map[string]any)
}
func (s *Spec) Properties(container string) map[string]any {
	if s.InvocationSchema == nil {
		return nil
	}
	return s.InvocationSchema["properties"].(map[string]any)[container].(map[string]any)["properties"].(map[string]any)
}

type Registry struct {
	Specs []*Spec
	byID  map[string]*Spec
}

func New() (*Registry, error) {
	r := &Registry{byID: map[string]*Spec{}}
	if err := json.Unmarshal(registryJSON, &r.Specs); err != nil {
		return nil, err
	}
	paths := map[string]bool{}
	for _, s := range r.Specs {
		if r.byID[s.ID] != nil || paths[s.Path] {
			return nil, errors.New("duplicate_registry_binding")
		}
		r.byID[s.ID] = s
		paths[s.Path] = true
		if s.ID == "system.invoke" {
			continue
		}
		var err error
		s.validator, err = protocol.Compile("https://seata.invalid/"+s.ID, s.InvocationSchema)
		if err != nil {
			return nil, fmt.Errorf("%s: %w", s.ID, err)
		}
	}
	return r, nil
}
func (r *Registry) Get(id string) *Spec { return r.byID[id] }

type Invocation struct {
	ProtocolVersion  string         `json:"protocol_version"`
	ContractRevision string         `json:"contract_revision"`
	Command          string         `json:"command"`
	Context          map[string]any `json:"context"`
	Input            map[string]any `json:"input"`
	Options          map[string]any `json:"options"`
}

func NewInvocation(id string) Invocation {
	return Invocation{"0.1", "R9", id, map[string]any{}, map[string]any{}, map[string]any{}}
}
func (r *Registry) Validate(v any) (Invocation, *protocol.Fault) {
	inv := NewInvocation("")
	obj, ok := v.(map[string]any)
	if !ok {
		return inv, protocol.Failure("validation", "schema_violation", "Invocation must be an object.")
	}
	id, _ := obj["command"].(string)
	inv.Command = id
	s := r.Get(id)
	if s == nil {
		return inv, protocol.Failure("validation", "unknown_command", "Unknown command ID.")
	}
	if id == "system.invoke" {
		return inv, protocol.Failure("validation", "recursive_invoke", "Invoke cannot target itself.")
	}
	if err := s.validator.Validate(v); err != nil {
		return inv, protocol.Failure("validation", "schema_violation", "Invocation does not match the command schema.")
	}
	b, _ := json.Marshal(v)
	d := json.NewDecoder(bytes.NewReader(b))
	d.UseNumber()
	if err := d.Decode(&inv); err != nil {
		return inv, protocol.Failure("internal", "unexpected", "Cannot decode validated invocation.")
	}
	if err := semantic(inv.Input); err != nil {
		return inv, protocol.Failure("validation", "invalid_argument", err.Error())
	}
	if n, ok := inv.Options["client_request_id"].(string); ok {
		inv.Options["client_request_id"] = strings.ToLower(n)
	}
	if n, ok := inv.Input["client_request_id"].(string); ok {
		inv.Input["client_request_id"] = strings.ToLower(n)
	}
	if _, ok := inv.Options["timeout_ms"]; !ok {
		inv.Options["timeout_ms"] = json.Number("30000")
	}
	return inv, nil
}
func semantic(m map[string]any) error {
	if id, ok := m["branch_id"].(string); ok {
		if n, err := strconv.ParseInt(id, 10, 64); err != nil || n <= 0 {
			return errors.New("branch_id must be a positive signed int64 string.")
		}
	}
	if tr, ok := m["time_range"].(map[string]any); ok {
		start, _ := tr["start"].(string)
		end, _ := tr["end"].(string)
		a, ea := time.Parse(time.RFC3339Nano, start)
		b, eb := time.Parse(time.RFC3339Nano, end)
		if ea != nil || eb != nil || !a.Before(b) {
			return errors.New("time_range requires start before end.")
		}
		tr["start"] = a.UTC().Format(time.RFC3339Nano)
		tr["end"] = b.UTC().Format(time.RFC3339Nano)
	}
	return nil
}
