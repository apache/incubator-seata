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
import org.apache.seata.metrics.exporter.prometheus.PrometheusExporter;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;

/**
 * Manages the transaction coordinator's metrics resources.
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

    private volatile Registry registry;
    private final Set<Exporter> exporters = new LinkedHashSet<>();
    private volatile PrometheusExporter sharedPrometheusExporter;
    private MetricsSubscriber subscriber;

    public Registry getRegistry() {
        return registry;
    }

    public PrometheusExporter getSharedPrometheusExporter() {
        return sharedPrometheusExporter;
    }

    public void init() {
        if (subscriber != null) {
            return;
        }
        boolean enabled = ConfigurationFactory.getInstance()
                .getBoolean(
                        ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        if (!enabled) {
            return;
        }
        try {
            registry = RegistryFactory.getInstance();
            if (registry == null) {
                return;
            }
            exporters.addAll(ExporterFactory.getInstanceList());
            if (!exporters.isEmpty()) {
                PrometheusExporter shared = null;
                for (Exporter exporter : exporters) {
                    exporter.setRegistry(registry);
                    if (exporter instanceof PrometheusExporter && ((PrometheusExporter) exporter).usesTcPort()) {
                        shared = (PrometheusExporter) exporter;
                    }
                }
                subscriber = new MetricsSubscriber(registry);
                EventBusManager.get().register(subscriber);
                sharedPrometheusExporter = shared;
            }
        } catch (RuntimeException | Error failure) {
            try {
                destroy();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Override
    public void destroy() {
        sharedPrometheusExporter = null;
        if (subscriber != null) {
            subscriber.deactivate();
            try {
                EventBusManager.get().unregister(subscriber);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to unregister metrics subscriber", e);
            } finally {
                subscriber = null;
            }
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
            exporters.clear();
            // The next server start needs fresh SPI instances to reopen their listeners.
            EnhancedServiceLoader.unload(Exporter.class);
        }
        if (registry != null) {
            Registry previousRegistry = registry;
            registry = null;
            try {
                previousRegistry.clearUp();
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to clear metrics registry", e);
            } finally {
                // An in-flight callback can finish against the retired registry after shutdown.
                EnhancedServiceLoader.unload(Registry.class);
            }
        }
    }
}
