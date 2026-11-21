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
import { Command, Option } from "commander";
import { matchCommand, type Spec } from "../command/registry.js";
import {
  type Json,
  type Obj,
  object,
  str,
  parseJson,
} from "../protocol/value.js";
import { CliError } from "../protocol/error.js";

export type Parsed =
  | { kind: "help"; path: string[] }
  | { kind: "invoke"; file?: string }
  | { kind: "typed"; value: Obj };
function convert(value: string, schema: Obj): Json {
  if (schema.type === "string") return value;
  return parseJson(Buffer.from(value));
}
export function parseArgs(argv: string[]): Parsed {
  if (!argv.length) return { kind: "help", path: [] };
  if (argv.includes("--help")) {
    if (
      argv.at(-1) !== "--help" ||
      argv.slice(0, -1).some((x) => x.startsWith("-"))
    )
      throw new CliError(
        "validation",
        "invalid_argument",
        "Use --help after a command path.",
      );
    return { kind: "help", path: argv.slice(0, -1) };
  }
  if (argv[0] === "invoke") {
    if (argv.length === 1) return { kind: "invoke" };
    if (argv.length === 3 && argv[1] === "--file" && argv[2])
      return { kind: "invoke", file: argv[2] };
    throw new CliError(
      "validation",
      "invalid_argument",
      "invoke accepts only --file <path> as an option.",
    );
  }
  const spec = matchCommand(argv);
  if (!spec)
    throw new CliError(
      "validation",
      "unknown_command",
      "Unknown command path. Use --help.",
    );
  const args = argv.slice(spec.path.length);
  const command = new Command(spec.path.join(" "));
  command
    .helpOption(false)
    .addHelpCommand(false)
    .allowExcessArguments(false)
    .exitOverride()
    .configureOutput({ writeOut: () => {}, writeErr: () => {} });
  const props = object(spec.invocation.properties);
  const fields = new Map<string, { group: string; key: string }>();
  for (const group of ["context", "input", "options"]) {
    for (const [key, schemaValue] of Object.entries(
      object(object(props[group]).properties),
    )) {
      const schema = object(schemaValue);
      const flag = key.replaceAll("_", "-");
      if (fields.has(flag)) {
        throw new CliError(
          "internal",
          "contract_violation",
          "Conflicting CLI binding.",
        );
      }
      fields.set(flag, { group, key });
      const option = new Option("--" + flag + " <value>");
      command.addOption(option.argParser(v => convert(v, schema)));
    }
  }
  const positional = positionalFor(spec);
  if (positional)
    command.argument(
      positional.variadic ? "[path...]" : "[" + positional.key + "]",
    );
  const seen = new Set<string>();
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === "--") break;
    if (!arg?.startsWith("--")) continue;
    const flag = arg.slice(2).split("=")[0] ?? "";
    if (seen.has(flag))
      throw new CliError(
        "validation",
        "duplicate_argument",
        "Repeated CLI flag.",
      );
    seen.add(flag);
    if (fields.has(flag) && !arg.includes("=")) {
      const next = args[++i];
      if (next === undefined || next.startsWith("--"))
        throw new CliError("validation", "invalid_argument", "Option requires a value.");
    }
  }
  try {
    command.parse(args, { from: "user" });
  } catch (e) {
    if (e instanceof CliError) throw e;
    throw new CliError(
      "validation",
      "invalid_argument",
      "Invalid command arguments. Use command --help.",
    );
  }
  const context: Obj = {},
    input: Obj = {},
    options: Obj = {};
  const values: Record<string, unknown> = command.opts();
  for (const option of command.options) {
    const field = fields.get(option.long?.slice(2) ?? "");
    const v = values[option.attributeName()];
    if (!field || v === undefined) continue;
    const target =
      field.group === "input"
        ? input
        : field.group === "context"
          ? context
          : options;
    // Round-trip through the strict parser to check Unicode in string flags as well.
    target[field.key] = parseJson(Buffer.from(JSON.stringify(v)));
  }
  if (positional && command.args.length) {
    if (input[positional.key] !== undefined)
      throw new CliError(
        "validation",
        "duplicate_argument",
        "Field supplied by both positional and flag.",
      );
    input[positional.key] = positional.variadic
      ? command.args
      : str(command.args[0]);
  }
  return {
    kind: "typed",
    value: {
      protocol_version: "0.1",
      contract_revision: "R9",
      command: spec.id,
      context,
      input,
      options,
    },
  };
}
function positionalFor(
  spec: Spec,
): { key: string; variadic: boolean } | undefined {
  if (spec.id === "system.help") return { key: "path", variadic: true };
  if (["system.schema", "system.capabilities"].includes(spec.id))
    return { key: "command_id", variadic: false };
  return undefined;
}
