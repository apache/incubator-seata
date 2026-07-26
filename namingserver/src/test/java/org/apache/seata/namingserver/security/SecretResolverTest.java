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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretResolverTest {

    @Test
    void resolves_env_reference() {
        Map<String, String> env = new HashMap<>();
        String encoded = Base64.getEncoder().encodeToString("hello-world".getBytes(StandardCharsets.UTF_8));
        env.put("MY_KEY", encoded);
        SecretResolver resolver = new SecretResolver(env::get);
        assertArrayEquals("hello-world".getBytes(StandardCharsets.UTF_8), resolver.resolve("env:MY_KEY"));
    }

    @Test
    void resolves_inline_base64() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        String encoded = Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), resolver.resolve("base64:" + encoded));
    }

    @Test
    void resolves_plain_scheme() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), resolver.resolve("plain:abc"));
    }

    @Test
    void rejects_unset_env_var() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("env:NOT_SET"));
    }

    @Test
    void rejects_blank_ref() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(""));
    }

    @Test
    void rejects_unknown_scheme() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("kms:my-key"));
    }

    @Test
    void rejects_invalid_base64() {
        SecretResolver resolver = new SecretResolver(Collections.<String, String>emptyMap()::get);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("base64:not!base64"));
    }
}
