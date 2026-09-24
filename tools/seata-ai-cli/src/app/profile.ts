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
    management.proxy_endpoint = endpoint(
      str(m.proxy_endpoint),
      environment,
      accepted,
    );
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
): Promise<Obj> {
  const store = new StateStore(undefined, Number(inv.options.timeout_ms));
  try {
    switch (inv.command) {
      case "setup.initialize":
      case "profile.create": {
        const config = profileConfig(
          object(inv.input.config),
          inv.input.accept_target === true,
        );
        if (
          inv.command === "setup.initialize" &&
          !object(config.config.management).naming_endpoint
        )
          throw new CliError(
            "api",
            "management_endpoint_missing",
            "Provide the explicit NamingServer endpoint; integrated discovery is not yet verified.",
          );
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
        return await store.call(
          "profile.update",
          {
            ...config,
            name: inv.input.name ?? null,
            expected_revision: inv.input.expected_revision ?? null,
          },
          signal,
        );
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
      case "auth.logout":
        return await store.call(
          "auth.logout",
          { name: inv.context.profile ?? null },
          signal,
        );
      case "auth.login.cancel":
        return await store.call(
          "flow.cancel",
          {
            name: inv.context.profile ?? null,
            flow_id: inv.input.flow_id ?? null,
          },
          signal,
        );
      default:
        return await store.call(inv.command, inv.input, signal);
    }
  } finally {
    await store.close();
  }
}
