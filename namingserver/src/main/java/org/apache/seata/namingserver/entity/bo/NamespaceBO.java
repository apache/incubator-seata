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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.entity.pojo.ClusterData;

public class NamespaceBO {

    Map<String, ClusterBO> clusterMap = new ConcurrentHashMap<>();

    public NamespaceBO() {
    }

    public Map<String, ClusterBO> getClusterMap() {
        return clusterMap;
    }

    public List<Cluster> getCluster(ConcurrentMap<String/* clusterName */, ClusterData> clusterDataMap) {
        List<Cluster> list = new ArrayList<>();
        clusterMap.forEach((clusterName, unitNameSet) -> {
            ClusterData clusterData = clusterDataMap.get(clusterName);
            if (clusterData != null) {
                list.add(clusterData.getClusterByUnits(unitNameSet.getUnitNames()));
            }
        });
        return list;
    }

    public void setClusterMap(Map<String, ClusterBO> clusterMap) {
        this.clusterMap = clusterMap;
    }

    public ClusterBO getCluster(String clusterName) {
        return clusterMap.computeIfAbsent(clusterName, k -> new ClusterBO());
    }

    public void removeOldCluster(String clusterName) {
        Set<String> clusterSet = clusterMap.keySet();
        if (clusterSet.size() <= 1) {
            return;
        }
        clusterSet.forEach(currentClusterName -> {
            if (!StringUtils.equals(currentClusterName, clusterName)) {
                clusterMap.remove(currentClusterName);
            }
        });
    }
}
