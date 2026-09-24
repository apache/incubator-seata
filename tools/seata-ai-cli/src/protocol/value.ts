// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import { createHash } from "node:crypto";
import { CliError } from "./error.js";

export type Json =
  | null
  | boolean
  | number
  | string
  | Json[]
  | { [key: string]: Json };
export type Obj = { [key: string]: Json };
export function isObject(value: unknown): value is Obj {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}
export function object(value: unknown): Obj {
  if (!isObject(value))
    throw new CliError("validation", "invalid_argument", "Expected an object.");
  return value;
}
export function str(value: unknown): string {
  if (typeof value !== "string")
    throw new CliError("validation", "invalid_argument", "Expected a string.");
  return value;
}
export function canonical(value: Json): string {
  if (Array.isArray(value)) return "[" + value.map(canonical).join(",") + "]";
  if (isObject(value))
    return (
      "{" +
      Object.keys(value)
        .sort()
        .map((k) => JSON.stringify(k) + ":" + canonical(value[k] ?? null))
        .join(",") +
      "}"
    );
  if (typeof value === "number" && !Number.isFinite(value))
    throw new CliError("internal", "contract_violation", "Non-finite number.");
  return JSON.stringify(value);
}
export function digest(value: Json): string {
  return (
    "sha256:" + createHash("sha256").update(canonical(value)).digest("hex")
  );
}

/** Parse before conversion: keys are compared after decoding; unsafe integers never round. */
export function parseJson(
  bytes: Uint8Array,
  preserveIntegers = false,
  maxBytes = 1024 * 1024,
): Json {
  if (bytes.byteLength > maxBytes)
    throw new CliError(
      "validation",
      "input_too_large",
      "JSON input exceeds its byte limit.",
    );
  let source: string;
  try {
    source = new TextDecoder("utf-8", { fatal: true, ignoreBOM: true }).decode(
      bytes,
    );
  } catch {
    throw new CliError(
      "validation",
      "invalid_utf8",
      "Input must be valid UTF-8.",
    );
  }
  let i = 0;
  const fail = (subtype = "invalid_json"): never => {
    throw new CliError(
      "validation",
      subtype,
      "Malformed or unsupported JSON input.",
    );
  };
  const ws = () => {
    while (/[\x20\t\r\n]/.test(source[i] ?? "\0")) i++;
  };
  const string = (): string => {
    const start = i++;
    for (; i < source.length; i++) {
      const ch = source[i];
      if (ch === "\\") {
        i++;
        continue;
      }
      if (ch === '"') {
        let value: unknown;
        try {
          value = JSON.parse(source.slice(start, ++i));
        } catch {
          return fail();
        }
        if (typeof value !== "string" || !value.isWellFormed())
          return fail("invalid_unicode");
        return value;
      }
    }
    return fail();
  };
  const value = (depth: number): Json => {
    ws();
    const ch = source[i];
    if (ch === '"') return string();
    if (ch === "{" || ch === "[") {
      if (depth >= 32) return fail("nesting_limit");
      i++;
      ws();
      const array = ch === "[";
      const closing = array ? "]" : "}";
      const result: Obj = Object.create(null);
      const items: Json[] = [];
      const keys = new Set<string>();
      if (source[i] === closing) {
        i++;
        return array ? items : result;
      }
      while (true) {
        if (array) items.push(value(depth + 1));
        else {
          if (source[i] !== '"') return fail();
          const key = string();
          if (keys.has(key)) return fail("duplicate_json_key");
          keys.add(key);
          ws();
          if (source[i++] !== ":") return fail();
          result[key] = value(depth + 1);
        }
        ws();
        if (source[i] === closing) {
          i++;
          return array ? items : result;
        }
        if (source[i++] !== ",") return fail();
        ws();
      }
    }
    for (const [literal, decoded] of [
      ["true", true],
      ["false", false],
      ["null", null],
    ] as const) {
      if (source.startsWith(literal, i)) {
        i += literal.length;
        return decoded;
      }
    }
    const token = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/.exec(
      source.slice(i),
    )?.[0];
    if (!token) return fail();
    i += token.length;
    if (
      !/[.eE]/.test(token) &&
      (BigInt(token) > BigInt(Number.MAX_SAFE_INTEGER) ||
        BigInt(token) < BigInt(Number.MIN_SAFE_INTEGER))
    ) {
      if (preserveIntegers) return token;
      return fail("unsafe_integer");
    }
    const number = Number(token);
    if (
      !Number.isFinite(number) ||
      (Number.isInteger(number) && !Number.isSafeInteger(number))
    )
      return fail("unsafe_integer");
    return number;
  };
  const result = value(0);
  ws();
  if (i !== source.length) return fail("trailing_json");
  return result;
}
