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

import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.listener.Watcher;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public interface NamingServerMetricsManager {

    // Metric names
    String METRIC_CLUSTER_NODE_COUNT = "seata_namingserver_cluster_node_count";
    String METRIC_WATCHER_COUNT = "seata_namingserver_watcher_count";
    String METRIC_CLUSTER_CHANGE_PUSH_TOTAL = "seata_namingserver_cluster_change_push_total";

    // Tag names
    String TAG_NAMESPACE = "namespace";
    String TAG_CLUSTER = "cluster";
    String TAG_UNIT = "unit";
    String TAG_VGROUP = "vgroup";

    void setNamespaceClusterDataSupplier(Supplier<ConcurrentMap<String, ConcurrentMap<String, ClusterData>>> supplier);

    void setWatchersSupplier(Supplier<Map<String, Queue<Watcher<?>>>> supplier);

    void refreshClusterNodeCountMetrics();

    void refreshWatcherCountMetrics();

    void incrementClusterChangePushCount(String vgroup);

    double getClusterChangePushCount(String vgroup);
}
