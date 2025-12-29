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
package org.apache.seata.server.controller;

import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.SeataHttpWatch;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClusterControllerTest extends BaseSpringBootTest {

    private static Environment environment;
    private static int port;

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        environment = context.getEnvironment();
        port = Integer.parseInt(environment.getProperty(SERVER_SERVICE_PORT_CAMEL, "18091"));
    }

    @Test
    @Order(1)
    void watchTimeoutTest() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        header.put(HTTP.CONN_KEEP_ALIVE, "close");
        Map<String, String> param = new HashMap<>();
        param.put("default-test", "1");
        try (Response response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", param, header, 5000)) {
            if (response != null) {
                Assertions.assertEquals(HttpStatus.SC_NOT_MODIFIED, response.code());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(2)
    void watchTimeoutTest_withHttp2() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("default-test", "1");

        // Use a short timeout (3 seconds) to test timeout behavior
        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                params,
                headers,
                ClusterWatchEvent.class)) {

            boolean keepaliveReceived = false;
            boolean timeoutReceived = false;
            long startTime = System.currentTimeMillis();

            while (watch.hasNext()) {
                SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
                SeataHttpWatch.Response.Type type = response.type;
                ClusterWatchEvent event = response.object;

                Assertions.assertNotNull(event, "Event data should not be null");

                if (type == SeataHttpWatch.Response.Type.KEEPALIVE) {
                    // Verify KEEPALIVE event is received when connection is established
                    Assertions.assertFalse(keepaliveReceived, "KEEPALIVE should only be received once");
                    keepaliveReceived = true;
                    Assertions.assertEquals("keepalive", event.getType(), "Event type should be 'keepalive'");
                    Assertions.assertEquals("default-test", event.getGroup(), "Group should match");
                    Assertions.assertNotNull(event.getTimestamp(), "Timestamp should not be null");
                    long elapsed = System.currentTimeMillis() - startTime;
                    Assertions.assertTrue(
                            elapsed < 1000,
                            "KEEPALIVE should be received immediately after connection, elapsed: " + elapsed + "ms");
                } else if (type == SeataHttpWatch.Response.Type.TIMEOUT) {
                    // Verify TIMEOUT event is received after timeout
                    Assertions.assertFalse(timeoutReceived, "TIMEOUT should only be received once");
                    timeoutReceived = true;
                    Assertions.assertEquals("timeout", event.getType(), "Event type should be 'timeout'");
                    Assertions.assertEquals("default-test", event.getGroup(), "Group should match");
                    long elapsed = System.currentTimeMillis() - startTime;
                    // Timeout is 3000ms, allow some tolerance (3000-5000ms)
                    Assertions.assertTrue(
                            elapsed >= 2800 && elapsed <= 6000,
                            "TIMEOUT should be received after ~3000ms, elapsed: " + elapsed + "ms");
                    // After timeout, the stream should be closed, so break the loop
                    break;
                } else {
                    Assertions.fail("Unexpected event type: " + type);
                }
            }

            // Verify both events were received
            Assertions.assertTrue(keepaliveReceived, "KEEPALIVE event should be received");
            Assertions.assertTrue(timeoutReceived, "TIMEOUT event should be received after timeout");

        } catch (IOException e) {
            throw new RuntimeException("Watch failed", e);
        }
    }

    @Test
    @Order(3)
    void watch() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test", "1");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test", 2, true));
            }
        });
        thread.start();
        try (Response response =
                HttpClientUtil.doPost("http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, 30000)) {
            if (response != null) {
                Assertions.assertEquals(HttpStatus.SC_OK, response.code());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(3)
    void watch_stream() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test", "1");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test", 2, true));
            }
        });
        thread.start();

        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, ClusterWatchEvent.class)) {
            while (watch.hasNext()) {
                SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
                // 执行业务逻辑
                processEvent(response);
            }
        } catch (IOException e) {

        }
    }

    private static void processEvent(SeataHttpWatch.Response<ClusterWatchEvent> response) {
        System.out.println("Event Type: " + response.type);
        if (response.object != null) {
            System.out.println("Event Data: " + response.object);
        }
    }

    @Test
    @Order(4)
    void testXssFilterBlocked_queryParam() throws Exception {
        String malicious = "<script>alert('xss')</script>";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        try (Response response = HttpClientUtil.doGet(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&testParam="
                        + URLEncoder.encode(malicious, String.valueOf(StandardCharsets.UTF_8)),
                new HashMap<>(),
                header,
                5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }

    @Test
    @Order(5)
    void testXssFilterBlocked_queryParam_withGetHttp2() throws Exception {
        String malicious = "<script>alert('xss')</script>";
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        // XSS filter should block the request and return 400 Bad Request
        // Watch method will throw FrameworkException when response is not successful
        org.apache.seata.common.exception.FrameworkException exception =
                Assertions.assertThrows(org.apache.seata.common.exception.FrameworkException.class, () -> {
                    try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watch(
                            "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&testParam="
                                    + URLEncoder.encode(malicious, String.valueOf(StandardCharsets.UTF_8)),
                            headers,
                            ClusterWatchEvent.class)) {
                        // Should not reach here, exception should be thrown during watch creation
                        Assertions.fail("XSS filter should have blocked the request");
                    }
                });

        // Verify the exception contains 400 status code
        String exceptionMessage = exception.getMessage();
        Assertions.assertNotNull(exceptionMessage, "Exception message should not be null");
        Assertions.assertTrue(
                exceptionMessage.contains("400") || exceptionMessage.contains("Watch request failed with code 400"),
                "Exception should indicate 400 Bad Request, but got: " + exceptionMessage);
    }

    @Test
    @Order(6)
    void testXssFilterBlocked_formParam_withPostHttp2() throws Exception {
        String malicious = "<script>alert('xss')</script>";
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("key", malicious);

        // XSS filter should block the request and return 400 Bad Request
        // Watch method will throw FrameworkException when response is not successful
        org.apache.seata.common.exception.FrameworkException exception =
                Assertions.assertThrows(org.apache.seata.common.exception.FrameworkException.class, () -> {
                    try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                            "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                            params,
                            headers,
                            ClusterWatchEvent.class)) {
                        // Should not reach here, exception should be thrown during watch creation
                        Assertions.fail("XSS filter should have blocked the request");
                    }
                });

        // Verify the exception contains 400 status code
        String exceptionMessage = exception.getMessage();
        Assertions.assertNotNull(exceptionMessage, "Exception message should not be null");
        Assertions.assertTrue(
                exceptionMessage.contains("400") || exceptionMessage.contains("Watch request failed with code 400"),
                "Exception should indicate 400 Bad Request, but got: " + exceptionMessage);
    }

    @Test
    @Order(7)
    void testXssFilterBlocked_bodyParam_withPostHttp2() throws Exception {
        String malicious = "<script>alert('xss')</script>";
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        // Create a Map with malicious content to be sent as JSON body
        // The watchPost method will convert Map to JSON body
        Map<String, String> params = new HashMap<>();
        params.put("key", malicious);

        // XSS filter should block the request and return 400 Bad Request
        // Watch method will throw FrameworkException when response is not successful
        org.apache.seata.common.exception.FrameworkException exception =
                Assertions.assertThrows(org.apache.seata.common.exception.FrameworkException.class, () -> {
                    try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                            "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                            params,
                            headers,
                            ClusterWatchEvent.class)) {
                        // Should not reach here, exception should be thrown during watch creation
                        Assertions.fail("XSS filter should have blocked the request");
                    }
                });

        // Verify the exception contains 400 status code
        String exceptionMessage = exception.getMessage();
        Assertions.assertNotNull(exceptionMessage, "Exception message should not be null");
        Assertions.assertTrue(
                exceptionMessage.contains("400") || exceptionMessage.contains("Watch request failed with code 400"),
                "Exception should indicate 400 Bad Request, but got: " + exceptionMessage);
    }

    @Test
    @Order(8)
    void testXssFilterBlocked_formParam() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("testParam", "<script>alert('xss')</script>");

        try (Response response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }

    @Test
    @Order(9)
    void testXssFilterBlocked_jsonBody() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        String jsonBody = "{\"testParam\":\"<script>alert('xss')</script>\"}";

        try (Response response = HttpClientUtil.doPostJson(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", jsonBody, headers, 5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }

    @Test
    @Order(10)
    void testXssFilterBlocked_headerParam() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        headers.put("X-Test-Header", "<script>alert('xss')</script>");

        Map<String, String> params = new HashMap<>();
        params.put("safeParam", "123");

        try (Response response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }

    @Test
    @Order(11)
    void testXssFilterBlocked_multiSource() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put("X-Test-Header", "<script>alert('xss')</script>");

        String jsonBody = "{\"testParam\":\"<script>alert('xss')</script>\"}";

        try (Response response = HttpClientUtil.doPostJson(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&urlParam="
                        + URLEncoder.encode("<script>alert('xss')</script>", String.valueOf(StandardCharsets.UTF_8)),
                jsonBody,
                headers,
                5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }

    @Test
    @Order(12)
    void testXssFilterBlocked_formParamWithUserCustomKeyWords() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("testParam", "custom1");

        try (Response response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
        }
    }
}
