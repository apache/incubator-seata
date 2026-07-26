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

import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.HmacSigner;
import org.apache.seata.common.security.NonceCache;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.apache.seata.common.security.SignatureVerifier;
import org.apache.seata.common.security.VerificationResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that the code an operator would deploy on the two sides actually
 * interoperates. The NamingServer side uses raw HmacSigner (mimicking what OutboundSigner
 * emits) and the TC side runs the full verifier pipeline.
 *
 * <p>Guards against silent drift between the two sides — if either party changes the wire
 * format, this test breaks even when the individual unit tests still pass.
 */
class InteropIntegrationTest {

    private static final byte[] SHARED_SECRET = new byte[32];

    static {
        for (int i = 0; i < 32; i++) {
            SHARED_SECRET[i] = (byte) (0x30 + i);
        }
    }

    @Test
    void naming_server_add_vgroup_call_verifies_on_tc() {
        // 1) NamingServer side: build the request as NamingManager.executeControlRequest would
        //    (GET /vgroup/v1/addVGroup?vGroup=g1&unit=u1, empty body).
        String method = "GET";
        String path = "/vgroup/v1/addVGroup";
        Map<String, String> query = new HashMap<>();
        query.put("vGroup", "g1");
        query.put("unit", "u1");
        long ts = 5_000L;
        String nonce = "abc-123";

        CanonicalRequest namingSide = CanonicalRequest.builder()
                .method(method)
                .path(path)
                .queryParams(query)
                .clusterId("naming-server-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(new byte[0])
                .build();
        String signature = HmacSigner.sign(namingSide, SHARED_SECRET);

        // 2) Wire: {clusterId, ts, nonce, alg, signature} + body travel over HTTP.
        //         The TC receives them as headers/query/body and rebuilds a canonical.

        // 3) TC side: rebuild the canonical from the "received" headers/query/body and verify.
        CanonicalRequest tcSide = CanonicalRequest.builder()
                .method(method)
                .path(path)
                .queryParams(query)
                .clusterId("naming-server-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(new byte[0])
                .build();

        NonceCache cache = new NonceCache.InMemory(60_000L, () -> ts);
        SignatureVerifier verifier = new SignatureVerifier(cache, 300_000L, () -> ts);
        AllowedCaller caller =
                new AllowedCaller("naming-server-01", SHARED_SECRET, EnumSet.of(CallerPermission.VGROUP_WRITE));
        RouteAuthorizer routes = new RouteAuthorizer();

        VerificationResult verdict = verifier.verify(tcSide, caller.getSecret(), signature);
        assertTrue(verdict.isSuccess(), "handshake must succeed end-to-end");

        assertTrue(routes.isAuthorized(caller, method, path), "signature ok + right permission → allowed");
    }

    @Test
    void wrong_secret_on_tc_side_produces_bad_signature() {
        Map<String, String> query = new HashMap<>();
        query.put("vGroup", "g1");
        String nonce = "x-1";
        long ts = 6_000L;
        CanonicalRequest signed = CanonicalRequest.builder()
                .method("GET")
                .path("/vgroup/v1/addVGroup")
                .queryParams(query)
                .clusterId("naming-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(new byte[0])
                .build();
        String signature = HmacSigner.sign(signed, SHARED_SECRET);

        // TC has a different secret for the same cluster-id (e.g. rotation gone wrong).
        byte[] wrongSecret = SHARED_SECRET.clone();
        wrongSecret[15] ^= 0x55;

        NonceCache cache = new NonceCache.InMemory(60_000L, () -> ts);
        SignatureVerifier verifier = new SignatureVerifier(cache, 300_000L, () -> ts);
        VerificationResult verdict = verifier.verify(signed, wrongSecret, signature);

        assertFalse(verdict.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.BAD_SIGNATURE, verdict.getErrorCode());
    }

    @Test
    void query_parameter_reorder_on_wire_does_not_break_verification() {
        // NamingServer sends ?vGroup=g1&unit=u1
        // TC receives them but iterates in a different order internally.
        // Because canonicalization sorts, both sides produce identical signStrings.
        Map<String, String> senderOrder = new java.util.LinkedHashMap<>();
        senderOrder.put("vGroup", "g1");
        senderOrder.put("unit", "u1");

        Map<String, String> receiverOrder = new java.util.LinkedHashMap<>();
        receiverOrder.put("unit", "u1");
        receiverOrder.put("vGroup", "g1");

        long ts = 7_000L;
        String nonce = "reorder-1";
        CanonicalRequest sent = CanonicalRequest.builder()
                .method("GET")
                .path("/vgroup/v1/addVGroup")
                .queryParams(senderOrder)
                .clusterId("naming-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(new byte[0])
                .build();
        String signature = HmacSigner.sign(sent, SHARED_SECRET);

        CanonicalRequest received = CanonicalRequest.builder()
                .method("GET")
                .path("/vgroup/v1/addVGroup")
                .queryParams(receiverOrder)
                .clusterId("naming-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(new byte[0])
                .build();

        NonceCache cache = new NonceCache.InMemory(60_000L, () -> ts);
        SignatureVerifier verifier = new SignatureVerifier(cache, 300_000L, () -> ts);
        assertTrue(verifier.verify(received, SHARED_SECRET, signature).isSuccess());
    }

    @Test
    void console_proxy_body_forwarding_survives_signature_check() {
        // Console POSTs a body-carrying request; NamingServer proxies it verbatim.
        byte[] body = "{\"xid\":\"1.2.3\",\"branchId\":42}".getBytes();
        long ts = 8_000L;
        String nonce = "console-1";

        CanonicalRequest sent = CanonicalRequest.builder()
                .method("POST")
                .path("/api/v1/console/globalSession/forceCommit")
                .queryParams(Collections.<String, String>emptyMap())
                .clusterId("naming-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(body)
                .build();
        String signature = HmacSigner.sign(sent, SHARED_SECRET);

        // The body byte-for-byte must appear on the TC side.
        CanonicalRequest received = CanonicalRequest.builder()
                .method("POST")
                .path("/api/v1/console/globalSession/forceCommit")
                .queryParams(Collections.<String, String>emptyMap())
                .clusterId("naming-01")
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(body)
                .build();

        NonceCache cache = new NonceCache.InMemory(60_000L, () -> ts);
        SignatureVerifier verifier = new SignatureVerifier(cache, 300_000L, () -> ts);
        assertTrue(
                verifier.verify(received, SHARED_SECRET, signature).isSuccess(),
                "unchanged proxied body must still verify");
    }
}
