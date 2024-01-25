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

import java.util.List;

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;

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

    private Registry registry;

    public Registry getRegistry() {
        return registry;
    }

    public void init() {
        boolean enabled = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        if (enabled) {
            registry = RegistryFactory.getInstance();
            if (registry != null) {
                List<Exporter> exporters = ExporterFactory.getInstanceList();
                //only at least one metrics exporter implement had imported in pom then need register MetricsSubscriber
                if (exporters.size() != 0) {
                    exporters.forEach(exporter -> exporter.setRegistry(registry));
                    EventBusManager.get().register(new MetricsSubscriber(registry));
                }
            }
        }
    }
}
