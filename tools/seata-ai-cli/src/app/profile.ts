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
import { type Obj, object, str, digest } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { StateStore } from "../adapter/statestore/client.js";
import { type Invocation } from "../command/registry.js";
import { cleanupCredentials, recoverCredentials } from "../adapter/auth/cleanup.js";
import { adapterId, ManagementReader } from "../adapter/management/read.js";
import { loadCa } from "../adapter/transport/ca.js";
import { profileProxy, proxyUrl } from "../adapter/transport/proxy.js";

export async function resolveSetupEndpoints(input: Obj, accepted: boolean, signal: AbortSignal): Promise<Obj> {
  const validated = profileConfig(input, accepted), management = object(validated.config.management);
  if (management.naming_endpoint !== undefined) return validated.config;
  if (!accepted) throw new CliError("policy", "trust_required", "Accept the supplied endpoint before probing its read-only interfaces.");
  if (management.compatibility_id !== adapterId)
    throw new CliError("policy", "compatibility_unverified", "No pinned endpoint discovery adapter exists for this compatibility ID.");
  if (management.deployment === "standalone_console")
    throw new CliError("api", "management_endpoint_missing", "A standalone Console requires an explicit NamingServer endpoint.");
  const proxy = profileProxy(management);
  const ca = loadCa(str(management.ca_ref), signal);
  const target = object(validated.config.target), base = str(management.console_endpoint);
  const client = new ManagementReader({ endpoint: base, consoleEndpoint: base, namespace: str(target.namespace),
    ...(target.vgroup !== undefined ? { vgroup: str(target.vgroup) } : { cluster: str(target.cluster) }) }, signal, undefined, async () => {
      if (loadCa(str(management.ca_ref), signal) !== ca)
        throw new CliError("policy", "ca_changed", "CA content changed during endpoint discovery.");
    }, ca, proxy);
  try {
    // Probe only pinned GET interfaces below the exact accepted base path.
    // No saved credential, discovered host, redirect or service-port guessing.
    await client.consoleProbe();
    await client.namespaces();
  } catch (error) {
    signal.throwIfAborted();
    if (error instanceof CliError && error.type === "cancelled") throw error;
    throw new CliError("api", "management_endpoint_missing", "The accepted endpoint did not provide both pinned anonymous read interfaces; specify the NamingServer endpoint explicitly.");
  }
  // This establishes available interfaces at one base URL, not physical
  // co-location or a certified integrated server deployment.
  return { ...validated.config, management: { ...management, naming_endpoint: base } };
}

