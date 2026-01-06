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
    Logger logger = LoggerFactory.getLogger(ClusterControllerTest.class);

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

        // For HTTP2, the connection should remain open and not timeout
        // The test verifies that the connection stays alive beyond the timeout period
        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000",
                params,
                headers,
                ClusterWatchEvent.class)) {

            boolean keepaliveReceived = false;
            long startTime = System.currentTimeMillis();
            SeataHttpWatch.Response<ClusterWatchEvent> firstResponse = watch.next();
            Assertions.assertEquals(
                    SeataHttpWatch.Response.Type.KEEPALIVE, firstResponse.type, "First event should be KEEPALIVE");
            Assertions.assertNotNull(firstResponse.object, "Event data should not be null");
            ClusterWatchEvent keepaliveEvent = firstResponse.object;
            Assertions.assertEquals("keepalive", keepaliveEvent.getType(), "Event type should be 'keepalive'");
            Assertions.assertEquals("default-test-group-1", keepaliveEvent.getGroup(), "Group should match");
            Assertions.assertNotNull(keepaliveEvent.getTimestamp(), "Timestamp should not be null");
            keepaliveReceived = true;

            long elapsed = System.currentTimeMillis() - startTime;
            Assertions.assertTrue(
                    elapsed < 1000,
                    "KEEPALIVE should be received immediately after connection, elapsed: " + elapsed + "ms");
            Assertions.assertTrue(keepaliveReceived, "Keepalive should be received after connection");

            long timeoutPeriod = 3000; // Timeout period from query parameter
            long waitTime = timeoutPeriod + 1000; // Wait timeout period + 1 second
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assertions.fail("Test interrupted while waiting for connection verification");
            }

            // Trigger a cluster change event to verify connection is still active
            Thread triggerThread = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterChangeEvent(this, "default-test-group-1", 2, true));
            });
            triggerThread.start();
            boolean clusterUpdateReceived = false;
            SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

            if (response.type == SeataHttpWatch.Response.Type.CLUSTER_UPDATE) {
                clusterUpdateReceived = true;
                Assertions.assertEquals(
                        "cluster-update", response.object.getType(), "Event type should be 'cluster-update'");
                Assertions.assertEquals("default-test-group-1", response.object.getGroup(), "Group should match");
                Assertions.assertEquals(2L, response.object.getTerm().longValue(), "Term should be 2");
            }

            // Verify connection was maintained beyond timeout period
            long totalElapsed = System.currentTimeMillis() - startTime;
            Assertions.assertTrue(
                    totalElapsed >= timeoutPeriod + 1000, // Should be at least timeout + 1 second
                    "Connection should be maintained beyond timeout period, elapsed: " + totalElapsed + "ms");
            Assertions.assertTrue(
                    clusterUpdateReceived,
                    "HTTP2 connection should remain open and receive cluster update event after timeout period");
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

    @Test
    @Order(4)
    void watchStream_http2() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test-group-3", "1");
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
            logger.info("准备接受链接建立事件");
            SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
            logger.info("接受到的事件类型{}", response.type);
            Assertions.assertNotNull(response.object, "KEEPALIVE event data should not be null");
            Assertions.assertEquals("keepalive", response.object.getType(), "Event type should be 'keepalive'");
            logger.info("准备接受变更事件");
            SeataHttpWatch.Response<ClusterWatchEvent> watchEventResponse = watch.next();
            logger.info("接受到的事件类型{}", response.type);
            Assertions.assertNotNull(watchEventResponse.object, "CLUSTER_UPDATE event data should not be null");
            Assertions.assertEquals(
                    "cluster-update", watchEventResponse.object.getType(), "Event type should be 'cluster-update'");
            Assertions.assertEquals("default-test-group-3", watchEventResponse.object.getGroup(), "Group should match");
            Assertions.assertNotNull(watchEventResponse.object.getTerm(), "Term should not be null");
            Assertions.assertEquals(2L, watchEventResponse.object.getTerm().longValue(), "Term should be 2");
        }
    }

    @Test
    @Order(5)
    void watchMultipleClusterUpdates_http2() throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        param.put("default-test-group-4", "1");

        // Trigger multiple cluster change events with different terms
        Thread triggerThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait for connection to be established
                ApplicationEventPublisher publisher = (ApplicationEventPublisher)
                        ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);

                // Trigger first cluster change event (term = 2)
                publisher.publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 2, true));
                Thread.sleep(1000); // Increased delay to ensure watcher is re-registered before next event

                // Trigger second cluster change event (term = 3)
                publisher.publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 3, true));
                Thread.sleep(500); // Increased delay to ensure watcher is re-registered before next event

                // Trigger third cluster change event (term = 4)
                publisher.publishEvent(new ClusterChangeEvent(this, "default-test-group-4", 4, true));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        triggerThread.start();

        boolean keepaliveReceived = false;
        int clusterUpdateCount = 0;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 10000; // Maximum wait time: 10 seconds
        long[] expectedTerms = {2L, 3L, 4L};

        try (SeataHttpWatch<ClusterWatchEvent> watch = HttpClientUtil.watchPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, ClusterWatchEvent.class)) {
            // Receive events until we get all expected cluster updates
            while (System.currentTimeMillis() - startTime < maxWaitTime && clusterUpdateCount < expectedTerms.length) {
                try {
                    if (watch.hasNext()) {
                        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
                        SeataHttpWatch.Response.Type type = response.type;
                        if (type == SeataHttpWatch.Response.Type.KEEPALIVE) {
                            keepaliveReceived = true;
                            Assertions.assertNotNull(response.object, "KEEPALIVE event data should not be null");
                            Assertions.assertEquals(
                                    "keepalive", response.object.getType(), "Event type should be 'keepalive'");
                        } else if (type == SeataHttpWatch.Response.Type.CLUSTER_UPDATE) {
                            logger.info("clusterUpdateCount当前值为" + clusterUpdateCount);
                            clusterUpdateCount++;
                            logger.info("clusterUpdateCount收到了一次变更事件当前值为" + clusterUpdateCount);
                            Assertions.assertNotNull(response.object, "CLUSTER_UPDATE event data should not be null");
                            Assertions.assertEquals(
                                    "cluster-update",
                                    response.object.getType(),
                                    "Event type should be 'cluster-update'");
                            Assertions.assertEquals(
                                    "default-test-group-4", response.object.getGroup(), "Group should match");
                            Assertions.assertNotNull(response.object.getTerm(), "Term should not be null");

                            // Verify term matches expected value
                            long expectedTerm = expectedTerms[clusterUpdateCount - 1];
                            long actualTerm = response.object.getTerm().longValue();
                            Assertions.assertEquals(
                                    expectedTerm,
                                    actualTerm,
                                    "Term should be " + expectedTerm + " but actualTerm is " + actualTerm
                                            + " for cluster update #" + clusterUpdateCount);
                        }
                    }
                } catch (Exception e) {
                    // If connection was closed unexpectedly, fail the test
                    if (clusterUpdateCount < expectedTerms.length) {
                        throw new RuntimeException("Unexpected exception while waiting for events", e);
                    }
                    break;
                }
            }

            // Verify all events were received
            Assertions.assertTrue(keepaliveReceived, "KEEPALIVE event should be received");
            Assertions.assertEquals(
                    expectedTerms.length,
                    clusterUpdateCount,
                    "Should receive " + expectedTerms.length + " cluster update events, but got " + clusterUpdateCount);
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
