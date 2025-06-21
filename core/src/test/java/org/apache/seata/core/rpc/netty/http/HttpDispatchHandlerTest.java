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

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDispatchHandlerTest {

    private HttpDispatchHandler handler;
    private EmbeddedChannel channel;
    private TestController testController = new TestController();

    class TestController {
        public String handleRequest(String param) {
            return "Processed: " + param;
        }
    }

    @BeforeEach
    void setUp() {
        handler = new HttpDispatchHandler();
        channel = new EmbeddedChannel(handler);
    }

    @Test
    void testGetRequestWithParameters() throws Exception {
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
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/notfound");

        channel.writeInbound(request);

        FullHttpResponse response = waitForResponse(5000);
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
    }

    @Test
    void testHttpHeadMethod() {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/head");

        channel.writeInbound(request);

        FullHttpResponse response = waitForResponse(5000);
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
        assertEquals(0, response.content().readableBytes());
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
