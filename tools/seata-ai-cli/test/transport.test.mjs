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
import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { fork } from "node:child_process";
import { once } from "node:events";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomUUID } from "node:crypto";
import { requestOnce, readRequest } from "../dist/adapter/transport/http.js";
import { sendWithPermit } from "../dist/safety/send.js";
import { StateStore } from "../dist/adapter/statestore/client.js";

async function server(t, handler) {
  const s = http.createServer(handler);
  s.listen(0, "127.0.0.1");
  await once(s, "listening");
  t.after(() => {
    s.closeAllConnections();
    s.close();
  });
  return "http://127.0.0.1:" + s.address().port;
}
test("login/write redirects never reach a second receiver", async (t) => {
  let first = 0,
    second = 0;
  const target = await server(t, (_req, res) => {
    second++;
    res.end("{}");
  });
  const endpoint = await server(t, (req, res) => {
    first++;
    res.writeHead(Number(req.url.split("=").at(-1)), {
      location: target + "/api/v1/test",
    });
    res.end("{}");
  });
  for (const method of ["POST", "PUT"])
    for (const status of [301, 302, 303, 307, 308])
      await assert.rejects(
        requestOnce(
          {
            endpoint,
            method,
            suffix: "/api/v1/test",
            query: { status: String(status) },
            body: "{}",
          },
          AbortSignal.timeout(2000),
        ),
        (e) => e.subtype === "redirect_denied",
      );
  assert.equal(first, 10);
  assert.equal(second, 0);
});
test("read-only retries are bounded; large IDs decode without rounding", async (t) => {
  let count = 0;
  const endpoint = await server(t, (_req, res) => {
    count++;
    res.writeHead(count < 3 ? 503 : 200);
    res.end('{"branchId":9223372036854775807,"count":2}');
  });
  const response = await readRequest(
    { endpoint, method: "GET", suffix: "/api/v1/test" },
    AbortSignal.timeout(3000),
  );
  assert.equal(count, 3);
  assert.equal(response.body.branchId, "9223372036854775807");
  assert.equal(response.body.count, 2);
  await assert.rejects(
    readRequest(
      { endpoint, method: "PUT", suffix: "/api/v1/test" },
      AbortSignal.timeout(1000),
    ),
  );
  assert.equal(count, 3);
});
test("aborts destroy request, malformed/oversized bodies reject, forged permits reject", async (t) => {
  let count = 0;
  const endpoint = await server(t, (req, res) => {
    count++;
    if (req.url.includes("hang")) return;
    if (req.url.includes("large"))
      res.end('"' + "x".repeat(4 * 1024 * 1024) + '"');
    else res.end('{"a":1,"a":2}');
  });
  for (const suffix of ["hang", "large", "duplicate"])
    await assert.rejects(
      requestOnce(
        { endpoint, method: "GET", suffix: "/api/v1/" + suffix },
        AbortSignal.timeout(200),
      ),
    );
  await assert.rejects(
    sendWithPermit({}, AbortSignal.timeout(200)),
    (e) => e.subtype === "send_permit_invalid",
  );
  assert.equal(count, 3);
});
for (const phase of ["before-intent", "after-intent", "after-response"])
  test(
    "SIGKILL recovery at " + phase + " never authorizes replay",
    async (t) => {
      let count = 0,
        url;
      const endpoint = await server(t, (req, res) => {
        count++;
        url = req.url;
        res.end('{"accepted":true}');
      });
      const directory = await mkdtemp(join(tmpdir(), "seata-r11-crash-")),
        requestId = randomUUID();
      const child = fork(
        new URL("./helpers/crash-child.mjs", import.meta.url),
        [],
        { stdio: ["ignore", "pipe", "pipe", "ipc"], execArgv: [] },
      );
      t.after(() => child.kill("SIGKILL"));
      let stderr = "";
      child.stderr.on("data", (b) => {
        stderr += b;
      });
      const ready = once(child, "message", {
        signal: AbortSignal.timeout(10000),
      });
      child.send({ directory, endpoint, phase, requestId });
      const [message] = await ready;
      assert.equal(message.ready, true, message.error ?? stderr);
      const observer = new StateStore(directory);
      t.after(() => observer.close());
      await assert.rejects(
        observer.call("execution.recover", { client_request_id: requestId }),
        (e) => e.subtype === "store_busy",
      );
      const dead = once(child, "exit");
      child.kill("SIGKILL");
      await dead;
      const recovered = await observer.call("execution.recover", {
        client_request_id: requestId,
      });
      assert.equal(
        recovered.execution.outcome,
        phase === "before-intent" ? "not_started" : "unknown",
      );
      assert.equal(recovered.send_permit, false);
      for (let i = 0; i < 3; i++)
        await observer.call("execution.get", { client_request_id: requestId });
      assert.equal(count, phase === "after-response" ? 1 : 0);
      if (phase === "after-response") {
        assert.equal(message.duplicateBlocked, true);
        assert.equal(url, "/api/v1/test/write?xid=x");
      }
      assert.equal(stderr, "");
    },
  );
