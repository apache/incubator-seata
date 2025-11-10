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

import okhttp3.Protocol;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.executor.HttpCallback;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.HttpClientUtil;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        try (CloseableHttpResponse response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", param, header, 5000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                Assertions.assertEquals(HttpStatus.SC_NOT_MODIFIED, statusLine.getStatusCode());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(2)
    void watchTimeoutTest_withHttp2() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("default-test", "1");

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(Protocol.H2_PRIOR_KNOWLEDGE, response.protocol());
                Assertions.assertEquals(HttpStatus.SC_NOT_MODIFIED, response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                Assertions.fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                Assertions.fail("Should not be cancelled");
            }
        };

        HttpClientUtil.doPostWithHttp2(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, callback);
        // Currently, the server side does not have the ability to send http2 responses,
        // so if no response is received here, it will definitely time out
        Assertions.assertFalse(latch.await(5, TimeUnit.SECONDS));
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
        try (CloseableHttpResponse response =
                HttpClientUtil.doPost("http://127.0.0.1:" + port + "/metadata/v1/watch", param, header, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                Assertions.assertEquals(HttpStatus.SC_OK, statusLine.getStatusCode());
                return;
            }
        }
        Assertions.fail();
    }

    @Test
    @Order(4)
    void testXssFilterBlocked_queryParam() throws Exception {
        String malicious = "<script>alert('xss')</script>";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        try (CloseableHttpResponse response = HttpClientUtil.doGet(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&testParam="
                        + URLEncoder.encode(malicious, String.valueOf(StandardCharsets.UTF_8)),
                new HashMap<>(),
                header,
                5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @Order(5)
    void testXssFilterBlocked_queryParam_withGetHttp2() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        String malicious = "<script>alert('xss')</script>";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                assertNotNull(response);
                Assertions.assertEquals(Protocol.H2_PRIOR_KNOWLEDGE, response.protocol());
                Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        HttpClientUtil.doGetWithHttp2(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&testParam="
                        + URLEncoder.encode(malicious, String.valueOf(StandardCharsets.UTF_8)),
                header,
                callback,
                5000);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    @Order(6)
    void testXssFilterBlocked_formParam_withPostHttp2() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        String malicious = "<script>alert('xss')</script>";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("key", malicious);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                assertNotNull(response);
                Assertions.assertEquals(Protocol.H2_PRIOR_KNOWLEDGE, response.protocol());
                Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        HttpClientUtil.doPostWithHttp2("http://127.0.0.1:" + port + "/random", params, header, callback, 5000);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    @Order(7)
    void testXssFilterBlocked_bodyParam_withPostHttp2() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        String malicious = "<script>alert('xss')</script>";
        Map<String, String> header = new HashMap<>();

        String jsonBody = "{\"key\":\"" + malicious + "\"}";

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response response) {
                assertNotNull(response);
                Assertions.assertEquals(Protocol.H2_PRIOR_KNOWLEDGE, response.protocol());
                Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        HttpClientUtil.doPostWithHttp2("http://127.0.0.1:" + port + "/random", jsonBody, header, callback, 5000);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    @Order(8)
    void testXssFilterBlocked_formParam() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("testParam", "<script>alert('xss')</script>");

        try (CloseableHttpResponse response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @Order(9)
    void testXssFilterBlocked_jsonBody() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        String jsonBody = "{\"testParam\":\"<script>alert('xss')</script>\"}";

        try (CloseableHttpResponse response = HttpClientUtil.doPostJson(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", jsonBody, headers, 5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
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

        try (CloseableHttpResponse response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @Order(11)
    void testXssFilterBlocked_multiSource() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put("X-Test-Header", "<script>alert('xss')</script>");

        String jsonBody = "{\"testParam\":\"<script>alert('xss')</script>\"}";

        try (CloseableHttpResponse response = HttpClientUtil.doPostJson(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000&urlParam="
                        + URLEncoder.encode("<script>alert('xss')</script>", String.valueOf(StandardCharsets.UTF_8)),
                jsonBody,
                headers,
                5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @Order(12)
    void testXssFilterBlocked_formParamWithUserCustomKeyWords() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> params = new HashMap<>();
        params.put("testParam", "custom1");

        try (CloseableHttpResponse response = HttpClientUtil.doPost(
                "http://127.0.0.1:" + port + "/metadata/v1/watch?timeout=3000", params, headers, 5000)) {
            Assertions.assertEquals(
                    HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }
}
