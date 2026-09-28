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
package org.apache.seata.server.metrics;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.event.EventBus;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MetricsManagerTest {
    private static Object originalEnvironment;

    @BeforeAll
    static void provideEnvironmentForConfigurationSpi() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        if (originalEnvironment == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new StandardEnvironment());
        }
    }

    @AfterAll
    static void restoreEnvironment() {
        if (originalEnvironment == null) {
            Map<?, ?> objects = (Map<?, ?>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
            objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        }
    }

    @Test
    void closesExportersAndUnregistersSubscriberOnce() throws Exception {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        Registry registry = mock(Registry.class);
        EventBus bus = mock(EventBus.class);
        Exporter first = mock(Exporter.class);
        Exporter second = mock(Exporter.class);
        doThrow(new IOException("injected close failure")).when(first).close();
        MetricsManager manager = new MetricsManager();
        try (MockedStatic<ConfigurationFactory> configs = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> factories = mockStatic(ExporterFactory.class);
                MockedStatic<EventBusManager> buses = mockStatic(EventBusManager.class)) {
            configs.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(registry);
            factories.when(ExporterFactory::getInstanceList).thenReturn(Arrays.asList(first, second));
            buses.when(EventBusManager::get).thenReturn(bus);
            manager.init();
            manager.init();
            ArgumentCaptor<Object> subscriber = ArgumentCaptor.forClass(Object.class);
            verify(bus).register(subscriber.capture());
            verify(first).setRegistry(registry);
            verify(second).setRegistry(registry);
            manager.destroy();
            manager.destroy();
            verify(bus).unregister(subscriber.getValue());
            verify(first).close();
            verify(second).close();
            assertNull(manager.getRegistry());
            factories.verify(ExporterFactory::getInstanceList);
        } finally {
            manager.destroy();
        }
    }

    @Test
    void releasesPrometheusPortAndCanRestart() throws Exception {
        int port;
        try (ServerSocket available = new ServerSocket(0)) {
            port = available.getLocalPort();
        }
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(config.getInt(anyString(), anyInt())).thenReturn(port);
        when(config.getConfig(anyString(), anyString())).thenReturn("prometheus");
        Registry registry = mock(Registry.class);
        MetricsManager manager = new MetricsManager();
        try (MockedStatic<ConfigurationFactory> configs = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class)) {
            configs.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(registry);

            for (int i = 0; i < 2; i++) {
                try {
                    manager.init();
                    assertSame(registry, manager.getRegistry());
                    assertThrows(IOException.class, () -> {
                        try (ServerSocket occupied = new ServerSocket(port)) {
                            fail("Exporter should own the port");
                        }
                    });
                } finally {
                    manager.destroy();
                }
                try (ServerSocket released = new ServerSocket(port)) {
                    assertEquals(port, released.getLocalPort());
                }
            }
        }
    }
}
