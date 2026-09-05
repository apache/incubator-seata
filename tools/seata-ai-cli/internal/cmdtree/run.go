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

package cmdtree

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/command"
	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
	"github.com/spf13/cobra"
	"github.com/spf13/pflag"
)

type flagValue struct {
	value any
	set   bool
	kind  string
}

func (v *flagValue) String() string { return "" }
func (v *flagValue) Type() string   { return v.kind }
func (v *flagValue) Set(s string) error {
	if v.set {
		return fmt.Errorf("duplicate flag")
	}
	v.set = true
	switch v.kind {
	case "string":
		v.value = s
	default:
		x, e := protocol.Decode(strings.NewReader(s))
		if e != nil {
			return fmt.Errorf("invalid JSON flag value")
		}
		v.value = x
	}
	return nil
}

func Run(args []string, stdin io.Reader, stdout, stderr io.Writer) int {
	started := time.Now()
	reg, err := command.New()
	if err != nil {
		return emitInternal(stdout, stderr, started)
	}
	emitter, err := protocol.NewEmitter(reg.OutputValidators())
	if err != nil {
		return emitInternal(stdout, stderr, started)
	}
	env := protocol.NewEnvelope("system.help", started)
	var selected *command.Spec
	var result *protocol.Envelope
	root := &cobra.Command{Use: "seata-ai", SilenceErrors: true, SilenceUsage: true, CompletionOptions: cobra.CompletionOptions{DisableDefaultCmd: true}}
	root.SetOut(io.Discard)
	root.SetErr(io.Discard)
	nodes := map[string]*cobra.Command{"": root}
	root.SetHelpCommand(&cobra.Command{Use: "__disabled_help", Hidden: true})
	root.SetHelpFunc(func(c *cobra.Command, _ []string) {
		path := strings.Fields(strings.TrimPrefix(c.CommandPath(), "seata-ai"))
		txt, e := reg.Help(path, "")
		if e == nil {
			_, _ = io.WriteString(stdout, txt)
		}
	})
	root.RunE = func(c *cobra.Command, a []string) error {
		if len(a) > 0 {
			return fmt.Errorf("unexpected argument")
		}
		inv := command.NewInvocation("system.help")
		e := reg.Dispatch(inv, started)
		result = &e
		return nil
	}
	for _, spec := range reg.Specs {
		tokens := strings.Fields(strings.TrimPrefix(spec.Path, "seata-ai "))
		prefix := ""
		var leaf *cobra.Command
		for _, token := range tokens {
			next := strings.TrimSpace(prefix + " " + token)
			leaf = nodes[next]
			if leaf == nil {
				leaf = &cobra.Command{Use: token, SilenceErrors: true, SilenceUsage: true}
				nodes[prefix].AddCommand(leaf)
				nodes[next] = leaf
			}
			prefix = next
		}
		s := spec
		c := leaf
		if s.ID == "system.invoke" {
			c.RunE = func(c *cobra.Command, a []string) error {
				selected = s
				if len(a) > 0 || c.Flags().NFlag() > 0 {
					return fmt.Errorf("invoke reads only stdin at this checkpoint")
				}
				v, err := protocol.Decode(stdin)
				if err != nil {
					e := protocol.NewEnvelope(s.ID, started)
					e.Fail(protocol.Failure("validation", err.Error(), "Invalid canonical JSON input."))
					result = &e
					return nil
				}
				inv, f := reg.Validate(v)
				id := s.ID
				if reg.Get(inv.Command) != nil {
					id = inv.Command
				}
				e := protocol.NewEnvelope(id, started)
				if f != nil {
					e.Fail(f)
				} else {
					e = reg.Dispatch(inv, started)
				}
				e.Meta["invoked_via"] = "system.invoke"
				result = &e
				return nil
			}
			continue
		}
		bindings := map[string]map[string]*flagValue{}
		for _, container := range []string{"context", "input", "options"} {
			bindings[container] = map[string]*flagValue{}
			for name, raw := range s.Properties(container) {
				prop := raw.(map[string]any)
				kind, _ := prop["type"].(string)
				flag := strings.ReplaceAll(name, "_", "-")
				if name == "confirm_high_risk" {
					flag = "yes"
				}
				// Lookup request IDs belong to Input. Write request IDs belong to Options.
				if name == "client_request_id" && container == "options" && s.InputProperties()[name] != nil {
					continue
				}
				v := &flagValue{kind: kind}
				bindings[container][name] = v
				c.Flags().Var(v, flag, container+"."+name)
				if kind == "boolean" {
					c.Flags().Lookup(flag).NoOptDefVal = "true"
				}
			}
		}
		c.RunE = func(c *cobra.Command, a []string) error {
			selected = s
			inv := command.NewInvocation(s.ID)
			for container, fields := range bindings {
				dest := inv.Input
				if container == "context" {
					dest = inv.Context
				}
				if container == "options" {
					dest = inv.Options
				}
				for key, v := range fields {
					if v.set {
						dest[key] = v.value
					}
				}
			}
			positional := ""
			switch s.ID {
			case "system.schema", "system.capabilities":
				positional = "command_id"
			case "skills.read":
				positional = "skill_id"
			case "system.help":
				if len(a) > 0 {
					if _, ok := inv.Input["path"]; ok {
						return fmt.Errorf("duplicate path")
					}
					inv.Input["path"] = a
					a = nil
				}
			}
			if len(a) > 0 {
				if len(a) != 1 || positional == "" {
					return fmt.Errorf("unexpected arguments")
				}
				if _, ok := inv.Input[positional]; ok {
					return fmt.Errorf("duplicate positional")
				}
				inv.Input[positional] = a[0]
			}
			b, err := json.Marshal(inv)
			if err != nil {
				return err
			}
			v, err := protocol.Decode(bytes.NewReader(b))
			if err != nil {
				return err
			}
			normalized, f := reg.Validate(v)
			e := protocol.NewEnvelope(s.ID, started)
			if f != nil {
				e.Fail(f)
			} else {
				e = reg.Dispatch(normalized, started)
			}
			result = &e
			return nil
		}
	}
	// Ensure groups do not emit Cobra's implicit text help in JSON mode.
	for path, c := range nodes {
		if path != "" && c.RunE == nil {
			p := path
			c.RunE = func(c *cobra.Command, a []string) error {
				if len(a) > 0 {
					return fmt.Errorf("unexpected argument")
				}
				inv := command.NewInvocation("system.help")
				parts := []any{}
				for _, t := range strings.Fields(p) {
					parts = append(parts, t)
				}
				inv.Input["path"] = parts
				e := reg.Dispatch(inv, started)
				result = &e
				return nil
			}
		}
	}
	root.SetFlagErrorFunc(func(c *cobra.Command, e error) error { return fmt.Errorf("invalid arguments") })
	root.SetArgs(args)
	executed, executeErr := root.ExecuteC()
	err = executeErr
	if selected == nil && executed != nil {
		for _, s := range reg.Specs {
			if s.Path == executed.CommandPath() {
				selected = s
				break
			}
		}
	}
	if err != nil {
		if selected != nil {
			env.Command = selected.ID
		}
		env.Fail(protocol.Failure("validation", "invalid_argument", "Arguments are invalid, ambiguous, or unsupported."))
		result = &env
	}
	if result == nil {
		return 0
	} // Explicit --help is the sole text-mode exception.
	code, err := emitter.Emit(*result, stdout, stderr)
	if err != nil {
		return 5
	}
	return code
}
func emitInternal(stdout, stderr io.Writer, started time.Time) int {
	// Initialization errors never include raw schemas, library errors, or user input.
	e := protocol.NewEnvelope("system.help", started)
	e.Fail(protocol.Failure("internal", "contract_violation", "Protocol initialization failed."))
	b, err := json.Marshal(e)
	if err == nil {
		_, _ = stderr.Write(append(b, '\n'))
	}
	return 5
}

var _ pflag.Value = (*flagValue)(nil)
