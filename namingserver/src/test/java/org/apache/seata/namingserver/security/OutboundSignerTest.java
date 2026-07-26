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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.HmacSigner;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundSignerTest {

    private static final byte[] SECRET = new byte[32];
    static {
        for (int i = 0; i < 32; i++) {
            SECRET[i] = (byte) i;
        }
    }

    @Test
    void produces_all_required_headers() {
        OutboundSigner signer = new OutboundSigner(
                "naming-01", SECRET, SignatureAlgorithm.HMAC_SHA256, () -> 1234L, () -> "fixed");
        Map<String, String> headers = signer.signHeaders("POST", "/vgroup/v1/addVGroup",
                singletonQuery("vGroup", "g1"), null);

        assertEquals("naming-01", headers.get(SecurityConstants.HEADER_CLUSTER_ID));
        assertEquals("1234", headers.get(SecurityConstants.HEADER_TIMESTAMP));
        assertEquals("fixed", headers.get(SecurityConstants.HEADER_NONCE));
        assertEquals("HMAC-SHA256", headers.get(SecurityConstants.HEADER_SIGN_ALG));
        assertTrue(headers.get(SecurityConstants.HEADER_SIGNATURE).length() > 0);
    }

    @Test
    void signature_verifies_with_same_secret() {
        OutboundSigner signer = new OutboundSigner(
                "naming-01", SECRET, SignatureAlgorithm.HMAC_SHA256, () -> 5000L, () -> "n-1");
        byte[] body = "{\"a\":1}".getBytes();
        Map<String, String> headers = signer.signHeaders("POST", "/vgroup/v1/addVGroup",
                singletonQuery("vGroup", "g1"), body);

        CanonicalRequest req = CanonicalRequest.builder()
                .method("POST")
                .path("/vgroup/v1/addVGroup")
                .queryParams(singletonQuery("vGroup", "g1"))
                .clusterId("naming-01")
                .timestampMillis(5000L)
                .nonce("n-1")
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(body)
                .build();

        assertTrue(HmacSigner.verify(req, SECRET, headers.get(SecurityConstants.HEADER_SIGNATURE)));
    }

    @Test
    void nonce_supplier_is_called_per_request() {
        AtomicLong counter = new AtomicLong();
        OutboundSigner signer = new OutboundSigner(
                "naming-01", SECRET, SignatureAlgorithm.HMAC_SHA256,
                () -> 0L, () -> "n-" + counter.incrementAndGet());

        Map<String, String> h1 = signer.signHeaders("GET", "/a", null, null);
        Map<String, String> h2 = signer.signHeaders("GET", "/a", null, null);
        assertEquals("n-1", h1.get(SecurityConstants.HEADER_NONCE));
        assertEquals("n-2", h2.get(SecurityConstants.HEADER_NONCE));
    }

    private static Map<String, String> singletonQuery(String k, String v) {
        Map<String, String> m = new HashMap<>();
        m.put(k, v);
        return m;
    }
}
