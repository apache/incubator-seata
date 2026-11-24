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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Namingserver login API.
 *
 * <p>Verifies the authentication endpoint {@code POST /api/v1/auth/login}
 * with valid and invalid credentials.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class LoginTests {

    /**
     * HTTP client for sending login requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver login endpoint URL.
     */
    String url = "http://127.0.0.1:8081/api/v1/auth/login";

    /**
     * Test login with correct username and password.
     *
     * <p>Expects a success response with a non-null token in the data field.
     */
    @Test
    void loginSuccess() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", "seata");
        body.put("password", "seata");
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals(Result.SUCCESS_CODE, singleResult.getCode());
        assertEquals(Result.SUCCESS_MSG, singleResult.getMessage());
        assertNotNull(singleResult.getData());
    }

    /**
     * Test login with correct username but incorrect password.
     *
     * <p>Expects a 401 response with "Login failed" message and null data.
     */
    @Test
    void loginPasswordFailed() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", "seata");
        body.put("password", UUID.randomUUID().toString());
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals("401", singleResult.getCode());
        assertEquals("Login failed", singleResult.getMessage());
        assertNull(singleResult.getData());
    }

    /**
     * Test login with non-existent username and random password.
     *
     * <p>Expects a 401 response with "Login failed" message and null data.
     */
    @Test
    void loginUsernameFailed() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", UUID.randomUUID().toString());
        body.put("password", UUID.randomUUID().toString());
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals("401", singleResult.getCode());
        assertEquals("Login failed", singleResult.getMessage());
        assertNull(singleResult.getData());
    }

    /**
     * Test login with empty username and correct password.
     *
     * <p>Expects a 401 response with "Login failed" message and null data.
     */
    @Test
    void loginUsernameEmpty() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", "");
        body.put("password", "seata");
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals("401", singleResult.getCode());
        assertEquals("Login failed", singleResult.getMessage());
        assertNull(singleResult.getData());
    }

    /**
     * Test login with correct username and empty password.
     *
     * <p>Expects a 401 response with "Login failed" message and null data.
     */
    @Test
    void loginPasswordEmpty() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", "seata");
        body.put("password", "");
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals("401", singleResult.getCode());
        assertEquals("Login failed", singleResult.getMessage());
        assertNull(singleResult.getData());
    }

    /**
     * Test login with empty username and null password.
     *
     * <p>Expects a 401 response with "Login failed" message and null data.
     */
    @Test
    void loginUsernameEmptyPasswordNull() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", "");
        body.put("password", null);
        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, httpHeaders);
        SingleResult<String> singleResult = restTemplate.postForObject(url, httpEntity, SingleResult.class);
        assertNotNull(singleResult);
        assertEquals("401", singleResult.getCode());
        assertEquals("Login failed", singleResult.getMessage());
        assertNull(singleResult.getData());
    }
}
