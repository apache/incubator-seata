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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class Http2HttpHandlerTest {
    private Http2HttpHandler handler;
    private EmbeddedChannel channel;
    private TestController testController = new TestController();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static class TestController {
        public String handleRequest(String param) {
            return "Processed: " + param;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        handler = new Http2HttpHandler();
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
    }

    private Http2StreamFrame waitForHttp2Response(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Http2StreamFrame response = null;
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

    @Test
    void testHttp2GetRequestWithParameters() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            Http2Headers headers = new DefaultHttp2Headers();
            headers.method("GET");
            headers.path("/test?param=testValue");
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);
            channel.writeInbound(headersFrame);

            Http2StreamFrame responseHeadersFrame = waitForHttp2Response(5000);
            assertNotNull(responseHeadersFrame);
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;
            assertEquals("200", respHeaders.headers().status().toString());

            Http2StreamFrame responseDataFrame = waitForHttp2Response(5000);
            assertNotNull(responseDataFrame);
            assertTrue(responseDataFrame instanceof DefaultHttp2DataFrame);
            DefaultHttp2DataFrame respData = (DefaultHttp2DataFrame) responseDataFrame;
            String content = respData.content().toString(StandardCharsets.UTF_8);
            assertTrue(content.contains("Processed: testValue"));
        }
    }

    @Test
    void testHttp2RequestToNonexistentPath() {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            Http2Headers headers = new DefaultHttp2Headers();
            headers.method("GET");
            headers.path("/notfound");
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);
            channel.writeInbound(headersFrame);

            Http2StreamFrame responseHeadersFrame = channel.readOutbound();
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;
            assertEquals("404", respHeaders.headers().status().toString());
        }
    }

    @Test
    void testHttp2PostRequestWithJsonBody() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            String json = OBJECT_MAPPER.writeValueAsString(new HashMap<String, Object>() {
                {
                    put("foo", "bar");
                }
            });
            Http2Headers headers = new DefaultHttp2Headers();
            headers.method("POST");
            headers.path("/test?param=jsonValue");
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
            channel.writeInbound(headersFrame);
            DefaultHttp2DataFrame dataFrame =
                    new DefaultHttp2DataFrame(Unpooled.copiedBuffer(json, StandardCharsets.UTF_8), true);
            channel.writeInbound(dataFrame);

            Http2StreamFrame frame1 = null, frame2 = null;
            long deadline = System.currentTimeMillis() + 5000;
            while ((frame1 == null || frame2 == null) && System.currentTimeMillis() < deadline) {
                if (frame1 == null) frame1 = channel.readOutbound();
                if (frame2 == null) frame2 = channel.readOutbound();
                if (frame1 == null || frame2 == null) Thread.sleep(500);
            }
            assertNotNull(frame1);
            assertNotNull(frame2);
            DefaultHttp2HeadersFrame respHeaders;
            DefaultHttp2DataFrame respData;
            if (frame1 instanceof DefaultHttp2HeadersFrame) {
                respHeaders = (DefaultHttp2HeadersFrame) frame1;
                respData = (DefaultHttp2DataFrame) frame2;
            } else {
                respHeaders = (DefaultHttp2HeadersFrame) frame2;
                respData = (DefaultHttp2DataFrame) frame1;
            }
            assertEquals("200", respHeaders.headers().status().toString());
            String content = respData.content().toString(StandardCharsets.UTF_8);
            assertTrue(content.contains("Processed: jsonValue"));
        }
    }

    @Test
    void testHttp2BadRequest() {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doNothing().when(mockChain).doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            Http2Headers headers = new DefaultHttp2Headers();
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);
            channel.writeInbound(headersFrame);
            Http2StreamFrame responseHeadersFrame = channel.readOutbound();
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;
            assertEquals("400", respHeaders.headers().status().toString());
        }
    }

    @Test
    void testHttp2GetRequestWithXSSParam_shouldBeBlocked() {

        Http2Headers headers = new DefaultHttp2Headers();
        headers.method("GET");
        headers.path("/test?param=<script>alert(1)</script>");
        Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected javascript:"))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            channel.writeInbound(headersFrame);

            Http2StreamFrame responseHeadersFrame = waitForHttp2Response(3000);
            assertNotNull(responseHeadersFrame);
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;

            assertEquals("400", respHeaders.headers().status().toString());
        }
    }

    @Test
    void testHttp2GetRequestWithOnloadParam_shouldBeBlocked() {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected javascript:"))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            Http2Headers headers = new DefaultHttp2Headers();
            headers.method("GET");
            headers.path("/test?param=onload=alert(1)");
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, true);

            channel.writeInbound(headersFrame);

            Http2StreamFrame responseHeadersFrame = waitForHttp2Response(3000);
            assertNotNull(responseHeadersFrame);
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;

            assertEquals("400", respHeaders.headers().status().toString());
        }
    }

    @Test
    void testHttp2PostRequestWithXssJson_shouldBeBlocked() throws Exception {
        try (MockedStatic<HttpRequestFilterManager> mockedStatic = mockStatic(HttpRequestFilterManager.class)) {
            HttpRequestFilterChain mockChain = mock(HttpRequestFilterChain.class);
            doThrow(new HttpRequestFilterException("Detected javascript:"))
                    .when(mockChain)
                    .doFilter(any());
            mockedStatic.when(HttpRequestFilterManager::getFilterChain).thenReturn(mockChain);
            String maliciousJson = "{\"param\": \"<script>alert('xss')</script>\"}";

            Http2Headers headers = new DefaultHttp2Headers();
            headers.method("POST");
            headers.path("/test?param=abc");
            headers.set("content-type", "application/json");

            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
            DefaultHttp2DataFrame dataFrame =
                    new DefaultHttp2DataFrame(Unpooled.copiedBuffer(maliciousJson, StandardCharsets.UTF_8), true);

            channel.writeInbound(headersFrame);
            channel.writeInbound(dataFrame);

            Http2StreamFrame responseHeadersFrame = waitForHttp2Response(3000);
            assertNotNull(responseHeadersFrame);
            assertTrue(responseHeadersFrame instanceof DefaultHttp2HeadersFrame);
            DefaultHttp2HeadersFrame respHeaders = (DefaultHttp2HeadersFrame) responseHeadersFrame;

            assertEquals("400", respHeaders.headers().status().toString());
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        // Clean up ControllerManager
        Field field = ControllerManager.class.getDeclaredField("HTTP_CONTROLLER_MAP");
        field.setAccessible(true);
        Map<String, HttpInvocation> map = (Map<String, HttpInvocation>) field.get(null);
        map.clear();
    }
}
