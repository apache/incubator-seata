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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.common.security.CanonicalRequest;
import org.apache.seata.common.security.SecurityConstants;
import org.apache.seata.common.security.SignatureAlgorithm;
import org.apache.seata.common.security.SignatureVerifier;
import org.apache.seata.common.security.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * TC-side inbound filter that authenticates requests coming from NamingServer.
 *
 * <p>Structure mirrors {@code namingserver.security.SecurityFilter} — same pipeline:
 * exclude → parse headers → identity lookup → buffer body → verify signature → authorise route.
 * The two live in separate modules because they have different dependency footprints, but a
 * mismatch in behaviour will show up in the shared integration tests.
 *
 * <p>The filter registers at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} in
 * the auto-configuration so it runs before any of the existing TC filters
 * (e.g. {@code XSSHttpRequestFilter}, {@code RaftRequestFilter}).
 */
public class SeataServerAuthFilter implements Filter {

    /** Exposes the authenticated caller on the request attributes. */
    public static final String ATTR_CALLER = "seata.server.security.caller";

    /** Machine-readable error code on the response, mirroring the NamingServer side. */
    public static final String RESP_HEADER_ERROR = "X-Seata-Auth-Error";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataServerAuthFilter.class);

    private final ServerSecurityProperties properties;

    private final AllowedCallerRegistry registry;

    private final SignatureVerifier verifier;

    private final RouteAuthorizer routeAuthorizer;

    public SeataServerAuthFilter(
            ServerSecurityProperties properties,
            AllowedCallerRegistry registry,
            SignatureVerifier verifier,
            RouteAuthorizer routeAuthorizer) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.routeAuthorizer = Objects.requireNonNull(routeAuthorizer, "routeAuthorizer");
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

        String path = httpReq.getRequestURI();
        String method = httpReq.getMethod();

        // 1. Route filter: only intercept paths TC deems sensitive. If the route is not
        //    on the protected list, pass through untouched — TC has other filters for the
        //    Netty side, static assets, etc.
        CallerPermission required = routeAuthorizer.requiredPermission(method, path);
        if (required == null || isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract security headers.
        String clusterId = httpReq.getHeader(SecurityConstants.HEADER_CLUSTER_ID);
        String tsStr = httpReq.getHeader(SecurityConstants.HEADER_TIMESTAMP);
        String nonce = httpReq.getHeader(SecurityConstants.HEADER_NONCE);
        String algName = httpReq.getHeader(SecurityConstants.HEADER_SIGN_ALG);
        String sig = httpReq.getHeader(SecurityConstants.HEADER_SIGNATURE);

        if (isBlank(clusterId) || isBlank(tsStr) || isBlank(nonce) || isBlank(sig)) {
            reject(
                    httpReq,
                    httpResp,
                    chain,
                    SecurityConstants.ErrorCode.MISSING_SIGNATURE,
                    "one or more required security headers are missing",
                    null);
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            reject(
                    httpReq,
                    httpResp,
                    chain,
                    SecurityConstants.ErrorCode.BAD_REQUEST,
                    "timestamp header is not a valid long",
                    null);
            return;
        }
        SignatureAlgorithm alg;
        try {
            alg = algName == null ? SignatureAlgorithm.HMAC_SHA256 : SignatureAlgorithm.fromWireName(algName);
        } catch (IllegalArgumentException e) {
            reject(httpReq, httpResp, chain, SecurityConstants.ErrorCode.UNSUPPORTED_ALG, e.getMessage(), null);
            return;
        }

        // 3. Identity lookup.
        Optional<AllowedCaller> callerOpt = registry.find(clusterId);
        if (!callerOpt.isPresent()) {
            reject(
                    httpReq,
                    httpResp,
                    chain,
                    SecurityConstants.ErrorCode.UNKNOWN_CLUSTER_ID,
                    "cluster-id not in allowed-callers: " + clusterId,
                    null);
            return;
        }
        AllowedCaller caller = callerOpt.get();

        // 4. Buffer body — needed so the verifier + controller can both read it.
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpReq);

        // 5. Verify signature.
        CanonicalRequest canonical = CanonicalRequest.builder()
                .method(method)
                .path(path)
                .queryParams(collectQuery(httpReq))
                .clusterId(clusterId)
                .timestampMillis(ts)
                .nonce(nonce)
                .algorithm(alg)
                .body(wrapper.getCachedBody())
                .build();

        VerificationResult verdict = verifier.verify(canonical, caller.getSecret(), sig);
        if (!verdict.isSuccess()) {
            reject(wrapper, httpResp, chain, verdict.getErrorCode(), verdict.getMessage(), caller);
            return;
        }

        // 6. Route-level permission — signature verified, but does the caller hold the right?
        if (!caller.hasPermission(required)) {
            reject(
                    wrapper,
                    httpResp,
                    chain,
                    SecurityConstants.ErrorCode.FORBIDDEN,
                    "caller " + clusterId + " lacks permission " + required,
                    caller);
            return;
        }

        // 7. Success.
        wrapper.setAttribute(ATTR_CALLER, caller);
        chain.doFilter(wrapper, httpResp);
    }

    private boolean isExcluded(String path) {
        List<String> excluded = properties.getExcludePaths();
        if (excluded == null || excluded.isEmpty()) {
            return false;
        }
        for (String pattern : excluded) {
            if (matches(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        return pattern.equals(path);
    }

    private void reject(
            HttpServletRequest req,
            HttpServletResponse resp,
            FilterChain chain,
            SecurityConstants.ErrorCode code,
            String message,
            AllowedCaller caller)
            throws IOException, ServletException {
        ServerSecurityProperties.Mode mode = properties.getMode();
        if (mode == ServerSecurityProperties.Mode.WARN) {
            LOGGER.warn(
                    "[seata-server][security][WARN] {} {} rejected code={} msg={} caller={}",
                    req.getMethod(),
                    req.getRequestURI(),
                    code,
                    message,
                    caller == null ? "unknown" : caller.getId());
            if (caller != null) {
                req.setAttribute(ATTR_CALLER, caller);
            }
            chain.doFilter(req, resp);
            return;
        }
        int status = (code == SecurityConstants.ErrorCode.FORBIDDEN)
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_UNAUTHORIZED;
        LOGGER.warn(
                "[seata-server][security][ENFORCE] {} {} status={} code={} msg={} caller={}",
                req.getMethod(),
                req.getRequestURI(),
                status,
                code,
                message,
                caller == null ? "unknown" : caller.getId());
        resp.setHeader(RESP_HEADER_ERROR, code.name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(status);
        resp.getWriter().write("{\"code\":\"" + code.name() + "\",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private static Map<String, String> collectQuery(HttpServletRequest req) {
        Map<String, String> params = new LinkedHashMap<>();
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, req.getParameter(name));
        }
        return params;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private static String escapeJson(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Same wrapper idea as on the NamingServer side. See that class for design notes. */
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
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener l) {
                    /* no-op */
                }

                @Override
                public int read() {
                    return stream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        private static byte[] readAll(HttpServletRequest request) throws IOException {
            try (ServletInputStream in = request.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
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
