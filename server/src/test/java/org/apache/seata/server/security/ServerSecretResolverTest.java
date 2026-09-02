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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerSecretResolverTest {

    @Test
    void env_scheme_reads_base64_from_env() {
        Map<String, String> env = new HashMap<>();
        String v = Base64.getEncoder().encodeToString("hi".getBytes(StandardCharsets.UTF_8));
        env.put("MY_KEY", v);
        assertArrayEquals(
                "hi".getBytes(StandardCharsets.UTF_8), new ServerSecretResolver(env::get).resolve("env:MY_KEY"));
    }

    @Test
    void plain_scheme_returns_raw_utf8() {
        assertArrayEquals(
                "abc".getBytes(StandardCharsets.UTF_8),
                new ServerSecretResolver(Collections.<String, String>emptyMap()::get).resolve("plain:abc"));
    }

    @Test
    void base64_scheme_decodes_inline_value() {
        String encoded = Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(
                "abc".getBytes(StandardCharsets.UTF_8),
                new ServerSecretResolver(Collections.<String, String>emptyMap()::get).resolve("base64:" + encoded));
    }

    @Test
    void rejects_missing_env_var() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServerSecretResolver(Collections.<String, String>emptyMap()::get).resolve("env:MISSING"));
    }

    @Test
    void rejects_invalid_scheme_or_blank_input() {
        ServerSecretResolver r = new ServerSecretResolver(Collections.<String, String>emptyMap()::get);
        assertThrows(IllegalArgumentException.class, () -> r.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> r.resolve(""));
        assertThrows(IllegalArgumentException.class, () -> r.resolve("kms:x"));
        assertThrows(IllegalArgumentException.class, () -> r.resolve("base64:???"));
    }
}
