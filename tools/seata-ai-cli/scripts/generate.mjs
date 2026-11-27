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
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { outputs } from "./output-schemas.mjs";
const root = fileURLToPath(new URL("../", import.meta.url));
const read = async (p) => JSON.parse(await readFile(resolve(root, p), "utf8"));
const write = async (p, value) => {
  await mkdir(resolve(root, p, ".."), { recursive: true });
  await writeFile(resolve(root, p), JSON.stringify(value, null, 2) + "\n");
};
const contracts = await read("src/command/contracts.json");
const object = (properties, required = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});
const string = { type: "string", minLength: 1, maxLength: 4096 };
const uuid = {
  type: "string",
  pattern:
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
};
const registry = [];
for (const c of contracts) {
  const context = object(
    Object.fromEntries(
      ["profile", "namespace", "cluster", "vgroup"].map((k) => [k, string]),
    ),
    c.remote ? ["profile"] : [],
  );
  context.not = {
    required: ["cluster", "vgroup"],
    type: "object",
    properties: { cluster: {}, vgroup: {} },
  };
  const options = object({
    timeout_ms: {
      type: "integer",
      minimum: 1000,
      maximum: 120000,
      default: 30000,
    },
    client_request_id: uuid,
  });
  const invocation = c.id === "system.invoke" ? {} : {
    $schema: "https://json-schema.org/draft/2020-12/schema",
    title: c.id + " invocation",
    ...object(
      {
        protocol_version: { const: "0.1" },
        contract_revision: { const: "R9" },
        command: { const: c.id },
        context,
        input: c.input_schema,
        options,
      },
      [
        "protocol_version",
        "contract_revision",
        "command",
        "context",
        "input",
        "options",
      ],
    ),
  };
  const spec = {
    id: c.id,
    path: c.path.split(" ").slice(1),
    risk: c.risk,
    confirmation: c.confirmation,
    backends: c.backends,
    skills: c.skills,
    schema_version: "0.1",
    remote: c.remote,
    invocation,
    output_contract: c.output_contract,
  };
  registry.push(spec);
  if (c.id !== "system.invoke")
    await write("schemas/invocations/" + c.id + ".schema.json", invocation);
}
// Build the delegated schema from the same commands that invoke can dispatch.
const invoke = registry.find(spec => spec.id === "system.invoke");
invoke.invocation = {
  $schema: "https://json-schema.org/draft/2020-12/schema",
  title: "Seata AI CLI invoke request",
  oneOf: registry.filter(spec => spec.id !== invoke.id).map(spec => spec.invocation),
};
await write("api/protocol/registry.json", registry);
await write("api/protocol/outputs.json", outputs());

const envelope = await read("src/protocol/envelope.schema.json");
envelope.properties.command = { enum: registry.map(spec => spec.id) };
await write("schemas/envelope.schema.json", envelope);
const files = [
  "api/protocol/registry.json",
  "api/protocol/outputs.json",
  "schemas/envelope.schema.json",
];
await write("api/resources.json", Object.fromEntries(await Promise.all(files.map(async path => [
  path,
  createHash("sha256").update(await readFile(resolve(root, path))).digest("hex"),
]))));
