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

package protocol

import (
	"crypto/rand"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"time"

	"github.com/santhosh-tekuri/jsonschema/v6"
)

//go:embed envelope.schema.json
var envelopeJSON []byte

type Fault struct {
	Type    string         `json:"type"`
	Subtype string         `json:"subtype"`
	Message string         `json:"message"`
	Retry   string         `json:"retry"`
	Details map[string]any `json:"details"`
}

func (f *Fault) Error() string { return f.Type + "/" + f.Subtype }
func Failure(kind, subtype, message string) *Fault {
	return &Fault{kind, subtype, message, "never", map[string]any{}}
}

var exitCodes = map[string]int{"api": 1, "validation": 2, "authentication": 3, "authorization": 3, "config": 3, "network": 4, "internal": 5, "policy": 6, "not_found": 7, "conflict": 8, "partial_failure": 9, "confirmation": 10, "cancelled": 11, "indeterminate": 12}

type Envelope struct {
	ProtocolVersion  string              `json:"protocol_version"`
	ContractRevision string              `json:"contract_revision"`
	OK               bool                `json:"ok"`
	State            string              `json:"state"`
	Command          string              `json:"command"`
	RequestID        string              `json:"request_id"`
	Timestamp        string              `json:"timestamp"`
	Context          map[string]any      `json:"context"`
	Meta             map[string]any      `json:"meta"`
	Warnings         []map[string]string `json:"warnings"`
	Data             any                 `json:"data,omitempty"`
	Error            *Fault              `json:"error,omitempty"`
	Continuation     any                 `json:"continuation,omitempty"`
}

func NewEnvelope(command string, started time.Time) Envelope {
	id := make([]byte, 16)
	if _, err := rand.Read(id); err != nil {
		panic(err)
	}
	return Envelope{ProtocolVersion: "0.1", ContractRevision: "R9", OK: true, State: "completed", Command: command, RequestID: "req_" + hex.EncodeToString(id), Timestamp: time.Now().UTC().Format(time.RFC3339Nano), Context: map[string]any{}, Meta: map[string]any{"duration_ms": time.Since(started).Milliseconds(), "partial": false}, Warnings: []map[string]string{}}
}
func (e *Envelope) Fail(f *Fault) { e.OK = false; e.State = "failed"; e.Error = f }

func Compile(name string, doc any) (*jsonschema.Schema, error) {
	c := jsonschema.NewCompiler()
	c.DefaultDraft(jsonschema.Draft2020)
	c.AssertFormat()
	if err := c.AddResource(name, doc); err != nil {
		return nil, err
	}
	return c.Compile(name)
}
func EnvelopeSchema() map[string]any {
	var doc map[string]any
	if err := json.Unmarshal(envelopeJSON, &doc); err != nil {
		panic(err)
	}
	return doc
}

// Emitter validates and serializes the entire message before touching either stream.
// A short OS write still means the caller may not have received an envelope.
type Emitter struct{ schema *jsonschema.Schema }

func NewEmitter() (*Emitter, error) {
	s, e := Compile("https://seata.invalid/envelope", EnvelopeSchema())
	return &Emitter{s}, e
}
func (em *Emitter) Emit(e Envelope, stdout, stderr io.Writer) (int, error) {
	b, err := json.Marshal(e)
	if err != nil {
		return 5, err
	}
	if len(b) > 16<<20 {
		return 5, errors.New("output_too_large")
	}
	var obj any
	if err = json.Unmarshal(b, &obj); err != nil {
		return 5, err
	}
	if err = em.schema.Validate(obj); err != nil {
		return 5, errors.New("output_contract_violation")
	}
	code := 0
	out := stdout
	switch e.State {
	case "failed":
		var ok bool
		code, ok = exitCodes[e.Error.Type]
		if !ok {
			return 5, errors.New("unknown_error_type")
		}
		out = stderr
	case "partial":
		code = 9
	case "indeterminate":
		code = 12
	}
	b = append(b, '\n')
	n, err := out.Write(b)
	if err != nil {
		return code, err
	}
	if n != len(b) {
		return code, io.ErrShortWrite
	}
	return code, nil
}
