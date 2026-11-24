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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Namingserver batch register API.
 *
 * <p>Verifies the batch register endpoint {@code POST /api/v1/naming/batchRegister}
 * for registering multiple Seata server instances at once.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class BatchRegisterTests {

    /**
     * HTTP client for sending requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver batch register endpoint URL.
     */
    String batchRegisterUrl =
            "http://127.0.0.1:8081/api/v1/naming/batchRegister?namespace={namespace}&clusterName={clusterName}";

    /**
     * Namingserver unregister endpoint URL.
     */
    String unregisterUrl =
            "http://127.0.0.1:8081/api/v1/naming/unregister?namespace={namespace}&clusterName={clusterName}&unit={unit}";

    /**
     * Test batch registering multiple Seata server instances.
     *
     * <p>Expects a success response indicating all nodes were registered.
     */
    @Test
    void batchRegisterSuccess() {

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

        String unit1 = "unit-1";
        String unit2 = "unit-2";
        Map<String, Object> built1 = buildNode("127.0.0.1", 38091, 38092, 38093, unit1);
        Map<String, Object> built2 = buildNode("127.0.0.1", 48091, 48092, 48093, unit2);
        {
            List<Map<String, Object>> nodes = new ArrayList<>();
            nodes.add(built1);
            nodes.add(built2);

            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("namespace", "public");
            uriVariables.put("clusterName", "default");
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map<String, Object>>> httpEntity = new HttpEntity<>(nodes, httpHeaders);

            Result<?> result = restTemplate.postForObject(batchRegisterUrl, httpEntity, Result.class, uriVariables);
            assertNotNull(result);
            assertEquals(Result.SUCCESS_CODE, result.getCode());
            assertEquals("node has registered successfully!", result.getMessage());
        }

        {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("namespace", "public");
            uriVariables.put("clusterName", "default");

            {
                uriVariables.put("unit", unit1);
                HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(built1, httpHeaders);
                Result<?> unregisterResult =
                        restTemplate.postForObject(unregisterUrl, httpEntity, Result.class, uriVariables);
                assertNotNull(unregisterResult);
                assertEquals(Result.SUCCESS_CODE, unregisterResult.getCode());
            }

            {
                uriVariables.put("unit", unit2);
                HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(built2, httpHeaders);
                Result<?> unregisterResult =
                        restTemplate.postForObject(unregisterUrl, httpEntity, Result.class, uriVariables);
                assertNotNull(unregisterResult);
                assertEquals(Result.SUCCESS_CODE, unregisterResult.getCode());
            }
        }
    }

    /**
     * Builds a NamingServerNode with specified ports and unit name.
     *
     * @param host            the host address
     * @param controlPort     the control port
     * @param transactionPort the transaction port
     * @param internalPort    the internal port
     * @param unit            the unit name
     * @return a map representing the NamingServerNode
     */
    private Map<String, Object> buildNode(
            String host, int controlPort, int transactionPort, int internalPort, String unit) {
        Map<String, Object> node = new HashMap<>();
        Map<String, Object> control = new HashMap<>();
        control.put("host", host);
        control.put("port", controlPort);
        node.put("control", control);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("host", host);
        transaction.put("port", transactionPort);
        node.put("transaction", transaction);

        Map<String, Object> internal = new HashMap<>();
        internal.put("host", host);
        internal.put("port", internalPort);
        node.put("internal", internal);

        node.put("unit", unit);
        node.put("version", "2.0.0");
        node.put("group", "default");
        node.put("role", "MEMBER");
        return node;
    }
}
