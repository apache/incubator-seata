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
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.exporter.prometheus.PrometheusExporter;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;

import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;
import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_PREFIX;

/**
 * Metrics manager for init
 *
 */
public class MetricsManager {
    private static class SingletonHolder {
        private static MetricsManager INSTANCE = new MetricsManager();
    }

    public static final MetricsManager get() {
        return MetricsManager.SingletonHolder.INSTANCE;
    }

    private volatile Registry registry;
    private volatile boolean prometheusOnTcPort;

    public Registry getRegistry() {
        return registry;
    }

    public Registry getSharedPrometheusRegistry() {
        // An in-flight scrape must keep the registry whose endpoint it observed.
        Registry current = registry;
        return prometheusOnTcPort && current == registry ? current : null;
    }

    public void init() {
        prometheusOnTcPort = false;
        Configuration config = ConfigurationFactory.getInstance();
        boolean enabled =
                config.getBoolean(METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        if (enabled) {
            registry = RegistryFactory.getInstance();
            if (registry != null) {
                List<Exporter> exporters = ExporterFactory.getInstanceList();
                // only at least one metrics exporter implement had imported in pom then need register MetricsSubscriber
                if (exporters.size() != 0) {
                    exporters.forEach(exporter -> exporter.setRegistry(registry));
                    EventBusManager.get().register(new MetricsSubscriber(registry));
                    // An SPI instance can survive a restart with different port settings.
                    if (config.getInt(METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT)
                            == -1) {
                        prometheusOnTcPort = exporters.stream()
                                .anyMatch(exporter -> exporter instanceof PrometheusExporter
                                        && ((PrometheusExporter) exporter).usesTcPort());
                    }
                }
            }
        }
    }
}
