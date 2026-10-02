/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.namingserver.security;

import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.HmacSigner;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Produces the {@code X-Seata-*} headers that must accompany outgoing requests from
 * NamingServer to Seata Server (TC).
 *
 * <p>Two things justify the existence of this class:
 * <ol>
 *   <li>The signing convention (which headers, in what order, with what timestamp source) is
 *       identical for every call site — vGroup add/remove, ConsoleRemotingFilter proxy,
 *       MCP invocations. Centralising avoids drift.</li>
 *   <li>The clock / nonce sources are injectable so tests can produce deterministic signatures.</li>
 * </ol>
 */
public final class OutboundSigner {

    private final String selfClusterId;

    private final byte[] secret;

    private final SignatureAlgorithm algorithm;

    private final Supplier<Long> clock;

    private final Supplier<String> nonceSupplier;

    /**
     * Full constructor. The two suppliers exist purely so tests can inject deterministic values.
     *
     * @param selfClusterId   value to place in {@code X-Seata-Cluster-Id} — the NamingServer's own id
     * @param secret          shared secret paired with {@code selfClusterId} on the TC side
     * @param algorithm       MAC algorithm; usually {@link SignatureAlgorithm#HMAC_SHA256}
     * @param clock           supplies millisecond timestamps (defaults to system clock if null)
     * @param nonceSupplier   supplies unique nonces (defaults to random UUID if null)
     */
    public OutboundSigner(
            String selfClusterId,
            byte[] secret,
            SignatureAlgorithm algorithm,
            Supplier<Long> clock,
            Supplier<String> nonceSupplier) {
        this.selfClusterId = Objects.requireNonNull(selfClusterId, "selfClusterId");
        this.secret = Objects.requireNonNull(secret, "secret").clone();
        this.algorithm = algorithm == null ? SignatureAlgorithm.HMAC_SHA256 : algorithm;
        this.clock = clock == null ? System::currentTimeMillis : clock;
        this.nonceSupplier = nonceSupplier == null ? () -> UUID.randomUUID().toString() : nonceSupplier;
    }

    /** Convenience constructor for production wiring — uses default clock and UUID nonce. */
    public OutboundSigner(String selfClusterId, byte[] secret) {
        this(selfClusterId, secret, SignatureAlgorithm.HMAC_SHA256, null, null);
    }

    /**
     * Build the header map for a request. Callers copy these into whichever HTTP client they use.
     *
     * @param method       HTTP method, e.g. {@code GET}, {@code POST}
     * @param path         request path (no host, no query)
     * @param queryParams  query parameters (may be empty)
     * @param body         request body bytes (may be null/empty)
     * @return an ordered map ready to iterate onto the outbound request
     */
    public Map<String, String> signHeaders(String method, String path, Map<String, String> queryParams, byte[] body) {
        return signHeaders(method, path, queryParams, body, Collections.emptyMap());
    }

    public Map<String, String> signHeaders(
            String method,
            String path,
            Map<String, String> queryParams,
            byte[] body,
            Map<String, String> routingHeaders) {
        long ts = clock.get();
        String nonce = nonceSupplier.get();
        CanonicalRequest req = CanonicalRequest.builder()
                .method(method)
                .path(path)
                .queryParams(queryParams)
                .signedHeaders(routingHeaders)
                .clusterId(selfClusterId)
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(algorithm)
                .body(body)
                .build();
        String signature = HmacSigner.sign(req, secret);

        Map<String, String> headers = new LinkedHashMap<>(6);
        headers.put(SecurityConstants.HEADER_CLUSTER_ID, selfClusterId);
        headers.put(SecurityConstants.HEADER_TIMESTAMP, Long.toString(ts));
        headers.put(SecurityConstants.HEADER_NONCE, nonce);
        headers.put(SecurityConstants.HEADER_SIGN_ALG, algorithm.wireName());
        headers.put(SecurityConstants.HEADER_SIGNATURE, signature);
        return headers;
    }

    /** Sign the final request URI and serialized body, including proxy routing headers. */
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        Map<String, String> query = new LinkedHashMap<>();
        String rawQuery = request.getURI().getRawQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            for (String pair : rawQuery.split("&")) {
                String[] parts = pair.split("=", 2);
                query.putIfAbsent(
                        URLDecoder.decode(parts[0], "UTF-8"),
                        parts.length == 1 ? "" : URLDecoder.decode(parts[1], "UTF-8"));
            }
        }
        Map<String, String> headers = signHeaders(
                request.getMethod().name(),
                request.getURI().getRawPath(),
                query,
                body,
                SecurityConstants.routingHeaders(request.getHeaders()::getFirst));
        headers.forEach(request.getHeaders()::set);
        return execution.execute(request, body);
    }

    public String getSelfClusterId() {
        return selfClusterId;
    }
}
