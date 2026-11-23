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
 * Tests for Namingserver instance unregister API.
 *
 * <p>Verifies the unregister endpoint {@code POST /api/v1/naming/unregister}
 * for removing a Seata server instance from the naming server registry.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class UnregisterInstanceTests {

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
     * Test unregistering a Seata server instance.
     *
     * <p>First registers a node, then unregisters it.
     * Expects a success response indicating the node was unregistered.
     */
    @Test
    void unregisterSuccess() {

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

        Map<String, Object> node = buildNode();

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("namespace", "public");
        uriVariables.put("clusterName", "default");
        uriVariables.put("unit", "default");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        // Step 1: Register the node first
        HttpEntity<Map<String, Object>> registerEntity = new HttpEntity<>(node, httpHeaders);
        Result<?> registerResult = restTemplate.postForObject(registerUrl, registerEntity, Result.class, uriVariables);
        assertNotNull(registerResult);
        assertEquals(Result.SUCCESS_CODE, registerResult.getCode());

        // Step 2: Unregister the node
        HttpEntity<Map<String, Object>> unregisterEntity = new HttpEntity<>(node, httpHeaders);
        Result<?> unregisterResult =
                restTemplate.postForObject(unregisterUrl, unregisterEntity, Result.class, uriVariables);
        assertNotNull(unregisterResult);
        assertEquals(Result.SUCCESS_CODE, unregisterResult.getCode());
        assertEquals("node has unregistered successfully!", unregisterResult.getMessage());
    }

    /**
     * Builds a NamingServerNode with test data for registration.
     *
     * @return a map representing the NamingServerNode
     */
    private Map<String, Object> buildNode() {
        Map<String, Object> node = new HashMap<>();
        Map<String, Object> control = new HashMap<>();
        control.put("host", "127.0.0.1");
        control.put("port", 28091);
        node.put("control", control);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("host", "127.0.0.1");
        transaction.put("port", 28092);
        node.put("transaction", transaction);

        Map<String, Object> internal = new HashMap<>();
        internal.put("host", "127.0.0.1");
        internal.put("port", 28093);
        node.put("internal", internal);

        node.put("version", "2.0.0");
        node.put("group", "default");
        node.put("role", "MEMBER");
        return node;
    }
}
