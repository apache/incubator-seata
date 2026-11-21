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
import { CliError } from "../dist/protocol/error.js";
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
test("read retries record bounded times and a redacted selector for every HTTP attempt", async t => {
  let attempts = 0;
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    res.writeHead(attempts === 1 ? 503 : 200);
    res.end("{}");
  });
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test",
    query: { resourceId: "jdbc:mysql://user:SELECTOR_SECRET@host/orders" },
    headers: { authorization: "Bearer AUTH_SECRET" } }, AbortSignal.timeout(3000));
  assert.equal(attempts, 2);
  assert.deepEqual(result.readAttempts.map(({ attempt, path, outcome, http_status }) =>
    ({ attempt, path, outcome, http_status })), [
    { attempt: 1, path: "/api/v1/test", outcome: "http_response", http_status: 503 },
    { attempt: 2, path: "/api/v1/test", outcome: "http_response", http_status: 200 },
  ]);
  assert.match(result.readAttempts[0].selector_digest, /^sha256:[a-f0-9]{64}$/);
  assert.equal(result.readAttempts[0].selector_digest, result.readAttempts[1].selector_digest);
  for (const item of result.readAttempts) {
    assert.ok(Number.isFinite(Date.parse(item.started_at)));
    assert.ok(Date.parse(item.observed_at) >= Date.parse(item.started_at));
  }
  assert.doesNotMatch(JSON.stringify(result.readAttempts), /SELECTOR_SECRET|AUTH_SECRET|jdbc:mysql/);
});
test("rejected HTTP responses retain status metadata without body leakage or extra retries", async t => {
  for (const [kind, status, subtype] of [["json", 200, "protocol"], ["large", 200, "response_limit"],
    ["redirect", 302, "redirect_denied"], ["encoding", 200, "protocol"], ["upgrade", 101, "protocol"]]) {
    let requests = 0;
    const endpoint = await server(t, (_req, res) => {
      requests++;
      res.writeHead(kind === "json" && requests === 1 ? 503 : status, kind === "redirect" ? { location: "http://127.0.0.1:9/SECRET" }
        : kind === "encoding" ? { "content-encoding": "gzip" }
          : kind === "upgrade" ? { connection: "Upgrade", upgrade: "test" } : {});
      res.end(kind === "large" ? "x".repeat(4 * 1024 * 1024 + 1) : "SECRET invalid body");
    });
    const attempts = [];
    await assert.rejects(readRequest({ endpoint, method: "GET", suffix: "/api/v1/test",
      headers: { authorization: "Bearer SECRET" } }, AbortSignal.timeout(3000), undefined, entry => attempts.push(entry)),
      { subtype });
    assert.equal(requests, kind === "json" ? 2 : 1, kind);
    assert.deepEqual(attempts.map(a => [a.attempt, a.outcome, a.http_status]), kind === "json"
      ? [[1, "http_response", 503], [2, "http_response", 200]] : [[1, "http_response", status]], kind);
    assert.doesNotMatch(JSON.stringify(attempts), /SECRET|invalid body|location|authorization/);
  }
});

test("out-of-range HTTP status remains a failed status without invalid attempt metadata", async t => {
  let requests = 0;
  const endpoint = await server(t, (_req, res) => { requests++; res.writeHead(600); res.end("SECRET"); });
  const observed = [];
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" },
    AbortSignal.timeout(3000), undefined, item => observed.push(item));
  assert.equal(result.status, 600);
  assert.equal(requests, 1);
  assert.equal(result.readAttempts[0].outcome, "http_response");
  assert.equal(result.readAttempts[0].http_status, null);
  assert.deepEqual(observed, result.readAttempts);
  assert.doesNotMatch(JSON.stringify(observed), /SECRET/);
});

