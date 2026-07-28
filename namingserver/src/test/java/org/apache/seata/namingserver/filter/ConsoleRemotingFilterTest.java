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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link ConsoleRemotingFilter}.
 * <p>
 * Covers the GET/HEAD body-stripping regression on upstream proxy requests
 * and other proxy-forwarding behavior.
 */
// HttpMethod/MediaType static constants and NullBodyClientHttpRequestFactory inner anonymous class methods
// are treated as @Nullable by JDT nullness analysis; they are never null at runtime,
// so the type-safety warning is suppressed here uniformly.
@SuppressWarnings("null")
class ConsoleRemotingFilterTest {

    private NamingManager namingManager;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private ConsoleRemotingFilter filter;
    private FilterChain filterChain;

    private static final String NAMESPACE = "public";
    private static final String CLUSTER = "default";
    private static final String TARGET_HOST = "127.0.0.1";
    private static final int TARGET_PORT = 7091;

    @BeforeEach
    void setUp() {
        namingManager = mock(NamingManager.class);
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        filterChain = mock(FilterChain.class);
        filter = new ConsoleRemotingFilter(namingManager, restClientBuilder.build());

        // Set up a NamingServerNode with a control endpoint
        NamingServerNode node = new NamingServerNode();
        node.setControl(new Node.Endpoint(TARGET_HOST, TARGET_PORT, "http"));

        when(namingManager.getInstances(NAMESPACE, CLUSTER)).thenReturn(Collections.singletonList(node));
    }

