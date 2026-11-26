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
import { CliError } from "../../protocol/error.js";

/** Raw parser entries are private input, not safe public ConfigFacts. */
export interface ConfigEntry { key: string; value: string; line: number; document?: number; value_kind?: string }
const whitespace = (ch: string | undefined): boolean => ch === " " || ch === "\t" || ch === "\f";
function invalid(subtype = "config_syntax_invalid"): never {
  // Never interpolate source text: malformed keys and values may contain secrets.
  throw new CliError("config", subtype, "Configuration data is malformed or exceeds its parsing budget.");
}

function unescape(value: string): string {
  let output = "";
  for (let i = 0; i < value.length; i++) {
    const ch = value[i]!;
    if (ch !== "\\") { output += ch; continue; }
    const next = value[++i];
    if (next === undefined) invalid();
    if (next === "u") {
      const digits = value.slice(i + 1, i + 5);
      if (!/^[0-9a-fA-F]{4}$/.test(digits)) invalid();
      output += String.fromCharCode(parseInt(digits, 16)); i += 4;
    } else {
      // Java Properties silently drops the escape for unrecognised characters.
      output += ({ t: "\t", n: "\n", r: "\r", f: "\f" } as Record<string, string>)[next] ?? next;
    }
  }
  if (!output.isWellFormed()) invalid();
  return output;
}

/** Java Properties grammar, without interpolation, includes or code execution.
 * The caller owns decoding/encoding evidence; this function takes decoded text.
 * Unlike Properties.load, duplicate decoded keys are rejected, never last-wins.
 */
export function parseProperties(source: string): ConfigEntry[] {
  if (Buffer.byteLength(source, "utf8") > 1024 * 1024) invalid("config_limit_exceeded");
  if (!source.isWellFormed() || source.includes("\0")) invalid();
  const natural = source.split(/\r\n|\r|\n/), entries: ConfigEntry[] = [], seen = new Set<string>();
  for (let index = 0; index < natural.length; index++) {
    const startLine = index + 1;
    let line = natural[index]!, leading = 0;
    while (whitespace(line[leading])) leading++;
    line = line.slice(leading);
    if (line.length === 0 || line.startsWith("#") || line.startsWith("!")) continue;
    for (;;) {
      let slashes = 0;
      for (let p = line.length - 1; p >= 0 && line[p] === "\\"; p--) slashes++;
      if (slashes % 2 === 0) break;
      line = line.slice(0, -1);
      // A final continuation backslash has no following content in Java's grammar.
      if (++index >= natural.length) break;
      const following = natural[index]!;
      let offset = 0;
      while (whitespace(following[offset])) offset++;
      line += following.slice(offset);
    }
    let boundary = 0, escaped = false;
    for (; boundary < line.length; boundary++) {
      const ch = line[boundary]!;
      if (!escaped && (ch === "=" || ch === ":" || whitespace(ch))) break;
      if (ch === "\\") escaped = !escaped;
      else escaped = false;
    }
    let valueStart = boundary;
    while (whitespace(line[valueStart])) valueStart++;
    if (line[valueStart] === "=" || line[valueStart] === ":") valueStart++;
    while (whitespace(line[valueStart])) valueStart++;
    const key = unescape(line.slice(0, boundary)), value = unescape(line.slice(valueStart));
    if (seen.has(key)) invalid("config_duplicate_key");
    if (entries.length >= 10_000) invalid("config_limit_exceeded");
    seen.add(key); entries.push({ key, value, line: startLine });
  }
  return entries;
}
