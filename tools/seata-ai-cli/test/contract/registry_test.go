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

package contract

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"testing"

	"github.com/apache/seata/tools/seata-ai-cli/internal/command"
	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

func TestSuppliedOracleUnchanged(t *testing.T) {
	b, e := os.ReadFile("testdata/r9/checksums.json")
	if e != nil {
		t.Fatal(e)
	}
	var hashes map[string]string
	if e = json.Unmarshal(b, &hashes); e != nil {
		t.Fatal(e)
	}
	for file, want := range hashes {
		b, e = os.ReadFile(filepath.Join("testdata/r9", file))
		if e != nil {
			t.Fatal(e)
		}
		sum := sha256.Sum256(b)
		if hex.EncodeToString(sum[:]) != want {
			t.Errorf("supplied artifact changed: %s", file)
		}
	}
}
func TestRegistryAndSuppliedInvocations(t *testing.T) {
	r, e := command.New()
	if e != nil {
		t.Fatal(e)
	}
	if len(r.Specs) != 57 {
		t.Fatal(len(r.Specs))
	}
	count := map[string]int{}
	for _, s := range r.Specs {
		count[s.Risk]++
	}
	if count["controlled_write"] != 2 || count["high_risk_write"] != 7 {
		t.Fatal(count)
	}
	files, e := filepath.Glob("testdata/r9/examples/invocations/*.json")
	if e != nil || len(files) != 56 {
		t.Fatal(e, len(files))
	}
	for _, file := range files {
		t.Run(filepath.Base(file), func(t *testing.T) {
			b, e := os.ReadFile(file)
			if e != nil {
				t.Fatal(e)
			}
			v, e := protocol.Decode(bytes.NewReader(b))
			if e != nil {
				t.Fatal(e)
			}
			inv, f := r.Validate(v)
			if f != nil {
				t.Fatal(f)
			}
			raw, e := os.ReadFile("testdata/r9/schemas/invocations/" + inv.Command + ".schema.json")
			if e != nil {
				t.Fatal(e)
			}
			var oracle any
			if e = json.Unmarshal(raw, &oracle); e != nil {
				t.Fatal(e)
			}
			if !reflect.DeepEqual(withoutRedundantProperties(oracle), withoutRedundantProperties(r.Get(inv.Command).InvocationSchema)) {
				t.Fatal("registry diverges from supplied invocation schema")
			}
		})
	}
}
func TestSemanticAndNegativeContracts(t *testing.T) {
	r, e := command.New()
	if e != nil {
		t.Fatal(e)
	}
	tests := []struct {
		input string
		valid bool
	}{
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"branch.get","context":{"profile":"prod"},"input":{"xid":"x","branch_id":"9223372036854775807"},"options":{}}`, true},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"branch.get","context":{"profile":"prod"},"input":{"xid":"x","branch_id":"9223372036854775808"},"options":{}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"system.invoke","context":{},"input":{},"options":{}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"transaction.delete","context":{"profile":"prod"},"input":{"xid":"x"},"options":{"dry_run":true,"confirm_high_risk":true}}`, true},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"transaction.delete","context":{"profile":"prod"},"input":{"xid":"x"},"options":{}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"transaction.get","context":{"profile":"prod"},"input":{"xid":"x"},"options":{"confirm_high_risk":true}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"transaction.get","context":{"profile":"prod","cluster":"a","vgroup":"b"},"input":{"xid":"x"},"options":{}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"system.version","context":null,"input":{},"options":{}}`, false},
		{`{"protocol_version":"0.1","contract_revision":"R9","command":"system.version","context":{},"input":{},"options":{},"surprise":true}`, false},
	}
	for _, test := range tests {
		v, e := protocol.Decode(strings.NewReader(test.input))
		if e != nil {
			t.Fatal(e)
		}
		_, f := r.Validate(v)
		if (f == nil) != test.valid {
			t.Errorf("valid=%v got %v for %s", test.valid, f, test.input)
		}
	}
}

// Empty property schemas in open conditional objects only satisfy strictRequired;
// they impose no new constraints. Every other difference from the oracle fails.
func withoutRedundantProperties(v any) any {
	switch x := v.(type) {
	case map[string]any:
		out := map[string]any{}
		for k, v := range x {
			out[k] = withoutRedundantProperties(v)
		}
		if x["additionalProperties"] != false {
			if out["type"] == "object" {
				delete(out, "type")
			}
			if p, ok := out["properties"].(map[string]any); ok {
				for k, v := range p {
					if m, ok := v.(map[string]any); ok && len(m) == 0 {
						delete(p, k)
					}
				}
				if len(p) == 0 {
					delete(out, "properties")
				}
			}
		}
		return out
	case []any:
		out := make([]any, len(x))
		for i, v := range x {
			out[i] = withoutRedundantProperties(v)
		}
		return out
	}
	return v
}

func TestSharedCorpus(t *testing.T) {
	r, err := command.New()
	if err != nil {
		t.Fatal(err)
	}
	b, err := os.ReadFile("testdata/corpus.json")
	if err != nil {
		t.Fatal(err)
	}
	var cases []struct {
		Name       string         `json:"name"`
		Invocation map[string]any `json:"invocation"`
		Valid      bool           `json:"schema_valid"`
	}
	d := json.NewDecoder(bytes.NewReader(b))
	d.UseNumber()
	if err = d.Decode(&cases); err != nil {
		t.Fatal(err)
	}
	for _, c := range cases {
		t.Run(c.Name, func(t *testing.T) {
			_, f := r.Validate(c.Invocation)
			if (f == nil) != c.Valid {
				t.Fatalf("want valid %v, got %v", c.Valid, f)
			}
		})
	}
}
