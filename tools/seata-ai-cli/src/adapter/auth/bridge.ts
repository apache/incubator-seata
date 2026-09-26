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
import http, { type IncomingMessage, type ServerResponse } from "node:http";
import { randomBytes, timingSafeEqual } from "node:crypto";
import { once } from "node:events";
import { StateStore } from "../statestore/client.js";
import { CredentialStore } from "./credentials.js";
import { cleanupCredentials } from "./cleanup.js";
import { login } from "./login.js";
import { object, str, type Obj } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";
import { endpoint } from "../../app/profile.js";
import { adapterId } from "../management/read.js";
import { PrivateDirectory } from "../../platform/linux.js";
import { closeSync } from "node:fs";
import { loadCa } from "../transport/ca.js";
import { profileProxy } from "../transport/proxy.js";

const random = () => randomBytes(32).toString("base64url");
const escape = (text: string) => text.replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
const equal = (a: string, b: string) => Buffer.byteLength(a) === Buffer.byteLength(b) && timingSafeEqual(Buffer.from(a), Buffer.from(b));

export function loginTarget(profile: Obj): string {
  const management = object(profile.management);
  if (object(profile.trust).state !== "accepted") throw new CliError("policy", "trust_required", "Accept the complete target before login.");
  if (management.compatibility_id !== adapterId) throw new CliError("policy", "compatibility_unverified", "No login adapter is enabled for this compatibility ID.");
  profileProxy(management);
  return endpoint(str(management.console_endpoint), str(profile.environment), true);
}

