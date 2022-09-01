/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.metrics.service;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.metrics.event.EventBusManager;
import io.seata.metrics.exporter.Exporter;
import io.seata.metrics.exporter.ExporterFactory;
import io.seata.metrics.registry.Registry;
import io.seata.metrics.registry.RegistryFactory;

import java.util.List;

import static io.seata.metrics.IdConstants.ROLE_VALUE_SERVER;

/**
 * Metrics manager for init
 *
 * @author zhengyangyong
 */
public class MetricsManager {
    private static class SingletonHolder {
        private static MetricsManager INSTANCE = new MetricsManager();
    }

    public static final MetricsManager get() {
        return SingletonHolder.INSTANCE;
    }

    private Registry registry;

    private static String role = ROLE_VALUE_SERVER;

    public static void setRole(String role) {
        MetricsManager.role = role;
    }

    public String getRole() {
        return role;
    }


    public Registry getRegistry() {
        return registry;
    }

    public void init() {
        boolean enabled = getRole().equals(ROLE_VALUE_SERVER) ? ConfigurationFactory.getInstance().getBoolean(
                ConfigurationKeys.SERVER_METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, true) :
                ConfigurationFactory.getInstance().getBoolean(
                        ConfigurationKeys.CLIENT_METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, true);
        if (enabled) {
            registry = RegistryFactory.getInstance(this.getRole());
            if (registry != null) {
                List<Exporter> exporters = ExporterFactory.getInstanceList(this.getRole());
                //only at least one metrics exporter implement had imported in pom then need register MetricsSubscriber
                if (exporters.size() != 0) {
                    exporters.forEach(exporter -> exporter.setRegistry(registry));
                    EventBusManager.get().register(new MetricsSubscriber(registry));
                }
            }
        }
    }
}