test("a transient connection reset is recorded before a successful read retry", async t => {
  let attempts = 0;
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    if (attempts === 1) { res.destroy(); return; }
    res.end("{}");
  });
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, AbortSignal.timeout(3000));
  assert.equal(result.status, 200);
  assert.equal(attempts, 2);
  assert.deepEqual(result.readAttempts.map(item => [item.outcome, item.http_status]),
    [["connection_error", null], ["http_response", 200]]);
});
test("a read retries a reset after response headers without accepting the partial body", async t => {
  let attempts = 0;
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    if (attempts === 1) {
      res.writeHead(200, { "content-length": "20" });
      res.write('{"incomplete":');
      setImmediate(() => res.destroy());
      return;
    }
    res.end('{"complete":true}');
  });
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, AbortSignal.timeout(3000));
  assert.equal(attempts, 2);
  assert.equal(result.body.complete, true);
  assert.deepEqual(result.readAttempts.map(item => [item.outcome, item.http_status]),
    [["connection_error", null], ["http_response", 200]]);
});
test("a read retry sends the same pinned selector and headers it records", async t => {
  const seen = [];
  const endpoint = await server(t, (req, res) => {
    seen.push({ query: new URL(req.url, endpoint).searchParams.get("q"), authorization: req.headers.authorization });
    res.writeHead(seen.length === 1 ? 503 : 200);
    res.end("{}");
  });
  const query = { q: "original" }, headers = { authorization: "Bearer ORIGINAL_TEST" };
  let checked = 0;
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test", query, headers },
    AbortSignal.timeout(3000), async () => {
      if (++checked === 2) { query.q = "changed"; headers.authorization = "Bearer CHANGED_TEST"; }
    });
  assert.equal(result.status, 200);
  assert.deepEqual(seen, [
    { query: "original", authorization: "Bearer ORIGINAL_TEST" },
    { query: "original", authorization: "Bearer ORIGINAL_TEST" },
  ]);
  assert.equal(result.readAttempts[0].selector_digest, result.readAttempts[1].selector_digest);
  assert.doesNotMatch(JSON.stringify(result.readAttempts), /CHANGED_TEST|changed/);
});
test("retryable reads wait for a bounded numeric Retry-After before retrying", async (t) => {
  let attempts = 0;
  let firstAt = 0;
  let secondAt = 0;
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    if (attempts === 1) {
      firstAt = performance.now();
      res.writeHead(503, { "retry-after": "3600" });
      res.end();
    } else {
      secondAt = performance.now();
      res.writeHead(200);
      res.end("{}");
    }
  });
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, AbortSignal.timeout(4000));
  assert.equal(result.status, 200);
  assert.equal(attempts, 2);
  assert.ok(secondAt - firstAt >= 850, `Retried too early: ${secondAt - firstAt} ms`);
  assert.ok(secondAt - firstAt < 2500, `Retry-After was not capped: ${secondAt - firstAt} ms`);
});
test("retryable reads also honor a bounded HTTP-date Retry-After", async (t) => {
  let attempts = 0;
  let firstAt = 0;
  let secondAt = 0;
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    if (attempts === 1) {
      firstAt = performance.now();
      res.writeHead(503, { "retry-after": new Date(Date.now() + 3600000).toUTCString() });
      res.end();
    } else {
      secondAt = performance.now();
      res.writeHead(200);
      res.end("{}");
    }
  });
  const result = await readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, AbortSignal.timeout(4000));
  assert.equal(result.status, 200);
  assert.equal(attempts, 2);
  assert.ok(secondAt - firstAt >= 850, `Retried too early: ${secondAt - firstAt} ms`);
  assert.ok(secondAt - firstAt < 2500, `Retry-After was not capped: ${secondAt - firstAt} ms`);
});
test("a read deadline preserves the invocation timeout instead of generic cancellation", async (t) => {
  let attempts = 0;
  let observedFirst;
  const first = new Promise(resolve => observedFirst = resolve);
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    res.writeHead(503);
    res.end();
    observedFirst();
  });
  const abort = new AbortController();
  const pending = readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, abort.signal);
  await first;
  abort.abort(new CliError("network", "timeout", "Invocation deadline exceeded."));
  await assert.rejects(pending, error => error.type === "network" && error.subtype === "timeout");
  assert.equal(attempts, 1);
});
test("a read deadline during retry backoff keeps the original timeout", { timeout: 5000 }, async (t) => {
  let attempts = 0;
  let firstClosed;
  const closed = new Promise(resolve => firstClosed = resolve);
  const endpoint = await server(t, (_req, res) => {
    attempts++;
    if (attempts === 1) res.socket.once("close", firstClosed);
    res.writeHead(503);
    res.end();
  });
  const abort = new AbortController();
  const pending = readRequest({ endpoint, method: "GET", suffix: "/api/v1/test" }, abort.signal);
  await closed;
  await new Promise(resolve => setTimeout(resolve, 50));
  abort.abort(new CliError("network", "timeout", "Invocation deadline exceeded."));
  await assert.rejects(pending, error => error.type === "network" && error.subtype === "timeout");
  assert.equal(attempts, 1);
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
