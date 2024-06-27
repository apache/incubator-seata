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
package org.apache.seata.namingserver.manager;



import jdk.internal.net.http.common.Pair;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.pojo.AbstractClusterData;
import org.apache.seata.namingserver.pojo.ClusterData;
import org.apache.seata.namingserver.vo.monitor.ClusterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class NamingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingManager.class);
    private final HashMap<InetSocketAddress, Long> instanceLiveTable;
    private final HashMap<String/* VGroup */, HashMap<String/* namespace */, Pair<String/* clusterName */, String/* unitName */>>> VGroupMap;
    private final HashMap<String/* namespace */, HashMap<String/* clusterName */, ClusterData>> NamespaceClusterDataMap;
    private int HEARTBEAT_TIME_THRESHOLD = 90 * 1000;
    private int HEARTBEAT_CHECK_TIME_PERIOD = 60 * 1000;
    protected final ScheduledExecutorService heartBeatCheckService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("heartBeatCheckExcuter", 1, true));


    @Autowired
    private ApplicationContext applicationContext;


    public NamingManager(@Value("${heartbeat.threshold}") int heartbeatThreshold,
                         @Value("${heartbeat.period}") int heartbeatPeriod) {
        HEARTBEAT_CHECK_TIME_PERIOD = heartbeatPeriod;
        HEARTBEAT_TIME_THRESHOLD = heartbeatThreshold;
        this.instanceLiveTable = new HashMap<>();
        this.VGroupMap = new HashMap<>();
        this.NamespaceClusterDataMap = new HashMap<>();
        // start heartbeat checking
        this.heartBeatCheckService.scheduleAtFixedRate(() -> {
            try {
                instanceHeartBeatCheck();
            } catch (Exception e) {
                LOGGER.error("Heart Beat Check Exception", e);
            }
        }, 0, HEARTBEAT_CHECK_TIME_PERIOD, TimeUnit.MILLISECONDS);
    }

    public List<ClusterVO> monitorCluster(String namespace) {
        HashMap<String, ClusterVO> clusterVOHashMap = new HashMap<>();
        HashMap<String, ClusterData> clusterDataMap = NamespaceClusterDataMap.get(namespace);

        if (clusterDataMap != null) {
            for (Map.Entry<String, ClusterData> entry : clusterDataMap.entrySet()) {
                String clusterName = entry.getKey();
                ClusterData clusterData = entry.getValue();
                clusterVOHashMap.put(clusterName, ClusterVO.convertFromClusterData(clusterData));
            }
        } else {
            LOGGER.warn("no cluster in namespace:" + namespace);
        }

        for (Map.Entry<String, HashMap<String, Pair<String, String>>> entry : VGroupMap.entrySet()) {
            String vGroup = entry.getKey();
            HashMap<String, Pair<String, String>> namespaceMap = entry.getValue();
            Pair<String, String> pair = namespaceMap.get(namespace);
            String clusterName = pair.first;
            ClusterVO clusterVO = clusterVOHashMap.get(clusterName);
            if (clusterVO != null) {
                clusterVO.addMapping(vGroup);
            }
        }

        return new ArrayList<>(clusterVOHashMap.values());

    }

    public void changeGroup(String namespace, String clusterName, String unitName, String vGroup) {
        try {
            Pair<String, String> pair = new Pair<>(clusterName, unitName);
            HashMap<String, Pair<String, String>> stringPairHashMap = new HashMap<>();
            stringPairHashMap.put(namespace, pair);
            if (!VGroupMap.containsKey(vGroup) || !VGroupMap.get(vGroup).equals(stringPairHashMap)) {
                VGroupMap.put(vGroup, stringPairHashMap);
                applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
            }

        } catch (Exception e) {
            LOGGER.error("change vGroup mapping failed:{}", vGroup);
        }
    }

    public void notifyClusterChange(String namespace, String clusterName, String unitName) {
        for (Map.Entry<String, HashMap<String, Pair<String, String>>> entry : VGroupMap.entrySet()) {
            String vGroup = entry.getKey();
            HashMap<String, Pair<String, String>> namespaceMap = entry.getValue();

            // Iterating through an internal HashMap
            for (Map.Entry<String, Pair<String, String>> innerEntry : namespaceMap.entrySet()) {
                String namespace1 = innerEntry.getKey();
                Pair<String, String> pair = innerEntry.getValue();
                String clusterName1 = pair.first;
                String unitName1 = pair.second;
                if (namespace1.equals(namespace)
                        && clusterName1.equals(clusterName)
                        && (unitName1 == null || unitName1.equals(unitName))) {
                    applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
                }
            }
        }

    }

    public boolean registerInstance(Node node, String namespace, String clusterName, String unitName) {
        try {
            HashMap<String, ClusterData> clusterDataHashMap = NamespaceClusterDataMap.computeIfAbsent(namespace, k -> new HashMap<>());

            // add instance in cluster
            // create cluster when there is no cluster in clusterDataHashMap
            ClusterData clusterData = clusterDataHashMap.computeIfAbsent(clusterName, key -> new ClusterData(clusterName, (String) node.getMetadata().get("cluster-type")));
            node.getMetadata().remove("cluster-type");

            // if extended metadata includes vgroup mapping relationship, add it in clusterData
            Object mappingObj = node.getMetadata().get("vGroup");

            if (mappingObj instanceof HashMap) {
                HashMap<String, Object> mapping = (HashMap<String, Object>) mappingObj;
                mapping.forEach((vGroup, unitObj) -> {
                            changeGroup(namespace, clusterName, (String) unitObj, vGroup);
                        }
                );
                node.getMetadata().remove("vGroup");
            }

            boolean hasChanged = clusterData.registerInstance(node, unitName);
            if (hasChanged) {
                notifyClusterChange(namespace, clusterName, unitName);
            }
            instanceLiveTable.put(new InetSocketAddress(node.getTransaction().getHost(), node.getTransaction().getPort()), System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("Instance registered failed!", e);
            return false;
        }
        return true;
    }


    public boolean unregisterInstance(String unitName, Node node) {
        try {
            for (String namespace : NamespaceClusterDataMap.keySet()) {
                HashMap<String, ClusterData> clusterMap = NamespaceClusterDataMap.get(namespace);
                if (clusterMap == null) continue;
                for (String clusterName : clusterMap.keySet()) {
                    ClusterData clusterData = clusterMap.get(clusterName);
                    if (clusterData == null) continue;
                    if (clusterData.getUnitData() != null && clusterData.getUnitData().containsKey(unitName)) {
                        clusterData.removeInstance(node, unitName);
                        notifyClusterChange(namespace, clusterName, unitName);
                        instanceLiveTable.remove(new InetSocketAddress(node.getTransaction().getHost(), node.getTransaction().getPort()));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Instance unregistered failed!");
            return false;
        }
        return true;
    }


    public List<Cluster> getClusterListByVgroup(String vGroup, String namespace) {
        // find the cluster where the transaction group is located
        List<Cluster> clusterList = new ArrayList<>();
        try {
            Pair<String, String> clusterUnitPair = VGroupMap.get(vGroup).get(namespace);
            String clusterName = clusterUnitPair.first;
            String unitName = clusterUnitPair.second;
            ClusterData clusterData = NamespaceClusterDataMap.get(namespace).get(clusterName);
            clusterList.add(clusterData.getClusterByUnit(unitName));
        } catch (NullPointerException e) {
            LOGGER.error("no cluster mapping for vGroup: " + vGroup);
        }
        return clusterList;
    }

    public List<Node> getInstances(String namespace, String clusterName) {
        HashMap<String, ClusterData> clusterDataHashMap = NamespaceClusterDataMap.get(namespace);
        AbstractClusterData abstractClusterData = clusterDataHashMap.get(clusterName);
        if (abstractClusterData == null) {
            LOGGER.warn("no instances in {} : {}", namespace, clusterName);
            return Collections.EMPTY_LIST;
        }
        return abstractClusterData.getInstanceList();
    }

    public void instanceHeartBeatCheck() {
        for (String namespace : NamespaceClusterDataMap.keySet()) {
            for (ClusterData clusterData : NamespaceClusterDataMap.get(namespace).values()) {
                for (Unit unit : clusterData.getUnitData().values()) {
                    Iterator<Node> instanceIterator = unit.getNamingInstanceList().iterator();

                    while (instanceIterator.hasNext()) {
                        Node instance = instanceIterator.next();
                        InetSocketAddress inetSocketAddress = new InetSocketAddress(instance.getTransaction().getHost(), instance.getTransaction().getPort());
                        long lastHeatBeatTimeStamp = instanceLiveTable.getOrDefault(inetSocketAddress, (long) 0);

                        if (Math.abs(lastHeatBeatTimeStamp - System.currentTimeMillis()) > HEARTBEAT_TIME_THRESHOLD) {
                            instanceLiveTable.remove(inetSocketAddress);

                            instanceIterator.remove();  // Safe removal using iterator's remove method
                            clusterData.removeInstance(instance, unit.getUnitName());

                            notifyClusterChange(namespace, clusterData.getClusterName(), unit.getUnitName());
                            LOGGER.warn("{} instance has gone offline", instance.getTransaction().getHost() + ":" + instance.getTransaction().getPort());
                        }
                    }
                }
            }
        }


    }

}
