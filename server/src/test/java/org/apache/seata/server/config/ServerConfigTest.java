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
package org.apache.seata.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerConfig Test - Direct testing without mocks
 */
@DisplayName("ServerConfig Test")
class ServerConfigTest {

    @Test
    @DisplayName("test bean creation")
    void testBeanCreation() {
        ServerConfig serverConfig = new ServerConfig();
        assertNotNull(serverConfig);
    }

    @Test
    @DisplayName("test emptyServerProperties bean creation")
    void testEmptyServerPropertiesBeanCreation() {
        ServerConfig serverConfig = new ServerConfig();
        ServerProperties properties = serverConfig.emptyServerProperties();

        assertNotNull(properties);
    }

    @Test
    @DisplayName("test emptyServerProperties returns ServerProperties instance")
    void testEmptyServerPropertiesReturnsServerPropertiesInstance() {
        ServerConfig serverConfig = new ServerConfig();
        ServerProperties properties = serverConfig.emptyServerProperties();

        assertNotNull(properties);
        assertTrue(properties instanceof ServerProperties);
    }

    @Test
    @DisplayName("test multiple calls to emptyServerProperties")
    void testMultipleCallsToEmptyServerProperties() {
        ServerConfig serverConfig = new ServerConfig();
        ServerProperties properties1 = serverConfig.emptyServerProperties();
        ServerProperties properties2 = serverConfig.emptyServerProperties();

        assertNotNull(properties1);
        assertNotNull(properties2);
        // They might be different instances (depending on bean scope)
        assertTrue(properties1 instanceof ServerProperties);
        assertTrue(properties2 instanceof ServerProperties);
    }

    @Test
    @DisplayName("test configuration annotation present")
    void testConfigurationAnnotationPresent() {
        assertTrue(ServerConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    @DisplayName("test ServerProperties has bean annotation")
    void testServerPropertiesHasBeanAnnotation() {
        try {
            java.lang.reflect.Method method = ServerConfig.class.getMethod("emptyServerProperties");
            assertTrue(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class));
        } catch (NoSuchMethodException e) {
            fail("emptyServerProperties method not found");
        }
    }
}
