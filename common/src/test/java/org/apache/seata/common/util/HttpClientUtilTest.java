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

import okhttp3.Response;
import org.apache.seata.common.executor.HttpCallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
}
