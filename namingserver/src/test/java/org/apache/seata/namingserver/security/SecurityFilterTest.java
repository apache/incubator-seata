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

class SecurityFilterTest {

    private static final byte[] SECRET = new byte[32];

    static {
        for (int i = 0; i < 32; i++) {
            SECRET[i] = (byte) (i + 1);
        }
    }

    private static final String CLUSTER_ID = "tenant-a";

    private ClusterIdentityRegistry registry;
    private SecurityProperties props;
    private AtomicLong clock;
    private NonceCache nonceCache;
    private SignatureVerifier verifier;
    private PermissionChecker permissionChecker;
    private SecurityFilter filter;

    @BeforeEach
    void setUp() {
        registry = new ClusterIdentityRegistry();
        registry.register(new ClusterIdentity(
                CLUSTER_ID,
                SECRET,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList(),
                EnumSet.allOf(Permission.class)));

        props = new SecurityProperties();
        props.setEnabled(true);
        props.setMode(SecurityProperties.Mode.ENFORCE);
        props.setExcludePaths(Collections.singletonList("/naming/v1/health"));

        clock = new AtomicLong(1_700_000_000_000L);
        nonceCache = new NonceCache.InMemory(60_000L, clock::get);
        verifier = new SignatureVerifier(nonceCache, 5 * 60 * 1000L, clock::get);
        permissionChecker = new PermissionChecker();
        filter = new SecurityFilter(props, registry, verifier, permissionChecker);
    }

    // ------------------------------------------------------------------ excluded paths

    @Test
    void health_endpoint_bypasses_filter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/naming/v1/health");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get(), "excluded path should pass through");
        assertEquals(200, resp.getStatus(), "no rejection expected");
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void valid_signed_request_passes_and_exposes_identity() throws Exception {
        MockHttpServletRequest req = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod", "clusterName", "cluster-A"),
                "{}".getBytes(),
                clock.get(),
                "n-1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get(), "valid request must reach the chain");
        assertNotNull(chain.capturedRequest.get());
        ClusterIdentity injected =
                (ClusterIdentity) chain.capturedRequest.get().getAttribute(SecurityFilter.ATTR_IDENTITY);
        assertNotNull(injected, "authenticated identity should be exposed on the request");
        assertEquals(CLUSTER_ID, injected.getId());
    }

    // ------------------------------------------------------------------ missing headers

