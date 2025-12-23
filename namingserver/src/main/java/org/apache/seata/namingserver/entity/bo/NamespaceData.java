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
package org.apache.seata.namingserver.entity.bo;

import java.util.Map;
import java.util.Set;

/**
 * Business Object representing collected namespace data for v2 API.
 * <p>
 * This class encapsulates the collected data for namespaces, including
 * clusters, vgroups, and their mappings for both RAFT and non-RAFT clusters.
 */
public class NamespaceData {
    private final Map<String, Set<String>> clustersMap;
    private final Map<String, Set<String>> vgroupsMap;
    private final Map<String, Map<String, Set<String>>> clusterVgroupsMap;

    public NamespaceData(
            Map<String, Set<String>> clustersMap,
            Map<String, Set<String>> vgroupsMap,
            Map<String, Map<String, Set<String>>> clusterVgroupsMap) {
        this.clustersMap = clustersMap;
        this.vgroupsMap = vgroupsMap;
        this.clusterVgroupsMap = clusterVgroupsMap;
    }

    public Map<String, Set<String>> getClustersMap() {
        return clustersMap;
    }

    public Map<String, Set<String>> getVgroupsMap() {
        return vgroupsMap;
    }

    public Map<String, Map<String, Set<String>>> getClusterVgroupsMap() {
        return clusterVgroupsMap;
    }
}
