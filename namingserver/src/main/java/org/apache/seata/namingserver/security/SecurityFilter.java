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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.apache.seata.common.security.SignatureVerifier;
import org.apache.seata.common.security.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that authenticates every inbound HTTP request against a shared secret and
 * a per-caller permission set.
 *
 * <p>Life-cycle of a single request:
 * <ol>
 *   <li>Path is compared against {@link SecurityProperties#getExcludePaths()}; matches are
 *       passed straight through (e.g. {@code /naming/v1/health}).</li>
 *   <li>Extract security headers. If missing:
 *       <ul>
 *         <li>{@code WARN} mode → log and pass through.</li>
 *         <li>{@code ENFORCE} mode → reject with 401.</li>
 *       </ul>
 *   </li>
 *   <li>Look up {@link ClusterIdentity} by {@code X-Seata-Cluster-Id}. Unknown → 401.</li>
 *   <li>Buffer the request body so both the signature verifier and downstream controllers can
 *       read it — {@code ServletInputStream} is single-shot by default.</li>
 *   <li>Run {@link SignatureVerifier} — freshness, nonce, HMAC.</li>
 *   <li>Run {@link PermissionChecker} — route → permission and allow-list scoping.</li>
 *   <li>On success, expose the identity in the request attribute {@link #ATTR_IDENTITY} for
 *       downstream code (e.g. {@code ConsoleRemotingFilter} choosing the target TC) and pass on.</li>
 * </ol>
 */
public class SecurityFilter implements Filter {

    /** Request attribute name under which the authenticated identity is exposed. */
    public static final String ATTR_IDENTITY = "seata.security.identity";

    /** Response header used to surface the error code for machine consumers. */
    public static final String RESP_HEADER_ERROR = "X-Seata-Auth-Error";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    private final SecurityProperties properties;

    private final ClusterIdentityRegistry registry;

    private final SignatureVerifier verifier;

    private final PermissionChecker permissionChecker;

    public SecurityFilter(SecurityProperties properties,
                          ClusterIdentityRegistry registry,
                          SignatureVerifier verifier,
                          PermissionChecker permissionChecker) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Fast-path bypass: health checks and any other whitelisted path.
        if (isExcluded(httpReq.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String clusterId = httpReq.getHeader(SecurityConstants.HEADER_CLUSTER_ID);
        String timestamp = httpReq.getHeader(SecurityConstants.HEADER_TIMESTAMP);
        String nonce = httpReq.getHeader(SecurityConstants.HEADER_NONCE);
        String algName = httpReq.getHeader(SecurityConstants.HEADER_SIGN_ALG);
        String signature = httpReq.getHeader(SecurityConstants.HEADER_SIGNATURE);

        // ---- Missing headers ----
        if (isBlank(clusterId) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            handleFailure(httpReq, httpResp, chain,
                    SecurityConstants.ErrorCode.MISSING_SIGNATURE,
                    "one or more required security headers are missing", null);
            return;
        }

        // ---- Parse timestamp / algorithm ----
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            handleFailure(httpReq, httpResp, chain, SecurityConstants.ErrorCode.BAD_REQUEST,
                    "timestamp header is not a valid long", null);
            return;
        }
        SignatureAlgorithm algorithm;
        try {
            algorithm = algName == null ? SignatureAlgorithm.HMAC_SHA256
                    : SignatureAlgorithm.fromWireName(algName);
        } catch (IllegalArgumentException e) {
            handleFailure(httpReq, httpResp, chain, SecurityConstants.ErrorCode.UNSUPPORTED_ALG,
                    e.getMessage(), null);
            return;
        }

        // ---- Identity lookup ----
        Optional<ClusterIdentity> identityOpt = registry.find(clusterId);
        if (!identityOpt.isPresent()) {
            handleFailure(httpReq, httpResp, chain, SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID,
                    "no identity registered for cluster-id: " + clusterId, null);
            return;
        }
        ClusterIdentity identity = identityOpt.get();

        // ---- Buffer body so the verifier and the downstream controller can both read it. ----
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpReq);
        byte[] body = wrapper.getCachedBody();

        // ---- Verify signature ----
        CanonicalRequest canonical = CanonicalRequest.builder()
                .method(httpReq.getMethod())
                .path(httpReq.getRequestURI())
                .queryParams(parseQuery(httpReq))
                .clusterId(clusterId)
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(algorithm)
                .body(body)
                .build();

        VerificationResult result = verifier.verify(canonical, identity.getSecret(), signature);
        if (!result.isSuccess()) {
            handleFailure(wrapper, httpResp, chain, result.getErrorCode(), result.getMessage(), identity);
            return;
        }

        // ---- Permission check ----
        String namespace = httpReq.getParameter("namespace");
        String cluster = httpReq.getParameter("clusterName");
        String vgroup = httpReq.getParameter("vGroup");
        boolean allowed = permissionChecker.check(identity, httpReq.getMethod(),
                httpReq.getRequestURI(), namespace, cluster, vgroup);
        if (!allowed) {
            handleFailure(wrapper, httpResp, chain, SecurityConstants.ErrorCode.FORBIDDEN,
                    "caller " + clusterId + " lacks permission for " + httpReq.getMethod()
                            + " " + httpReq.getRequestURI(), identity);
            return;
        }

        // ---- Success: hand off to the rest of the chain ----
        wrapper.setAttribute(ATTR_IDENTITY, identity);
        chain.doFilter(wrapper, httpResp);
    }

    private boolean isExcluded(String uri) {
        List<String> excluded = properties.getExcludePaths();
        if (excluded == null || excluded.isEmpty()) {
            return false;
        }
        for (String pattern : excluded) {
            if (matches(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /** Minimal Ant-style matcher: supports trailing {@code /**} and exact match. */
    static boolean matches(String pattern, String uri) {
        if (pattern == null || uri == null) {
            return false;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return uri.startsWith(prefix);
        }
        return pattern.equals(uri);
    }

    private Map<String, String> parseQuery(HttpServletRequest req) {
        Map<String, String> params = new LinkedHashMap<>();
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, req.getParameter(name));
        }
        return params;
    }

    private void handleFailure(HttpServletRequest req,
                               HttpServletResponse resp,
                               FilterChain chain,
                               SecurityConstants.ErrorCode code,
                               String message,
                               ClusterIdentity identity) throws IOException, ServletException {
        SecurityProperties.Mode mode = properties.getMode();
        if (mode == SecurityProperties.Mode.WARN) {
            LOGGER.warn("[security][WARN] {} {} rejected: code={} msg={} caller={}",
                    req.getMethod(), req.getRequestURI(), code, message,
                    identity == null ? "unknown" : identity.getId());
            if (identity != null) {
                req.setAttribute(ATTR_IDENTITY, identity);
            }
            chain.doFilter(req, resp);
            return;
        }
        // ENFORCE
        int status = (code == SecurityConstants.ErrorCode.FORBIDDEN)
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_UNAUTHORIZED;
        LOGGER.warn("[security][ENFORCE] {} {} rejected status={} code={} msg={} caller={}",
                req.getMethod(), req.getRequestURI(), status, code, message,
                identity == null ? "unknown" : identity.getId());
        resp.setHeader(RESP_HEADER_ERROR, code.name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(status);
        resp.getWriter().write("{\"code\":\"" + code.name() + "\",\"message\":\""
                + escapeJson(message) + "\"}");
    }

    private static String escapeJson(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Wraps a request so its body can be read multiple times: once here for signing, once again
     * by Spring MVC when it binds the {@code @RequestBody}.
     */
    static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = readAll(request);
        }

        byte[] getCachedBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return stream.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener listener) { /* not used */ }
                @Override
                public int read() { return stream.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        private static byte[] readAll(HttpServletRequest request) throws IOException {
            try (ServletInputStream in = request.getInputStream()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        }
    }
}
