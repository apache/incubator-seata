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
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Namingserver addGroup API.
 *
 * <p>Verifies the addGroup endpoint {@code POST /api/v1/naming/addGroup}
 * for adding a vgroup mapping to a cluster in the naming server.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class AddGroupTests {

    /**
     * HTTP client for sending requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver register endpoint URL.
     */
    String registerUrl =
            "http://127.0.0.1:8081/api/v1/naming/register?namespace={namespace}&clusterName={clusterName}&unit={unit}";

    /**
     * Namingserver unregister endpoint URL.
     */
    String unregisterUrl =
            "http://127.0.0.1:8081/api/v1/naming/unregister?namespace={namespace}&clusterName={clusterName}&unit={unit}";

    /**
     * Namingserver addGroup endpoint URL.
     */
    String addGroupUrl =
            "http://127.0.0.1:8081/api/v1/naming/addGroup?namespace={namespace}&clusterName={clusterName}&unitName={unitName}&vGroup={vGroup}";

    /**
     * Test adding a vgroup to a cluster.
     *
     * <p>First registers a server instance so the cluster has units,
     * then adds a vgroup mapping. Note that a full addGroup requires a
     * running Seata TC server instance to proxy the control request to;
     * without one the HTTP control call will fail. This test verifies
     * the endpoint and core logic function correctly up to that point.
     */
    @Test
    void addGroupSuccess() {

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

        HttpEntity<Map<String, Object>> httpEntity;
        Map<String, String> uriVariables;

        // Step 1: Register a node so the cluster has units
        {
            Map<String, Object> node = buildNode(58091, 58092);
            uriVariables = new HashMap<>();
            uriVariables.put("namespace", "public");
            uriVariables.put("clusterName", "default");
            uriVariables.put("unit", "default");

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpEntity = new HttpEntity<>(node, httpHeaders);

            Result<?> registerResult = restTemplate.postForObject(registerUrl, httpEntity, Result.class, uriVariables);
            assertNotNull(registerResult);
            assertEquals(Result.SUCCESS_CODE, registerResult.getCode());
        }

        // Step 2: Add vgroup to cluster
        {
            Map<String, String> uriVariablesGroup = new HashMap<>();
            uriVariablesGroup.put("namespace", "public");
            uriVariablesGroup.put("clusterName", "default");
            uriVariablesGroup.put("unitName", "default");
            uriVariablesGroup.put("vGroup", "test-add-group");

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> httpEntityGroup = new HttpEntity<>(httpHeaders);

            Result<?> result =
                    restTemplate.postForObject(addGroupUrl, httpEntityGroup, Result.class, uriVariablesGroup);
            assertNotNull(result);
            assertEquals(Result.SUCCESS_CODE, result.getCode());
        }

        Result<?> unregisterResult = restTemplate.postForObject(unregisterUrl, httpEntity, Result.class, uriVariables);
        assertNotNull(unregisterResult);
        assertEquals(Result.SUCCESS_CODE, unregisterResult.getCode());
    }

    /**
     * Builds a NamingServerNode with test data for registration.
     */
    private Map<String, Object> buildNode(int controlPort, int transactionPort) {
        Map<String, Object> node = new HashMap<>();
        Map<String, Object> control = new HashMap<>();
        control.put("host", "127.0.0.1");
        control.put("port", controlPort);
        node.put("control", control);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("host", "127.0.0.1");
        transaction.put("port", transactionPort);
        node.put("transaction", transaction);

        Map<String, Object> internal = new HashMap<>();
        internal.put("host", "127.0.0.1");
        internal.put("port", controlPort + 1);
        node.put("internal", internal);

        node.put("unit", "default");
        node.put("version", "2.0.0");
        node.put("group", "default");
        node.put("role", "MEMBER");
        return node;
    }
}
