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
import { PrivateDirectory } from "../platform/linux.js";
import { StateStore, stateDirectory } from "../adapter/statestore/client.js";
import { existsSync } from "node:fs";
import { resolve, join } from "node:path";

export async function storageCapability(
  signal: AbortSignal,
): Promise<"available" | "unavailable" | "not_evaluated"> {
  const path = resolve(stateDirectory());
  if (!["linux", "darwin"].includes(process.platform) || path === "/") return "unavailable";
  if (!existsSync(path)) return "not_evaluated";
  try {
    const directory = new PrivateDirectory(path, false);
    directory.close();
    if (!existsSync(join(path, "state.db"))) return "not_evaluated";
    const store = new StateStore(path);
    try {
      await store.call("info", {}, signal);
      return "available";
    } finally {
      await store.close();
    }
  } catch {
    return "unavailable";
  }
}
