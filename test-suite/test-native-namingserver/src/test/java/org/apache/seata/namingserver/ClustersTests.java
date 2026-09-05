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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Namingserver clusters monitor API.
 *
 * <p>Verifies the clusters monitor endpoint
 * {@code GET /api/v1/naming/clusters}
 * for listing clusters in a namespace with their status.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class ClustersTests {

    /**
     * HTTP client for sending requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver clusters monitor endpoint URL.
     */
    String url = "http://127.0.0.1:8081/api/v1/naming/clusters?namespace={namespace}";

    /**
     * Test querying clusters in a namespace.
     *
     * <p>Expects a non-null response with cluster data.
     */
    @Test
    @SuppressWarnings("unchecked")
    void clustersSuccess() {

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

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("namespace", "public");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
        HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
        RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity, List.class);
        HttpMessageConverterExtractor<List> responseExtractor =
                new HttpMessageConverterExtractor<>(List.class, restTemplate.getMessageConverters());
        List<?> result = restTemplate.execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
        assertNotNull(result);
    }
}
