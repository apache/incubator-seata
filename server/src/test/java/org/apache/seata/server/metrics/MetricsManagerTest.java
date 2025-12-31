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

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MetricsManager Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsManager Test")
class MetricsManagerTest {

    @BeforeEach
    void setUp() {
        // Reset singleton for testing
    }

    @Test
    @DisplayName("test get singleton instance")
    void testGetSingletonInstance() {
        MetricsManager manager1 = MetricsManager.get();
        MetricsManager manager2 = MetricsManager.get();
        assertSame(manager1, manager2);
    }

    @Test
    @DisplayName("test init with metrics disabled")
    void testInitWithMetricsDisabled() {
        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class)) {
            Configuration mockConfig = mock(Configuration.class);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(anyString(), eq(DEFAULT_METRICS_ENABLED)))
                    .thenReturn(false);

            MetricsManager manager = MetricsManager.get();
            manager.init();

            assertNull(manager.getRegistry());
        }
    }

    @Test
    @DisplayName("test init with metrics enabled but no registry")
    void testInitWithMetricsEnabledButNoRegistry() {
        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registryMock = mockStatic(RegistryFactory.class)) {

            Configuration mockConfig = mock(Configuration.class);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(anyString(), eq(DEFAULT_METRICS_ENABLED)))
                    .thenReturn(true);
            registryMock.when(RegistryFactory::getInstance).thenReturn(null);

            MetricsManager manager = MetricsManager.get();
            manager.init();

            assertNull(manager.getRegistry());
        }
    }

    @Test
    @DisplayName("test init with metrics enabled and registry but no exporters")
    void testInitWithMetricsEnabledAndRegistryButNoExporters() {
        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registryMock = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> exporterMock = mockStatic(ExporterFactory.class)) {

            Configuration mockConfig = mock(Configuration.class);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(anyString(), eq(DEFAULT_METRICS_ENABLED)))
                    .thenReturn(true);

            Registry mockRegistry = mock(Registry.class);
            registryMock.when(RegistryFactory::getInstance).thenReturn(mockRegistry);

            exporterMock.when(ExporterFactory::getInstanceList).thenReturn(Collections.emptyList());

            MetricsManager manager = MetricsManager.get();
            manager.init();

            assertNotNull(manager.getRegistry());
        }
    }

    @Test
    @DisplayName("test init with metrics enabled, registry and exporters")
    void testInitWithMetricsEnabledRegistryAndExporters() {
        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registryMock = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> exporterMock = mockStatic(ExporterFactory.class);
                MockedStatic<EventBusManager> eventBusMock = mockStatic(EventBusManager.class)) {

            Configuration mockConfig = mock(Configuration.class);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(anyString(), eq(DEFAULT_METRICS_ENABLED)))
                    .thenReturn(true);

            Registry mockRegistry = mock(Registry.class);
            registryMock.when(RegistryFactory::getInstance).thenReturn(mockRegistry);

            List<Exporter> exporters = new ArrayList<>();
            Exporter mockExporter = mock(Exporter.class);
            exporters.add(mockExporter);
            exporterMock.when(ExporterFactory::getInstanceList).thenReturn(exporters);

            EventBusManager mockEventBus = mock(EventBusManager.class);
            eventBusMock.when(EventBusManager::get).thenReturn(mockEventBus);

            MetricsManager manager = MetricsManager.get();
            manager.init();

            assertNotNull(manager.getRegistry());
            verify(mockExporter).setRegistry(mockRegistry);
        }
    }

    @Test
    @DisplayName("test getRegistry before init")
    void testGetRegistryBeforeInit() {
        MetricsManager manager = MetricsManager.get();
        assertNull(manager.getRegistry());
    }

    @Test
    @DisplayName("test multiple exporters registration")
    void testMultipleExportersRegistration() {
        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registryMock = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> exporterMock = mockStatic(ExporterFactory.class);
                MockedStatic<EventBusManager> eventBusMock = mockStatic(EventBusManager.class)) {

            Configuration mockConfig = mock(Configuration.class);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(anyString(), eq(DEFAULT_METRICS_ENABLED)))
                    .thenReturn(true);

            Registry mockRegistry = mock(Registry.class);
            registryMock.when(RegistryFactory::getInstance).thenReturn(mockRegistry);

            List<Exporter> exporters = new ArrayList<>();
            Exporter exporter1 = mock(Exporter.class);
            Exporter exporter2 = mock(Exporter.class);
            exporters.add(exporter1);
            exporters.add(exporter2);
            exporterMock.when(ExporterFactory::getInstanceList).thenReturn(exporters);

            EventBusManager mockEventBus = mock(EventBusManager.class);
            eventBusMock.when(EventBusManager::get).thenReturn(mockEventBus);

            MetricsManager manager = MetricsManager.get();
            manager.init();

            verify(exporter1).setRegistry(mockRegistry);
            verify(exporter2).setRegistry(mockRegistry);
        }
    }
}
