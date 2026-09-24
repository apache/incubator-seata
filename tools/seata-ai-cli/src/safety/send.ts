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
import { StateStore } from "../adapter/statestore/client.js";
import {
  requestOnce,
  type HttpRequest,
  type HttpResponse,
} from "../adapter/transport/http.js";
import { type Obj, canonical, digest, object, str } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";

const permits = new WeakMap<
  object,
  { request: HttpRequest; digest: string; consumed: boolean }
>();
/** Internal model boundary: never exposed as a CLI command or public SDK. */
export async function commitSendPermit(
  store: StateStore,
  binding: Obj,
  request: HttpRequest,
  signal: AbortSignal,
): Promise<object> {
  signal.throwIfAborted();
  // Capture immutable bytes before awaiting the commit. Caller aliases cannot change the request.
  const serialized = JSON.stringify(request);
  const frozenRequest: HttpRequest = JSON.parse(serialized);
  const requestDigest = digest(JSON.parse(serialized));
  if (binding.request_digest !== requestDigest)
    throw new CliError(
      "conflict",
      "plan_changed",
      "Transport request differs from the approved binding.",
    );
  const committed = await store.call("execution.sendIntent", binding, signal);
  if (object(committed.execution).state !== "SEND_INTENT")
    throw new CliError(
      "internal",
      "journal_failure",
      "No durable send intent was committed.",
    );
  const permit = Object.freeze({});
  permits.set(permit, {
    request: frozenRequest,
    digest: requestDigest,
    consumed: false,
  });
  return permit;
}
export async function sendWithPermit(
  permit: object,
  signal: AbortSignal,
): Promise<HttpResponse> {
  const record = permits.get(permit);
  if (!record || record.consumed)
    throw new CliError(
      "policy",
      "send_permit_invalid",
      "No unused process-local send permit.",
    );
  record.consumed = true; // Synchronous, before any await or network activity.
  if (digest(JSON.parse(JSON.stringify(record.request))) !== record.digest)
    throw new CliError("conflict", "plan_changed", "Request binding changed.");
  try {
    return await requestOnce(record.request, signal);
  } catch {
    throw new CliError(
      "indeterminate",
      "write_outcome_unknown",
      "The write may have reached the server; use read-only reconciliation.",
      "read_only",
    );
  }
}
export function requestDigest(request: HttpRequest): string {
  return digest(JSON.parse(JSON.stringify(request)));
}
