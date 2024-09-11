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
package org.apache.seata.namingserver.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class ClusterData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterData.class);
    private String clusterName;
    private String clusterType;
    private final Map<String, Unit> unitData;
    
    private final Lock lock = new ReentrantLock();


    public ClusterData() {
        this.unitData = new ConcurrentHashMap<>();
    }

    public ClusterData(String clusterName) {
        this.unitData = new ConcurrentHashMap<>();
        this.clusterName = clusterName;
    }

    public ClusterData(String clusterName, String clusterType) {
        unitData = new ConcurrentHashMap<>();
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


    public Map<String, Unit> getUnitData() {
        return unitData;
    }

    public void removeInstance(Node node, String unitName) {
        Unit unit = unitData.get(unitName);
        if (Objects.isNull(unit)) {
            LOGGER.warn("unit {} is null", unitName);
            return;
        }
        unit.removeInstance(node);
        // remove unit if unit has no instance
        lock.lock();
        try {
            if (CollectionUtils.isEmpty(unit.getNamingInstanceList())) {
                unitData.remove(unitName);
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Node> getInstanceList() {
        return unitData.values().stream()
                .map(Unit::getNamingInstanceList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public Cluster getClusterByUnits(Set<String> unitNames) {
        Cluster clusterResponse = new Cluster();
        clusterResponse.setClusterName(clusterName);
        clusterResponse.setClusterType(clusterType);
        if (CollectionUtils.isEmpty(unitNames)) {
            clusterResponse.appendUnits(unitData.values());
        } else {
            for (String unitName : unitNames) {
                List<Unit> unitList = new ArrayList<>();
                Optional.ofNullable(unitData.get(unitName)).ifPresent(unitList::add);
                clusterResponse.appendUnits(unitList);
            }
        }

        return clusterResponse;
    }

    public boolean registerInstance(NamingServerNode instance, String unitName) {
        Unit currentUnit = unitData.computeIfAbsent(unitName, value -> {
            Unit unit = new Unit();
            List<NamingServerNode> instances = new CopyOnWriteArrayList<>();
            unit.setUnitName(unitName);
            unit.setNamingInstanceList(instances);
            return unit;
        });
        // ensure that when adding an instance, the remove side will not delete the unit.
        lock.lock();
        try {
            return currentUnit.addInstance(instance);
        } finally {
            lock.unlock();
        }

    }


}