    /**
     * Regression test: a GET request with a non-empty body should NOT forward
     * the body to the upstream server.
     * The body, Content-Length, and Transfer-Encoding headers must be stripped.
     */
    @Test
    void getRequestWithBodyShouldStripBody() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(HttpHeaders.CONTENT_LENGTH, "15");

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.CONTENT_LENGTH))
                .andExpect(headerDoesNotExist(HttpHeaders.TRANSFER_ENCODING))
                .andExpect(this::expectEmptyBody)
                .andRespond(withSuccess("{\"result\":\"ok\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
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

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.HEAD))
                .andExpect(this::expectEmptyBody)
                .andRespond(withStatus(HttpStatus.OK).contentType(org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
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

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "POST request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"created\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
    }

    @Test
    void putRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test-put\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("PUT");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "PUT request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"updated\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
    }

    @Test
    void deleteRequestShouldForwardBody() throws Exception {
        byte[] bodyBytes = "{\"data\":\"test-delete\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = createConsoleRequest("DELETE");
        request.setContent(bodyBytes);

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(clientHttpRequest -> assertEquals(
                        new String(bodyBytes, StandardCharsets.UTF_8),
                        new String(
                                ((MockClientHttpRequest) clientHttpRequest).getBodyAsBytes(), StandardCharsets.UTF_8),
                        "DELETE request body should be forwarded as-is"))
                .andRespond(
                        withSuccess("{\"result\":\"deleted\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();
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
    }

    /**
     * Upstream returning an HTML body with application/json Content-Type should
     * be replaced with a 502 error response.
     */
    @Test
    void nonJsonBodyWithJsonContentTypeShouldReturn502() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "<html><script>alert('xss')</script></html>",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(502, response.getStatus(), "Should return 502 when upstream body is not valid JSON");
        String body = response.getContentAsString();
        assertEquals("{\"error\":\"Upstream returned invalid response body\"}", body);
    }

    @Test
    void non2xxResponseShouldStillBeProxied() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"upstream failed\"}"));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(502, response.getStatus(), "Proxy mode should preserve upstream non-2xx status codes");
        assertEquals("{\"error\":\"upstream failed\"}", response.getContentAsString());
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

    private void expectEmptyBody(org.springframework.http.client.ClientHttpRequest request) throws IOException {
        assertEquals(
                0,
                ((MockClientHttpRequest) request).getBodyAsBytes().length,
                "GET/HEAD request body should be stripped");
    }

    /**
     * Regression test: when the upstream returns 204 No Content (no body),
     * the proxy should pass through 204 rather than returning 500.
     * <p>
     * Background: {@code executeProxyRequest} calls
     * {@code StreamUtils.copyToByteArray(response.getBody())} for non-HEAD requests.
     * Some HTTP client implementations (e.g. JDK HTTP Client) may return {@code null}
     * from {@code getBody()} for 204/304 no-body responses.
     * Spring 6.x {@code StreamUtils.copyToByteArray(null)} has an internal null guard,
     * but the defensive coding style should remain consistent with
     * {@code ConsoleLocalServiceImpl} and {@code ConsoleRemoteServiceImpl},
     * and guard against future Spring version behaviour changes or other HTTP client differences.
     */
    @Test
    void upstreamNoContentShouldReturn204() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("DELETE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(204, response.getStatus(), "Upstream 204 No Content should be proxied as 204, not 500");
        assertEquals(0, response.getContentAsByteArray().length, "204 response should have no body");
    }

    /**
     * Regression test: force {@code getBody()} to return {@code null} via a custom
     * {@link ClientHttpRequestFactory} to simulate the real behaviour of JDK HTTP Client
     * and similar implementations for no-body responses.
     * After fix: a null body stream is treated as an empty byte array and 204 is proxied.
     * Before fix (if {@code StreamUtils} did not handle null): NPE → caught → 500.
     */
    @Test
    void upstreamNullBodyStreamShouldReturn204NotServerError() throws Exception {
        // Build a RestClient whose getBody() returns null, simulating extreme behaviour
        // of real HTTP clients for 204 responses.
        RestClient nullBodyRestClient = RestClient.builder()
                .requestFactory(new NullBodyClientHttpRequestFactory(HttpStatus.NO_CONTENT))
                .build();
        ConsoleRemotingFilter filterWithNullBody = new ConsoleRemotingFilter(namingManager, nullBodyRestClient);

        MockHttpServletRequest request = createConsoleRequest("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filterWithNullBody.doFilter(request, response, filterChain);

        assertEquals(204, response.getStatus(), "Upstream 204 with null body should be proxied as 204, not 500");
        assertEquals(0, response.getContentAsByteArray().length, "204 null body response should have no response body");
    }

    /**
     * Regression test: when the upstream returns 304 Not Modified (no body),
     * the proxy should pass through 304.
     */
    @Test
    void upstreamNotModifiedShouldReturn304() throws Exception {
        MockHttpServletRequest request = createConsoleRequest("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        server.expect(once(), requestTo("http://127.0.0.1:7091/api/v1/console/globalSession/query"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_MODIFIED));

        filter.doFilter(request, response, filterChain);
        server.verify();

        assertEquals(304, response.getStatus(), "Upstream 304 Not Modified should be proxied as 304, not 500");
        assertEquals(0, response.getContentAsByteArray().length, "304 response should have no body");
    }

    /**
     * A {@link ClientHttpRequestFactory} whose responses always return {@code null}
     * from {@code getBody()}, simulating the behaviour of JDK HTTP Client and similar
     * implementations for no-body responses.
     */
    private static final class NullBodyClientHttpRequestFactory implements ClientHttpRequestFactory {

        private final HttpStatus status;

        NullBodyClientHttpRequestFactory(HttpStatus status) {
            this.status = status;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new ClientHttpRequest() {
                private final HttpHeaders headers = new HttpHeaders();

                @Override
                public ClientHttpResponse execute() {
                    // Simulate an upstream response with the given status code where getBody() is null.
                    return new MockClientHttpResponse(new byte[0], status) {
                        @Override
                        public InputStream getBody() {
                            // Force null return to simulate JDK HTTP Client behaviour for no-body responses.
                            return null;
                        }

                        @Override
                        public void close() {
                            // Skip close when getBody() is null to avoid NullPointerException.
                        }
                    };
                }

                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }

                @Override
                public URI getURI() {
                    return uri;
                }

                @Override
                public HttpMethod getMethod() {
                    return httpMethod;
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return Collections.emptyMap();
                }

                @Override
                public OutputStream getBody() throws IOException {
                    return OutputStream.nullOutputStream();
                }
            };
        }
    }
}
