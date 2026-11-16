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
import { readFile, writeFile, mkdir, copyFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { outputs } from "./output-schemas.mjs";
import { parse as parseYaml } from "yaml";
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
  const remoteWrite = ["controlled_write", "high_risk_write"].includes(c.risk);
  const context = object(
    Object.fromEntries(
      ["profile", "namespace", "cluster", "vgroup"].map((k) => [k, string]),
    ),
    c.remote || c.id.startsWith("auth.") ? ["profile"] : [],
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
  if (["read", "sensitive_read", "diagnostic_read"].includes(c.risk) && !c.id.startsWith("auth."))
    options.properties.export = { type: "string", minLength: 1, maxLength: 4096,
      description: "Explicitly export this result as private JSON; existing files are never overwritten." };
  if (remoteWrite)
    Object.assign(options.properties, {
      dry_run: { type: "boolean", default: false },
      confirm_high_risk: { type: "boolean", default: false },
      plan_id: { type: "string", minLength: 1, maxLength: 128 },
    });
  const invocation = {
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
  if (remoteWrite) {
    const then = {
      properties: {
        options: {
          required: ["client_request_id"],
          type: "object",
          properties: { client_request_id: {} },
        },
      },
      type: "object",
    };
    if (c.input_schema.properties.expected_direction)
      then.properties.input = {
        required: ["expected_direction"],
        type: "object",
        properties: { expected_direction: {} },
      };
    invocation.allOf = [
      {
        if: {
          not: {
            properties: {
              options: {
                required: ["dry_run"],
                properties: { dry_run: { const: true } },
                type: "object",
              },
            },
            type: "object",
          },
        },
        then,
      },
    ];
  }
  if (c.id === "diagnose.run")
    invocation.allOf = [
      {
        if: {
          properties: {
            input: {
              properties: { scope: { const: "transaction" } },
              type: "object",
            },
          },
          type: "object",
        },
        then: {
          properties: {
            context: {
              required: ["profile"],
              type: "object",
              properties: { profile: {} },
            },
          },
          type: "object",
        },
      },
    ];
  const spec = {
    id: c.id,
    path: c.path.split(" ").slice(1),
    risk: c.risk,
    confirmation: c.confirmation,
    backends: c.backends,
    // Expose the consolidated Skill while preserving the command baseline.
    skills: c.skills.includes("seata-") ? "seata-shared" : c.skills,
    schema_version: "0.1",
    remote: c.remote,
    invocation,
    output_contract: c.output_contract,
  };
  registry.push(spec);
  if (c.id !== "system.invoke")
    await write("schemas/invocations/" + c.id + ".schema.json", invocation);
}
// The delegated input describes the actual target schemas, including approved
// optional extensions. Never retain a separate stale copy of their options.
registry.find(spec => spec.id === "system.invoke").invocation.properties.input.properties.invocation.oneOf =
  registry.filter(spec => spec.id !== "system.invoke").map(spec => spec.invocation);
await write("api/protocol/registry.json", registry);
await write(
  "api/protocol/outputs.json",
  outputs(
    registry,
    await read("test/contract/r11/schemas/execution.schema.json"),
  ),
);
await mkdir(resolve(root, "schemas"), { recursive: true });
for (const name of ["envelope", "execution", "plan"])
  await copyFile(
    resolve(root, "test/contract/r11/schemas/" + name + ".schema.json"),
    resolve(root, "schemas/" + name + ".schema.json"),
  );
await mkdir(resolve(root, "api/store"), { recursive: true });
await copyFile(
  resolve(root, "src/adapter/statestore/schema.sql"),
  resolve(root, "api/store/schema.sql"),
);
const skillIds = ["seata-shared"];
const skillPaths = ["SKILL.md", "references/evidence.md"];
const skills = [];
for (const id of skillIds) {
  const entries = [];
  for (const path of skillPaths) {
    const bytes = await readFile(resolve(root, "skills", id, path));
    entries.push({ path, bytes: bytes.length, digest: createHash("sha256").update(bytes).digest("hex") });
  }
  const main = await readFile(resolve(root, "skills", id, "SKILL.md"), "utf8");
  const front = main.match(/^---\n([\s\S]*?)\n---\n/);
  if (!front || main.split("\n").length > 180) throw new Error("Invalid Skill entrypoint: " + id);
  const metadata = parseYaml(front[1]);
  if (metadata.name !== id || typeof metadata.description !== "string" || !metadata.description.trim())
    throw new Error("Invalid Skill metadata: " + id);
  skills.push({ skill_id: id, version: "0.1.0-r11.0", description: metadata.description,
    digest: createHash("sha256").update(JSON.stringify(entries)).digest("hex"), files: entries });
}
await write("api/skills.json", { version: "0.1.0-r11.0", skills });
const files = [
  "api/skills.json",
  ...skillIds.flatMap(id => skillPaths.map(path => `skills/${id}/${path}`)),
  "api/protocol/registry.json",
  "api/protocol/outputs.json",
  "api/store/schema.sql",
  "schemas/envelope.schema.json",
  "schemas/plan.schema.json",
  "schemas/execution.schema.json",
  "compatibility/manifest.json",
];
await write(
  "api/resources.json",
  Object.fromEntries(
    await Promise.all(
      files.map(async (p) => [
        p,
        createHash("sha256")
          .update(await readFile(resolve(root, p)))
          .digest("hex"),
      ]),
    ),
  ),
);
