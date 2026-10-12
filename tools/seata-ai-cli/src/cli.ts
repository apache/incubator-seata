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
import { basename, dirname, resolve } from "node:path";
import type { ExportFile } from "./adapter/export/file.js";
import type { Obj } from "./protocol/value.js";
let outputStarted = false;

async function main(): Promise<void> {
  const started = performance.now();
  const [major, minor] = process.versions.node.split(".").map(Number);
  if (!((major === 22 && minor !== undefined && minor >= 13) ||
        (major === 24 && minor !== undefined && minor >= 19)))
    throw new Error("unsupported_runtime");
  const [
    { parseArgs },
    { validate, registry, validateOutput },
    { parseJson, object },
    { dispatch },
    { envelope, invocationEnvelope, serialize, write },
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
  let timer: NodeJS.Timeout | undefined;
  let exportTarget: ExportFile | undefined;
  const warnings: Obj[] = [];
  try {
    const args = process.argv.slice(2);
    const known = [...registry]
      .sort((a, b) => b.path.length - a.path.length)
      .find((s) => s.path.every((p, i) => args[i] === p));
    command = known?.id ?? "system.help";
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
      const chunks: Buffer[] = [];
      let size = 0;
      const file = parsed.file
        ? await open(parsed.file, constants.O_RDONLY | constants.O_NOFOLLOW)
        : undefined;
      try {
        if (file && !(await file.stat()).isFile())
          throw new CliError(
            "policy",
            "unsafe_path",
            "Invocation source must be a regular file.",
          );
        if (file && !process.stdin.isTTY) {
          const stop = () =>
            process.stdin.destroy(
              abort.signal.reason instanceof Error
                ? abort.signal.reason
                : undefined,
            );
          abort.signal.addEventListener("abort", stop, { once: true });
          timer = setTimeout(
            () =>
              abort.abort(
                new CliError(
                  "validation",
                  "invalid_argument",
                  "Close unused stdin when passing --file.",
                ),
              ),
            30000,
          );
          try {
            for await (const chunk of process.stdin)
              if (chunk.length)
                throw new CliError(
                  "validation",
                  "invalid_argument",
                  "Do not supply both stdin and --file.",
                );
          } finally {
            clearTimeout(timer);
            abort.signal.removeEventListener("abort", stop);
          }
        }
        const input = file
          ? file.createReadStream({ autoClose: false })
          : process.stdin;
        const stop = () =>
          input.destroy(
            abort.signal.reason instanceof Error
              ? abort.signal.reason
              : undefined,
          );
        abort.signal.addEventListener("abort", stop, { once: true });
        timer = setTimeout(
          () =>
            abort.abort(
              new CliError("network", "timeout", "Input deadline exceeded."),
            ),
          30000,
        );
        try {
          for await (const chunk of input) {
            const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
            size += bytes.length;
            if (size > 1024 * 1024)
              throw new CliError(
                "validation",
                "input_too_large",
                "Invocation exceeds 1 MiB.",
              );
            chunks.push(bytes);
          }
        } finally {
          abort.signal.removeEventListener("abort", stop);
          clearTimeout(timer);
        }
      } finally {
        await file?.close();
      }
      value = parseJson(Buffer.concat(chunks));
      const candidate = object(value).command;
      if (
        typeof candidate === "string" &&
        registry.some((s) => s.id === candidate)
      )
        command = candidate;
    } else value = parsed.value;
    const inv = validate(value);
    command = inv.command;
    context = inv.context;
    timer = setTimeout(
      () =>
        abort.abort(
          new CliError("network", "timeout", "Invocation deadline exceeded."),
        ),
      Number(inv.options.timeout_ms),
    );
    if (typeof inv.options.export === "string") {
      const path = inv.options.export;
      if (/[\\\x00-\x1f\x7f]/.test(path) || path.split("/").includes(".."))
        throw new CliError("validation", "unsafe_export_path", "Export path must not contain traversal or ambiguous separators.");
      const absolute = resolve(path);
      const { ExportFile } = await import("./adapter/export/file.js");
      exportTarget = new ExportFile(dirname(absolute), basename(absolute), false, undefined, Number(inv.options.timeout_ms));
      await exportTarget.prepare(abort.signal);
    }
    let data: Obj, partial: unknown;
    try { data = await dispatch(inv, abort.signal, warning => warnings.push(warning)); }
    catch (error) {
      if (!(error instanceof PartialResult)) throw error;
      data = error.data; partial = error;
    }
    validateOutput(inv.command, data);
    const result = invocationEnvelope(inv, started, data, partial, invoked, warnings);
    const text = serialize(result);
    if (exportTarget) await exportTarget.write(Buffer.from(text), abort.signal);
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
      warnings,
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
    clearTimeout(timer);
    await exportTarget?.close();
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
