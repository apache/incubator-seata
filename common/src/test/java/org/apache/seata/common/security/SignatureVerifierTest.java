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

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureVerifierTest {

    private static final byte[] SECRET = new byte[] {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
    };

    private static final long NOW = 1_700_000_000_000L;

    @Test
    void happy_path_returns_ok() {
        SignatureVerifier verifier = newVerifier(NOW);
        CanonicalRequest req = req(NOW).build();
        String sig = HmacSigner.sign(req, SECRET);

        VerificationResult result = verifier.verify(req, SECRET, sig);
        assertTrue(result.isSuccess());
    }

    @Test
    void missing_signature_returns_missing_signature_error() {
        SignatureVerifier verifier = newVerifier(NOW);
        VerificationResult r = verifier.verify(req(NOW).build(), SECRET, null);
        assertFalse(r.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.MISSING_SIGNATURE, r.getErrorCode());
    }

    @Test
    void missing_secret_returns_unknown_cluster_id_error() {
        SignatureVerifier verifier = newVerifier(NOW);
        VerificationResult r = verifier.verify(req(NOW).build(), new byte[0], "sig");
        assertFalse(r.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID, r.getErrorCode());
    }

    @Test
    void timestamp_outside_window_returns_skew_error() {
        SignatureVerifier verifier = newVerifier(NOW);
        long staleTs = NOW - (10 * 60 * 1000L); // 10 minutes in the past
        CanonicalRequest req = req(staleTs).build();
        String sig = HmacSigner.sign(req, SECRET);

        VerificationResult r = verifier.verify(req, SECRET, sig);
        assertFalse(r.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.TIMESTAMP_SKEW, r.getErrorCode());
    }

    @Test
    void replay_of_valid_request_returns_replay_error() {
        SignatureVerifier verifier = newVerifier(NOW);
        CanonicalRequest req = req(NOW).build();
        String sig = HmacSigner.sign(req, SECRET);

        assertTrue(verifier.verify(req, SECRET, sig).isSuccess());
        VerificationResult replay = verifier.verify(req, SECRET, sig);
        assertFalse(replay.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.REPLAY_DETECTED, replay.getErrorCode());
    }

    @Test
    void tampered_body_returns_bad_signature() {
        SignatureVerifier verifier = newVerifier(NOW);
        CanonicalRequest signed = req(NOW).body("clean".getBytes()).build();
        String sig = HmacSigner.sign(signed, SECRET);

        CanonicalRequest tampered =
                req(NOW).nonce("nonce-2").body("dirty".getBytes()).build();
        VerificationResult r = verifier.verify(tampered, SECRET, sig);
        assertFalse(r.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.BAD_SIGNATURE, r.getErrorCode());
    }

    @Test
    void constructor_rejects_null_cache_or_bad_window() {
        NonceCache cache = new NonceCache.InMemory(1000L, () -> 0L);
        assertThrows(NullPointerException.class, () -> new SignatureVerifier(null, 100L, () -> 0L));
        assertThrows(IllegalArgumentException.class, () -> new SignatureVerifier(cache, 0L, () -> 0L));
    }

    @Test
    void timestamp_ordering_is_symmetric_around_now() {
        SignatureVerifier verifier = newVerifier(NOW);
        // Timestamp 4 minutes ahead of "now" is inside the 5 minute window and must pass.
        CanonicalRequest future = req(NOW + 4 * 60 * 1000L).build();
        String sig = HmacSigner.sign(future, SECRET);
        assertTrue(verifier.verify(future, SECRET, sig).isSuccess());
    }

    private SignatureVerifier newVerifier(long fixedNow) {
        AtomicLong clock = new AtomicLong(fixedNow);
        NonceCache cache = new NonceCache.InMemory(60_000L, clock::get);
        return new SignatureVerifier(cache, 5 * 60 * 1000L, clock::get);
    }

    private CanonicalRequest.Builder req(long ts) {
        return CanonicalRequest.builder()
                .method("POST")
                .path("/naming/v1/register")
                .clusterId("tenant-a")
                .timestampMillis(ts)
                .nonce("nonce-" + ts)
                .body("body".getBytes());
    }
}
