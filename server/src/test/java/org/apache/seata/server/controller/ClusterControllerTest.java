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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static Logger logger = LoggerFactory.getLogger(ClusterControllerTest.class);

    private static Environment environment;
    private static int port;

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        environment = context.getEnvironment();
        port = Integer.parseInt(environment.getProperty(SERVER_SERVICE_PORT_CAMEL, "18091"));
    }

    @Test
    @Order(1)
    void watchTimeoutTest_http1() throws Exception {
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
    void watchTimeoutTest_http2() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("default-test-group-1", "1");

        // Verify that connection establishment immediately receives data
        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                params,
                headers,
                ClusterWatchEvent.class)) {

            long startTime = System.currentTimeMillis();
            SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

            // Verify event is received immediately after connection
            long elapsed = System.currentTimeMillis() - startTime;
            Assertions.assertTrue(
                    elapsed < 1000,
                    "First event should be received immediately after connection, elapsed: " + elapsed + "ms");

            // Verify event data
            Assertions.assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type, "First event should be UPDATE");
            Assertions.assertNotNull(response.object, "Event data should not be null");
            Assertions.assertNotNull(response.object.getMetadata(), "Metadata should not be null");
            Assertions.assertEquals("default-test-group-1", response.object.getGroup(), "Group should match");
            Assertions.assertNotNull(response.object.getTimestamp(), "Timestamp should not be null");
        }
    }

    @Test
    @Order(3)
    void watch_http1() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test-group-2", "1");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-2", 2, true));
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

    /**
     * Verification points:
     * 1. Verify HTTP/2 data push continuity: client can continuously receive events when server pushes them sequentially
     * 2. Note: Cannot verify MetadataResponse due to inability to simulate real term changes in test environment
     */
    @Test
    @Order(4)
    void watchStream_http2() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test-group-3", "1");

        // Trigger a cluster change event after connection is established
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-3", 2, true));
            }
        });
        thread.start();

        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, ClusterWatchEvent.class)) {

            // Verify HTTP/2 data push continuity: receive first event (connection established)
            SeataHttpWatch.Response<ClusterWatchEvent> firstResponse = watch.next();
            Assertions.assertNotNull(firstResponse.object, "First event data should not be null");
            Assertions.assertEquals(
                    SeataHttpWatch.Response.Type.UPDATE, firstResponse.type, "First event should be UPDATE");

            // Verify HTTP/2 data push continuity: receive second event (cluster change event)
            SeataHttpWatch.Response<ClusterWatchEvent> secondResponse = watch.next();
            Assertions.assertNotNull(secondResponse.object, "Second event data should not be null");
            Assertions.assertEquals(
                    SeataHttpWatch.Response.Type.UPDATE, secondResponse.type, "Second event should be UPDATE");
            Assertions.assertEquals("default-test-group-3", secondResponse.object.getGroup(), "Group should match");

            logger.info("Successfully received two consecutive events from server");
        }
    }

    /**
     * Verification points:
     * 1. Verify HTTP/2 data push continuity: client can continuously receive multiple events when server pushes them sequentially
     * 2. Note: Cannot verify MetadataResponse due to inability to simulate real term changes in test environment
     */
    @Test
    @Order(5)
    void watchMultipleClusterUpdates_http2() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test-group-4", "1");

        // Trigger multiple cluster change events sequentially
        Thread triggerThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait for connection to be established

                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 2, true));
                Thread.sleep(1000);

                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 3, true));
                Thread.sleep(500);

                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 4, true));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        triggerThread.start();

        boolean firstEventReceived = false;
        int clusterUpdateCount = 0;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 10000;
        int expectedUpdateCount = 3;

        // Verify HTTP/2 data push continuity: continuously receive multiple events
        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, ClusterWatchEvent.class)) {

            while (System.currentTimeMillis() - startTime < maxWaitTime && clusterUpdateCount < expectedUpdateCount) {
                try {
                    SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
                    if (response.type == SeataHttpWatch.Response.Type.UPDATE) {
                        Assertions.assertNotNull(response.object, "Event data should not be null");

                        if (!firstEventReceived) {
                            firstEventReceived = true;
                            logger.info("First event (connection established) received");
                        } else {
                            clusterUpdateCount++;
                            Assertions.assertEquals(
                                    "default-test-group-4", response.object.getGroup(), "Group should match");
                            logger.info("Received cluster update event #{}", clusterUpdateCount);
                        }
                    }
                } catch (Exception e) {
                    if (clusterUpdateCount < expectedUpdateCount) {
                        throw new RuntimeException("Unexpected exception while waiting for events", e);
                    }
                    break;
                }
            }

            Assertions.assertTrue(firstEventReceived, "First event (connection established) should be received");
            Assertions.assertEquals(
                    expectedUpdateCount,
                    clusterUpdateCount,
                    "Should receive " + expectedUpdateCount + " cluster update events, but got " + clusterUpdateCount);

            logger.info("Successfully received {} consecutive cluster update events from server", clusterUpdateCount);
        } catch (IOException e) {
            throw new RuntimeException("Watch failed", e);
        }
    }

    @Test
    @Order(6)
    void testXssFilterBlocked_queryParam_http1() throws Exception {
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
    @Order(7)
    void testXssFilterBlocked_queryParam_http2() throws Exception {
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
    @Order(8)
    void testXssFilterBlocked_formParam_http2() throws Exception {
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
    @Order(9)
    void testXssFilterBlocked_bodyParam_http2() throws Exception {
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
    @Order(10)
    void testXssFilterBlocked_formParam_http1() throws Exception {
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
    @Order(11)
    void testXssFilterBlocked_jsonBody_http1() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        String jsonBody = "{\"testParam\":\"<script>alert('xss')</script>\"}";

        try (Response response = HttpClientUtil.doPostJson(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", jsonBody, headers, 5000)) {
            Assertions.assertEquals(400, response.code());
        }
    }

    @Test
    @Order(12)
    void testXssFilterBlocked_headerParam_http1() throws Exception {
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
    @Order(13)
    void testXssFilterBlocked_multiSource_http1() throws Exception {
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
    @Order(14)
    void testXssFilterBlocked_formParamWithUserCustomKeyWords_http1() throws Exception {
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
