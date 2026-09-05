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
	"encoding/json"
	"strings"
	"testing"
)

func TestStrictDecode(t *testing.T) {
	for _, s := range []string{`{"x":1,"x":2}`, `{"x":1,"\u0078":2}`, `{"nested":{"x":1,"x":2}}`, `{} {}`, `null false`, `{"x":NaN}`, string([]byte{'"', 255, '"'}), strings.Repeat("[", 33) + strings.Repeat("]", 33), `"` + strings.Repeat("a", MaxInput) + `"`} {
		if _, e := Decode(strings.NewReader(s)); e == nil {
			t.Errorf("accepted invalid input of %d bytes", len(s))
		}
	}
	v, e := Decode(strings.NewReader(`{"id":9223372036854775807}`))
	if e != nil {
		t.Fatal(e)
	}
	if v.(map[string]any)["id"].(json.Number).String() != "9223372036854775807" {
		t.Fatal("integer precision lost")
	}
	if _, e := Decode(strings.NewReader(strings.Repeat("[", 32) + strings.Repeat("]", 32))); e != nil {
		t.Fatal(e)
	}
}
