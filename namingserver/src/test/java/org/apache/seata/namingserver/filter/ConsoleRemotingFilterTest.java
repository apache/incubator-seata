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

import jakarta.servlet.FilterChain;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.namingserver.manager.NamingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConsoleRemotingFilter}.
 * <p>
 * Covers the GET/HEAD body-stripping regression (OkHttp IllegalArgumentException)
 * and other proxy-forwarding behavior.
 */
class ConsoleRemotingFilterTest {

    private NamingManager namingManager;
    private RestTemplate restTemplate;
    private ConsoleRemotingFilter filter;
    private FilterChain filterChain;

    private static final String NAMESPACE = "public";
    private static final String CLUSTER = "default";
    private static final String TARGET_HOST = "127.0.0.1";
    private static final int TARGET_PORT = 7091;

    @BeforeEach
    void setUp() {
        namingManager = mock(NamingManager.class);
        restTemplate = mock(RestTemplate.class);
        filterChain = mock(FilterChain.class);
        filter = new ConsoleRemotingFilter(namingManager, restTemplate);

        // Set up a NamingServerNode with a control endpoint
        NamingServerNode node = new NamingServerNode();
        node.setControl(new Node.Endpoint(TARGET_HOST, TARGET_PORT, "http"));

        when(namingManager.getInstances(NAMESPACE, CLUSTER))
                .thenReturn(Collections.singletonList(node));
    }

    /**
     * Regression test: a GET request with a non-empty body should NOT forward
     * the body to the upstream server (to avoid OkHttp's IllegalArgumentException).
     * The body, Content-Length, and Transfer-Encoding headers must be stripped.
     */
    @Test
    void getRequestWithBodyShouldStripBody() throws Exception {
        // Prepare a GET request with a body (some clients/frameworks may attach one)
        MockHttpServletRequest request = createConsoleRequest("GET");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(HttpHeaders.CONTENT_LENGTH, "15");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Stub RestTemplate to return a successful JSON response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        ResponseEntity<byte[]> upstreamResponse = new ResponseEntity<>(
                "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8),
                responseHeaders,
                HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(upstreamResponse);

        filter.doFilter(request, response, filterChain);

        // Capture the HttpEntity sent to RestTemplate
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), entityCaptor.capture(), eq(byte[].class));

        HttpEntity<byte[]> capturedEntity = entityCaptor.getValue();
        // Body must be null (stripped for GET)
        assertNull(capturedEntity.getBody(), "GET request body should be stripped (null)");
        // Content-Length and Transfer-Encoding headers must not be forwarded
        assertNull(capturedEntity.getHeaders().get(HttpHeaders.CONTENT_LENGTH),
                "Content-Length header should be removed for GET");
        assertNull(capturedEntity.getHeaders().get(HttpHeaders.TRANSFER_ENCODING),
                "Transfer-Encoding header should be removed for GET");

        // Verify filterChain was NOT invoked (proxied)
        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    /**
     * HEAD request should also strip the body, same as GET.
     */
    @Test
    void headRequestShouldStripBody() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("HEAD");
        request.setContent("some body".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json");
        ResponseEntity<byte[]> upstreamResponse = new ResponseEntity<>(
                null, responseHeaders, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(upstreamResponse);

        filter.doFilter(request, response, filterChain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.HEAD), entityCaptor.capture(), eq(byte[].class));

        assertNull(entityCaptor.getValue().getBody(), "HEAD request body should be stripped (null)");
        verify(filterChain, never()).doFilter(any(), any());
    }

    /**
     * POST request should forward the body as-is.
     */
    @Test
    void postRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("POST");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        ResponseEntity<byte[]> upstreamResponse = new ResponseEntity<>(
                "{\"result\":\"created\"}".getBytes(StandardCharsets.UTF_8),
                responseHeaders,
                HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(upstreamResponse);

        filter.doFilter(request, response, filterChain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(byte[].class));

        byte[] capturedBody = entityCaptor.getValue().getBody();
        assertNotNull(capturedBody, "POST body should not be null");
        assertEquals(new String(bodyBytes, StandardCharsets.UTF_8),
                new String(capturedBody, StandardCharsets.UTF_8),
                "POST request body should be forwarded as-is");
    }

    /**
     * Non-matching URL should pass through the filter chain without proxying.
     */
    @Test
    void nonConsoleUrlShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/other/endpoint");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(restTemplate, never()).exchange(any(URI.class), any(), any(HttpEntity.class), eq(byte[].class));
    }

    /**
     * Upstream returning an HTML body with application/json Content-Type should
     * be replaced with a 502 error response.
     */
    @Test
    void nonJsonBodyWithJsonContentTypeShouldReturn502() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json");
        // Upstream sends HTML disguised as JSON
        byte[] htmlBody = "<html><script>alert('xss')</script></html>".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<byte[]> upstreamResponse = new ResponseEntity<>(
                htmlBody, responseHeaders, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(upstreamResponse);

        filter.doFilter(request, response, filterChain);

        assertEquals(502, response.getStatus(),
                "Should return 502 when upstream body is not valid JSON");
        String body = response.getContentAsString();
        assertEquals("{\"error\":\"Upstream returned invalid response body\"}", body);
    }

    /**
     * Helper: create a MockHttpServletRequest that matches the console URL pattern
     * and includes the required namespace/cluster headers.
     */
    private MockHttpServletRequest createConsoleRequest(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/console/globalSession/query");
        request.addHeader("x-seata-namespace", NAMESPACE);
        request.addHeader("x-seata-cluster", CLUSTER);
        return request;
    }
}

