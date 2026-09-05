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
package org.apache.seata.server;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerConfigTest {

    private Configuration originalFileConfiguration;
    private Object originalEnvironment;

    @BeforeEach
    void setUp() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        SeataCoreEnvironmentPostProcessor.init();
        originalFileConfiguration = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    }

    @AfterEach
    void tearDown() {
        ConfigurationFactory.CURRENT_FILE_INSTANCE = originalFileConfiguration;
        if (originalEnvironment != null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }

    @Test
    void shouldUseRegistryConfigurationFirst() {
        Configuration registryConfiguration = mock(Configuration.class);
        Configuration configCenterConfiguration = mock(Configuration.class);
        ConfigurationFactory.CURRENT_FILE_INSTANCE = registryConfiguration;
        when(registryConfiguration.getConfig("registry.preferredNetworks")).thenReturn("172.30.31.*");

        try (MockedStatic<ConfigurationFactory> configurationFactory =
                mockStatic(ConfigurationFactory.class, CALLS_REAL_METHODS)) {
            configurationFactory.when(ConfigurationFactory::getInstance).thenReturn(configCenterConfiguration);

            assertEquals("172.30.31.*", Server.getRegistryConfig("registry.preferredNetworks"));
            verify(configCenterConfiguration, never()).getConfig("registry.preferredNetworks");
        }
    }

    @Test
    void shouldFallbackToConfigCenterWhenRegistryConfigurationMissing() {
        Configuration registryConfiguration = mock(Configuration.class);
        Configuration configCenterConfiguration = mock(Configuration.class);
        ConfigurationFactory.CURRENT_FILE_INSTANCE = registryConfiguration;
        when(registryConfiguration.getConfig("registry.ignoredInterfaces")).thenReturn(null);
        when(configCenterConfiguration.getConfig("registry.ignoredInterfaces")).thenReturn("bridge.*");

        try (MockedStatic<ConfigurationFactory> configurationFactory =
                mockStatic(ConfigurationFactory.class, CALLS_REAL_METHODS)) {
            configurationFactory.when(ConfigurationFactory::getInstance).thenReturn(configCenterConfiguration);

            assertEquals("bridge.*", Server.getRegistryConfig("registry.ignoredInterfaces"));
        }
    }
}
