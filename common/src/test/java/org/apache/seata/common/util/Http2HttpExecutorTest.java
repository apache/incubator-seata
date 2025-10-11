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
import org.apache.seata.common.http.Http2HttpExecutor;
import org.apache.seata.common.http.HttpResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Http2HttpExecutorTest {

    @Test
    void testDoPostHttp_param_onSuccess() throws Exception {

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpResult responseHttpResult =
                Http2HttpExecutor.getInstance().doPost("https://www.cloudflare.com/", params, headers, 10000);

        assertNotNull(responseHttpResult);
        assertEquals(Protocol.HTTP_2, ((Response)responseHttpResult.getRawResponse()).protocol());
    }

    @Test
    void testDoPostHttp_param_onFailure() {
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        assertThrows(Exception.class, () -> Http2HttpExecutor.getInstance()
                .doPost("http://localhost:9999/invalid", params, headers, 3000));
    }

    @Test
    void testDoPostHttp_body_onSuccess() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpResult result = Http2HttpExecutor.getInstance()
                .doPost("https://www.cloudflare.com/", "{\"key\":\"value\"}", headers, 10000);
        assertNotNull(result);
        assertEquals(Protocol.HTTP_2, ((Response)result.getRawResponse()).protocol());
    }

    @Test
    void testDoPostHttp_body_onFailure() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        assertThrows(Exception.class, () -> Http2HttpExecutor.getInstance()
                .doPost("http://localhost:9999/invalid", "{\"key\":\"value\"}", headers, 3000));
    }

    @Test
    void testDoPostHttp_param_onSuccess_forceHttp1() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpResult result =
                Http2HttpExecutor.getInstance().doPost("http://httpbin.org/post", params, headers, 10000);
        assertNotNull(result);
        assertEquals(Protocol.HTTP_1_1, ((Response)result.getRawResponse()).protocol());
    }

    @Test
    void testDoGetHttp_onSuccess() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        HttpResult result =
                Http2HttpExecutor.getInstance().doGet("https://www.cloudflare.com/", headers, 10000);
        assertNotNull(result);
        assertEquals(Protocol.HTTP_2, ((Response)result.getRawResponse()).protocol());
    }

    @Test
    void testDoPostHttp_body_onSuccess_forceHttp1() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpResult result = Http2HttpExecutor.getInstance()
                .doPost("http://httpbin.org/post", "{\"key\":\"value\"}", headers, 10000);
        assertNotNull(result);
        assertEquals(Protocol.HTTP_1_1, ((Response)result.getRawResponse()).protocol());
    }
}
