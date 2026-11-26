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
import { mkdir } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

if (process.platform === "darwin") {
  const root = fileURLToPath(new URL("../", import.meta.url));
  const output = resolve(root, "dist/native/seata-darwin-helper");
  await mkdir(resolve(root, "dist/native"), { recursive: true });
  const result = spawnSync("/usr/bin/clang", ["-O2", "-fPIE", "-arch", "arm64", "-arch", "x86_64",
    "-Wno-deprecated-declarations",
    "-framework", "Security", "-framework", "CoreFoundation",
    "-o", output, resolve(root, "scripts/darwin-helper.c")], { stdio: "inherit" });
  if (result.status !== 0) throw new Error("Darwin helper build failed; Command Line Tools are required.");
}
