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
import { spawn } from "node:child_process";
import { randomBytes } from "node:crypto";
import { fileURLToPath } from "node:url";
import { StateStore, stateDirectory } from "../adapter/statestore/client.js";
import { loginTarget } from "../adapter/auth/bridge.js";
import { object, isObject, type Obj } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { type Invocation } from "../command/registry.js";
import { readerFor } from "./read.js";
import { cleanupCredentials, recoverCredentials } from "../adapter/auth/cleanup.js";

export function localBrowser(): boolean {
  if (!["linux", "darwin"].includes(process.platform)) return false;
  // Explicit host-runtime assertion, not a remote browser tunnel and not a
  // certification claim. Headless/SSH defaults never assume shared loopback.
  if (process.env.SEATA_AI_LOCAL_AUTH === "shared_loopback") return true;
  if (process.platform === "darwin")
    return !process.env.SSH_CONNECTION && !process.env.SSH_TTY && !process.env.CI &&
      process.stdin.isTTY === true && process.stdout.isTTY === true;
  return !process.env.SSH_CONNECTION && !process.env.SSH_TTY && !!(process.env.DISPLAY || process.env.WAYLAND_DISPLAY);
}

function statusFromBinding(binding: Obj): Obj {
  const profile = object(binding.profile);
  return { authenticated: binding.credential !== null, generation: profile.auth_generation ?? null,
    storage: object(profile.policy).storage_policy ?? "keyring_required", verification: "not_evaluated", expiry_hint: null };
}

export async function authStatus(inv: Invocation, signal: AbortSignal, warn?: (warning: Obj) => void): Promise<Obj> {
  const store = new StateStore(undefined, Number(inv.options.timeout_ms));
  try {
    const local = await store.call("auth.status", { name: inv.context.profile ?? null }, signal);
    await recoverCredentials(store, signal, warn, Number(inv.options.timeout_ms));
    const withAssurance = (status: Obj): Obj => ({ ...status,
      assurance: status.storage === "protected_file" ? "local_user_file" : "os_keyring" });
    if (inv.input.verify !== true) return withAssurance(local);
    const binding = await store.call("auth.binding", { name: inv.context.profile ?? null }, signal);
    if (binding.credential === null) {
      const current = await store.call("auth.status", { name: inv.context.profile ?? null }, signal);
      return { ...withAssurance(current), verification: current.authenticated === true ? "credential_changed" : "not_authenticated" };
    }
    const credential = object(binding.credential);
    try {
      const reader = await readerFor(inv, signal);
      await reader.namespaces();
      const current = await store.call("auth.binding", { name: inv.context.profile ?? null }, signal);
      if (current.credential === null || object(current.credential).credential_ref !== credential.credential_ref)
        return { ...withAssurance(await store.call("auth.status", { name: inv.context.profile ?? null }, signal)), verification: "credential_changed" };
      return { ...withAssurance(statusFromBinding(current)), verification: "unavailable", verification_scope: "naming_namespace_read",
        probe_result: "read_succeeded", reason_code: "token_validity_unproven_by_read", observed_at: new Date().toISOString() };
    } catch (error) {
      if (error instanceof CliError && error.type === "authentication" && error.subtype === "authentication_required") {
        const invalidated = await store.call("credential.invalidate", { name: inv.context.profile ?? null, credential_ref: credential.credential_ref ?? null }, signal);
        const current = await store.call("auth.status", { name: inv.context.profile ?? null }, signal);
        return { ...withAssurance(current), verification: invalidated.changed === true ? "invalid" : "credential_changed",
          credential_cleanup: await cleanupCredentials(store, signal) };
      }
      if (error instanceof CliError && error.type === "cancelled") throw error;
      const current = await store.call("auth.status", { name: inv.context.profile ?? null }, signal);
      const changed = current.authenticated !== local.authenticated || current.generation !== local.generation ||
        (error instanceof CliError && error.subtype === "credential_changed");
      return { ...withAssurance(current), verification: changed ? "credential_changed" : "unavailable",
        verification_scope: "naming_namespace_read",
        reason_code: error instanceof CliError ? error.subtype : "verification_failed" };
    }
  } finally { await store.close(); }
}

export async function authLogin(inv: Invocation, signal: AbortSignal, warn?: (warning: Obj) => void): Promise<Obj> {
  if (!localBrowser()) throw new CliError("authentication", "local_auth_unavailable", "The host must provide a browser sharing this CLI's loopback network; headless login is not automatically enabled.");
  const root = stateDirectory(), store = new StateStore(root, Number(inv.options.timeout_ms));
  try {
    const profile = object((await store.call("profile.get", { name: inv.context.profile ?? null }, signal)).profile);
    loginTarget(profile);
    await recoverCredentials(store, signal, warn, Number(inv.options.timeout_ms));
    const created = await store.call("flow.create", { name: inv.context.profile ?? null }, signal), flow = object(created.flow);
    const data: Obj = { flow_id: flow.flow_id ?? null, flow_state: "pending", expires_at: flow.expires_at ?? null,
      assurance: object(profile.policy).storage_policy === "protected_file" ? "local_user_file" : "os_keyring",
      host_support: "not_evaluated" };
    if (created.created !== true) return { ...data, verification_url_available: false };
    const child = spawn(process.execPath, [fileURLToPath(new URL("../adapter/auth/helper.js", import.meta.url))], {
      detached: true, stdio: ["ignore", "ignore", "ignore", "ipc"],
    });
    const token = randomBytes(32).toString("base64url");
    try {
      const port = await new Promise<number>((resolve, reject) => {
        const fail = () => reject(new CliError("authentication", "local_auth_unavailable", "Login helper did not become ready; no password was sent."));
        const timer = setTimeout(fail, 5000);
        signal.addEventListener("abort", fail, { once: true });
        const clean = () => { clearTimeout(timer); signal.removeEventListener("abort", fail); };
        child.once("error", () => { clean(); fail(); });
        child.once("exit", () => { clean(); fail(); });
        child.once("message", value => {
          if (!isObject(value) || value.kind !== "ready" || typeof value.port !== "number" || !Number.isInteger(value.port) || value.port < 1 || value.port > 65535) {
            clean(); fail(); return;
          }
          const port = value.port;
          child.send({ kind: "ack" }, error => {
            clean();
            if (error || signal.aborted) fail(); else resolve(port);
          });
        });
        child.send({ kind: "init", root, flow_id: flow.flow_id, helper_instance_id: flow.helper_instance_id, token }, error => { if (error) { clean(); fail(); } });
      });
      child.disconnect(); child.unref();
      return { ...data, verification_url: "http://127.0.0.1:" + port + "/login/" + token };
    } catch (error) {
      child.kill(); if (child.connected) child.disconnect(); child.unref();
      await store.call("flow.fail", { flow_id: flow.flow_id ?? null, helper_instance_id: flow.helper_instance_id ?? null, reason: "helper_unavailable" });
      throw error;
    }
  } finally { await store.close(); }
}
