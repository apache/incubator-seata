#!/usr/bin/env node
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
// Imports are deliberately dynamic so startup failures use one protocol envelope.
import { randomUUID } from "node:crypto";
import { open } from "node:fs/promises";
import { constants } from "node:fs";
import type { CliError as InvocationError } from "./protocol/error.js";
let outputStarted = false;

async function readInvocation(
  path: string | undefined,
  abort: AbortController,
  CliError: typeof InvocationError,
): Promise<Buffer> {
  const timer = setTimeout(() => {
    abort.abort(new CliError("network", "timeout", "Input deadline exceeded."));
  }, 30000);
  let file: Awaited<ReturnType<typeof open>> | undefined;
  try {
    if (path !== undefined) {
      try {
        file = await open(
          path,
          constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK,
        );
      } catch (error) {
        const code = (error as NodeJS.ErrnoException | undefined)?.code;
        if (code === "ELOOP")
          throw new CliError(
            "policy",
            "unsafe_path",
            "Invocation source must be a regular file without a symbolic link.",
          );
        if (code && ["ENOENT", "ENOTDIR", "EACCES", "EPERM", "ENAMETOOLONG", "EINVAL"].includes(code))
          throw new CliError("validation", "invalid_argument", "Cannot open the invocation file.");
        throw error;
      }
      if (!(await file.stat()).isFile())
        throw new CliError("policy", "unsafe_path", "Invocation source must be a regular file.");
    }
    abort.signal.throwIfAborted();
    const input = file ? file.createReadStream({ autoClose: false }) : process.stdin;
    const stop = () => input.destroy(abort.signal.reason as Error);
    abort.signal.addEventListener("abort", stop, { once: true });
    try {
      const chunks: Buffer[] = [];
      let size = 0;
      for await (const chunk of input) {
        const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
        size += bytes.length;
        if (size > 1024 * 1024)
          throw new CliError("validation", "input_too_large", "Invocation exceeds 1 MiB.");
        chunks.push(bytes);
      }
      return Buffer.concat(chunks, size);
    } finally {
      abort.signal.removeEventListener("abort", stop);
    }
  } finally {
    clearTimeout(timer);
    await file?.close();
  }
}

async function main(): Promise<void> {
  const started = performance.now();
  const [major, minor] = process.versions.node.split(".").map(Number);
  if (!((major === 22 && minor !== undefined && minor >= 13) ||
        (major === 24 && minor !== undefined && minor >= 19)))
    throw new Error("unsupported_runtime");
  const [
    { parseArgs },
    { validate, matchCommand, registry, validateOutput },
    { parseJson, object },
    { dispatch },
    { envelope, serialize, write },
    { CliError, PartialResult },
  ] = await Promise.all([
    import("./cmdtree/parse.js"),
    import("./command/registry.js"),
    import("./protocol/value.js"),
    import("./app/dispatch.js"),
    import("./output/emitter.js"),
    import("./protocol/error.js"),
  ]);
  const abort = new AbortController();
  const cancel = () =>
    abort.abort(
      new CliError("cancelled", "context_cancelled", "Invocation cancelled."),
    );
  process.once("SIGINT", cancel);
  process.once("SIGTERM", cancel);
  let command = "system.help",
    context = {},
    invoked = false;
  let clientRequestId: string | undefined;
  try {
    const args = process.argv.slice(2);
    command = matchCommand(args)?.id ?? "system.help";
    const parsed = parseArgs(args);
    if (parsed.kind === "help") {
      const { renderHelp } = await import("./help/render.js");
      const text = renderHelp(parsed.path);
      outputStarted = true;
      await write(process.stdout, text);
      return;
    }
    let value;
    if (parsed.kind === "invoke") {
      invoked = true;
      command = "system.invoke";
      value = parseJson(await readInvocation(parsed.file, abort, CliError));
      const candidate = object(value).command;
      if (
        typeof candidate === "string" &&
        registry.some(spec => spec.id === candidate)
      )
        command = candidate;
    } else value = parsed.value;
    const inv = validate(value);
    command = inv.command;
    context = inv.context;
    if (typeof inv.options.client_request_id === "string")
      clientRequestId = inv.options.client_request_id;
    const data = dispatch(inv, abort.signal);
    validateOutput(inv.command, data);
    const result = envelope(command, context, started, data, undefined, invoked, clientRequestId);
    const text = serialize(result);
    outputStarted = true;
    await write(
      result.stream === "stdout" ? process.stdout : process.stderr,
      text,
    );
    process.exitCode = result.exit;
  } catch (error) {
    if (outputStarted) throw error;
    const data = error instanceof PartialResult ? error.data : undefined;
    if (data) validateOutput(command, data);
    const result = envelope(
      command,
      context,
      started,
      data,
      error,
      invoked,
      clientRequestId,
    );
    // Once output begins, EPIPE is handled by the outer boundary, never by a second envelope.
    const text = serialize(result);
    outputStarted = true;
    await write(
      result.stream === "stdout" ? process.stdout : process.stderr,
      text,
    );
    process.exitCode = result.exit;
  } finally {
    process.removeListener("SIGINT", cancel);
    process.removeListener("SIGTERM", cancel);
  }
}
main().catch((error) => {
  if (outputStarted) {
    process.exitCode = 5;
    return;
  }
  if (
    error &&
    typeof error === "object" &&
    "code" in error &&
    error.code === "EPIPE"
  ) {
    process.exitCode = 5;
    return;
  }
  const unsupported =
    error instanceof Error && error.message === "unsupported_runtime";
  const result = {
    protocol_version: "0.1",
    contract_revision: "R9",
    ok: false,
    state: "failed",
    command: "system.invoke",
    request_id: randomUUID(),
    timestamp: new Date().toISOString(),
    context: {},
    meta: { duration_ms: 0, partial: false },
    warnings: [],
    error: {
      type: unsupported ? "policy" : "internal",
      subtype: unsupported ? "unsupported_runtime" : "startup_failure",
      message: unsupported
        ? "Node.js >=22.13.0 <23 or >=24.19.0 <25 is required."
        : "CLI startup or output failed.",
      retry: "never",
      details: {},
    },
  };
  process.exitCode = unsupported ? 6 : 5;
  process.stderr.on("error", () => {});
  process.stderr.write(JSON.stringify(result) + "\n");
});
