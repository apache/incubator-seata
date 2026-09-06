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
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Namingserver health check API.
 *
 * <p>Verifies the health check endpoint {@code GET /naming/v1/health}
 * returns a success response. This endpoint is in the security ignore list
 * and does not require authentication.
 *
 * <p><b>Prerequisite:</b> These are integration tests that send HTTP requests to a running
 * Namingserver native binary on {@code 127.0.0.1:8081}. The CI workflow starts the binary
 * automatically, but running locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets) and starting
 * the resulting binary before executing the tests.
 */
class HealthTests {

    /**
     * HTTP client for sending requests.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Namingserver health check endpoint URL.
     */
    String url = "http://127.0.0.1:8081/naming/v1/health";

    /**
     * Test the health check endpoint returns success.
     *
     * <p>Expects a response with code "200" and message "success".
     */
    @Test
    void healthSuccess() {
        Result<?> result = restTemplate.getForObject(url, Result.class);
        assertNotNull(result);
        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals(Result.SUCCESS_MSG, result.getMessage());
    }
}
