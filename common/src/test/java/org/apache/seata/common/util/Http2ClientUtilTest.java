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

import okhttp3.Protocol;
import okhttp3.Response;
import org.apache.seata.common.executor.HttpCallback;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class Http2ClientUtilTest {

    @Test
    void testDoPost_param_onSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response result) {
                assertNotNull(result);
                assertEquals(Protocol.HTTP_2, result.protocol());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
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

        Http2ClientUtil.doPost("https://www.apache.org/", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPost_param_onFailure() throws Exception {
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

        Http2ClientUtil.doPost("http://localhost:9999/invalid", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPost_body_onSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response result) {
                assertNotNull(result);
                assertEquals(Protocol.HTTP_2, result.protocol());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http2ClientUtil.doPost("https://www.apache.org/", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPost_body_onFailure() throws Exception {
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

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http2ClientUtil.doPost("http://localhost:9999/invalid", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_param_onSuccess_force1() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response result) {
                assertNotNull(result);
                assertEquals(Protocol.HTTP_1_1, result.protocol());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
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

        Http2ClientUtil.doPost("http://httpbin.org/post", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoGet_onSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response result) {
                assertNotNull(result);
                assertEquals(Protocol.HTTP_2, result.protocol());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        Http2ClientUtil.doGet("https://www.apache.org/", headers, callback, 1);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_body_onSuccess_force1() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<Response> callback = new HttpCallback<Response>() {
            @Override
            public void onSuccess(Response result) {
                assertNotNull(result);
                assertEquals(Protocol.HTTP_1_1, result.protocol());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http2ClientUtil.doPost("http://httpbin.org/post", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}
