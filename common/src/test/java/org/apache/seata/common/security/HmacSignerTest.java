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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacSignerTest {

    private static final byte[] SECRET_32 = new byte[] {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
    };

    @Test
    void sign_then_verify_returns_true() {
        CanonicalRequest req = req().build();
        String sig = HmacSigner.sign(req, SECRET_32);
        assertTrue(HmacSigner.verify(req, SECRET_32, sig), "self-verification must succeed");
    }

    @Test
    void verify_fails_when_body_changes() {
        CanonicalRequest signed = req().body("original".getBytes()).build();
        String sig = HmacSigner.sign(signed, SECRET_32);

        CanonicalRequest tampered = req().body("tampered".getBytes()).build();
        assertFalse(HmacSigner.verify(tampered, SECRET_32, sig), "body tamper must be detected");
    }

    @Test
    void verify_fails_when_secret_differs() {
        CanonicalRequest req = req().build();
        String sig = HmacSigner.sign(req, SECRET_32);
        byte[] otherSecret = SECRET_32.clone();
        otherSecret[0] ^= 0x55; // flip a few bits
        assertFalse(HmacSigner.verify(req, otherSecret, sig));
    }

    @Test
    void verify_returns_false_for_null_or_empty_signature() {
        CanonicalRequest req = req().build();
        assertFalse(HmacSigner.verify(req, SECRET_32, null));
        assertFalse(HmacSigner.verify(req, SECRET_32, ""));
    }

    @Test
    void verify_returns_false_for_non_base64_signature() {
        CanonicalRequest req = req().build();
        // Contains an illegal char for standard Base64 alphabet:
        assertFalse(HmacSigner.verify(req, SECRET_32, "###not-base64###"));
    }

    @Test
    void sign_rejects_short_secret() {
        CanonicalRequest req = req().build();
        byte[] tooShort = new byte[16];
        assertThrows(IllegalArgumentException.class, () -> HmacSigner.sign(req, tooShort));
    }

    @Test
    void sign_rejects_null_secret() {
        CanonicalRequest req = req().build();
        assertThrows(IllegalArgumentException.class, () -> HmacSigner.sign(req, null));
    }

    @Test
    void sign_with_sha512_produces_different_output_than_sha256() {
        CanonicalRequest sha256 =
                req().algorithm(SignatureAlgorithm.HMAC_SHA256).build();
        CanonicalRequest sha512 =
                req().algorithm(SignatureAlgorithm.HMAC_SHA512).build();
        String s256 = HmacSigner.sign(sha256, SECRET_32);
        String s512 = HmacSigner.sign(sha512, SECRET_32);
        assertFalse(s256.equals(s512), "different algorithms must produce different MACs");
        assertTrue(HmacSigner.verify(sha512, SECRET_32, s512));
    }

    @Test
    void constant_time_equals_returns_false_on_length_mismatch() {
        assertFalse(HmacSigner.constantTimeEquals(new byte[] {1, 2, 3}, new byte[] {1, 2}));
    }

    @Test
    void constant_time_equals_returns_true_on_equal_bytes() {
        assertTrue(HmacSigner.constantTimeEquals(new byte[] {1, 2, 3}, new byte[] {1, 2, 3}));
    }

    @Test
    void constant_time_equals_returns_false_on_null_input() {
        assertFalse(HmacSigner.constantTimeEquals(null, new byte[] {1}));
        assertFalse(HmacSigner.constantTimeEquals(new byte[] {1}, null));
    }

    @Test
    void mac_output_length_matches_algorithm() {
        byte[] out256 = HmacSigner.mac(SignatureAlgorithm.HMAC_SHA256, SECRET_32, "msg".getBytes());
        byte[] out512 = HmacSigner.mac(SignatureAlgorithm.HMAC_SHA512, SECRET_32, "msg".getBytes());
        assertEquals(32, out256.length, "HMAC-SHA256 must produce 256 bits");
        assertEquals(64, out512.length, "HMAC-SHA512 must produce 512 bits");
    }

    @Test
    void mac_is_deterministic() {
        byte[] a = HmacSigner.mac(SignatureAlgorithm.HMAC_SHA256, SECRET_32, "same-input".getBytes());
        byte[] b = HmacSigner.mac(SignatureAlgorithm.HMAC_SHA256, SECRET_32, "same-input".getBytes());
        assertTrue(HmacSigner.constantTimeEquals(a, b), "same key + same input must yield identical MAC");
    }

    private static CanonicalRequest.Builder req() {
        return CanonicalRequest.builder()
                .method("POST")
                .path("/naming/v1/register")
                .clusterId("tenant-a")
                .timestampMillis(1_700_000_000_000L)
                .nonce("nonce-1");
    }
}
