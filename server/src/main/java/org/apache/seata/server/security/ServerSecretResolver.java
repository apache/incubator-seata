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
package org.apache.seata.server.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

/**
 * Same three-scheme resolver as {@code namingserver.security.SecretResolver}, duplicated here
 * so the two modules do not need a shared runtime dependency beyond {@code seata-common}.
 * If the two files ever drift, the tests in each module will catch it.
 */
public final class ServerSecretResolver {

    private static final String ENV_PREFIX = "env:";
    private static final String BASE64_PREFIX = "base64:";
    private static final String PLAIN_PREFIX = "plain:";

    private final Function<String, String> envLookup;

    public ServerSecretResolver() {
        this(System::getenv);
    }

    public ServerSecretResolver(Function<String, String> envLookup) {
        this.envLookup = Objects.requireNonNull(envLookup, "envLookup");
    }

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
            return secretRef.substring(PLAIN_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("unsupported secretRef scheme: " + secretRef);
    }

    private static byte[] decodeBase64(String v) {
        try {
            return Base64.getDecoder().decode(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid base64 in secretRef", e);
        }
    }
}
