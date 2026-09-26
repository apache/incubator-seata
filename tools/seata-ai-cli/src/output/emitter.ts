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
import { randomUUID } from "node:crypto";
import { type Writable } from "node:stream";
import { type Invocation, validateEnvelope } from "../command/registry.js";
import { type Obj } from "../protocol/value.js";
import { CliError, safeError, exits } from "../protocol/error.js";
import { object, str } from "../protocol/value.js";
import type { Json } from "../protocol/value.js";

export interface Result {
  envelope: Obj;
  exit: number;
  stream: "stdout" | "stderr";
}
export function envelope(
  command: string,
  context: Obj,
  start: number,
  data?: Obj,
  error?: unknown,
  invoked = false,
  warnings: Obj[] = [],
): Result {
  const failure = error === undefined ? undefined : safeError(error);
  const state =
    failure?.type === "indeterminate"
      ? "indeterminate"
      : failure?.type === "partial_failure"
        ? "partial"
        : failure
          ? "failed"
          : "completed";
  const result: Obj = {
    protocol_version: "0.1",
    contract_revision: "R9",
    ok: !failure,
    state,
    command,
    request_id: randomUUID(),
    timestamp: new Date().toISOString(),
    context,
    meta: {
      duration_ms: Math.round(performance.now() - start),
      partial: state === "partial",
      ...(invoked ? { invoked_via: "system.invoke" } : {}),
    },
    warnings,
  };
  if (data) result.data = data;
  if (failure)
    result.error = {
      type: failure.type,
      subtype: failure.subtype,
      message: failure.message,
      retry: failure.retry,
      details: {},
    };
  return wireResult(result);
}
export function wireResult(value: Obj): Result {
  if (!validateEnvelope(value))
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid output envelope.",
    );
  const state = str(value.state);
  if (state === "completed" || state === "pending")
    return { envelope: value, exit: 0, stream: "stdout" };
  const error = object(value.error),
    type = str(error.type);
  if (state === "partial")
    return { envelope: value, exit: 9, stream: "stdout" };
  if (state === "indeterminate")
    return { envelope: value, exit: 12, stream: "stdout" };
  if (
    state !== "failed" ||
    type === "indeterminate" ||
    type === "partial_failure"
  )
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid error/stream combination.",
    );
  for (const [key, exit] of Object.entries(exits))
    if (key === type) return { envelope: value, exit, stream: "stderr" };
  throw new CliError("internal", "contract_violation", "Unknown error type.");
}
export function invocationEnvelope(
  inv: Invocation,
  start: number,
  data?: Obj,
  error?: unknown,
  via = false,
  warnings: Obj[] = [],
): Result {
  const result = envelope(inv.command, inv.context, start, data, error, via, warnings);
  if (!error && data?.flow_state === "pending" && ["auth.login", "auth.login.status"].includes(inv.command)) {
    result.envelope.state = "pending";
    result.envelope.continuation = { reason: "authentication_pending", not_before_ms: 1000,
      invocation: { protocol_version: "0.1", contract_revision: "R9", command: "auth.login.status",
        context: inv.context, input: { flow_id: data.flow_id ?? null }, options: { timeout_ms: inv.options.timeout_ms ?? 30000 } } };
    return wireResult(result.envelope);
  }
  return result;
}
export async function emit(
  result: Result,
  stdout: Writable,
  stderr: Writable,
): Promise<void> {
  await write(result.stream === "stdout" ? stdout : stderr, serialize(result));
}
export function serialize(result: Result): string {
  const maximum = 16 * 1024 * 1024;
  let size = 0;
  const chunks: string[] = [];
  const limit = () =>
    new CliError(
      "internal",
      "output_limit",
      "Output exceeds its byte or nesting limit.",
    );
  const append = (text: string) => {
    size += Buffer.byteLength(text);
    if (size > maximum) throw limit();
    chunks.push(text);
  };
  const string = (value: string) => {
    if (Buffer.byteLength(value) > maximum - size) throw limit();
    append(JSON.stringify(value));
  };
  const visit = (value: Json, depth: number): void => {
    if (depth > 128) throw limit();
    if (typeof value === "string") {
      string(value);
      return;
    }
    if (
      value === null ||
      typeof value === "boolean" ||
      typeof value === "number"
    ) {
      append(JSON.stringify(value));
      return;
    }
    if (Array.isArray(value)) {
      append("[");
      for (let i = 0; i < value.length; i++) {
        if (i) append(",");
        visit(value[i] ?? null, depth + 1);
      }
      append("]");
      return;
    }
    append("{");
    let first = true;
    for (const [key, item] of Object.entries(value)) {
      if (!first) append(",");
      first = false;
      string(key);
      append(":");
      visit(item, depth + 1);
    }
    append("}");
  };
  visit(result.envelope, 0);
  append("\n");
  return chunks.join("");
}
export async function write(stream: Writable, text: string): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    const onError = (error: Error) => {
      cleanup();
      reject(error);
    };
    const cleanup = () => stream.removeListener("error", onError);
    stream.once("error", onError);
    stream.write(text, (error) => {
      if (error) {
        reject(error);
      } else {
        cleanup();
        resolve();
      }
    });
  });
}
