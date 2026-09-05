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
	"fmt"
	"runtime"
	"strings"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

// Dispatch is shared by typed CLI and canonical invoke. Unimplemented handlers
// fail closed; registration alone does not advertise an executable capability.
func (r *Registry) Dispatch(inv Invocation, started time.Time) protocol.Envelope {
	e := protocol.NewEnvelope(inv.Command, started)
	// Discovery does not echo caller-controlled context or probe supplied endpoints.
	spec := r.Get(inv.Command)
	if spec == nil || inv.Command == "system.invoke" {
		e.Fail(protocol.Failure("validation", "invalid_argument", "Dispatch requires a validated target command."))
		return e
	}
	if !spec.Available() {
		e.Fail(protocol.Failure("policy", "command_unavailable", "This handler is not implemented in this development checkpoint."))
		return e
	}
	switch inv.Command {
	case "system.version":
		e.Data = map[string]any{"versions": map[string]any{"cli": "0.1.0-dev", "protocol": "0.1", "go": runtime.Version()}, "contract_revision": "R9", "state_store_id": nil}
	case "system.help":
		path := []string{}
		if a, ok := inv.Input["path"].([]any); ok {
			for _, v := range a {
				path = append(path, v.(string))
			}
		}
		field, _ := inv.Input["field"].(string)
		help, err := r.Help(path, field)
		if err != nil {
			e.Fail(protocol.Failure("validation", "invalid_argument", err.Error()))
			return e
		}
		e.Data = map[string]any{"help_text": help, "resolved_path": path, "format": "text"}
	case "system.schema":
		id := inv.Input["command_id"].(string)
		s := r.Get(id)
		if s == nil || s.InvocationSchema == nil {
			e.Fail(protocol.Failure("validation", "invalid_argument", "Select an executable target command ID."))
			return e
		}
		scope := "common_envelope_only"
		if s.OutputDataSchema != nil {
			scope = "command"
		} else {
			e.Warnings = append(e.Warnings, map[string]string{"code": "output_schema_incomplete", "message": "This unimplemented handler has no command-specific data schema yet."})
		}
		e.Data = map[string]any{"command_id": id, "schema_version": "0.1", "invocation_schema": s.InvocationSchema, "output_schema": s.Schema(), "output_schema_scope": scope}
	case "system.capabilities":
		id, _ := inv.Input["command_id"].(string)
		if id != "" && r.Get(id) == nil {
			e.Fail(protocol.Failure("validation", "invalid_argument", "Unknown command ID."))
			return e
		}
		e.Data = r.Capabilities(id)
	}
	return e
}
func (r *Registry) Help(path []string, field string) (string, error) {
	prefix := "seata-ai"
	if len(path) > 0 {
		prefix += " " + strings.Join(path, " ")
	}
	if len(path) == 0 {
		if field != "" {
			return "", fmt.Errorf("Field help requires a leaf command.")
		}
		return "seata-ai: Seata management CLI development checkpoint\n\nDiscover: help [domain [action]], schema <command-id>, capabilities [command-id]\nInvoke: invoke < invocation.json (stdin; R9 canonical contract)\n\nDomains: setup profile auth skills environment topology config transaction branch lock diagnose execution\nOnly protocol discovery is implemented. Management commands are unavailable.\n", nil
	}
	lines := []string{}
	for _, s := range r.Specs {
		if s.Path == prefix {
			if field != "" {
				p, ok := s.InputProperties()[field]
				if !ok {
					return "", fmt.Errorf("Unknown input field.")
				}
				return fmt.Sprintf("%s input.%s\n%v\n", s.Path, field, p), nil
			}
			return fmt.Sprintf("%s\nCommand: %s\nRisk: %s; confirmation: %s\nInput flags use kebab-case; objects and arrays use JSON values.\nUse schema %s for exact fields and constraints.\n", s.Path, s.ID, s.Risk, s.Confirmation, s.ID), nil
		}
		if strings.HasPrefix(s.Path, prefix+" ") {
			lines = append(lines, fmt.Sprintf("%s (%s)", s.Path, s.Risk))
		}
	}
	if field != "" || len(lines) == 0 {
		return "", fmt.Errorf("Unknown command path or field on a non-leaf path.")
	}
	return strings.Join(lines, "\n") + "\n", nil
}
