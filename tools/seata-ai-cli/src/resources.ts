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
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { object, parseJson, str } from "./protocol/value.js";
import { CliError } from "./protocol/error.js";

export const packageRoot = fileURLToPath(new URL("../", import.meta.url));
const contents = new Map<string, Buffer>();
const manifest = object(
  parseJson(readFileSync(resolve(packageRoot, "api/resources.json"))),
);
for (const [path, expected] of Object.entries(manifest)) {
  if (
    !/^(api|schemas|compatibility|skills)\/[a-zA-Z0-9/_.-]+$/.test(path) ||
    path.includes("..")
  )
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid package resource path.",
    );
  const bytes = readFileSync(resolve(packageRoot, path));
  if (createHash("sha256").update(bytes).digest("hex") !== str(expected))
    throw new CliError(
      "internal",
      "contract_violation",
      "Package resource digest mismatch.",
    );
  contents.set(path, bytes);
}
export function resource(path: string): Buffer {
  const value = contents.get(path);
  if (!value)
    throw new CliError(
      "internal",
      "contract_violation",
      "Required package resource is missing.",
    );
  return Buffer.from(value);
}
