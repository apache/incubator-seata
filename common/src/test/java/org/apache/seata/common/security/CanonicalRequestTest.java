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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalRequestTest {

    @Test
    void builder_populates_all_fields() {
        Map<String, String> q = new HashMap<>();
        q.put("k", "v");
        CanonicalRequest req = CanonicalRequest.builder()
                .method("post")
                .path("/x")
                .queryParams(q)
                .clusterId("c")
                .timestampMillis(42L)
                .nonce("n")
                .algorithm(SignatureAlgorithm.HMAC_SHA512)
                .body("hi".getBytes())
                .build();

        assertEquals("POST", req.getMethod(), "method must be uppercased");
        assertEquals("/x", req.getPath());
        assertEquals("v", req.getQueryParams().get("k"));
        assertEquals("c", req.getClusterId());
        assertEquals(42L, req.getTimestampMillis());
        assertEquals("n", req.getNonce());
        assertEquals(SignatureAlgorithm.HMAC_SHA512, req.getAlgorithm());
        assertEquals("hi", req.getBodyAsString());
    }

    @Test
    void defaults_apply_when_optional_fields_omitted() {
        CanonicalRequest req = CanonicalRequest.builder()
                .method("GET")
                .path("/")
                .clusterId("c")
                .timestampMillis(1L)
                .nonce("n")
                .build();
        assertEquals(SignatureAlgorithm.HMAC_SHA256, req.getAlgorithm(), "algorithm should default to HMAC-SHA256");
        assertEquals(0, req.getBody().length, "null body should be normalised to empty");
        assertTrue(req.getQueryParams().isEmpty());
    }

    @Test
    void body_getter_returns_defensive_copy() {
        byte[] original = "abc".getBytes();
        CanonicalRequest req = CanonicalRequest.builder()
                .method("GET")
                .path("/")
                .clusterId("c")
                .timestampMillis(1L)
                .nonce("n")
                .body(original)
                .build();
        byte[] a = req.getBody();
        byte[] b = req.getBody();
        assertNotSame(a, b, "each call should return a fresh copy");
        a[0] = 0;
        assertEquals('a', req.getBody()[0], "mutating the returned array must not affect state");
    }

    @Test
    void required_fields_are_null_checked() {
        assertThrows(NullPointerException.class, () -> CanonicalRequest.builder()
                .method(null)
                .path("/")
                .clusterId("c")
                .timestampMillis(0)
                .nonce("n")
                .build());
        assertThrows(NullPointerException.class, () -> CanonicalRequest.builder()
                .method("GET")
                .path(null)
                .clusterId("c")
                .timestampMillis(0)
                .nonce("n")
                .build());
        assertThrows(NullPointerException.class, () -> CanonicalRequest.builder()
                .method("GET")
                .path("/")
                .clusterId(null)
                .timestampMillis(0)
                .nonce("n")
                .build());
        assertThrows(NullPointerException.class, () -> CanonicalRequest.builder()
                .method("GET")
                .path("/")
                .clusterId("c")
                .timestampMillis(0)
                .nonce(null)
                .build());
    }

    @Test
    void query_params_are_immutable_view() {
        Map<String, String> q = new HashMap<>();
        q.put("k", "v");
        CanonicalRequest req = CanonicalRequest.builder()
                .method("GET")
                .path("/")
                .clusterId("c")
                .timestampMillis(1L)
                .nonce("n")
                .queryParams(q)
                .build();
        assertThrows(
                UnsupportedOperationException.class, () -> req.getQueryParams().put("x", "y"));
    }
}
