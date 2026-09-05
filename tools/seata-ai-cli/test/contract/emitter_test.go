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
	"encoding/json"
	"os"
	"testing"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/command"
	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

func TestEmitterStateStreamAndExit(t *testing.T) {
	em, err := protocol.NewEmitter()
	if err != nil {
		t.Fatal(err)
	}
	for _, tc := range []struct {
		file   string
		exit   int
		stderr bool
	}{{"confirmation", 10, true}, {"indeterminate", 12, false}} {
		b, err := os.ReadFile("testdata/r9/examples/envelopes/" + tc.file + ".json")
		if err != nil {
			t.Fatal(err)
		}
		var e protocol.Envelope
		if err = json.Unmarshal(b, &e); err != nil {
			t.Fatal(err)
		}
		var out, errout bytes.Buffer
		code, err := em.Emit(e, &out, &errout)
		if err != nil || code != tc.exit {
			t.Fatalf("%s: %d %v", tc.file, code, err)
		}
		if (errout.Len() > 0) != tc.stderr || (out.Len() > 0) == tc.stderr {
			t.Fatal("wrong stream")
		}
		e.OK = true
		out.Reset()
		errout.Reset()
		if _, err = em.Emit(e, &out, &errout); err == nil || out.Len()+errout.Len() != 0 {
			t.Fatal("invalid outcome emitted")
		}
	}
	for _, kind := range []string{"api", "validation", "authentication", "network", "internal", "policy", "not_found", "conflict", "cancelled"} {
		e := protocol.NewEnvelope("system.version", time.Now())
		e.Fail(protocol.Failure(kind, "test", "test"))
		var out, errout bytes.Buffer
		if code, err := em.Emit(e, &out, &errout); err != nil || code == 0 || out.Len() != 0 || errout.Len() == 0 {
			t.Fatalf("%s: %d %v", kind, code, err)
		}
	}
}

func TestCommandDataFailureIsRejectedBeforeOutput(t *testing.T) {
	r, err := command.New()
	if err != nil {
		t.Fatal(err)
	}
	emitter, err := protocol.NewEmitter(r.OutputValidators())
	if err != nil {
		t.Fatal(err)
	}
	e := protocol.NewEnvelope("system.version", time.Now())
	e.Data = map[string]any{"versions": map[string]any{"cli": "0.1.0-dev", "protocol": "0.1", "go": "go1.27.1"}, "contract_revision": "R9", "state_store_id": nil}
	var out, errout bytes.Buffer
	if code, err := emitter.Emit(e, &out, &errout); err != nil || code != 0 {
		t.Fatal(code, err)
	}
	e.Data.(map[string]any)["versions"].(map[string]any)["cli"] = 17
	out.Reset()
	errout.Reset()
	if _, err = emitter.Emit(e, &out, &errout); err == nil || out.Len()+errout.Len() != 0 {
		t.Fatal("invalid typed data was emitted")
	}
}
