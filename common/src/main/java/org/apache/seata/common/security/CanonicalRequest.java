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
package org.apache.seata.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing everything the signer needs to know about a request.
 *
 * <p>The signature covers method + path + query + identity headers + body digest, so any
 * tampering with any of these fields invalidates the signature. Headers other than the
 * identity headers are intentionally excluded — proxies routinely add or rewrite them.
 */
public final class CanonicalRequest {

    private final String method;

    private final String path;

    private final Map<String, String> queryParams;

    private final String clusterId;

    private final long timestampMillis;

    private final String nonce;

    private final SignatureAlgorithm algorithm;

    private final byte[] body;

    private CanonicalRequest(Builder builder) {
        this.method = Objects.requireNonNull(builder.method, "method").toUpperCase();
        this.path = Objects.requireNonNull(builder.path, "path");
        this.queryParams = builder.queryParams == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
        this.clusterId = Objects.requireNonNull(builder.clusterId, "clusterId");
        this.timestampMillis = builder.timestampMillis;
        this.nonce = Objects.requireNonNull(builder.nonce, "nonce");
        this.algorithm = builder.algorithm == null ? SignatureAlgorithm.HMAC_SHA256 : builder.algorithm;
        this.body = builder.body == null ? new byte[0] : builder.body.clone();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getClusterId() {
        return clusterId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getNonce() {
        return nonce;
    }

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

    /** Defensive copy — never expose the internal array. */
    public byte[] getBody() {
        return body.clone();
    }

    /** UTF-8 view of the body, useful for logging and tests. */
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder to keep call sites readable given the number of fields. */
    public static final class Builder {
        private String method;
        private String path;
        private Map<String, String> queryParams;
        private String clusterId;
        private long timestampMillis;
        private String nonce;
        private SignatureAlgorithm algorithm;
        private byte[] body;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder clusterId(String clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public Builder timestampMillis(long timestampMillis) {
            this.timestampMillis = timestampMillis;
            return this;
        }

        public Builder nonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder algorithm(SignatureAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public CanonicalRequest build() {
            return new CanonicalRequest(this);
        }
    }
}
