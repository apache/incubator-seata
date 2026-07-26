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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureCanonicalizerTest {

    @Test
    void canonicalize_produces_stable_string_regardless_of_query_order() {
        Map<String, String> orderA = new LinkedHashMap<>();
        orderA.put("beta", "2");
        orderA.put("alpha", "1");

        Map<String, String> orderB = new LinkedHashMap<>();
        orderB.put("alpha", "1");
        orderB.put("beta", "2");

        CanonicalRequest reqA = baseRequest().queryParams(orderA).build();
        CanonicalRequest reqB = baseRequest().queryParams(orderB).build();

        String a = SignatureCanonicalizer.canonicalize(reqA);
        String b = SignatureCanonicalizer.canonicalize(reqB);

        assertEquals(a, b, "same query set must produce identical canonical strings");
        assertTrue(a.contains("alpha=1&beta=2"), "keys must appear in ascending order in the canonical query");
    }

    @Test
    void canonicalize_body_change_changes_the_string() {
        CanonicalRequest a = baseRequest().body("hello".getBytes()).build();
        CanonicalRequest b = baseRequest().body("hellО".getBytes()).build();
        assertNotEquals(
                SignatureCanonicalizer.canonicalize(a),
                SignatureCanonicalizer.canonicalize(b),
                "any byte change in body must flip the SHA-256 tail");
    }

    @Test
    void canonicalize_method_case_is_normalized_upper() {
        CanonicalRequest lower = baseRequest().method("post").build();
        CanonicalRequest upper = baseRequest().method("POST").build();
        assertEquals(SignatureCanonicalizer.canonicalize(upper), SignatureCanonicalizer.canonicalize(lower));
    }

    @Test
    void canonicalize_empty_query_and_body_still_produces_valid_string() {
        CanonicalRequest req =
                baseRequest().body(new byte[0]).queryParams(new HashMap<>()).build();
        String s = SignatureCanonicalizer.canonicalize(req);
        // 7 fixed segments split by \n. Body digest for empty input is a well-known constant.
        assertTrue(
                s.endsWith("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                "empty body must hash to the well-known SHA-256 of empty input");
    }

    @Test
    void canonical_query_url_encodes_special_characters() {
        Map<String, String> params = new HashMap<>();
        params.put("k", "a b/c=d&e");
        String canonical = SignatureCanonicalizer.canonicalQuery(params);
        // spaces become %20 (not '+'), slash '/' stays literal per URLEncoder,
        // '=' inside value becomes %3D, '&' becomes %26.
        assertEquals("k=a%20b%2Fc%3Dd%26e", canonical);
    }

    @Test
    void canonical_query_handles_null_value_as_empty() {
        Map<String, String> params = new HashMap<>();
        params.put("k", null);
        assertEquals("k=", SignatureCanonicalizer.canonicalQuery(params));
    }

    @Test
    void sha256_hex_of_empty_matches_reference_vector() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SignatureCanonicalizer.sha256Hex(new byte[0]));
    }

    @Test
    void sha256_hex_of_abc_matches_reference_vector() {
        // Standard NIST test vector for SHA-256("abc").
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SignatureCanonicalizer.sha256Hex("abc".getBytes()));
    }

    private static CanonicalRequest.Builder baseRequest() {
        return CanonicalRequest.builder()
                .method("POST")
                .path("/naming/v1/register")
                .clusterId("tenant-a")
                .timestampMillis(1_700_000_000_000L)
                .nonce("nonce-1")
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body("body".getBytes());
    }
}
