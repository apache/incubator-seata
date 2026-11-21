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
import { type Invocation } from "../command/registry.js";
import { type Obj, object, str } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { StateStore, stateDirectory } from "../adapter/statestore/client.js";
import { CredentialStore } from "../adapter/auth/credentials.js";
import { adapterId, ManagementReader, transactionDTO, branchDTO, lockDTO, evidence } from "../adapter/management/read.js";
import { endpoint } from "./profile.js";
import { loadCa } from "../adapter/transport/ca.js";
import { profileProxy } from "../adapter/transport/proxy.js";

export const readImplemented = new Set([
  "namespace.list", "cluster.list", "server.list", "transaction.list", "transaction.get",
  "branch.list", "branch.get", "lock.list", "lock.check",
]);

export async function readerFor(inv: Invocation, signal: AbortSignal, verifyBinding?: (binding: Obj) => void): Promise<ManagementReader> {
  const directory = stateDirectory(), deadline = performance.now() + Number(inv.options.timeout_ms ?? 30000);
  const store = new StateStore(directory, Math.max(1, deadline - performance.now()));
  try {
    const binding = await store.call("auth.binding", { name: inv.context.profile ?? null }, signal);
    verifyBinding?.(binding);
    const profile = object(binding.profile);
    const management = object(profile.management), target = object(profile.target);
    if (object(profile.trust).state !== "accepted")
      throw new CliError("policy", "trust_required", "Explicitly accept the complete management target before making requests.");
    if (management.compatibility_id !== adapterId)
      throw new CliError("policy", "compatibility_unverified", "No pinned read adapter exists for this compatibility ID.");
    if (!management.naming_endpoint)
      throw new CliError("api", "management_endpoint_missing", "An explicit NamingServer endpoint is required.");
    const proxy = profileProxy(management);
    const ca = loadCa(str(management.ca_ref), signal);
    if (binding.credential === null)
      throw new CliError("authentication", "authentication_required", "Log in to the trusted profile before making management reads.");
    const credential = object(binding.credential), policy = str(object(profile.policy).storage_policy);
    if (credential.provider !== (policy === "protected_file" ? "protected_file" : "keyring"))
      throw new CliError("policy", "store_unsafe", "Credential provider differs from the trusted profile policy.");
    const secrets = new CredentialStore(directory, policy, str(binding.state_store_id));
    const token = await secrets.get(str(credential.credential_ref), signal);
    const current = await store.call("auth.binding", { name: inv.context.profile ?? null }, signal);
    verifyBinding?.(current);
    if (object(current.profile).auth_generation !== profile.auth_generation || current.target_digest !== binding.target_digest ||
        current.credential === null || object(current.credential).credential_ref !== credential.credential_ref)
      throw new CliError("authentication", "credential_changed", "Credential was invalidated while loading; retry the read with the current profile.");
    const route = inv.context.cluster !== undefined ? { cluster: str(inv.context.cluster) }
      : inv.context.vgroup !== undefined ? { vgroup: str(inv.context.vgroup) }
      : target.vgroup !== undefined ? { vgroup: str(target.vgroup) } : { cluster: str(target.cluster) };
    const beforeAttempt = async (): Promise<void> => {
      signal.throwIfAborted();
      if (loadCa(str(management.ca_ref), signal) !== ca)
        throw new CliError("policy", "ca_changed", "CA content changed during collection; start a new read after verifying trust.");
      if (performance.now() >= deadline)
        throw new CliError("cancelled", "context_cancelled", "Management read deadline expired.");
      const check = new StateStore(directory, Math.max(1, deadline - performance.now()));
      try {
        const current = await check.call("auth.binding", { name: inv.context.profile ?? null }, signal);
        verifyBinding?.(current);
        const p = object(current.profile);
        const ref = (b: Obj) => b.credential === null ? null : object(b.credential).credential_ref;
        if (current.state_store_id !== binding.state_store_id || p.profile_id !== profile.profile_id ||
            p.revision !== profile.revision || p.auth_generation !== profile.auth_generation ||
            current.target_digest !== binding.target_digest || current.trust_digest !== binding.trust_digest || ref(current) !== ref(binding))
          throw new CliError("authentication", "credential_changed", "Profile or authentication changed during collection; no further request was sent.");
      } finally { await check.close(); }
    };
    return new ManagementReader({ endpoint: endpoint(str(management.naming_endpoint), str(profile.environment), true),
      consoleEndpoint: endpoint(str(management.console_endpoint), str(profile.environment), true),
      namespace: str(inv.context.namespace ?? target.namespace), ...route }, signal, token, beforeAttempt, ca, proxy);
  } finally { await store.close(); }
}

export async function readCommand(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  const client = await readerFor(inv, signal), input = inv.input;
  switch (inv.command) {
    case "namespace.list": return client.namespaces();
    case "cluster.list": return client.clusters();
    case "server.list": return client.servers();
    case "transaction.list":
    case "lock.list": {
      const kind = inv.command === "transaction.list" ? "transaction" : "lock";
      const result = await client.page(kind, input), observed = new Date().toISOString();
      return { [kind === "transaction" ? "transactions" : "locks"]:
        result.rows.map(row => kind === "transaction" ? transactionDTO(row, observed) : lockDTO(row, observed)),
        pagination: result.pagination, completeness: result.completeness };
    }
    case "transaction.get": {
      const result = await client.transaction(str(input.xid));
      const transaction = transactionDTO(result.row, new Date().toISOString());
      return { transaction, completeness: result.completeness,
        evidence: [evidence(inv.command, transaction.xid ?? null, transaction, result.completeness)] };
    }
    case "branch.list":
    case "branch.get": {
      const xid = str(input.xid), result = await client.branches(xid), observed = new Date().toISOString();
      const branches = result.rows.map(row => branchDTO(row, xid, observed));
      if (inv.command === "branch.get") {
        const branch = branches.find(row => row.branch_id === input.branch_id);
        if (!branch) throw new CliError("not_found", "branch_not_found", "Branch was not present in the observed parent branch set.");
        return { branch, completeness: result.completeness,
          evidence: [evidence(inv.command, { xid: branch.xid ?? null, branch_id: branch.branch_id ?? null }, branch, result.completeness)] };
      }
      const page = Number(input.page ?? 1), size = Math.min(Number(input.page_size ?? 20), Number(input.limit ?? 10000));
      const selected = branches.slice((page - 1) * size, page * size);
      return { branches: selected, pagination: { page, page_size: size, returned: selected.length,
        total: branches.length, has_more: page * size < branches.length }, completeness: { ...result.completeness, scope: "single_page" } };
    }
    case "lock.check": return client.checkLock(str(input.xid), str(input.branch_id));
    default: throw new CliError("internal", "contract_violation", "Unknown read handler.");
  }
}
