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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.rpc.Disposable;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;

/**
 * Metrics manager for init
 *
 */
public class MetricsManager implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsManager.class);

    private static class SingletonHolder {
        private static MetricsManager INSTANCE = new MetricsManager();
    }

    public static final MetricsManager get() {
        return MetricsManager.SingletonHolder.INSTANCE;
    }

    private Registry registry;
    private final List<Exporter> exporters = new ArrayList<>();
    private MetricsSubscriber subscriber;

    public Registry getRegistry() {
        return registry;
    }

    public synchronized void init() {
        if (registry != null) {
            return;
        }
        boolean enabled = ConfigurationFactory.getInstance()
                .getBoolean(
                        ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        if (enabled) {
            registry = RegistryFactory.getInstance();
            if (registry != null) {
                exporters.addAll(ExporterFactory.getInstanceList());
                // only at least one metrics exporter implement had imported in pom then need register MetricsSubscriber
                if (exporters.size() != 0) {
                    exporters.forEach(exporter -> exporter.setRegistry(registry));
                    subscriber = new MetricsSubscriber(registry);
                    EventBusManager.get().register(subscriber);
                }
            }
        }
    }

    @Override
    public synchronized void destroy() {
        if (subscriber != null) {
            EventBusManager.get().unregister(subscriber);
            subscriber = null;
        }
        for (Exporter exporter : exporters) {
            try {
                exporter.close();
            } catch (Exception e) {
                LOGGER.warn(
                        "Failed to close metrics exporter {}",
                        exporter.getClass().getName(),
                        e);
            }
        }
        if (!exporters.isEmpty()) {
            // Closed SPI instances cannot be reused when the server starts again.
            EnhancedServiceLoader.unload(Exporter.class);
            exporters.clear();
        }
        registry = null;
    }
}