    @Test
    void missing_signature_headers_are_rejected_in_enforce_mode() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/naming/v1/register");
        req.setContent("{}".getBytes());
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(401, resp.getStatus());
        assertEquals(
                SecurityConstants.ErrorCode.MISSING_SIGNATURE.name(), resp.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    @Test
    void missing_headers_pass_through_in_warn_mode() throws Exception {
        props.setMode(SecurityProperties.Mode.WARN);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/naming/v1/register");
        req.setContent("{}".getBytes());
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertTrue(chain.called.get(), "WARN mode must not block");
        assertEquals(200, resp.getStatus());
    }

    // ------------------------------------------------------------------ unknown cluster

    @Test
    void unknown_cluster_id_is_rejected() throws Exception {
        MockHttpServletRequest req = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{}".getBytes(),
                clock.get(),
                "n-2");
        req.removeHeader(SecurityConstants.HEADER_CLUSTER_ID);
        req.addHeader(SecurityConstants.HEADER_CLUSTER_ID, "someone-else");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(401, resp.getStatus());
        assertEquals(
                SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID.name(),
                resp.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    // ------------------------------------------------------------------ bad timestamp / signature

    @Test
    void stale_timestamp_is_rejected() throws Exception {
        long stale = clock.get() - 10 * 60 * 1000L;
        MockHttpServletRequest req = signedRequest(
                "POST", "/naming/v1/register", singletonQuery("namespace", "prod"), "{}".getBytes(), stale, "n-3");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.TIMESTAMP_SKEW.name(), resp.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    @Test
    void tampered_body_produces_bad_signature() throws Exception {
        // Sign with body-A, then swap in body-B without re-signing.
        MockHttpServletRequest req = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{\"a\":1}".getBytes(),
                clock.get(),
                "n-4");
        req.setContent("{\"a\":2}".getBytes());
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.BAD_SIGNATURE.name(), resp.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    @Test
    void replay_of_valid_request_is_rejected() throws Exception {
        MockHttpServletRequest req1 = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{}".getBytes(),
                clock.get(),
                "same-nonce");
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        filter.doFilter(req1, resp1, new RecordingChain());
        assertEquals(200, resp1.getStatus());

        MockHttpServletRequest req2 = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{}".getBytes(),
                clock.get(),
                "same-nonce");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        RecordingChain chain2 = new RecordingChain();
        filter.doFilter(req2, resp2, chain2);
        assertFalse(chain2.called.get());
        assertEquals(
                SecurityConstants.ErrorCode.REPLAY_DETECTED.name(), resp2.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    // ------------------------------------------------------------------ authorization

    @Test
    void caller_without_required_permission_is_forbidden() throws Exception {
        // Reload with an identity that only holds CONSOLE_READ.
        registry.reload(Collections.singletonList(new ClusterIdentity(
                CLUSTER_ID,
                SECRET,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList(),
                EnumSet.of(Permission.CONSOLE_READ))));

        MockHttpServletRequest req = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{}".getBytes(),
                clock.get(),
                "n-5");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(403, resp.getStatus());
        assertEquals(SecurityConstants.ErrorCode.FORBIDDEN.name(), resp.getHeader(SecurityFilter.RESP_HEADER_ERROR));
    }

    @Test
    void namespace_outside_allow_list_is_forbidden() throws Exception {
        registry.reload(Collections.singletonList(new ClusterIdentity(
                CLUSTER_ID,
                SECRET,
                Collections.singleton("staging"),
                Collections.emptySet(),
                Collections.emptyList(),
                EnumSet.of(Permission.REGISTER))));

        MockHttpServletRequest req = signedRequest(
                "POST",
                "/naming/v1/register",
                singletonQuery("namespace", "prod"),
                "{}".getBytes(),
                clock.get(),
                "n-6");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, resp, chain);

        assertFalse(chain.called.get());
        assertEquals(403, resp.getStatus());
    }

    // ------------------------------------------------------------------ body readable downstream

    @Test
    void body_is_readable_by_downstream_after_filter() throws Exception {
        byte[] body = "{\"a\":42}".getBytes();
        MockHttpServletRequest req = signedRequest(
                "POST", "/naming/v1/register", singletonQuery("namespace", "prod"), body, clock.get(), "n-7");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<String> downstreamBody = new AtomicReference<>();
        FilterChain chain = (r, rp) -> {
            byte[] read = ((jakarta.servlet.http.HttpServletRequest) r)
                    .getInputStream()
                    .readAllBytes();
            downstreamBody.set(new String(read));
        };

        filter.doFilter(req, resp, chain);

        assertEquals("{\"a\":42}", downstreamBody.get(), "wrapper must let controllers still read the original body");
    }

    // ------------------------------------------------------------------ matches() helper

    @Test
    void matches_helper_supports_exact_and_wildcard() {
        assertTrue(SecurityFilter.matches("/naming/v1/health", "/naming/v1/health"));
        assertFalse(SecurityFilter.matches("/naming/v1/health", "/naming/v1/health/x"));
        assertTrue(SecurityFilter.matches("/naming/v1/**", "/naming/v1/health"));
        assertTrue(SecurityFilter.matches("/naming/v1/**", "/naming/v1/anything/deep"));
        assertFalse(SecurityFilter.matches("/naming/v1/**", "/other"));
        assertFalse(SecurityFilter.matches(null, "/x"));
        assertFalse(SecurityFilter.matches("/x", null));
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpServletRequest signedRequest(
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
                .clusterId(CLUSTER_ID)
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(SignatureAlgorithm.HMAC_SHA256)
                .body(body)
                .build();
        String sig = HmacSigner.sign(canonical, SECRET);

        req.addHeader(SecurityConstants.HEADER_CLUSTER_ID, CLUSTER_ID);
        req.addHeader(SecurityConstants.HEADER_TIMESTAMP, String.valueOf(ts));
        req.addHeader(SecurityConstants.HEADER_NONCE, nonce);
        req.addHeader(SecurityConstants.HEADER_SIGN_ALG, SignatureAlgorithm.HMAC_SHA256.wireName());
        req.addHeader(SecurityConstants.HEADER_SIGNATURE, sig);
        return req;
    }

    private static Map<String, String> singletonQuery(String... kv) {
        if ((kv.length & 1) != 0) {
            throw new IllegalArgumentException("kv must be pairs");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    /** Captures whether the chain was invoked and the request that was passed. */
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
