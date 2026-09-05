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
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"unicode/utf8"
)

const MaxInput = 1 << 20

// Decode rejects ambiguity before a schema validator or a handler sees input.
// Numbers remain decimal tokens, including values above the float64 safe range.
func Decode(r io.Reader) (any, error) { return DecodeLimit(r, MaxInput) }

// DecodeLimit is used for bounded server responses; canonical input always uses
// Decode's fixed 1 MiB limit.
func DecodeLimit(r io.Reader, limit int64) (any, error) {
	if limit < 1 || limit > 16<<20 {
		return nil, errors.New("invalid_input_limit")
	}
	b, err := io.ReadAll(io.LimitReader(r, limit+1))
	if err != nil {
		return nil, errors.New("input_read_failed")
	}
	if int64(len(b)) > limit {
		return nil, errors.New("input_too_large")
	}
	if !utf8.Valid(b) {
		return nil, errors.New("invalid_utf8")
	}
	d := json.NewDecoder(bytes.NewReader(b))
	d.UseNumber()
	v, err := value(d, 0)
	if err != nil {
		return nil, err
	}
	if _, err = d.Token(); err != io.EOF {
		return nil, errors.New("trailing_json")
	}
	return v, nil
}
func value(d *json.Decoder, depth int) (any, error) {
	t, e := d.Token()
	if e != nil {
		return nil, errors.New("invalid_json")
	}
	delim, ok := t.(json.Delim)
	if !ok {
		return t, nil
	}
	if depth >= 32 {
		return nil, errors.New("input_too_deep")
	}
	switch delim {
	case '{':
		m := map[string]any{}
		for d.More() {
			k, e := d.Token()
			if e != nil {
				return nil, errors.New("invalid_json")
			}
			s, ok := k.(string)
			if !ok {
				return nil, errors.New("invalid_json")
			}
			if _, ok = m[s]; ok {
				return nil, errors.New("duplicate_json_key")
			}
			v, e := value(d, depth+1)
			if e != nil {
				return nil, e
			}
			m[s] = v
		}
		t, e = d.Token()
		if e != nil || t != json.Delim('}') {
			return nil, errors.New("invalid_json")
		}
		return m, nil
	case '[':
		a := []any{}
		for d.More() {
			v, e := value(d, depth+1)
			if e != nil {
				return nil, e
			}
			a = append(a, v)
		}
		t, e = d.Token()
		if e != nil || t != json.Delim(']') {
			return nil, errors.New("invalid_json")
		}
		return a, nil
	}
	return nil, errors.New("invalid_json")
}
