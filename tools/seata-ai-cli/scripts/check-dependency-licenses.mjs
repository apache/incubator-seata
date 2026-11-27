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

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { copyFileSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";

const scannerPath = process.argv[2];
assert.ok(scannerPath, "Pass the path to the License Eye executable.");
const scanner = resolve(scannerPath);
const lock = JSON.parse(readFileSync(new URL("../npm-shrinkwrap.json", import.meta.url), "utf8"));
const expected = Object.entries(lock.packages)
  .filter(([path, pkg]) => path.startsWith("node_modules/") && !pkg.dev)
  .map(([path, pkg]) => `${path.slice("node_modules/".length)}@${pkg.version}`)
  .sort();
const scratch = mkdtempSync(join(tmpdir(), "seata-cli-licenses-"));
try {
  const cwd = realpathSync(scratch);
  for (const name of ["package.json", "npm-shrinkwrap.json", ".npmrc"])
    copyFileSync(new URL("../" + name, import.meta.url), join(cwd, name));

  // Install separately so an npm failure cannot be swallowed by the scanner.
  execFileSync("npm", ["ci", "--omit=dev", "--ignore-scripts", "--no-audit"], { cwd, stdio: "inherit" });
  writeFileSync(join(cwd, ".licenserc.yaml"), [
    "header:",
    "  license:",
    "    spdx-id: Apache-2.0",
    "dependency:",
    "  files:",
    "    - package.json",
    "",
  ].join("\n"));
  writeFileSync(join(cwd, "licenses.tpl"), "{{ .Groups | toJson }}\n");

  // License Eye prompts for a second install. Reuse the already verified tree.
  const scanOptions = { cwd, input: "s\n", stdio: ["pipe", "inherit", "inherit"] };
  execFileSync(scanner, ["dependency", "resolve", "--summary", join(cwd, "licenses.tpl")], scanOptions);
  const report = JSON.parse(readFileSync(join(cwd, "LICENSE"), "utf8"));
  const resolved = report.flatMap(group => group.Deps.map(dep => `${dep.Name}@${dep.Version}`)).sort();
  assert.deepEqual(resolved, expected, "License Eye must resolve every locked CLI runtime dependency.");

  execFileSync(scanner, ["dependency", "check"], scanOptions);
  console.log(`License Eye checked all ${resolved.length} locked CLI runtime dependencies.`);
} finally {
  rmSync(scratch, { recursive: true, force: true });
}