/** Private helper server. No password or URL capability is persisted. */
export async function startBridge(root: string, flowId: string, helperId: string, token: string): Promise<{ port: number; close: () => Promise<void> }> {
  if (!/^[A-Za-z0-9_-]{43}$/.test(token)) throw new CliError("internal", "contract_violation", "Invalid private login capability.");
  const store = new StateStore(root), abort = new AbortController();
  let heartbeat: NodeJS.Timeout | undefined, expiry: NodeJS.Timeout | undefined, stopping: Promise<void> | undefined;
  let server: http.Server | undefined;
  let directory: PrivateDirectory | undefined, lifetime: number | undefined;
  const pending = new Set<Promise<void>>();
  const sessions = new Map<string, string>();
  const close = (): Promise<void> => stopping ??= (async () => {
    clearTimeout(heartbeat); clearTimeout(expiry); abort.abort(); sessions.clear();
    if (server?.listening) {
      server.closeAllConnections();
      await new Promise<void>(resolve => server!.close(() => resolve()));
    }
    await Promise.allSettled([...pending]);
    if (lifetime !== undefined) { closeSync(lifetime); lifetime = undefined; }
    directory?.close(); directory = undefined;
    await cleanupCredentials(store, AbortSignal.timeout(5000), root);
    await store.close();
  })();
  try {
    const binding = await store.call("flow.helper", { flow_id: flowId, helper_instance_id: helperId });
    directory = new PrivateDirectory(root, false);
    lifetime = directory.lock("auth-flow-" + flowId + ".lock");
    const profile = object(binding.profile), flow = object(binding.flow), consoleEndpoint = loginTarget(profile);
    const caReference = str(object(profile.management).ca_ref), ca = loadCa(caReference, abort.signal);
    const proxy = profileProxy(object(profile.management));
    const policy = str(object(profile.policy).storage_policy);
    const secrets = new CredentialStore(root, policy, str(binding.state_store_id));
    const path = "/login/" + token;
    let origin = "", host = "", submitting = false, window = Date.now(), requests = 0;
    const respond = (res: ServerResponse, code: number, html: string, cookie?: string) => {
      res.writeHead(code, { "content-type": "text/html; charset=utf-8", "cache-control": "no-store",
        "referrer-policy": "strict-origin", "x-content-type-options": "nosniff",
        "content-security-policy": "default-src 'none'; style-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'",
        ...(cookie ? { "set-cookie": cookie } : {}) });
      res.end(html);
    };
    const terminal = (res: ServerResponse, code: number, text: string) => {
      res.once("finish", () => { void close(); });
      respond(res, code, text);
    };
    const failFlow = async (reason: string) => {
      try { await store.call("flow.fail", { flow_id: flowId, helper_instance_id: helperId, reason }); }
      catch { /* Heartbeat recovery also fails this helper closed. */ }
      await cleanupCredentials(store, AbortSignal.timeout(5000), root);
    };
    const handle = async (req: IncomingMessage, res: ServerResponse): Promise<void> => {
      req.setTimeout(5000, () => req.destroy());
      if (Date.now() - window > 10000) { window = Date.now(); requests = 0; }
      if (++requests > 30) { respond(res, 429, "Request limit reached."); return; }
      const sensitiveHeaders = new Set(["host", "origin", "cookie", "content-type", "sec-fetch-site"]);
      const seen = new Set<string>();
      for (let i = 0; i < req.rawHeaders.length; i += 2) {
        const header = req.rawHeaders[i]!.toLowerCase();
        if (sensitiveHeaders.has(header) && seen.has(header)) { respond(res, 400, "Invalid request."); return; }
        seen.add(header);
      }
      if (req.headers.host !== host || ![path, path + "/submit"].includes(req.url ?? "")) { respond(res, 403, "Invalid local login request."); return; }
      if (submitting) { respond(res, 409, "Login is already being processed."); return; }
      const heartbeatResult = await store.call("flow.heartbeat", { flow_id: flowId, helper_instance_id: helperId }, abort.signal);
      if (!heartbeatResult.active) { terminal(res, 410, "Login flow has ended."); return; }
      if (req.method === "GET" && req.url === path) {
        if (sessions.size >= 4) { respond(res, 429, "Too many login pages. Cancel this flow to start again."); return; }
        const session = random(), csrf = random(); sessions.set(session, csrf);
        const html = '<!doctype html><html lang="en"><meta charset="utf-8"><title>Seata login</title><body><h1>Seata Console login</h1>' +
          '<p>Profile: ' + escape(str(profile.name)) + '</p><p>Environment: ' + escape(str(profile.environment)) + '</p>' +
          '<p>Console: ' + escape(consoleEndpoint) + '</p><p>Credential storage: ' + escape(secrets.assurance) + '</p>' +
          '<p>Proxy: ' + escape(proxy ?? 'direct') + '</p>' +
          '<form method="post" action="' + path + '/submit"><input type="hidden" name="csrf" value="' + csrf + '">' +
          '<label>Username <input name="username" autocomplete="username" required maxlength="4096"></label>' +
          '<label>Password <input name="password" type="password" autocomplete="current-password" required maxlength="4096"></label>' +
          '<button type="submit">Sign in</button></form></body></html>';
        respond(res, 200, html, 'seata_login=' + session + '; HttpOnly; SameSite=Strict; Path=' + path);
        return;
      }
      if (req.method !== "POST" || req.url !== path + "/submit") { respond(res, 405, "Method not allowed."); return; }
      if (req.headers.origin !== origin || (req.headers["sec-fetch-site"] !== undefined && req.headers["sec-fetch-site"] !== "same-origin") ||
        req.headers["content-type"]?.split(";")[0]?.trim() !== "application/x-www-form-urlencoded") {
        respond(res, 403, "Invalid login origin or form."); return;
      }
      const cookies = (req.headers.cookie ?? "").split(";").map(s => s.trim()).filter(s => s.startsWith("seata_login="));
      const session = cookies.length === 1 ? cookies[0]!.slice("seata_login=".length) : "";
      const csrf = sessions.get(session);
      if (!csrf) { respond(res, 403, "Invalid login session."); return; }
      const chunks: Buffer[] = []; let size = 0;
      for await (const chunk of req) {
        size += chunk.length;
        if (size > 16384) { respond(res, 413, "Login form is too large."); req.resume(); return; }
        chunks.push(Buffer.from(chunk));
      }
      let fields: URLSearchParams;
      try { fields = new URLSearchParams(new TextDecoder("utf-8", { fatal: true }).decode(Buffer.concat(chunks))); }
      catch { respond(res, 400, "Invalid form encoding."); return; }
      for (const key of ["csrf", "username", "password"]) if (fields.getAll(key).length !== 1) { respond(res, 400, "Invalid login fields."); return; }
      if ([...fields.keys()].some(k => !["csrf", "username", "password"].includes(k)) || !equal(fields.get("csrf")!, csrf)) {
        respond(res, 403, "Invalid login form."); return;
      }
      if (!fields.get("username") || !fields.get("password") || fields.get("username")!.length > 4096 || fields.get("password")!.length > 4096) {
        respond(res, 400, "Invalid login fields."); return;
      }
      // Consume in memory before the first await; SQLite CAS is the authority
      // across helpers, cancel/logout and concurrent invocations.
      if (submitting) { respond(res, 409, "Login is already being processed."); return; }
      submitting = true; sessions.clear();
      try {
        await store.call("flow.submit", { flow_id: flowId }, abort.signal);
        if (loadCa(caReference, abort.signal) !== ca)
          throw new CliError("policy", "ca_changed", "CA content changed during login; no password request was sent.");
        const credential = await login(consoleEndpoint, fields.get("username")!, fields.get("password")!, abort.signal, ca, proxy);
        fields.delete("username"); fields.delete("password");
        const stage = await store.call("flow.stage", { flow_id: flowId, provider: policy === "protected_file" ? "protected_file" : "keyring" }, abort.signal);
        await secrets.put(str(stage.credential_ref), credential, abort.signal);
        await store.call("flow.activate", { flow_id: flowId, credential_ref: stage.credential_ref ?? null }, abort.signal);
        await cleanupCredentials(store, AbortSignal.timeout(5000), root);
        terminal(res, 200, "Login completed. You may close this window.");
      } catch (error) {
        fields.delete("username"); fields.delete("password");
        const reason = error instanceof CliError && ["login_rejected", "invalid_credential", "credential_store_unavailable", "ca_changed", "unsafe_ca_file"].includes(error.subtype)
          ? error.subtype : "login_transport_failed";
        await failFlow(reason);
        terminal(res, 400, "Login was not completed. Check the CLI flow status and start a new login if needed.");
      }
    };
    server = http.createServer({ maxHeaderSize: 16384, headersTimeout: 5000, requestTimeout: 5000, keepAliveTimeout: 1000 }, (req, res) => {
      const work = handle(req, res).catch(() => { if (!res.headersSent) respond(res, 400, "Local login request failed."); else res.destroy(); });
      pending.add(work); void work.finally(() => pending.delete(work));
    });
    server.on("clientError", (_error, socket) => { socket.end("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n"); });
    server.listen(0, "127.0.0.1"); await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("listener unavailable");
    host = "127.0.0.1:" + address.port; origin = "http://" + host;
    const beat = async () => {
      try {
        const state = await store.call("flow.heartbeat", { flow_id: flowId, helper_instance_id: helperId }, abort.signal);
        if (!state.active) { await close(); return; }
        heartbeat = setTimeout(() => { void beat(); }, 2000);
      } catch { await close(); }
    };
    heartbeat = setTimeout(() => { void beat(); }, 2000);
    expiry = setTimeout(() => { void close(); }, Math.max(1, Date.parse(str(flow.expires_at)) - Date.now()));
    return { port: address.port, close };
  } catch (error) { await close(); throw error; }
}
