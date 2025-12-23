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
package org.apache.seata.core.rpc.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterManager;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HttpDispatchHandlerTest {

    private HttpDispatchHandler handler;
    private EmbeddedChannel channel;
    private TestController testController = new TestController();

    @Mock
    private ChannelHandlerContext mockCtx;

    private FullHttpRequest testHttpRequest;
    private ExecutorService testExecutor;
    private EmbeddedChannel embeddedChannel;

    class TestController {
        public String handleRequest(String param) {
            return "Processed: " + param;
        }
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        MockitoAnnotations.openMocks(this);
        handler = new HttpDispatchHandler();
        channel = new EmbeddedChannel(handler);
        Method method = TestController.class.getMethod("handleRequest", String.class);
        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("param");
        ParamMetaData[] paramMetaDatas = new ParamMetaData[] {paramMetaData};

        HttpInvocation invocation = new HttpInvocation();
        invocation.setController(testController);
        invocation.setMethod(method);
        invocation.setPath("/test");
        invocation.setParamMetaData(paramMetaDatas);

        ControllerManager.addHttpInvocation(invocation);

        testHttpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "/test",
                Unpooled.copiedBuffer("{\"name\":\"test\"}", StandardCharsets.UTF_8));
        embeddedChannel = new EmbeddedChannel();
        when(mockCtx.channel()).thenReturn(embeddedChannel);
        when(mockCtx.pipeline()).thenReturn(embeddedChannel.pipeline());
        testExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void after() throws Exception {
        clearControllerManager();
        Field field2 = HttpRequestFilterManager.class.getDeclaredField("initialized");
        field2.setAccessible(true);
        field2.set(null, false);

        if (testExecutor != null) {
            testExecutor.shutdown();
            testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        }
        HttpFilterContext.clearCurrentContext();
        if (embeddedChannel != null) {
            embeddedChannel.close();
        }
    }

    @Test
    void testGetRequestWithParameters() throws Exception {

        HttpRequestFilterManager.initializeFilters();
        try {

            HttpRequest request =
                    new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?param=testValue");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.OK, response.status());
            String content = response.content().toString(StandardCharsets.UTF_8);
            assertTrue(content.contains("Processed: testValue"));
        } finally {
            clearControllerManager();
        }
    }

    @Test
    void testRequestToNonexistentPath() {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/notfound");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
        }
    }

    @Test
    void testHttpHeadMethod() {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/head");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
            assertEquals(0, response.content().readableBytes());
        }
    }

    @Test
    void testRequestFilteredByHttpRequestFilter() throws Exception {
        HttpRequestFilterChain mockFilterChain = mock(HttpRequestFilterChain.class);

        doThrow(new HttpRequestFilterException("Mock filter block"))
                .when(mockFilterChain)
                .doFilter(any());

        MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class);
        mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockFilterChain);
        mockedStatic.when(() -> HttpRequestFilterManager.getFilterChain(any())).thenReturn(mockFilterChain);
        try {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        } finally {
            mockedStatic.close();
        }
    }

    @Test
    void testRequestFilteredByXssScript() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected <script>"))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            mockedStatic
                    .when(() -> HttpRequestFilterManager.getFilterChain(any()))
                    .thenReturn(mockChain);
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?param=<script>alert(1)</script>");

            channel.writeInbound(request);
            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        }
    }

    @Test
    void testRequestFilteredByXssJavascriptUrl() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected javascript:"))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            mockedStatic
                    .when(() -> HttpRequestFilterManager.getFilterChain(any()))
                    .thenReturn(mockChain);
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?param=javascript:alert('XSS')");

            channel.writeInbound(request);
            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        }
    }

    @Test
    void testRequestFilteredByXssOnloadEvent() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected onload="))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            mockedStatic
                    .when(() -> HttpRequestFilterManager.getFilterChain(any()))
                    .thenReturn(mockChain);
            HttpRequest request =
                    new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?param=onload=alert(1)");

            channel.writeInbound(request);
            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        }
    }

    @Test
    void testPostRequestWithFormData() throws Exception {
        Method method = TestController.class.getMethod("handleRequest", String.class);
        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("param");
        ParamMetaData[] paramMetaDatas = new ParamMetaData[] {paramMetaData};

        HttpInvocation invocation = new HttpInvocation();
        invocation.setController(testController);
        invocation.setMethod(method);
        invocation.setPath("/testPost");
        invocation.setParamMetaData(paramMetaDatas);

        ControllerManager.addHttpInvocation(invocation);
        HttpRequestFilterManager.initializeFilters();
        try {

            String body = "param=postValue";
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.POST,
                    "/testPost",
                    io.netty.buffer.Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
            request.headers().set("Content-Type", "application/x-www-form-urlencoded");
            request.headers().set("Content-Length", body.length());

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.OK, response.status());
            String content = response.content().toString(StandardCharsets.UTF_8);
            assertTrue(content.contains("Processed"));
        } finally {
            clearControllerManager();
        }
    }

    @Test
    void testGetRequestWithConnectionClose() throws Exception {
        Method method = TestController.class.getMethod("handleRequest", String.class);
        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("param");
        ParamMetaData[] paramMetaDatas = new ParamMetaData[] {paramMetaData};

        HttpInvocation invocation = new HttpInvocation();
        invocation.setController(testController);
        invocation.setMethod(method);
        invocation.setPath("/testClose");
        invocation.setParamMetaData(paramMetaDatas);

        ControllerManager.addHttpInvocation(invocation);
        HttpRequestFilterManager.initializeFilters();
        try {

            HttpRequest request =
                    new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/testClose?param=closeValue");
            request.headers().set("Connection", "close");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.OK, response.status());
        } finally {
            clearControllerManager();
        }
    }

    @Test
    void testRequestWithUnexpectedExceptionDuringFilter() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new RuntimeException("Unexpected error")).when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            mockedStatic
                    .when(() -> HttpRequestFilterManager.getFilterChain(any()))
                    .thenReturn(mockChain);
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

            channel.writeInbound(request);

            FullHttpResponse response = waitForResponse(5000);
            assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
        }
    }

    @Test
    void testHttpFilterContextAvailableInThreadLocalDuringFilterExecution()
            throws InterruptedException, ExecutionException, TimeoutException {
        class MockHttpAspect {
            public void beforeFilter() {
                HttpFilterContext<?> context = HttpFilterContext.getCurrentContext();
                assertNotNull(context, "get request and response");

                HttpRequest request = (HttpRequest) context.getRequest();
                assertNotNull(request);
                assertEquals("/test", request.uri());
                assertEquals(HttpMethod.GET, request.method());
                assertEquals("test-invocation", context.getAttribute("testKey"));
            }

            public void afterFilter() {
                HttpFilterContext<?> context = HttpFilterContext.getCurrentContext();
                assertNotNull(context, "get request and response");

                FullHttpResponse response = (FullHttpResponse) context.getResponse();
                assertNotNull(response);
                assertEquals(HttpResponseStatus.OK, response.status());
            }

            public void afterFinally() {
                assertNull(HttpFilterContext.getCurrentContext(), "clean the request and response");
            }
        }

        MockHttpAspect mockAspect = new MockHttpAspect();

        HttpRequestFilter businessFilter = new HttpRequestFilter() {
            @Override
            public void doFilter(HttpFilterContext<?> ctx, HttpRequestFilterChain chain)
                    throws HttpRequestFilterException {
                mockAspect.beforeFilter();

                chain.doFilter(ctx);

                FullHttpResponse response =
                        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
                ctx.setResponse(response);

                mockAspect.afterFilter();
            }

            @Override
            public boolean shouldApply() {
                return false;
            }
        };

        HttpRequestFilterChain filterChain = new HttpRequestFilterChain(Arrays.asList(businessFilter), ctx -> {});

        HttpFilterContext<HttpRequest> context = new HttpFilterContext<>(
                testHttpRequest,
                mockCtx,
                true,
                HttpContext.HTTP_1_1,
                () -> new HttpRequestParamWrapper(null, null, null, null));
        context.setAttribute("testKey", "test-invocation");

        testExecutor
                .submit(() -> {
                    try {
                        HttpFilterContext.setCurrentContext(context);
                        filterChain.doFilter(context);
                    } catch (HttpRequestFilterException e) {
                        fail("Filter failure: " + e.getMessage());
                    } finally {
                        HttpFilterContext.clearCurrentContext();
                        mockAspect.afterFinally();
                    }
                })
                .get(1, TimeUnit.SECONDS);

        assertNull(HttpFilterContext.getCurrentContext());
    }

    private FullHttpResponse waitForResponse(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        FullHttpResponse response = null;

        while (response == null && (System.currentTimeMillis() - startTime) < timeoutMs) {
            response = channel.readOutbound();
            if (response == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for response", e);
                }
            }
        }

        return response;
    }

    private void clearControllerManager() throws Exception {
        Field field = ControllerManager.class.getDeclaredField("HTTP_CONTROLLER_MAP");
        field.setAccessible(true);
        Map<String, HttpInvocation> map = (java.util.Map<String, HttpInvocation>) field.get(null);
        map.clear();
    }
}
