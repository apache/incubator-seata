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
import { StateStore } from "../../dist/adapter/statestore/client.js";
import { profileConfig } from "../../dist/app/profile.js";
import { digest } from "../../dist/protocol/value.js";
import {
  commitSendPermit,
  requestDigest,
  sendWithPermit,
} from "../../dist/safety/send.js";
import { randomUUID } from "node:crypto";

process.once("message", async ({ directory, endpoint, phase, requestId }) => {
  try {
    const store = new StateStore(directory);
    const normalized = profileConfig(
      {
        name: "p",
        environment: "development",
        management: {
          console_endpoint: endpoint,
          naming_endpoint: endpoint,
          compatibility_id: "fixture",
        },
        target: { namespace: "public", cluster: "default" },
      },
      true,
    );
    const profile = (await store.call("profile.create", normalized)).profile;
    const { flow } = await store.call("flow.create", { name: "p" });
    await store.call("flow.submit", { flow_id: flow.flow_id });
    const reference = (
      await store.call("flow.stage", {
        flow_id: flow.flow_id,
        provider: "keyring",
      })
    ).credential_ref;
    await store.call("flow.activate", {
      flow_id: flow.flow_id,
      credential_ref: reference,
    });
    const claim = {
      client_request_id: requestId,
      command: "transaction.retry.pause",
      intent: { xid: "x" },
      intent_digest: digest({ xid: "x" }),
      target: { profile_id: profile.profile_id },
      target_digest: normalized.target_digest,
      profile_id: profile.profile_id,
    };
    const e = (await store.call("execution.claim", claim)).execution;
    await store.call("execution.lock", { execution_id: e.execution_id });
    const request = {
      endpoint,
      suffix: "/api/v1/test/write",
      method: "PUT",
      query: { xid: "x" },
    };
    const plan = {
      plan_id: randomUUID(),
      request_digest: requestDigest(request),
      material_digest: digest({ status: 4 }),
      created_at: new Date().toISOString(),
      expires_at: new Date(Date.now() + 120000).toISOString(),
    };
    const prepared = (
      await store.call("execution.prepare", {
        execution_id: e.execution_id,
        plan,
      })
    ).execution;
    if (phase === "before-intent") {
      process.send({ ready: true, executionId: e.execution_id });
      return;
    }
    const binding = {
      execution_id: e.execution_id,
      record_version: prepared.record_version,
      intent_digest: claim.intent_digest,
      material_digest: plan.material_digest,
      request_digest: plan.request_digest,
      auth_generation: 0,
      profile_revision: 1,
      trust_digest: normalized.trust_digest,
      credential_ref: reference,
    };
    const permit = await commitSendPermit(
      store,
      binding,
      request,
      AbortSignal.timeout(5000),
    );
    if (phase === "after-intent") {
      process.send({ ready: true, executionId: e.execution_id });
      return;
    }
    request.query.xid = "MUTATED_AFTER_COMMIT";
    const response = await sendWithPermit(permit, AbortSignal.timeout(5000));
    let duplicateBlocked = false;
    try {
      await sendWithPermit(permit, AbortSignal.timeout(5000));
    } catch {
      duplicateBlocked = true;
    }
    process.send({
      ready: true,
      executionId: e.execution_id,
      response,
      duplicateBlocked,
    });
  } catch (error) {
    process.send({ error: String(error) });
  }
});
