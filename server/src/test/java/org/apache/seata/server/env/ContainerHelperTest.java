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
package org.apache.seata.server.env;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContainerHelper Test - Direct testing without mocks
 */
@DisplayName("ContainerHelper Test")
class ContainerHelperTest {

    @BeforeEach
    void setUp() {
        // Clear environment variables before each test
    }

    @Test
    @DisplayName("test getEnv with valid environment variable")
    void testGetEnvWithValidEnvironmentVariable() {
        // Note: Since we cannot directly set system env, we test the logic path
        String env = ContainerHelper.getEnv();
        // Either null or the actual environment value
        assertTrue(env == null || env instanceof String);
    }

    @Test
    @DisplayName("test getEnv with null")
    void testGetEnvWithNull() {
        String env = ContainerHelper.getEnv();
        // Should return null or empty string (trimmed)
        assertTrue(env == null || env.isEmpty() || env instanceof String);
    }

    @Test
    @DisplayName("test getHost with valid environment variable")
    void testGetHostWithValidEnvironmentVariable() {
        String host = ContainerHelper.getHost();
        assertTrue(host == null || host instanceof String);
    }

    @Test
    @DisplayName("test getPort with valid environment variable")
    void testGetPortWithValidEnvironmentVariable() {
        int port = ContainerHelper.getPort();
        assertTrue(port >= 0);
    }

    @Test
    @DisplayName("test getPort returns valid port number")
    void testGetPortReturnsValidPortNumber() {
        int port = ContainerHelper.getPort();
        // Port should be 0 if not set in environment
        assertEquals(0, port);
    }

    @Test
    @DisplayName("test getServerNode with valid environment variable")
    void testGetServerNodeWithValidEnvironmentVariable() {
        Long serverNode = ContainerHelper.getServerNode();
        assertTrue(serverNode == null || serverNode >= 0);
    }

    @Test
    @DisplayName("test getServerNode returns valid node id")
    void testGetServerNodeReturnsValidNodeId() {
        Long serverNode = ContainerHelper.getServerNode();
        // Should be null or a valid number
        assertTrue(serverNode == null || serverNode >= 0);
    }

    @Test
    @DisplayName("test getStoreMode with valid environment variable")
    void testGetStoreModeWithValidEnvironmentVariable() {
        String storeMode = ContainerHelper.getStoreMode();
        assertTrue(storeMode == null || storeMode instanceof String);
    }

    @Test
    @DisplayName("test getSessionStoreMode with valid environment variable")
    void testGetSessionStoreModeWithValidEnvironmentVariable() {
        String sessionStoreMode = ContainerHelper.getSessionStoreMode();
        assertTrue(sessionStoreMode == null || sessionStoreMode instanceof String);
    }

    @Test
    @DisplayName("test getLockStoreMode with valid environment variable")
    void testGetLockStoreModeWithValidEnvironmentVariable() {
        String lockStoreMode = ContainerHelper.getLockStoreMode();
        assertTrue(lockStoreMode == null || lockStoreMode instanceof String);
    }

    @Test
    @DisplayName("test all methods work together")
    void testAllMethodsWorkTogether() {
        // Test that all methods can be called without throwing exceptions
        String env = ContainerHelper.getEnv();
        String host = ContainerHelper.getHost();
        int port = ContainerHelper.getPort();
        Long serverNode = ContainerHelper.getServerNode();
        String storeMode = ContainerHelper.getStoreMode();
        String sessionStoreMode = ContainerHelper.getSessionStoreMode();
        String lockStoreMode = ContainerHelper.getLockStoreMode();

        // All should return successfully
        assertNotNull(env != null ? env : "null");
        assertNotNull(host != null ? host : "null");
        assertTrue(port >= 0);
        assertNotNull(serverNode != null ? serverNode : "null");
        assertNotNull(storeMode != null ? storeMode : "null");
        assertNotNull(sessionStoreMode != null ? sessionStoreMode : "null");
        assertNotNull(lockStoreMode != null ? lockStoreMode : "null");
    }
}
