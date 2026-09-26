// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import { StateStore, stateDirectory } from "../statestore/client.js";
import { CredentialStore } from "./credentials.js";
import { object, str, type Obj } from "../../protocol/value.js";
import { PrivateDirectory } from "../../platform/linux.js";
import { closeSync } from "node:fs";

/** Cleanup is subordinate to already-committed fencing, never a prerequisite. */
export async function cleanupCredentials(store: StateStore, signal: AbortSignal, root = stateDirectory(), reapFlows = false): Promise<string> {
  try {
    const result = await store.call("credential.cleanup.list", { reap_flows: reapFlows }, signal);
    if (!Array.isArray(result.credentials)) return "pending";
    let failed = false;
    for (const item of result.credentials) {
      if (signal.aborted) return "pending";
      let directory: PrivateDirectory | undefined, lock: number | undefined;
      try {
        const record = object(item), provider = str(record.provider);
        directory = new PrivateDirectory(root, false);
        lock = directory.lock("auth-flow-" + str(record.flow_id) + ".lock");
        const secrets = new CredentialStore(root, provider === "protected_file" ? provider : "keyring_required", str(result.state_store_id));
        await secrets.delete(str(record.credential_ref), signal);
        await store.call("credential.cleanup.done", { credential_ref: record.credential_ref ?? null }, signal);
      } catch { failed = true; }
      finally { if (lock !== undefined) closeSync(lock); directory?.close(); }
      if (signal.aborted) return "pending";
    }
    return failed || result.credentials.length === 128 ? "pending" : "complete";
  } catch { return "pending"; }
}

export async function recoverCredentials(store: StateStore, signal: AbortSignal, warn?: (warning: Obj) => void, timeoutMs = 30000): Promise<void> {
  const budgetMs = Math.max(100, Math.min(1000, Math.floor(timeoutMs / 4)));
  const bounded = AbortSignal.any([signal, AbortSignal.timeout(budgetMs)]);
  const result = await cleanupCredentials(store, bounded, stateDirectory(), true);
  signal.throwIfAborted();
  if (result === "pending")
    warn?.({ code: "credential_cleanup_pending", message: "Local credential cleanup is still pending." });
}
