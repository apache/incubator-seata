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
import { Ajv2020 } from "ajv/dist/2020.js";
import type { ValidateFunction } from "ajv";
import formats from "ajv-formats";
import { resource } from "../resources.js";
import {
  type Json,
  type Obj,
  object,
  str,
  parseJson,
  isObject,
} from "../protocol/value.js";
import { CliError } from "../protocol/error.js";

export const ajv = new Ajv2020({
  strict: true,
  strictRequired: true,
  strictTypes: true,
  allErrors: true,
  validateFormats: true,
});
formats.default(ajv);
export interface Spec {
  id: string;
  path: string[];
  risk: string;
  confirmation: string;
  backends: string;
  schema_version: string;
  invocation: Obj;
  output_contract: string;
}
const catalog = parseJson(
  resource("api/protocol/registry.json"),
  false,
  4 * 1024 * 1024,
);
if (!Array.isArray(catalog))
  throw new CliError("internal", "contract_violation", "Invalid registry.");
export const registry: readonly Spec[] = catalog.map((v) => {
  const o = object(v);
  if (!Array.isArray(o.path))
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid command path.",
    );
  return {
    id: str(o.id),
    path: o.path.map(str),
    risk: str(o.risk),
    confirmation: str(o.confirmation),
    backends: str(o.backends),
    schema_version: str(o.schema_version),
    invocation: object(o.invocation),
    output_contract: str(o.output_contract),
  };
});
const validators = new Map<string, ValidateFunction>();
export const validateEnvelope = ajv.compile(
  object(
    parseJson(resource("schemas/envelope.schema.json"), false, 4 * 1024 * 1024),
  ),
);
export const outputSchemas = object(
  parseJson(resource("api/protocol/outputs.json"), false, 4 * 1024 * 1024),
);
const outputValidators = new Map<string, ValidateFunction>();
export function validateOutput(command: string, data: Obj): void {
  let check = outputValidators.get(command);
  if (!check && outputSchemas[command]) {
    check = ajv.compile(object(outputSchemas[command]));
    outputValidators.set(command, check);
  }
  if (!check || !check(data))
    throw new CliError(
      "internal",
      "contract_violation",
      "Handler output violates its declared DTO schema.",
    );
}
export function specFor(id: string): Spec {
  const spec = registry.find((s) => s.id === id);
  if (!spec)
    throw new CliError("validation", "unknown_command", "Unknown command ID.");
  return spec;
}
export interface Invocation {
  protocol_version: "0.1";
  contract_revision: "R9";
  command: string;
  context: Obj;
  input: Obj;
  options: Obj;
}
export function validate(value: Json): Invocation {
  const o = object(value);
  const command = str(o.command);
  if (command === "system.invoke")
    throw new CliError(
      "validation",
      "recursive_invoke",
      "An invocation cannot target system.invoke.",
    );
  let validator = validators.get(command);
  if (!validator) {
    validator = ajv.compile(specFor(command).invocation);
    validators.set(command, validator);
  }
  if (!validator(o))
    throw new CliError(
      "validation",
      "schema_violation",
      "Invocation does not satisfy its command schema. Use schema for the exact fields.",
    );
  const context = object(o.context),
    input = object(o.input),
    options = object(o.options);
  semantic(input);
  if (typeof options.client_request_id === "string")
    options.client_request_id = options.client_request_id.toLowerCase();
  if (typeof input.client_request_id === "string")
    input.client_request_id = input.client_request_id.toLowerCase();
  options.timeout_ms ??= 30000;
  return {
    protocol_version: "0.1",
    contract_revision: "R9",
    command,
    context,
    input,
    options,
  };
}
function semantic(value: Json): void {
  if (Array.isArray(value)) {
    value.forEach(semantic);
    return;
  }
  if (!isObject(value)) return;
  for (const [key, item] of Object.entries(value)) {
    if (
      key === "branch_id" &&
      typeof item === "string" &&
      (!/^[1-9][0-9]*$/.test(item) || BigInt(item) > 9223372036854775807n)
    )
      throw new CliError(
        "validation",
        "invalid_argument",
        "branch_id must be a positive signed int64 string.",
      );
    if (
      key === "time_range" &&
      isObject(item) &&
      typeof item.start === "string" &&
      typeof item.end === "string" &&
      Date.parse(item.start) >= Date.parse(item.end)
    )
      throw new CliError(
        "validation",
        "invalid_argument",
        "Time range start must precede end.",
      );
    semantic(item);
  }
}
