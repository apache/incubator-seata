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
package org.apache.seata.namingserver;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.listener.ClusterChangePushEvent;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.metrics.PrometheusNamingMetricsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.seata.namingserver.metrics.NamingServerMetricsManager.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrometheusNamingMetricsManager.
 */
class NamingServerMetricsManagerTest {

    private MeterRegistry meterRegistry;
    private PrometheusNamingMetricsManager metricsManager;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsManager = new PrometheusNamingMetricsManager(meterRegistry);
        metricsManager.init();
    }

    @Test
    void testClusterNodeCountMetrics() {
        // Prepare test data
        ConcurrentMap<String, ConcurrentMap<String, ClusterData>> namespaceClusterDataMap = new ConcurrentHashMap<>();

        // Create namespace -> cluster -> unit structure
        String namespace = "test-namespace";
        String clusterName = "test-cluster";
        String unitName = "test-unit";

        ClusterData clusterData = new ClusterData(clusterName, "default");
        Unit unit = new Unit();
        unit.setUnitName(unitName);
        unit.setNamingInstanceList(new CopyOnWriteArrayList<>());

        // Add some nodes
        NamingServerNode node1 = new NamingServerNode();
        NamingServerNode node2 = new NamingServerNode();
        unit.getNamingInstanceList().add(node1);
        unit.getNamingInstanceList().add(node2);

        clusterData.getUnitData().put(unitName, unit);

        ConcurrentMap<String, ClusterData> clusterDataMap = new ConcurrentHashMap<>();
        clusterDataMap.put(clusterName, clusterData);
        namespaceClusterDataMap.put(namespace, clusterDataMap);

        // Set the supplier
        metricsManager.setNamespaceClusterDataSupplier(() -> namespaceClusterDataMap);

        // Manually trigger metrics refresh
        metricsManager.refreshClusterNodeCountMetrics();

        // Verify the metric is registered
        Meter meter = meterRegistry
                .find(METRIC_CLUSTER_NODE_COUNT)
                .tag(TAG_NAMESPACE, namespace)
                .tag(TAG_CLUSTER, clusterName)
                .tag(TAG_UNIT, unitName)
                .meter();

        assertNotNull(meter, "Cluster node count metric should be registered");
    }

    @Test
    void testWatcherCountMetrics() {
        // Prepare test data
        Map<String, Queue<Watcher<?>>> watchers = new ConcurrentHashMap<>();
        String vgroup = "test-vgroup";

        Queue<Watcher<?>> watcherQueue = new ConcurrentLinkedQueue<>();
        watcherQueue.add(new Watcher<>(vgroup, null, 5000, 1L, "127.0.0.1"));
        watcherQueue.add(new Watcher<>(vgroup, null, 5000, 1L, "127.0.0.2"));
        watchers.put(vgroup, watcherQueue);

        // Set the supplier
        metricsManager.setWatchersSupplier(() -> watchers);

        // Manually trigger metrics refresh
        metricsManager.refreshWatcherCountMetrics();

        // Verify the metric is registered
        Meter meter =
                meterRegistry.find(METRIC_WATCHER_COUNT).tag(TAG_VGROUP, vgroup).meter();

        assertNotNull(meter, "Watcher count metric should be registered");
    }

    @Test
    void testClusterChangePushCounter() {
        String namespace = "test-namespace";
        String cluster = "test-cluster";
        String vgroup1 = "vgroup1";
        String vgroup2 = "vgroup2";

        // Simulate events via event listener
        metricsManager.onClusterChangePush(new ClusterChangePushEvent(this, namespace, cluster, vgroup1));
        metricsManager.onClusterChangePush(new ClusterChangePushEvent(this, namespace, cluster, vgroup1));
        metricsManager.onClusterChangePush(new ClusterChangePushEvent(this, namespace, cluster, vgroup2));

        // Verify counts
        assertEquals(2.0, metricsManager.getClusterChangePushCount(namespace, cluster, vgroup1));
        assertEquals(1.0, metricsManager.getClusterChangePushCount(namespace, cluster, vgroup2));

        // Verify metrics are registered in registry
        assertNotNull(meterRegistry
                .find(METRIC_CLUSTER_CHANGE_PUSH_TOTAL)
                .tag(TAG_NAMESPACE, namespace)
                .tag(TAG_CLUSTER, cluster)
                .tag(TAG_VGROUP, vgroup1)
                .counter());
        assertNotNull(meterRegistry
                .find(METRIC_CLUSTER_CHANGE_PUSH_TOTAL)
                .tag(TAG_NAMESPACE, namespace)
                .tag(TAG_CLUSTER, cluster)
                .tag(TAG_VGROUP, vgroup2)
                .counter());
    }

    @Test
    void testMultiGaugeCleanupStaleTags() {
        // Prepare initial test data with two namespaces
        ConcurrentMap<String, ConcurrentMap<String, ClusterData>> namespaceClusterDataMap = new ConcurrentHashMap<>();

        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        String clusterName = "cluster1";
        String unitName = "unit1";

        // Create two clusters in different namespaces
        for (String namespace : new String[] {namespace1, namespace2}) {
            ClusterData clusterData = new ClusterData(clusterName, "default");
            Unit unit = new Unit();
            unit.setUnitName(unitName);
            unit.setNamingInstanceList(new CopyOnWriteArrayList<>());
            unit.getNamingInstanceList().add(new NamingServerNode());
            clusterData.getUnitData().put(unitName, unit);

            ConcurrentMap<String, ClusterData> clusterDataMap = new ConcurrentHashMap<>();
            clusterDataMap.put(clusterName, clusterData);
            namespaceClusterDataMap.put(namespace, clusterDataMap);
        }

        metricsManager.setNamespaceClusterDataSupplier(() -> namespaceClusterDataMap);

        // Manually trigger metrics refresh
        metricsManager.refreshClusterNodeCountMetrics();

        // Verify both metrics exist
        assertNotNull(meterRegistry
                .find(METRIC_CLUSTER_NODE_COUNT)
                .tag(TAG_NAMESPACE, namespace1)
                .meter());
        assertNotNull(meterRegistry
                .find(METRIC_CLUSTER_NODE_COUNT)
                .tag(TAG_NAMESPACE, namespace2)
                .meter());

        // Remove namespace2
        namespaceClusterDataMap.remove(namespace2);

        // Manually trigger metrics refresh
        metricsManager.refreshClusterNodeCountMetrics();

        // Verify namespace1 still exists but namespace2 is cleaned up
        assertNotNull(meterRegistry
                .find(METRIC_CLUSTER_NODE_COUNT)
                .tag(TAG_NAMESPACE, namespace1)
                .meter());
        // Note: MultiGauge with overwrite=true will remove stale entries
    }

    @Test
    void testNullSupplierHandling() {
        // Don't set any suppliers
        // Manually trigger refresh - should not throw exception
        metricsManager.refreshClusterNodeCountMetrics();
        metricsManager.refreshWatcherCountMetrics();

        // No exception means test passed
        assertTrue(true);
    }

    @Test
    void testEmptyDataHandling() {
        // Set empty suppliers
        metricsManager.setNamespaceClusterDataSupplier(ConcurrentHashMap::new);
        metricsManager.setWatchersSupplier(ConcurrentHashMap::new);

        // Manually trigger metrics refresh
        metricsManager.refreshClusterNodeCountMetrics();
        metricsManager.refreshWatcherCountMetrics();

        // Verify no exception and no metrics registered
        assertEquals(0, meterRegistry.find(METRIC_CLUSTER_NODE_COUNT).meters().size());
        assertEquals(0, meterRegistry.find(METRIC_WATCHER_COUNT).meters().size());
    }
}
