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
import { parseAllDocuments, isAlias, isMap, isSeq, isScalar, isNode, LineCounter, Lexer } from "yaml";
import { CliError } from "../../protocol/error.js";
import type { ConfigEntry } from "./properties.js";

function fail(subtype = "config_syntax_invalid"): never {
  throw new CliError("config", subtype, "YAML configuration is malformed, unsupported or exceeds its parsing budget.");
}

/** Walk the data-only AST, never converting it into prototype-bearing objects.
 * Alias expansion has a shared node budget and a cycle/depth guard. No merge
 * tags, custom constructors, includes, expression evaluation or secret lookup.
 */
export function parseYaml(source: string): ConfigEntry[] {
  if (Buffer.byteLength(source) > 1024 * 1024) fail("config_limit_exceeded");
  if (!source.isWellFormed() || source.includes("\0")) fail();
  // Bound work before composition and duplicate-key checking, not only after
  // the AST has been built. Quoted/block scalar contents are single tokens.
  let tokens = 0, flowDepth = 0;
  for (const token of new Lexer().lex(source)) {
    if (++tokens > 60_000) fail("config_limit_exceeded");
    if (token === "[" || token === "{") { if (++flowDepth > 32) fail("config_limit_exceeded"); }
    else if (token === "]" || token === "}") flowDepth--;
  }
  const counter = new LineCounter();
  let docs: ReturnType<typeof parseAllDocuments>;
  try {
    docs = parseAllDocuments(source, { schema: "core", version: "1.2", strict: true, uniqueKeys: true,
      stringKeys: true, intAsBigInt: true, lineCounter: counter, prettyErrors: false, logLevel: "silent", merge: false });
  } catch { fail(); }
  if (docs.length > 16) fail("config_limit_exceeded");
  const entries: ConfigEntry[] = [];
  let visited = 0, outputBytes = 0;
  for (const [document, doc] of docs.entries()) {
    if (doc.errors.length || doc.warnings.length) {
      if (doc.errors.some(e => e.code === "DUPLICATE_KEY")) fail("config_duplicate_key");
      fail();
    }
    if (doc.contents === null) continue;
    if (!isMap(doc.contents)) fail("config_root_not_mapping");
    const seen = new Set<string>(), ancestors = new Set<unknown>();
    const walk = (node: unknown, key: string, line: number, depth: number): void => {
      if (++visited > 20_000 || depth > 32) fail("config_limit_exceeded");
      if (ancestors.has(node)) fail("config_alias_cycle");
      if (isNode(node) && node.tag && !["tag:yaml.org,2002:str", "tag:yaml.org,2002:bool", "tag:yaml.org,2002:int",
        "tag:yaml.org,2002:float", "tag:yaml.org,2002:null", "tag:yaml.org,2002:map", "tag:yaml.org,2002:seq"].includes(node.tag)) fail();
      if (isAlias(node)) {
        const resolved = node.resolve(doc);
        if (!resolved) fail();
        ancestors.add(node); walk(resolved, key, line, depth + 1); ancestors.delete(node); return;
      }
      if (isMap(node) && node.items.length > 0) {
        ancestors.add(node);
        for (const pair of node.items) {
          if (!isScalar(pair.key) || typeof pair.key.value !== "string" || pair.key.value === "<<") fail();
          const name = pair.key.value;
          if (!name || !name.isWellFormed()) fail();
          walk(pair.value, key ? `${key}.${name}` : name, counter.linePos(pair.key.range?.[0] ?? 0).line, depth + 1);
        }
        ancestors.delete(node); return;
      }
      if (isSeq(node) && node.items.length > 0) {
        ancestors.add(node);
        node.items.forEach((item, index) => walk(item, `${key}[${index}]`, line, depth + 1));
        ancestors.delete(node); return;
      }
      const emptyCollection = isMap(node) || isSeq(node);
      if (node !== null && !isScalar(node) && !emptyCollection) fail();
      if (emptyCollection && key === "") return;
      const value: unknown = node === null ? null : isScalar(node) ? node.value : isMap(node) ? "{}" : "[]";
      if (typeof value === "number" && !Number.isFinite(value)) fail();
      if (!["string", "number", "bigint", "boolean"].includes(typeof value) && value !== null) fail();
      // YAML null is retained as an unresolved literal rather than an effective empty string.
      const text = value === null ? "null" : String(value);
      if (!text.isWellFormed()) fail();
      if (seen.has(key)) fail("config_duplicate_key");
      seen.add(key); outputBytes += Buffer.byteLength(key) + Buffer.byteLength(text);
      if (entries.length >= 10_000 || outputBytes > 1024 * 1024) fail("config_limit_exceeded");
      entries.push({ key, value: text, line, document, value_kind: emptyCollection ? "collection" : value === null ? "null" : typeof value });
    };
    walk(doc.contents, "", 1, 0);
  }
  return entries;
}