export function endpoint(
  raw: string,
  environment: string,
  accepted: boolean,
): string {
  if (
    /[\u0000-\u0020\\?#]/.test(raw) ||
    /%(?:2f|5c|2e|00|25)/i.test(raw) ||
    /(?:^|\/)\.{1,2}(?:\/|$)/.test(raw)
  )
    throw new CliError(
      "validation",
      "invalid_argument",
      "Endpoint contains ambiguous path, query, fragment, or whitespace.",
    );
  let url: URL;
  try {
    url = new URL(raw);
  } catch {
    throw new CliError(
      "validation",
      "invalid_argument",
      "Invalid endpoint URL.",
    );
  }
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash
  )
    throw new CliError(
      "validation",
      "invalid_argument",
      "Endpoint must be HTTP(S) without credentials, query or fragment.",
    );
  if (
    url.protocol === "http:" &&
    (environment !== "development" ||
      !["127.0.0.1", "[::1]", "localhost"].includes(url.hostname) ||
      !accepted)
  )
    throw new CliError(
      "policy",
      "trust_required",
      "HTTP is allowed only for an explicitly accepted development loopback endpoint.",
    );
  return url.href.replace(/\/$/, "");
}
export function profileConfig(
  input: Obj,
  accepted: boolean,
): { config: Obj; target_digest: string; trust_digest: string } {
  const environment = str(input.environment),
    m = object(input.management),
    target = object(input.target);
  const management: Obj = {
    ...m,
    console_endpoint: endpoint(str(m.console_endpoint), environment, accepted),
    proxy_mode: m.proxy_mode ?? "direct",
    ca_ref: m.ca_ref ?? "system",
  };
  if (m.naming_endpoint !== undefined)
    management.naming_endpoint = endpoint(
      str(m.naming_endpoint),
      environment,
      accepted,
    );
  if (management.proxy_mode === "explicit" && !m.proxy_endpoint)
    throw new CliError(
      "validation",
      "invalid_argument",
      "Explicit proxy mode requires proxy_endpoint.",
    );
  if (management.proxy_mode !== "explicit" && m.proxy_endpoint)
    throw new CliError(
      "validation",
      "invalid_argument",
      "proxy_endpoint requires explicit proxy mode.",
    );
  if (m.proxy_endpoint)
    // A CONNECT proxy is a separately accepted hop, not the management origin.
    // HTTPS remains mandatory for production Console/Naming endpoints above.
    management.proxy_endpoint = proxyUrl(str(m.proxy_endpoint)).href.replace(/\/$/, "");
  profileProxy(management);
  const policy: Obj = {
    require_https: true,
    allow_selector_only_high_risk: false,
    storage_policy: "keyring_required",
    ...(input.policy ? object(input.policy) : {}),
  };
  if (environment === "production" && policy.require_https === false)
    throw new CliError(
      "policy",
      "trust_required",
      "Production profiles require HTTPS.",
    );
  const target_digest = digest({ management, target, environment, policy });
  const trust: Obj = {
    state: accepted ? "accepted" : "unverified",
    accepted_target_digest: accepted ? target_digest : null,
  };
  return {
    config: {
      name: str(input.name),
      environment,
      management,
      target,
      policy,
      trust,
    },
    target_digest,
    trust_digest: digest(trust),
  };
}
export async function localCommand(
  inv: Invocation,
  signal: AbortSignal,
  warn?: (warning: Obj) => void,
): Promise<Obj> {
  const store = new StateStore(undefined, Number(inv.options.timeout_ms));
  try {
    switch (inv.command) {
      case "setup.initialize":
      case "profile.create": {
        const accepted = inv.input.accept_target === true;
        const input = inv.command === "setup.initialize"
          ? await resolveSetupEndpoints(object(inv.input.config), accepted, signal) : object(inv.input.config);
        const config = profileConfig(input, accepted);
        const result = await store.call("profile.create", config, signal);
        return inv.command === "setup.initialize"
          ? {
              ...result,
              next_steps: ["auth.status", "auth.login", "connectivity.check"],
            }
          : result;
      }
      case "profile.update": {
        const old = object(
          (
            await store.call(
              "profile.get",
              { name: inv.input.name ?? null },
              signal,
            )
          ).profile,
        );
        const merged = {
          name: old.name,
          environment: old.environment,
          management: old.management,
          target: old.target,
          policy: old.policy,
          ...object(inv.input.patch),
        };
        const config = profileConfig(
          object(merged),
          inv.input.accept_target === true,
        );
        const result = await store.call(
          "profile.update",
          {
            ...config,
            name: inv.input.name ?? null,
            expected_revision: inv.input.expected_revision ?? null,
          },
          signal,
        );
        return { ...result, credential_cleanup: await cleanupCredentials(store, signal) };
      }
      case "auth.status": {
        if (inv.input.verify === true)
          throw new CliError(
            "policy",
            "command_unavailable",
            "No verified management authentication probe is enabled yet.",
          );
        return await store.call(
          "auth.status",
          { name: inv.context.profile ?? null },
          signal,
        );
      }
      case "auth.logout": {
        const result = await store.call(
          "auth.logout",
          { name: inv.context.profile ?? null },
          signal,
        );
        return { ...result, credential_cleanup: await cleanupCredentials(store, signal) };
      }
      case "profile.delete": {
        const result = await store.call("profile.delete", inv.input, signal);
        return { ...result, credential_cleanup: await cleanupCredentials(store, signal) };
      }
      case "auth.login.cancel": {
        try {
          return await store.call(
            "flow.cancel",
            {
              name: inv.context.profile ?? null,
              flow_id: inv.input.flow_id ?? null,
            },
            signal,
          );
        } finally {
          if (await cleanupCredentials(store, signal) === "pending")
            warn?.({ code: "credential_cleanup_pending", message: "Local credential cleanup is still pending." });
        }
      }
      case "auth.login.status": {
        try {
          return await store.call("flow.status", { name: inv.context.profile ?? null, flow_id: inv.input.flow_id ?? null }, signal);
        } finally {
          await recoverCredentials(store, signal, warn, Number(inv.options.timeout_ms));
        }
      }
      default:
        return await store.call(inv.command, inv.input, signal);
    }
  } finally {
    await store.close();
  }
}
