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

import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves a {@code secretRef} string from configuration into raw key material.
 *
 * <p>Supported schemes:
 * <ul>
 *   <li>{@code env:VAR_NAME} — reads {@code System.getenv(VAR_NAME)} and Base64-decodes it.</li>
 *   <li>{@code base64:...}   — inline Base64 (discouraged; only for local development).</li>
 *   <li>{@code plain:...}    — inline UTF-8 bytes (discouraged; only for local development).</li>
 * </ul>
 *
 * <p>The environment-lookup function is injected so tests can supply a fake env.
 * Additional schemes (vault, k8s-secret, KMS) can be layered on by wrapping this class.
 */
public final class SecretResolver {

    private static final String ENV_PREFIX = "env:";

    private static final String BASE64_PREFIX = "base64:";

    private static final String PLAIN_PREFIX = "plain:";

    private final Function<String, String> envLookup;

    public SecretResolver() {
        this(System::getenv);
    }

    /** @param envLookup how to resolve {@code env:} references (defaults to {@link System#getenv(String)}). */
    public SecretResolver(Function<String, String> envLookup) {
        this.envLookup = Objects.requireNonNull(envLookup, "envLookup");
    }

    /**
     * Resolve one reference.
     *
     * @param secretRef the string from configuration
     * @return raw secret bytes
     * @throws IllegalArgumentException if the ref is malformed, the env var is unset,
     *                                  or the Base64 payload is invalid
     */
    public byte[] resolve(String secretRef) {
        if (secretRef == null || secretRef.isEmpty()) {
            throw new IllegalArgumentException("secretRef is blank");
        }
        if (secretRef.startsWith(ENV_PREFIX)) {
            String envName = secretRef.substring(ENV_PREFIX.length());
            String value = envLookup.apply(envName);
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("env var not set or empty: " + envName);
            }
            return decodeBase64(value);
        }
        if (secretRef.startsWith(BASE64_PREFIX)) {
            return decodeBase64(secretRef.substring(BASE64_PREFIX.length()));
        }
        if (secretRef.startsWith(PLAIN_PREFIX)) {
            return secretRef.substring(PLAIN_PREFIX.length()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("unsupported secretRef scheme: " + secretRef
                + " (supported: env:, base64:, plain:)");
    }

    private static byte[] decodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid base64 in secretRef", e);
        }
    }
}
