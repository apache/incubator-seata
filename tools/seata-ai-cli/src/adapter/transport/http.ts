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
import http from "node:http";
import https from "node:https";
import { setTimeout as delay } from "node:timers/promises";
import { type Json, type Obj, parseJson, digest } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";
import { privateAgent, proxyUrl, tlsTunnel, transientConnectCodes, validateProxyRoute } from "./proxy.js";

export interface HttpRequest {
  endpoint: string;
  suffix: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  query?: Readonly<Record<string, string>>;
  headers?: Readonly<Record<string, string>>;
  body?: string;
  ca?: string;
  proxyEndpoint?: string;
}
export interface HttpResponse {
  status: number;
  body: Json;
  authorization?: string;
  retryAfter?: string;
  readAttempts?: ReadAttempt[];
}
export interface ReadAttempt extends Obj {
  attempt: number;
  path: string;
  selector_digest: string;
  started_at: string;
  observed_at: string;
  outcome: "http_response" | "connection_error";
  http_status: number | null;
}
function urlFor(request: HttpRequest): URL {
  const url = new URL(request.endpoint);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    /[\\?#]|%(?:2f|5c|2e|25)/i.test(request.endpoint) ||
    /(?:^|\/)\.{1,2}(?:\/|$)/.test(request.endpoint)
  )
    throw new CliError(
      "policy",
      "trust_required",
      "Invalid trusted base endpoint.",
    );
  if (!/^\/api\/v1\/[A-Za-z0-9/_-]+$/.test(request.suffix))
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid pinned API suffix.",
    );
  url.pathname = url.pathname.replace(/\/$/, "") + request.suffix;
  for (const [key, value] of Object.entries(request.query ?? {}))
    url.searchParams.set(key, value);
  return url;
}
/** One HTTP/1.1 attempt on a fresh private Agent, with no redirect/replay middleware. */
export async function requestOnce(
  request: HttpRequest,
  signal: AbortSignal,
  onResponseStatus?: (status: number) => void,
): Promise<HttpResponse> {
  signal.throwIfAborted();
  const url = urlFor(request), secure = url.protocol === "https:",
    proxy = request.proxyEndpoint === undefined ? undefined : proxyUrl(request.proxyEndpoint);
  if (proxy) validateProxyRoute(proxy, url);
  const destination = proxy && !secure ? proxy : url;
  const agent = privateAgent(destination, request.ca);
  let tunnel: Awaited<ReturnType<typeof tlsTunnel>> | undefined;
  try {
    if (proxy && secure) {
      tunnel = await tlsTunnel(proxy, url, request.ca, signal);
      const socket = tunnel.socket;
      agent.createConnection = () => socket;
    }
    signal.throwIfAborted();
    return await new Promise<HttpResponse>((resolve, reject) => {
      const send = destination.protocol === "https:" ? https.request : http.request;
      const req = send(
        destination,
        {
          method: request.method,
          agent,
          signal,
          maxHeaderSize: 16384,
          ...(proxy && !secure ? { path: url.href } : {}),
          headers: {
            ...request.headers,
            host: url.host,
            "accept-encoding": "identity",
            connection: "close",
            ...(request.body === undefined
              ? {}
              : {
                  "content-type": "application/json",
                  "content-length": Buffer.byteLength(request.body),
                }),
          },
        },
        (res) => {
          if (res.statusCode !== undefined) onResponseStatus?.(res.statusCode);
          const fail = (error: CliError) => {
            res.destroy();
            reject(error);
          };
          // Error status is authoritative even when an ingress returns HTML
          // or an empty body. Never echo or parse an untrusted error page.
          if ((res.statusCode ?? 0) >= 400) {
            const status = res.statusCode ?? 0;
            const retryAfter = res.headersDistinct["retry-after"];
            res.destroy();
            resolve({ status, body: null,
              ...(retryAfter?.length === 1 && retryAfter[0]!.length <= 128 ? { retryAfter: retryAfter[0] } : {}) });
            return;
          }
          if ((res.statusCode ?? 0) >= 300 && (res.statusCode ?? 0) < 400) {
            fail(
              new CliError(
                "network",
                "redirect_denied",
                "Management redirects are not followed.",
              ),
            );
            return;
          }
          if (
            res.headers["content-encoding"] &&
            res.headers["content-encoding"] !== "identity"
          ) {
            fail(
              new CliError(
                "network",
                "protocol",
                "Compressed response was not negotiated.",
              ),
            );
            return;
          }
          const chunks: Buffer[] = [];
          let size = 0;
          res.on("data", (chunk: Buffer) => {
            size += chunk.length;
            if (size > 4 * 1024 * 1024)
              fail(
                new CliError(
                  "network",
                  "response_limit",
                  "Response exceeds 4 MiB.",
                ),
              );
            else chunks.push(chunk);
          });
          res.once("error", (error: NodeJS.ErrnoException) =>
            reject(signal.aborted
              ? signal.reason instanceof CliError ? signal.reason :
                new CliError("cancelled", "context_cancelled", "HTTP request was cancelled.")
              : transientConnectCodes.has(error.code ?? "")
                ? new CliError("network", "connection", "HTTP response connection reset.")
                : new CliError("network", "protocol", "Response was interrupted.")),
          );
          res.once("end", () => {
            if (!res.complete) {
              reject(
                new CliError(
                  "network",
                  "protocol",
                  "Incomplete HTTP response.",
                ),
              );
              return;
            }
            try {
              resolve({
                status: res.statusCode ?? 0,
                body: parseJson(Buffer.concat(chunks), true, 4 * 1024 * 1024),
                ...(request.suffix === "/api/v1/auth/login" && typeof res.headers.authorization === "string" && res.headersDistinct.authorization?.length === 1
                  ? { authorization: res.headers.authorization } : {}),
              });
            } catch {
              reject(
                new CliError(
                  "network",
                  "protocol",
                  "Response is not valid bounded JSON.",
                ),
              );
            }
          });
        },
      );
      req.once("error", (error: NodeJS.ErrnoException) =>
        reject(
          signal.aborted
            ? signal.reason instanceof CliError
              ? signal.reason
              : new CliError(
                  "cancelled",
                  "context_cancelled",
                  "HTTP request was cancelled.",
                )
            : transientConnectCodes.has(error.code ?? "")
              ? new CliError("network", "connection", "HTTP connection failed.")
              : new CliError("network", "transport_failure", "HTTP transport failed without a retryable connection error."),
        ),
      );
      req.once("upgrade", (res, socket) => {
        if (res.statusCode !== undefined) onResponseStatus?.(res.statusCode);
        socket.destroy();
        reject(
          new CliError("network", "protocol", "HTTP upgrade is forbidden."),
        );
      });
      req.end(request.body);
    });
  } finally {
    agent.destroy();
    tunnel?.close();
  }
}
export async function readRequest(
  request: HttpRequest,
  signal: AbortSignal,
  beforeAttempt?: () => Promise<void>,
  onAttempt?: (attempt: ReadAttempt) => void,
): Promise<HttpResponse> {
  if (request.method !== "GET")
    throw new CliError(
      "internal",
      "contract_violation",
      "Read transport accepts only pinned read requests.",
    );
  const pinnedRequest: HttpRequest = { ...request,
    ...(request.query ? { query: { ...request.query } } : {}),
    ...(request.headers ? { headers: { ...request.headers } } : {}) };
  const selector_digest = digest({ endpoint: pinnedRequest.endpoint, path: pinnedRequest.suffix, query: pinnedRequest.query ?? {},
    namespace: pinnedRequest.headers?.["x-seata-namespace"] ?? null, cluster: pinnedRequest.headers?.["x-seata-cluster"] ?? null });
  const readAttempts: ReadAttempt[] = [];
  const recordAttempt = (entry: ReadAttempt) => {
    // Preserve the raw status for adapter error classification, but never
    // emit a nonstandard status outside the existing evidence schema range.
    const recorded: ReadAttempt = { ...entry, http_status: typeof entry.http_status === "number" &&
      Number.isInteger(entry.http_status) && entry.http_status >= 100 && entry.http_status <= 599 ? entry.http_status : null };
    readAttempts.push(recorded);
    onAttempt?.({ ...recorded });
  };
  for (let attempt = 0; ; attempt++) {
    signal.throwIfAborted();
    await beforeAttempt?.();
    signal.throwIfAborted();
    const started_at = new Date().toISOString();
    let retryAfter: string | undefined;
    let observedHttpStatus: number | undefined;
    try {
      const result = await requestOnce(pinnedRequest, signal, status => { observedHttpStatus = status; });
      recordAttempt({ attempt: attempt + 1, path: pinnedRequest.suffix, selector_digest, started_at,
        observed_at: new Date().toISOString(), outcome: "http_response", http_status: result.status });
      if (![429, 502, 503, 504].includes(result.status) || attempt >= 2)
        return { ...result, readAttempts };
      retryAfter = result.retryAfter;
    } catch (error) {
      if (error instanceof CliError && error.subtype === "connection")
        recordAttempt({ attempt: attempt + 1, path: pinnedRequest.suffix, selector_digest, started_at,
          observed_at: new Date().toISOString(), outcome: "connection_error", http_status: null });
      else if (observedHttpStatus !== undefined)
        recordAttempt({ attempt: attempt + 1, path: pinnedRequest.suffix, selector_digest, started_at,
          observed_at: new Date().toISOString(), outcome: "http_response", http_status: observedHttpStatus });
      if (
        !(error instanceof CliError) ||
        error.subtype !== "connection" ||
        attempt >= 2 ||
        signal.aborted
      )
        throw error;
    }
    const backoff = (attempt === 0 ? 100 : 300) + Math.floor(Math.random() * 30);
    const retryDate = retryAfter?.endsWith(" GMT") ? Date.parse(retryAfter) : NaN;
    const serverWait = retryAfter && /^\d+$/.test(retryAfter) && Number(retryAfter) > 0
      ? 1000 : Number.isFinite(retryDate) ? Math.max(0, Math.min(1000, retryDate - Date.now())) : 0;
    const wait = Math.max(backoff, serverWait);
    try {
      await delay(
        wait,
        undefined,
        { signal },
      );
    } catch (error) {
      signal.throwIfAborted();
      throw error;
    }
  }
}
