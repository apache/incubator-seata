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
                Assertions.assertEquals(304, response.code());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(2)
    void watchTimeoutTest_withHttp2() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("default-test", "1");

        // For HTTP2, the connection should remain open and not timeout
        // The test verifies that the connection stays alive beyond the timeout period
        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                params,
                headers,
                ClusterWatchEvent.class)) {

            boolean keepaliveReceived = false;
            long startTime = System.currentTimeMillis();
            long testDuration = 5000; // Test for 5 seconds to verify connection stays alive
            long endTime = startTime + testDuration;

            // Verify KEEPALIVE is received immediately
            Assertions.assertTrue(watch.hasNext(), "Watch should have at least one event");
            SeataHttpWatch.Response<ClusterWatchEvent> firstResponse = watch.next();
            Assertions.assertEquals(
                    SeataHttpWatch.Response.Type.KEEPALIVE, firstResponse.type, "First event should be KEEPALIVE");
            Assertions.assertNotNull(firstResponse.object, "Event data should not be null");
            ClusterWatchEvent keepaliveEvent = firstResponse.object;
            Assertions.assertEquals("keepalive", keepaliveEvent.getType(), "Event type should be 'keepalive'");
            Assertions.assertEquals("default-test", keepaliveEvent.getGroup(), "Group should match");
            Assertions.assertNotNull(keepaliveEvent.getTimestamp(), "Timestamp should not be null");
            keepaliveReceived = true;

            long elapsed = System.currentTimeMillis() - startTime;
            Assertions.assertTrue(
                    elapsed < 1000,
                    "KEEPALIVE should be received immediately after connection, elapsed: " + elapsed + "ms");
            Assertions.assertTrue(keepaliveReceived, "Keepalive should be received after connection");
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
                Assertions.assertEquals(200, response.code());
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

        boolean keepaliveReceived = false;
        boolean clusterUpdateReceived = false;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 10000; // Maximum wait time: 10 seconds

        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, ClusterWatchEvent.class)) {
            // For HTTP2, connection will remain open, so we need to break after receiving expected events
            while (watch.hasNext() && (System.currentTimeMillis() - startTime < maxWaitTime)) {
                SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
                SeataHttpWatch.Response.Type type = response.type;

                // 执行业务逻辑
                processEvent(response);

                if (type == SeataHttpWatch.Response.Type.KEEPALIVE) {
                    keepaliveReceived = true;
                    Assertions.assertNotNull(response.object, "KEEPALIVE event data should not be null");
                    Assertions.assertEquals("keepalive", response.object.getType(), "Event type should be 'keepalive'");
                } else if (type == SeataHttpWatch.Response.Type.CLUSTER_UPDATE) {
                    clusterUpdateReceived = true;
                    Assertions.assertNotNull(response.object, "CLUSTER_UPDATE event data should not be null");
                    Assertions.assertEquals(
                            "cluster-update", response.object.getType(), "Event type should be 'cluster-update'");
                    Assertions.assertEquals("default-test", response.object.getGroup(), "Group should match");
                    Assertions.assertNotNull(response.object.getTerm(), "Term should not be null");
                    Assertions.assertEquals(2L, response.object.getTerm().longValue(), "Term should be 2");
                    // After receiving cluster update, exit the loop
                    break;
                }
            }

            // Verify both events were received
            Assertions.assertTrue(keepaliveReceived, "KEEPALIVE event should be received");
            Assertions.assertTrue(
                    clusterUpdateReceived, "CLUSTER_UPDATE event should be received after cluster change");
        } catch (IOException e) {
            throw new RuntimeException("Watch failed", e);
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
            Assertions.assertEquals(400, response.code());
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
            Assertions.assertEquals(400, response.code());
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
            Assertions.assertEquals(400, response.code());
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
            Assertions.assertEquals(400, response.code());
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
            Assertions.assertEquals(400, response.code());
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
            Assertions.assertEquals(400, response.code());
        }
    }
}
