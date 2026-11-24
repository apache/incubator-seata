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
package org.apache.seata.namingserver.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.apache.seata.namingserver.contants.NamingConstant.CONSOLE_PATTERN;

public class ConsoleRemotingFilter implements Filter {

    private final NamingManager namingManager;

    private final RestClient restClient;

    private final Pattern urlPattern = Pattern.compile(CONSOLE_PATTERN);

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleRemotingFilter.class);

    public ConsoleRemotingFilter(NamingManager namingManager, RestClient restClient) {
        this.namingManager = namingManager;
        this.restClient = restClient;
    }

    /**
     * Check whether the proxied Content-Type is safe (will not be rendered as
     * HTML / XML by the browser).  Only allow known-safe MIME types through
     * (allowlist approach); everything else is replaced with
     * {@code application/json}.
     */
    private static boolean isSafeContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        // Extract the primary MIME type (ignore parameters such as charset)
        int semicolonIdx = lower.indexOf(';');
        String mimeType = (semicolonIdx >= 0 ? lower.substring(0, semicolonIdx) : lower).trim();
        return "application/json".equals(mimeType)
                || "text/plain".equals(mimeType)
                || "application/octet-stream".equals(mimeType);
    }

    /**
     * Validate that the given byte array looks like well-formed JSON
     * (starts with '{' or '[' after trimming leading whitespace).
     * This is a lightweight sanity check to prevent forwarding
     * arbitrary HTML / script payloads disguised as JSON.
     */
    private static boolean looksLikeJson(byte[] body) {
        if (body == null || body.length == 0) {
            return true;
        }
        int i = 0;
        // Skip optional UTF-8 BOM (0xEF, 0xBB, 0xBF)
        if (body.length >= 3 && (body[0] & 0xFF) == 0xEF && (body[1] & 0xFF) == 0xBB && (body[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        // skip leading whitespace (including Unicode NBSP / BOM that survived as whitespace)
        while (i < body.length && (body[i] == ' ' || body[i] == '\t' || body[i] == '\r' || body[i] == '\n')) {
            i++;
        }
        if (i >= body.length) {
            return true;
        }
        byte first = body[i];
        return first == '{'
                || first == '['
                || first == '"'
                || first == 't'
                || first == 'f'
                || first == 'n'
                || (first >= '0' && first <= '9')
                || first == '-';
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            if (urlPattern
                    .matcher(((HttpServletRequest) servletRequest).getRequestURI())
                    .matches()) {
                CachedBodyHttpServletRequest request =
                        new CachedBodyHttpServletRequest((HttpServletRequest) servletRequest);
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                String namespace = request.getHeader("x-seata-namespace");
                String cluster = request.getHeader("x-seata-cluster");
                String vgroup = request.getParameter("vgroup");
                if (StringUtils.isNotBlank(namespace)
                        && (StringUtils.isNotBlank(cluster) || StringUtils.isNotBlank(vgroup))) {
                    List<NamingServerNode> list = Collections.emptyList();
                    if (StringUtils.isNotBlank(vgroup)) {
                        list = namingManager.getInstancesByVgroupAndNamespace(
                                namespace,
                                vgroup,
                                StringUtils.equalsIgnoreCase(request.getMethod(), HttpMethod.GET.name()));
                    } else if (StringUtils.isNotBlank(cluster)) {
                        list = namingManager.getInstances(namespace, cluster);
                    }
                    if (CollectionUtils.isNotEmpty(list)) {
                        // Randomly select a node from the list
                        NamingServerNode node = (NamingServerNode)
                                list.get(ThreadLocalRandom.current().nextInt(list.size()));
                        Node.Endpoint controlEndpoint = node.getControl();
                        if (controlEndpoint != null) {
                            // Construct the target URL
                            String targetUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort()
                                    + request.getRequestURI()
                                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

                            // Copy headers from the original request, stripping hop-by-hop
                            // headers (RFC 7230 §6.1) and Host (so the client library sets
                            // the correct value for the upstream target).
                            HttpHeaders headers = new HttpHeaders();
                            if (node.getRole() == ClusterRole.LEADER) {
                                headers.add(RAFT_GROUP_HEADER, node.getUnit());
                            }
                            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                                if (!HttpHeaders.HOST.equalsIgnoreCase(headerName)
                                        && !HttpHeaders.CONNECTION.equalsIgnoreCase(headerName)
                                        && !"Keep-Alive".equalsIgnoreCase(headerName)
                                        && !HttpHeaders.PROXY_AUTHENTICATE.equalsIgnoreCase(headerName)
                                        && !HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(headerName)
                                        && !HttpHeaders.TE.equalsIgnoreCase(headerName)
                                        && !HttpHeaders.TRAILER.equalsIgnoreCase(headerName)
                                        && !HttpHeaders.UPGRADE.equalsIgnoreCase(headerName)) {
                                    String headerValue = request.getHeader(headerName);
                                    // headerName comes from the Servlet API Enumeration, which JDT treats as @Nullable.
                                    // The Servlet specification guarantees header names are never null;
                                    // this defensive null guard is added solely to suppress the type-safety warning.
                                    if (headerName != null && headerValue != null) {
                                        headers.add(headerName, headerValue);
                                    }
                                }
                            });

                            // Create the HttpEntity with headers and body
                            HttpMethod httpMethod;
                            try {
                                httpMethod = HttpMethod.valueOf(Objects.requireNonNull(request.getMethod()));
                            } catch (IllegalArgumentException ex) {
                                LOGGER.error("Unsupported HTTP method: {}", request.getMethod(), ex);
                                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                                return;
                            }

                            // GET/HEAD methods should not have a body; other methods may include a body as needed.
                            HttpEntity<byte[]> httpEntity;
                            if (HttpMethod.GET.equals(httpMethod) || HttpMethod.HEAD.equals(httpMethod)) {
                                headers.remove(HttpHeaders.CONTENT_LENGTH);
                                headers.remove(HttpHeaders.TRANSFER_ENCODING);
                                // headers-only
                                httpEntity = new HttpEntity<>(headers);
                            } else {
                                byte[] body = request.getCachedBody();
                                if (body == null || body.length == 0) {
                                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                                    headers.remove(HttpHeaders.TRANSFER_ENCODING);
                                    // headers-only for empty body
                                    httpEntity = new HttpEntity<>(headers);
                                } else {
                                    // Remove potentially stale length/transfer headers and let the client recompute
                                    // them
                                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                                    headers.remove(HttpHeaders.TRANSFER_ENCODING);
                                    httpEntity = new HttpEntity<>(body, headers);
                                }
                            }

                            try {
                                ResponseEntity<byte[]> responseEntity =
                                        executeProxyRequest(URI.create(targetUrl), httpMethod, httpEntity);
                                // Copy headers from proxied response, skipping hop-by-hop and headers we manage
                                // ourselves to mitigate
                                // security risks from Content-Type manipulation
                                responseEntity.getHeaders().forEach((key, value) -> {
                                    if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(key)
                                            && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key)
                                            && !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(key)
                                            && !"X-Content-Type-Options".equalsIgnoreCase(key)
                                            && !HttpHeaders.CONNECTION.equalsIgnoreCase(key)
                                            && !"Keep-Alive".equalsIgnoreCase(key)
                                            && !HttpHeaders.PROXY_AUTHENTICATE.equalsIgnoreCase(key)
                                            && !HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(key)
                                            && !HttpHeaders.TE.equalsIgnoreCase(key)
                                            && !HttpHeaders.TRAILER.equalsIgnoreCase(key)
                                            && !HttpHeaders.UPGRADE.equalsIgnoreCase(key)) {
                                        value.forEach(v -> response.addHeader(key, v));
                                    }
                                });
                                // Force a safe Content-Type: reject HTML/XML types that could
                                // execute scripts; fall back to application/json
                                String proxiedContentType =
                                        responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                                String safeContentType;
                                if (proxiedContentType != null && isSafeContentType(proxiedContentType)) {
                                    safeContentType = proxiedContentType;
                                } else {
                                    safeContentType = "application/json;charset=UTF-8";
                                }
                                response.setContentType(safeContentType);
                                response.setHeader("X-Content-Type-Options", "nosniff");
                                response.setStatus(
                                        responseEntity.getStatusCode().value());
                                byte[] responseBody = responseEntity.getBody();
                                // HEAD responses must not include a message body (RFC 7231 §4.3.2)
                                if (!HttpMethod.HEAD.equals(httpMethod)
                                        && responseBody != null
                                        && responseBody.length > 0) {
                                    // For JSON content type, validate that the body actually looks
                                    // like JSON to prevent XSS via crafted upstream responses
                                    if (safeContentType.toLowerCase(Locale.ROOT).contains("application/json")
                                            && !looksLikeJson(responseBody)) {
                                        LOGGER.warn(
                                                "Upstream returned non-JSON body for Content-Type {}, replacing with error response",
                                                safeContentType);
                                        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                                        response.setContentType("application/json;charset=UTF-8");
                                        responseBody = "{\"error\":\"Upstream returned invalid response body\"}"
                                                .getBytes(StandardCharsets.UTF_8);
                                    }
                                    try (ServletOutputStream outputStream = response.getOutputStream()) {
                                        outputStream.write(responseBody);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        // Client likely disconnected (broken pipe); log at debug
                                        // level and do NOT attempt sendError – the response may
                                        // already be committed.
                                        LOGGER.debug(
                                                "Failed to write proxy response body (client disconnect?): {}",
                                                e.getMessage(),
                                                e);
                                    }
                                }
                            } catch (Exception ex) {
                                LOGGER.error(ex.getMessage(), ex);
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            }
                            return;
                        }
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private ResponseEntity<byte[]> executeProxyRequest(
            URI targetUrl, HttpMethod httpMethod, HttpEntity<byte[]> httpEntity) {
        RestClient.RequestBodySpec requestSpec = restClient
                .method(Objects.requireNonNull(httpMethod))
                .uri(Objects.requireNonNull(targetUrl))
                .headers(headers -> headers.addAll(httpEntity.getHeaders()));
        RestClient.RequestHeadersSpec<?> exchangeSpec = requestSpec;
        byte[] requestBody = httpEntity.getBody();
        if (requestBody != null
                && requestBody.length > 0
                && !HttpMethod.GET.equals(httpMethod)
                && !HttpMethod.HEAD.equals(httpMethod)) {
            exchangeSpec = requestSpec.body(requestBody);
        }
        return exchangeSpec.exchange((req, response) -> {
            byte[] bodyBytes;
            if (HttpMethod.HEAD.equals(httpMethod)) {
                // HEAD responses must not contain a message body per RFC 7231 §4.3.2.
                bodyBytes = null;
            } else {
                // For 204/304 and similar no-body responses, some HTTP client implementations
                // (e.g. JDK HTTP Client) may return null from getBody().
                // Treat a null stream as an empty byte array defensively,
                // consistent with the null guards in ConsoleLocalServiceImpl and ConsoleRemoteServiceImpl.
                InputStream bodyStream = response.getBody();
                bodyBytes = (bodyStream != null) ? StreamUtils.copyToByteArray(bodyStream) : new byte[0];
            }
            return new ResponseEntity<>(bodyBytes, response.getHeaders(), response.getStatusCode());
        });
    }
}
