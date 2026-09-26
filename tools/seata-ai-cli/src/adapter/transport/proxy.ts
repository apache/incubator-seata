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
import http from "node:http";
import https from "node:https";
import tls from "node:tls";
import { isIP } from "node:net";
import type { Duplex } from "node:stream";
import { CliError } from "../../protocol/error.js";
import { type Obj, str } from "../../protocol/value.js";

export const transientConnectCodes = new Set(["ECONNRESET", "ECONNREFUSED", "ETIMEDOUT", "EAI_AGAIN", "EPIPE"]);

export function profileProxy(management: Obj): string | undefined {
  if (management.proxy_mode === "direct" && management.proxy_endpoint === undefined) return undefined;
  if (management.proxy_mode === "explicit") {
    const proxy = proxyUrl(str(management.proxy_endpoint));
    for (const name of ["console_endpoint", "naming_endpoint"])
      if (typeof management[name] === "string") validateProxyRoute(proxy, new URL(management[name]));
    return proxy.href.replace(/\/$/, "");
  }
  throw new CliError("policy", "transport_policy_unsupported", "Use direct transport or an explicitly trusted HTTP(S) proxy.");
}
export function validateProxyRoute(proxy: URL, target: URL): void {
  if (target.protocol === "http:" && proxy.protocol === "http:" && !["localhost", "127.0.0.1", "[::1]"].includes(proxy.hostname))
    throw new CliError("policy", "trust_required", "Plain HTTP management traffic cannot be sent through a non-loopback plaintext proxy; use HTTPS.");
}

export function proxyUrl(raw: string): URL {
  try {
    const url = new URL(raw);
    if (!raw || /[\x00-\x20\x7f\\?#%]/.test(raw) || /(?:^|\/)\.{1,2}(?:\/|$)/.test(raw) || !["http:", "https:"].includes(url.protocol) ||
        url.username || url.password || url.pathname !== "/" || url.search || url.hash) throw new Error();
    return url;
  } catch { throw new CliError("validation", "invalid_proxy_endpoint", "Proxy endpoint must be an HTTP(S) origin without credentials, path, query or fragment."); }
}
export function privateAgent(url: URL, ca?: string): http.Agent {
  const hostname = url.hostname.replace(/^\[|\]$/g, "");
  return url.protocol === "https:" ? new https.Agent({ keepAlive: false, maxSockets: 1,
    servername: isIP(hostname) ? "" : hostname,
    checkServerIdentity: (_name, cert) => tls.checkServerIdentity(hostname, cert),
    ALPNProtocols: ["http/1.1"], rejectUnauthorized: true, ...(ca === undefined ? {} : { ca }) }) :
    new http.Agent({ keepAlive: false, maxSockets: 1 });
}

/** CONNECT carries only authority metadata. Target headers/password bytes are
 * not serialized until an independently authenticated target TLS session exists. */
export async function tlsTunnel(proxy: URL, target: URL, ca: string | undefined, signal: AbortSignal): Promise<{ socket: tls.TLSSocket; close: () => void }> {
  const agent = privateAgent(proxy, ca);
  let transport: Duplex | undefined, socket: tls.TLSSocket | undefined;
  const abort = () => { socket?.destroy(); transport?.destroy(); };
  const close = () => { signal.removeEventListener("abort", abort); abort(); agent.destroy(); };
  signal.addEventListener("abort", abort, { once: true });
  try {
    signal.throwIfAborted();
    transport = await new Promise<Duplex>((resolve, reject) => {
      const send = proxy.protocol === "https:" ? https.request : http.request;
      const authority = target.hostname + ":" + (target.port || "443");
      const request = send(proxy, { method: "CONNECT", path: authority, agent, signal, maxHeaderSize: 16384,
        headers: { host: authority, connection: "keep-alive" } });
      request.once("error", reject);
      request.once("response", response => { response.destroy(); reject(new CliError("network", "proxy_rejected", "Proxy did not establish the requested tunnel.")); });
      request.once("connect", (response, connection, head) => {
        if (signal.aborted || response.statusCode !== 200 || head.length !== 0) {
          connection.destroy(); reject(new CliError("network", "proxy_rejected", "Proxy did not establish a clean tunnel; no target request was sent."));
        } else resolve(connection);
      });
      request.end();
    });
    signal.throwIfAborted();
    const hostname = target.hostname.replace(/^\[|\]$/g, "");
    socket = tls.connect({ socket: transport, rejectUnauthorized: true, ALPNProtocols: ["http/1.1"],
      ...(isIP(hostname) ? {} : { servername: hostname }), checkServerIdentity: (_name, cert) => tls.checkServerIdentity(hostname, cert),
      ...(ca === undefined ? {} : { ca }) });
    await new Promise<void>((resolve, reject) => {
      socket!.once("secureConnect", () => resolve());
      socket!.once("error", reject);
      socket!.once("close", () => reject(new CliError("network", "connection", "Target TLS connection closed before authentication.")));
    });
    signal.throwIfAborted();
    return { socket, close };
  } catch (error) {
    close();
    if (signal.aborted) throw signal.reason instanceof CliError ? signal.reason :
      new CliError("cancelled", "context_cancelled", "Proxy connection was cancelled.");
    if (error instanceof CliError) throw error;
    throw transientConnectCodes.has((error as NodeJS.ErrnoException)?.code ?? "")
      ? new CliError("network", "connection", "Proxy or target TLS connection failed.")
      : new CliError("network", "transport_failure", "Proxy or target TLS transport failed without a retryable connection error.");
  }
}
