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
package org.apache.seata.namingserver.entity.vo.monitor;

import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.namingserver.entity.pojo.ClusterData;

import java.util.ArrayList;
import java.util.List;

public class ClusterVO {
    private String clusterName;
    private String clusterType;

    private List<String /*vgroup*/> vGroupMapping;
    private final List<Unit> unitData;


    public ClusterVO() {
        this.vGroupMapping = new ArrayList<>();
        this.unitData = new ArrayList<>();
    }

    public ClusterVO(String clusterName, String clusterType, List<Unit> unitData) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
        this.unitData = unitData;
        this.vGroupMapping = new ArrayList<>();
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public List<Unit> getUnitData() {
        return unitData;
    }

    public List<String> getvGroupMapping() {
        return vGroupMapping;
    }

    public void setvGroupMapping(List<String> vGroupMapping) {
        this.vGroupMapping = vGroupMapping;
    }

    public static ClusterVO convertFromClusterData(ClusterData cluster) {
        List<Unit> unitList = new ArrayList<>();
        if (cluster.getUnitData() != null) {
            unitList.addAll(cluster.getUnitData().values());
        }
        return new ClusterVO(cluster.getClusterName(), cluster.getClusterType(), unitList);
    }

    public void addMapping(String vGroup) {
        if (!vGroupMapping.contains(vGroup)) {
            vGroupMapping.add(vGroup);
        }
    }
}
