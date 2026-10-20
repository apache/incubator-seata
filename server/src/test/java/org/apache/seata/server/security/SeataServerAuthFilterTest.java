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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.HmacSigner;
import org.apache.seata.common.security.NonceCache;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.apache.seata.common.security.SignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeataServerAuthFilterTest {

    private static final byte[] SECRET = new byte[32];

    static {
        for (int i = 0; i < 32; i++) {
            SECRET[i] = (byte) (i + 7);
        }
    }

    private static final String CALLER_ID = "naming-server-01";

    private ServerSecurityProperties props;
    private AllowedCallerRegistry registry;
    private AtomicLong clock;
    private SignatureVerifier verifier;
    private RouteAuthorizer authorizer;
    private SeataServerAuthFilter filter;

    @BeforeEach
    void setUp() {
        props = new ServerSecurityProperties();
        props.setEnabled(true);
        props.setMode(ServerSecurityProperties.Mode.ENFORCE);

        registry = new AllowedCallerRegistry();
        registry.register(new AllowedCaller(CALLER_ID, SECRET, EnumSet.allOf(CallerPermission.class)));

        clock = new AtomicLong(1_700_000_000_000L);
        NonceCache cache = new NonceCache.InMemory(60_000L, clock::get);
        verifier = new SignatureVerifier(cache, 5 * 60 * 1000L, clock::get);
        authorizer = new RouteAuthorizer();
        filter = new SeataServerAuthFilter(props, registry, verifier, authorizer);
    }

    @Test
    void unprotected_route_bypasses_signature_check() throws Exception {
        // /health is not on the RouteAuthorizer list → filter passes through.
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get());
        assertEquals(200, resp.getStatus());
    }

    @Test
    void protected_route_with_valid_signature_passes() throws Exception {
        MockHttpServletRequest req = signed(
                "GET", "/vgroup/v1/addVGroup", map("vGroup", "g1", "unit", "u1"), new byte[0], clock.get(), "n-1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get(), "valid request must pass");
        AllowedCaller injected =
                (AllowedCaller) chain.capturedRequest.get().getAttribute(SeataServerAuthFilter.ATTR_CALLER);
        assertNotNull(injected);
        assertEquals(CALLER_ID, injected.getId());
    }

    @Test
    void protected_route_without_headers_is_rejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/vgroup/v1/addVGroup");
        req.addParameter("vGroup", "g1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(401, resp.getStatus());
        assertEquals(
                SecurityConstants.ErrorCode.MISSING_SIGNATURE.name(),
                resp.getHeader(SeataServerAuthFilter.RESP_HEADER_ERROR));
    }

    @Test
    void warn_mode_lets_unsigned_request_pass_through_but_logs() throws Exception {
        props.setMode(ServerSecurityProperties.Mode.WARN);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/vgroup/v1/addVGroup");
        req.addParameter("vGroup", "g1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get());
        assertEquals(200, resp.getStatus());
    }

    @Test
    void unknown_caller_is_rejected() throws Exception {
        MockHttpServletRequest req =
                signed("GET", "/vgroup/v1/addVGroup", map("vGroup", "g1"), new byte[0], clock.get(), "n-2");
        req.removeHeader(SecurityConstants.HEADER_CLUSTER_ID);
        req.addHeader(SecurityConstants.HEADER_CLUSTER_ID, "stranger");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID.name(),
                resp.getHeader(SeataServerAuthFilter.RESP_HEADER_ERROR));
    }

    @Test
    void caller_missing_permission_is_forbidden() throws Exception {
        // Reload with a caller that only holds CONSOLE_PROXY.
        registry.reload(Collections.singletonList(
                new AllowedCaller(CALLER_ID, SECRET, EnumSet.of(CallerPermission.CONSOLE_PROXY))));

        MockHttpServletRequest req =
                signed("GET", "/vgroup/v1/addVGroup", map("vGroup", "g1"), new byte[0], clock.get(), "n-3");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(403, resp.getStatus());
        assertEquals(
                SecurityConstants.ErrorCode.FORBIDDEN.name(), resp.getHeader(SeataServerAuthFilter.RESP_HEADER_ERROR));
    }

    @Test
    void replay_of_valid_request_is_rejected() throws Exception {
        MockHttpServletRequest req1 =
                signed("GET", "/vgroup/v1/addVGroup", map("vGroup", "g1"), new byte[0], clock.get(), "dup");
        filter.doFilter(req1, new MockHttpServletResponse(), new RecordingChain());

        MockHttpServletRequest req2 =
                signed("GET", "/vgroup/v1/addVGroup", map("vGroup", "g1"), new byte[0], clock.get(), "dup");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req2, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.REPLAY_DETECTED.name(),
                resp.getHeader(SeataServerAuthFilter.RESP_HEADER_ERROR));
    }

    @Test
    void tampered_query_is_detected() throws Exception {
        // Sign for vGroup=g1, then flip to vGroup=g2 without re-signing.
        MockHttpServletRequest req =
                signed("GET", "/vgroup/v1/addVGroup", map("vGroup", "g1"), new byte[0], clock.get(), "n-4");
        req.removeParameter("vGroup");
        req.addParameter("vGroup", "g2");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.BAD_SIGNATURE.name(),
                resp.getHeader(SeataServerAuthFilter.RESP_HEADER_ERROR));
    }

    @Test
    void matches_helper_handles_exact_and_wildcard() {
        assertTrue(SeataServerAuthFilter.matches("/health", "/health"));
        assertFalse(SeataServerAuthFilter.matches("/health", "/health/x"));
        assertTrue(SeataServerAuthFilter.matches("/actuator/**", "/actuator/env"));
        assertFalse(SeataServerAuthFilter.matches("/actuator/**", "/other"));
        assertFalse(SeataServerAuthFilter.matches(null, "/x"));
    }

    // -------------------------------------------------- helpers

    private MockHttpServletRequest signed(
            String method, String path, Map<String, String> query, byte[] body, long ts, String nonce) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setContent(body == null ? new byte[0] : body);
        for (Map.Entry<String, String> e : query.entrySet()) {
            req.addParameter(e.getKey(), e.getValue());
        }
        CanonicalRequest canonical = CanonicalRequest.builder()
                .method(method)
                .path(path)
                .queryParams(query)
                .clusterId(CALLER_ID)
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(body)
                .build();
        String sig = HmacSigner.sign(canonical, SECRET);

        req.addHeader(SecurityConstants.HEADER_CLUSTER_ID, CALLER_ID);
        req.addHeader(SecurityConstants.HEADER_TIMESTAMP, Long.toString(ts));
        req.addHeader(SecurityConstants.HEADER_NONCE, nonce);
        req.addHeader(SecurityConstants.HEADER_SIGN_ALG, SignatureAlgorithm.HMAC_SHA256.wireName());
        req.addHeader(SecurityConstants.HEADER_SIGNATURE, sig);
        return req;
    }

    private static Map<String, String> map(String... kv) {
        if ((kv.length & 1) != 0) {
            throw new IllegalArgumentException("kv must be pairs");
        }
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static final class RecordingChain implements FilterChain {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            called.set(true);
            capturedRequest.set(request);
        }
    }
}
