// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import test from "node:test";
import assert from "node:assert/strict";
import { parseProperties } from "../dist/adapter/config/properties.js";

test("properties parser handles Java whitespace, escaped separators and natural line numbers", () => {
  assert.deepEqual(parseProperties(" # ignored\r\n! ignored\r\n seata.enabled : true  \rkey\\ with\\:colon=value=retained\nempty\n=empty-key"), [
    { key: "seata.enabled", value: "true  ", line: 3 },
    { key: "key with:colon", value: "value=retained", line: 4 },
    { key: "empty", value: "", line: 5 },
    { key: "", value: "empty-key", line: 6 },
  ]);
});

test("properties continuations preserve comment characters and distinguish odd/even escapes", () => {
  const source = "a=first\\\n  #second\\\r\n\tthird\nb=two\\\\\nc=last\\";
  assert.deepEqual(parseProperties(source), [
    { key: "a", value: "first#secondthird", line: 1 },
    { key: "b", value: "two\\", line: 4 },
    { key: "c", value: "last", line: 5 },
  ]);
});

test("properties escapes decode before duplicate detection without prototype side effects", () => {
  assert.deepEqual(parseProperties(String.raw`name=\u4E2D\u6587\t\n\r\f\z
__proto__=safe
constructor=also-safe
face=\uD83D\uDE00`), [
    { key: "name", value: "中文\t\n\r\fz", line: 1 },
    { key: "__proto__", value: "safe", line: 2 },
    { key: "constructor", value: "also-safe", line: 3 },
    { key: "face", value: "😀", line: 4 },
  ]);
  assert.throws(() => parseProperties("key=1\n\\u006bey=SECRET"), e =>
    e.subtype === "config_duplicate_key" && !e.message.includes("SECRET"));
});

test("properties parser rejects malformed escapes, unpaired surrogates and resource overruns", () => {
  for (const source of [String.raw`x=SECRET\u123`, String.raw`x=SECRET\uZZZZ`, String.raw`x=\uD800`, "x=\0"]) {
    assert.throws(() => parseProperties(source), e => e.subtype === "config_syntax_invalid" && !e.message.includes("SECRET"));
  }
  assert.throws(() => parseProperties("x=" + "a".repeat(1024 * 1024)), e => e.subtype === "config_limit_exceeded");
  assert.throws(() => parseProperties(Array.from({ length: 10001 }, (_, i) => `k${i}=v`).join("\n")), e => e.subtype === "config_limit_exceeded");
});

test("properties parser never resolves placeholders, includes or executable-looking input", () => {
  const source = "password=${SECRET}\nspring.config.import=https://untrusted.invalid/config\nexpr=#{T(java.lang.Runtime).getRuntime()}";
  assert.deepEqual(parseProperties(source).map(entry => entry.value), [
    "${SECRET}", "https://untrusted.invalid/config", "#{T(java.lang.Runtime).getRuntime()}",
  ]);
});
