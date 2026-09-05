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
	"os"
	"os/exec"
	"strings"
	"testing"
)

func TestMain(m *testing.M) {
	if os.Getenv("SEATA_AI_TEST_PROCESS") == "1" {
		os.Exit(Run(os.Args[2:], os.Stdin, os.Stdout, os.Stderr))
	}
	os.Exit(m.Run())
}
func TestProcessWire(t *testing.T) {
	cases := []struct {
		args    []string
		input   string
		exit    int
		stream  string
		command string
	}{
		{[]string{"version"}, "", 0, "stdout", "system.version"},
		{[]string{"capabilities"}, "", 0, "stdout", "system.capabilities"},
		{[]string{"schema", "transaction.delete"}, "", 0, "stdout", "system.schema"},
		{[]string{"help", "transaction", "delete", "--field", "xid"}, "", 0, "stdout", "system.help"},
		{[]string{"transaction"}, "", 0, "stdout", "system.help"},
		{[]string{"transaction", "get", "--profile", "prod", "--xid", "x"}, "", 6, "stderr", "transaction.get"},
		{[]string{"transaction", "get", "--profile", "prod", "--xid", "x", "--xid", "y"}, "", 2, "stderr", ""},
		{[]string{"version", "--surprise", "secret-marker"}, "", 2, "stderr", ""},
		{[]string{"invoke"}, `{"protocol_version":"0.1","contract_revision":"R9","command":"system.version","context":{},"input":{},"options":{}}`, 0, "stdout", "system.version"},
		{[]string{"invoke"}, `{"command":"system.version","command":"secret-marker"}`, 2, "stderr", "system.invoke"},
		{[]string{"invoke"}, `{"protocol_version":"0.1","contract_revision":"R9","command":"system.invoke","context":{},"input":{},"options":{}}`, 2, "stderr", "system.invoke"},
		{[]string{"invoke", "--profile", "prod"}, `{}`, 2, "stderr", ""},
	}
	for _, tc := range cases {
		t.Run(strings.Join(tc.args, " ")+tc.input, func(t *testing.T) {
			p := exec.Command(os.Args[0], append([]string{"--"}, tc.args...)...)
			p.Env = append(os.Environ(), "SEATA_AI_TEST_PROCESS=1")
			p.Stdin = strings.NewReader(tc.input)
			var out, errout bytes.Buffer
			p.Stdout = &out
			p.Stderr = &errout
			e := p.Run()
			code := 0
			if e != nil {
				exit, ok := e.(*exec.ExitError)
				if !ok {
					t.Fatal(e)
				}
				code = exit.ExitCode()
			}
			if code != tc.exit {
				t.Fatalf("exit %d want %d stdout=%s stderr=%s", code, tc.exit, &out, &errout)
			}
			payload := out.Bytes()
			other := errout.Bytes()
			if tc.stream == "stderr" {
				payload, other = other, payload
			}
			if len(other) != 0 {
				t.Fatalf("other stream %s", other)
			}
			var env map[string]any
			if e = json.Unmarshal(payload, &env); e != nil {
				t.Fatalf("not one envelope: %s: %v", payload, e)
			}
			if tc.command != "" && env["command"] != tc.command {
				t.Fatal(env)
			}
			if bytes.Contains(payload, []byte("secret-marker")) {
				t.Fatal("input leaked")
			}
		})
	}
}
func TestTypedInvokeParityAndNoFakeAvailability(t *testing.T) {
	var a, b, errout bytes.Buffer
	if Run([]string{"capabilities"}, strings.NewReader(""), &a, &errout) != 0 {
		t.Fatal(&errout)
	}
	if Run([]string{"invoke"}, strings.NewReader(`{"protocol_version":"0.1","contract_revision":"R9","command":"system.capabilities","context":{},"input":{},"options":{}}`), &b, &errout) != 0 {
		t.Fatal(&errout)
	}
	var x, y map[string]any
	json.Unmarshal(a.Bytes(), &x)
	json.Unmarshal(b.Bytes(), &y)
	xa, _ := json.Marshal(x["data"])
	ya, _ := json.Marshal(y["data"])
	if !bytes.Equal(xa, ya) {
		t.Fatal("dispatch mismatch")
	}
	cs := x["data"].(map[string]any)["commands"].([]any)
	if len(cs) != 57 {
		t.Fatal(len(cs))
	}
	for _, raw := range cs {
		c := raw.(map[string]any)
		if !strings.HasPrefix(c["id"].(string), "system.") && c["availability"] != "unavailable" {
			t.Fatal(c)
		}
	}
}
