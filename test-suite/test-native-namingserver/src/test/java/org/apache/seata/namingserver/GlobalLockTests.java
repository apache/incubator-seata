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
package org.apache.seata.namingserver;

import org.apache.seata.common.result.Result;
import org.apache.seata.common.result.SingleResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Namingserver global lock query API.
 *
 * <p>Verifies the global lock query endpoint
 * {@code GET /api/v1/console/globalLock/query}
 * with valid authentication, namespace, and cluster headers.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class GlobalLockTests {

    /**
     * HTTP client for sending requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver global lock query endpoint URL.
     */
    String url =
            "http://127.0.0.1:8081/api/v1/console/globalLock/query?pageSize={pageSize}&pageNum={pageNum}&namespace={namespace}&cluster={cluster}&vgroup={vgroup}";

    /**
     * Test querying global locks with valid authentication.
     *
     * <p>Expects a success response with non-null data.
     */
    @Test
    void querySuccess() {

        String token;
        {
            String loginUrl = "http://127.0.0.1:8081/api/v1/auth/login";
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = new HashMap<>();
            body.put("username", "seata");
            body.put("password", "seata");
            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
            SingleResult<String> singleResult = restTemplate.postForObject(loginUrl, httpEntity, SingleResult.class);
            assertNotNull(singleResult);
            assertEquals(Result.SUCCESS_CODE, singleResult.getCode());
            assertEquals(Result.SUCCESS_MSG, singleResult.getMessage());
            assertNotNull(singleResult.getData());
            token = singleResult.getData();
        }

        String namespace = "public";
        String cluster = "default";

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("pageSize", "10");
        uriVariables.put("pageNum", "1");
        uriVariables.put("namespace", namespace);
        uriVariables.put("cluster", cluster);
        uriVariables.put("vgroup", "");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, token);

        httpHeaders.set("x-seata-namespace", namespace);
        httpHeaders.set("x-seata-cluster", cluster);

        HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
        RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity, SingleResult.class);
        HttpMessageConverterExtractor<SingleResult> responseExtractor =
                new HttpMessageConverterExtractor<>(SingleResult.class, restTemplate.getMessageConverters());
        SingleResult singleResult =
                restTemplate.execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
        assertNotNull(singleResult);
        assertEquals(Result.SUCCESS_CODE, singleResult.getCode());
        assertEquals(Result.SUCCESS_MSG, singleResult.getMessage());
        assertNotNull(singleResult.getData());
    }
}
