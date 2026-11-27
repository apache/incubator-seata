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
import org.apache.seata.core.event.EventBus;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.exporter.prometheus.PrometheusExporter;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.ServerRunner;
import org.apache.seata.server.event.EventBusManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;
import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_ENABLED;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_PREFIX;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@MockitoBean(types = ServerRunner.class)
class SharedPrometheusEndpointTest extends BaseSpringBootTest {
    @Test
    void publishesOnlyTheRegistrySelectedForSharedPrometheusScrapes() {
        Configuration config = mock(Configuration.class);
        PrometheusExporter exporter = mock(PrometheusExporter.class);
        Registry registry = mock(Registry.class);
        EventBus bus = mock(EventBus.class);

        when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                .thenReturn(true);
        when(config.getInt(METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT))
                .thenReturn(-1);
        when(exporter.usesTcPort()).thenReturn(true);
        try (MockedStatic<ConfigurationFactory> configs = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> exporters = mockStatic(ExporterFactory.class);
                MockedStatic<EventBusManager> eventBuses = mockStatic(EventBusManager.class)) {
            configs.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(registry);
            exporters.when(ExporterFactory::getInstanceList).thenReturn(Collections.singletonList(exporter));
            eventBuses.when(EventBusManager::get).thenReturn(bus);

            MetricsManager shared = new MetricsManager();
            shared.init();
            assertSame(registry, shared.getSharedPrometheusRegistry());
            shared.init();
            assertSame(registry, shared.getSharedPrometheusRegistry());

            // The SPI loader can still return a previously created shared exporter.
            when(config.getInt(METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT))
                    .thenReturn(9898);
            assertNull(startMetrics().getSharedPrometheusRegistry());

            when(config.getInt(METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT))
                    .thenReturn(-1);
            when(exporter.usesTcPort()).thenReturn(false);
            assertNull(startMetrics().getSharedPrometheusRegistry());

            when(exporter.usesTcPort()).thenReturn(true);
            when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                    .thenReturn(false);
            assertNull(startMetrics().getSharedPrometheusRegistry());

            when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                    .thenReturn(true);
            registries.when(RegistryFactory::getInstance).thenReturn(null);
            assertNull(startMetrics().getSharedPrometheusRegistry());

            registries.when(RegistryFactory::getInstance).thenReturn(registry);
            exporters.when(ExporterFactory::getInstanceList).thenReturn(Collections.emptyList());
            assertNull(startMetrics().getSharedPrometheusRegistry());
        }
    }

    private MetricsManager startMetrics() {
        MetricsManager manager = new MetricsManager();
        manager.init();
        return manager;
    }
}
