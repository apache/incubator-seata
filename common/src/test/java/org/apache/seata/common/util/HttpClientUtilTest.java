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
package org.apache.seata.common.util;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.seata.common.executor.HttpCallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HttpClientUtilTest {

    @Test
    public void testDoPost() throws IOException {
        Assertions.assertNull(HttpClientUtil.doPost("test", new HashMap<>(), new HashMap<>(), 0));
        Assertions.assertNull(HttpClientUtil.doGet("test", new HashMap<>(), new HashMap<>(), 0));
    }

    @Test
    void testDoPostWithHttp2_param_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpClientUtil.doPostWithHttp2("http://localhost:9999/invalid", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostWithHttp2_body_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        String body = "{\"key\":\"value\"}";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpClientUtil.doPostWithHttp2("http://localhost:9999/invalid", body, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostWithHttp2_body_withCharset_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        HttpClientUtil.doPostWithHttp2("http://localhost:9999/invalid", params, headers, callback, 30000);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostWithHttp2_withEmptyParam_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        HttpClientUtil.doPostWithHttp2("http://localhost:9999/invalid", params, headers, callback, 30000);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoGetHttp_param_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpClientUtil.doGetWithHttp2("http://localhost:9999/invalid", headers, callback, 30000);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testShutdownHookExecution() throws Exception {
        String javaVersion = System.getProperty("java.version");
        Assumptions.assumeTrue(
                javaVersion.startsWith("1.8"), () -> "Skipping test: only runs on Java 8, current=" + javaVersion);
        Class.forName("org.apache.seata.common.util.HttpClientUtil");

        Class<?> clazz = Class.forName("java.lang.ApplicationShutdownHooks");
        Field hooksField = clazz.getDeclaredField("hooks");
        hooksField.setAccessible(true);
        Map<Thread, Thread> hooks = (Map<Thread, Thread>) hooksField.get(null);
        Thread targetHook = hooks.keySet().stream()
                .filter(h -> {
                    try {
                        Field targetField = Thread.class.getDeclaredField("target");
                        targetField.setAccessible(true);
                        Object target = targetField.get(h);
                        return target != null && target.toString().contains("HttpClientUtil");
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No HttpClientUtil shutdown hook found"));

        Field httpClientMapField = HttpClientUtil.class.getDeclaredField("HTTP_CLIENT_MAP");
        httpClientMapField.setAccessible(true);
        Map<Integer, Object> httpClientMap = (Map<Integer, Object>) httpClientMapField.get(null);

        Field http2ClientMapField = HttpClientUtil.class.getDeclaredField("HTTP2_CLIENT_MAP");
        http2ClientMapField.setAccessible(true);
        Map<Integer, OkHttpClient> http2ClientMap = (Map<Integer, OkHttpClient>) http2ClientMapField.get(null);

        CloseableHttpClient mockCloseableClient = mock(CloseableHttpClient.class);
        OkHttpClient mockHttp2Client = mock(OkHttpClient.class, RETURNS_DEEP_STUBS);

        httpClientMap.put(1, mockCloseableClient);
        http2ClientMap.put(2, mockHttp2Client);

        targetHook.run();

        verify(mockCloseableClient, atLeastOnce()).close();
        verify(mockHttp2Client.dispatcher().executorService(), atLeastOnce()).shutdown();
        verify(mockHttp2Client.connectionPool(), atLeastOnce()).evictAll();
    }
}
