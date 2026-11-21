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
import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { once } from "node:events";
import { randomBytes } from "node:crypto";
import { mkdtemp, readFile } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig } from "../dist/app/profile.js";
import { startBridge } from "../dist/adapter/auth/bridge.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";

// An explicit external installation keeps browser tooling out of the CLI package.
// Install playwright@1.58.2 in a separate test-tools directory, then run its
// `playwright install chromium --only-shell`. Set PLAYWRIGHT_BROWSERS_PATH for
// both installation and execution when using a private browser cache.
// SEATA_TEST_PLAYWRIGHT=/absolute/test-tools/node_modules/playwright/index.mjs npm run test:browser
assert.ok(process.env.SEATA_TEST_PLAYWRIGHT, "Set SEATA_TEST_PLAYWRIGHT to playwright/index.mjs");
const { chromium } = await import(pathToFileURL(process.env.SEATA_TEST_PLAYWRIGHT).href);

test("Chromium form submission authenticates with real CSP, Origin and session cookies", { timeout: 30000 }, async t => {
  let store, bridge, browser;
  const token = "Bearer SYNTHETIC_BROWSER_TOKEN", password = "SYNTHETIC_BROWSER_PASSWORD", received = [];
  const consoleServer = http.createServer(async (req, res) => {
    let body = "";
    for await (const chunk of req) body += chunk;
    received.push({ method: req.method, path: req.url, body: JSON.parse(body) });
    res.writeHead(200, { authorization: token });
    res.end(JSON.stringify({ code: "200", data: token }));
  });
  t.after(async () => {
    await browser?.close();
    await bridge?.close();
    await store?.close();
    await new Promise(resolve => consoleServer.close(resolve));
  });
  consoleServer.listen(0, "127.0.0.1"); await once(consoleServer, "listening");
  const root = await mkdtemp(join(tmpdir(), "seata-browser-login-"));
  store = new StateStore(root);
  const endpoint = `http://127.0.0.1:${consoleServer.address().port}/trusted-prefix`;
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  const capability = randomBytes(32).toString("base64url");
  bridge = await startBridge(root, flow.flow_id, flow.helper_instance_id, capability);
  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const url = `http://127.0.0.1:${bridge.port}/login/${capability}`;
  assert.equal((await page.goto(url)).status(), 200);
  assert.equal(received.length, 0);
  await page.locator('input[name="username"]').fill("browser-user");
  await page.locator('input[name="password"]').fill(password);
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url() === `${url}/submit` && r.request().method() === "POST"),
    page.getByRole("button", { name: "Sign in" }).click()
  ]);
  const headers = await response.request().allHeaders();
  assert.equal(headers.origin, new URL(url).origin, "Browser must preserve the exact form Origin");
  assert.ok(!String(headers.referer ?? "").includes(capability), "Referrer must not expose the loopback capability");
  assert.equal(response.status(), 200, await response.text());
  assert.deepEqual(received, [{ method: "POST", path: "/trusted-prefix/api/v1/auth/login", body: { username: "browser-user", password } }]);
  assert.equal((await store.call("flow.status", { name: "p", flow_id: flow.flow_id })).flow_state, "authenticated");
  const binding = await store.call("auth.binding", { name: "p" });
  assert.equal(await new CredentialStore(root, "protected_file", binding.state_store_id)
    .get(binding.credential.credential_ref, AbortSignal.timeout(1000)), token);
  const body = await response.text();
  assert.ok(!body.includes(token) && !body.includes(password));
  for (const name of ["state.db", "state.db-wal"]) {
    const bytes = await readFile(join(root, name));
    for (const secret of [token, password, capability]) assert.equal(bytes.includes(Buffer.from(secret)), false);
  }
  t.diagnostic(`Chromium ${browser.version()}; isolated loopback Console fixture`);
});
