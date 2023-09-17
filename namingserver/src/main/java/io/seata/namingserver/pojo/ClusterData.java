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
package io.seata.namingserver.pojo;


import io.seata.common.metadata.Cluster;
import io.seata.common.metadata.Node;
import io.seata.common.metadata.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ClusterData extends AbstractClusterData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterData.class);
    private String clusterName;
    private String clusterType;
    private final HashMap<String, Unit> unitData;


    public ClusterData() {
        unitData = new HashMap<>(32);
    }

    public ClusterData(String clusterName) {
        unitData = new HashMap<>(32);
        this.clusterName = clusterName;
    }

    public ClusterData(String clusterName, String clusterType) {
        unitData = new HashMap<>(32);
        this.clusterName = clusterName;
        this.clusterType = clusterType;
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


    public HashMap<String, Unit> getUnitData() {
        return unitData;
    }

    public void removeInstance(Node node, String unitName) {
        Unit unit = unitData.get(unitName);
        if (Objects.isNull(unit)) {
            LOGGER.warn("unit {} is null", unitName);
            return;
        }
        unit.removeInstance(node);
        if (unit.getNamingInstanceList() == null || unit.getNamingInstanceList().size() == 0) {
            unitData.remove(unitName);
        }
    }

    public List<Node> getInstanceList() {
        return unitData.values().stream()
                .map(Unit::getNamingInstanceList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public Cluster getClusterByUnit(String unitName) {
        Cluster clusterResponse = new Cluster();
        clusterResponse.setClusterName(clusterName);
        clusterResponse.setClusterType(clusterType);
        if (unitName == null) {
            clusterResponse.setUnitData(new ArrayList<>(unitData.values()));
        } else {
            List<Unit> unitList = new ArrayList<>();
            unitList.add(unitData.get(unitName));
            clusterResponse.setUnitData(unitList);
        }

        return clusterResponse;
    }

    public void registerInstance(Node instance, String unitName) {
        List<Node> instances = new ArrayList<>();
        instances.add(instance);
        Unit unit = new Unit();
        unit.setUnitName(unitName);
        unit.setNamingInstanceList(instances);
        unitData.put(unitName, unit);
    }


}
