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
package org.apache.seata.namingserver.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.listener.ClusterChangePushEvent;
import org.apache.seata.namingserver.listener.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Prometheus-based implementation of NamingServerMetricsManager.
 * Uses Micrometer MultiGauge for dynamic tag management and automatic cleanup of stale metrics.
 */
@Component
@ConditionalOnProperty(name = "seata.namingserver.metrics.enabled", havingValue = "true")
public class PrometheusNamingMetricsManager implements NamingServerMetricsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusNamingMetricsManager.class);

    private final MeterRegistry meterRegistry;

    private MultiGauge clusterNodeCountGauge;

    private MultiGauge watcherCountGauge;

    private final ConcurrentMap<String, Counter> clusterChangePushCounters = new ConcurrentHashMap<>();

    private Supplier<ConcurrentMap<String, ConcurrentMap<String, ClusterData>>> namespaceClusterDataSupplier;
    private Supplier<Map<String, Queue<Watcher<?>>>> watchersSupplier;

    public PrometheusNamingMetricsManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // Initialize MultiGauge for cluster node count
        this.clusterNodeCountGauge = MultiGauge.builder(METRIC_CLUSTER_NODE_COUNT)
                .description("Number of alive Seata Server nodes in the registry")
                .register(meterRegistry);

        // Initialize MultiGauge for watcher count
        this.watcherCountGauge = MultiGauge.builder(METRIC_WATCHER_COUNT)
                .description("Number of HTTP connections waiting for change notifications (long polling)")
                .register(meterRegistry);

        LOGGER.info("NamingServer Prometheus metrics manager initialized with event-driven refresh");
    }

    /**
     * Listens for ClusterChangePushEvent and increments the push counter.
     */
    @EventListener
    public void onClusterChangePush(ClusterChangePushEvent event) {
        incrementClusterChangePushCount(event.getNamespace(), event.getClusterName(), event.getVgroup());
    }

    @Override
    public void setNamespaceClusterDataSupplier(
            Supplier<ConcurrentMap<String, ConcurrentMap<String, ClusterData>>> supplier) {
        this.namespaceClusterDataSupplier = supplier;
    }

    @Override
    public void setWatchersSupplier(Supplier<Map<String, Queue<Watcher<?>>>> supplier) {
        this.watchersSupplier = supplier;
    }

    @Override
    public void refreshClusterNodeCountMetrics() {
        if (namespaceClusterDataSupplier == null) {
            return;
        }

        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        ConcurrentMap<String, ConcurrentMap<String, ClusterData>> namespaceClusterDataMap =
                namespaceClusterDataSupplier.get();

        if (namespaceClusterDataMap != null) {
            namespaceClusterDataMap.forEach((namespace, clusterDataMap) -> {
                if (clusterDataMap != null) {
                    clusterDataMap.forEach((clusterName, clusterData) -> {
                        if (clusterData != null && clusterData.getUnitData() != null) {
                            Map<String, Unit> unitData = clusterData.getUnitData();
                            unitData.forEach((unitName, unit) -> {
                                int nodeCount = 0;
                                if (unit != null && unit.getNamingInstanceList() != null) {
                                    nodeCount = unit.getNamingInstanceList().size();
                                }
                                rows.add(MultiGauge.Row.of(
                                        Tags.of(
                                                TAG_NAMESPACE, namespace,
                                                TAG_CLUSTER, clusterName,
                                                TAG_UNIT, unitName),
                                        nodeCount));
                            });
                        }
                    });
                }
            });
        }

        clusterNodeCountGauge.register(rows, true);
    }

    @Override
    public void refreshWatcherCountMetrics() {
        if (watchersSupplier == null) {
            return;
        }

        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        Map<String, Queue<Watcher<?>>> watchers = watchersSupplier.get();

        if (watchers != null) {
            watchers.forEach((vgroup, watcherQueue) -> {
                int count = 0;
                if (watcherQueue != null) {
                    count = watcherQueue.size();
                }
                rows.add(MultiGauge.Row.of(Tags.of(TAG_VGROUP, vgroup), count));
            });
        }

        watcherCountGauge.register(rows, true);
    }

    private void incrementClusterChangePushCount(String namespace, String cluster, String vgroup) {
        String key = namespace + "|" + cluster + "|" + vgroup;
        Counter counter =
                clusterChangePushCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_CLUSTER_CHANGE_PUSH_TOTAL)
                        .description("Total number of cluster change push notifications to watchers")
                        .tag(TAG_NAMESPACE, namespace)
                        .tag(TAG_CLUSTER, cluster)
                        .tag(TAG_VGROUP, vgroup)
                        .register(meterRegistry));
        counter.increment();
    }

    /**
     * Gets the current count of cluster change push notifications.
     * Exposed for testing purposes.
     */
    public double getClusterChangePushCount(String namespace, String cluster, String vgroup) {
        String key = namespace + "|" + cluster + "|" + vgroup;
        Counter counter = clusterChangePushCounters.get(key);
        return counter != null ? counter.count() : 0;
    }
}
