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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.namingserver.constants.NamingServerConstants;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.pojo.AbstractClusterData;
import org.apache.seata.namingserver.pojo.ClusterData;
import org.apache.seata.namingserver.vo.monitor.ClusterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;


import static org.apache.seata.namingserver.constants.NamingServerConstants.CONSTANT_GROUP;

@Component
public class NamingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingManager.class);
    private final ConcurrentMap<InetSocketAddress, Long> instanceLiveTable;
    private final ConcurrentMap<String/* VGroup */,
        ConcurrentMap<String/* namespace */, Pair<String/* clusterName */, String/* unitName */>>>    vGroupMap;
    private final ConcurrentMap<String/* namespace */, ConcurrentMap<String/* clusterName */, ClusterData>> namespaceClusterDataMap;

    @Value("${heartbeat.threshold:90000}")
    private int heartbeatTimeThreshold;

    @Value("${heartbeat.period:60000}")
    private int heartbeatCheckTimePeriod;

    protected final ScheduledExecutorService heartBeatCheckService =
        new ScheduledThreadPoolExecutor(1, new CustomizableThreadFactory("heartBeatCheckExcuter"));

    @Autowired
    private ApplicationContext applicationContext;

    public NamingManager() {
        this.instanceLiveTable = new ConcurrentHashMap<>();
        this.vGroupMap = new ConcurrentHashMap<>();
        this.namespaceClusterDataMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        this.heartBeatCheckService.scheduleAtFixedRate(() -> {
            try {
                instanceHeartBeatCheck();
            } catch (Exception e) {
                LOGGER.error("Heart Beat Check Exception", e);
            }
        }, heartbeatCheckTimePeriod, heartbeatCheckTimePeriod, TimeUnit.MILLISECONDS);
    }

    public List<ClusterVO> monitorCluster(String namespace) {
        Map<String, ClusterVO> clusterVOHashMap = new HashMap<>();
        Map<String, ClusterData> clusterDataMap = namespaceClusterDataMap.get(namespace);

        if (clusterDataMap != null) {
            for (Map.Entry<String, ClusterData> entry : clusterDataMap.entrySet()) {
                String clusterName = entry.getKey();
                ClusterData clusterData = entry.getValue();
                clusterVOHashMap.put(clusterName, ClusterVO.convertFromClusterData(clusterData));
            }
        } else {
            LOGGER.warn("no cluster in namespace:" + namespace);
        }

        for (Map.Entry<String, ConcurrentMap<String, Pair<String, String>>> entry : vGroupMap.entrySet()) {
            String vGroup = entry.getKey();
            Map<String, Pair<String, String>> namespaceMap = entry.getValue();
            Pair<String, String> pair = namespaceMap.get(namespace);
            String clusterName = pair.getKey();
            ClusterVO clusterVO = clusterVOHashMap.get(clusterName);
            if (clusterVO != null) {
                clusterVO.addMapping(vGroup);
            }
        }

        return new ArrayList<>(clusterVOHashMap.values());
    }

    public Result<?> addGroup(String namespace, String vGroup, String clusterName, String unitName) {
        changeGroup(namespace,clusterName,unitName,vGroup);
        // add vGroup in new cluster
        List<Node> nodeList = getInstances(namespace, clusterName);
        if (nodeList == null || nodeList.size() == 0) {
            LOGGER.error("no instance in cluster {}", clusterName);
            return new Result<>("301", "no instance in cluster" + clusterName);
        } else {
            Node node = nodeList.get(0);
            String controlHost = node.getControl().getHost();
            int controlPort = node.getControl().getPort();
            String httpUrl = NamingServerConstants.HTTP_PREFIX + controlHost + NamingServerConstants.IP_PORT_SPLIT_CHAR
                + controlPort + NamingServerConstants.HTTP_ADD_GROUP_SUFFIX;
            HashMap<String, String> params = new HashMap<>();
            params.put(CONSTANT_GROUP, vGroup);
            params.put(NamingServerConstants.CONSTANT_UNIT, unitName);
            Map<String, String> header = new HashMap<>();
            header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

            try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.doGet(httpUrl, params, header, 30000)) {
                if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                    return new Result<>(String.valueOf(closeableHttpResponse.getStatusLine().getStatusCode()),
                        "add vGroup in new cluster failed");
                }
            } catch (IOException e) {
                LOGGER.warn("add vGroup in new cluster failed");
                return new Result<>("500", "add vGroup in new cluster failed");
            }
        }
        return new Result<>("200", "add vGroup successfully!");
    }

    public Result<?> removeGroup(String namespace, String vGroup, String unitName) {
        List<Cluster> clusterList = getClusterListByVgroup(vGroup, namespace);
        for (Cluster cluster : clusterList) {
            if (cluster.getUnitData() != null && cluster.getUnitData().size() > 0) {
                Unit unit = cluster.getUnitData().get(0);
                if (unit != null && unit.getNamingInstanceList() != null && unit.getNamingInstanceList().size() > 0) {
                    Node node = unit.getNamingInstanceList().get(0);
                    String httpUrl = NamingServerConstants.HTTP_PREFIX + node.getControl().getHost()
                        + NamingServerConstants.IP_PORT_SPLIT_CHAR + node.getControl().getPort()
                        + NamingServerConstants.HTTP_REMOVE_GROUP_SUFFIX;
                    HashMap<String, String> params = new HashMap<>();
                    params.put(CONSTANT_GROUP, vGroup);
                    params.put(NamingServerConstants.CONSTANT_UNIT, unitName);
                    Map<String, String> header = new HashMap<>();
                    header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
                    try (CloseableHttpResponse closeableHttpResponse =
                        HttpClientUtil.doGet(httpUrl, params, header, 30000)) {
                        if (closeableHttpResponse == null
                            || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                            LOGGER.warn("remove vGroup in old cluster failed");
                            return new Result<>(String.valueOf(closeableHttpResponse.getStatusLine().getStatusCode()),
                                "removing vGroup " + vGroup + " in old cluster " + cluster + " failed");
                        }
                    } catch (IOException e) {
                        LOGGER.warn("handle removing vGroup in old cluster failed");
                        return new Result<>("500",
                            "handle removing vGroup " + vGroup + " in old cluster " + cluster + " failed");
                    }
                }
            }
        }
        return new Result<>("200", "remove group in old cluster successfully!");
    }

    public void changeGroup(String namespace, String clusterName, String unitName, String vGroup) {
        try {
            Pair<String, String> pair = Pair.of(clusterName, unitName);
            ConcurrentMap<String, Pair<String, String>> stringPairHashMap = new ConcurrentHashMap<>();
            stringPairHashMap.put(namespace, pair);
            if (!vGroupMap.containsKey(vGroup) || !vGroupMap.get(vGroup).equals(stringPairHashMap)) {
                vGroupMap.put(vGroup, stringPairHashMap);
                applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
            }
        } catch (Exception e) {
            LOGGER.error("change vGroup mapping failed:{}", vGroup);
        }
    }

    public void notifyClusterChange(String namespace, String clusterName, String unitName) {
        for (Map.Entry<String, ConcurrentMap<String, Pair<String, String>>> entry : vGroupMap.entrySet()) {
            String vGroup = entry.getKey();
            Map<String, Pair<String, String>> namespaceMap = entry.getValue();

            // Iterating through an internal HashMap
            for (Map.Entry<String, Pair<String, String>> innerEntry : namespaceMap.entrySet()) {
                String namespace1 = innerEntry.getKey();
                Pair<String, String> pair = innerEntry.getValue();
                String clusterName1 = pair.getKey();
                String unitName1 = pair.getValue();
                if (namespace1.equals(namespace) && clusterName1.equals(clusterName)
                    && (unitName1 == null || unitName1.equals(unitName))) {
                    applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
                }
            }
        }

    }

    public boolean registerInstance(NamingServerNode node, String namespace, String clusterName, String unitName) {
        try {
            Map<String, ClusterData> clusterDataHashMap =
                namespaceClusterDataMap.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

            // add instance in cluster
            // create cluster when there is no cluster in clusterDataHashMap
            ClusterData clusterData = clusterDataHashMap.computeIfAbsent(clusterName,
                key -> new ClusterData(clusterName, (String)node.getMetadata().remove("cluster-type")));

            // if extended metadata includes vgroup mapping relationship, add it in clusterData
            Optional.ofNullable(node.getMetadata().remove(CONSTANT_GROUP)).ifPresent(mappingObj -> {
                if (mappingObj instanceof List) {
                    List<String> vGroups = (List<String>)mappingObj;
                    for (String vGroup : vGroups) {
                        changeGroup(namespace, clusterName, unitName, vGroup);
                    }
                }
            });

            boolean hasChanged = clusterData.registerInstance(node, unitName);
            if (hasChanged) {
                notifyClusterChange(namespace, clusterName, unitName);
            }
            instanceLiveTable.put(
                new InetSocketAddress(node.getTransaction().getHost(), node.getTransaction().getPort()),
                System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("Instance registered failed!", e);
            return false;
        }
        return true;
    }

    public boolean unregisterInstance(String unitName, Node node) {
        try {
            for (String namespace : namespaceClusterDataMap.keySet()) {
                Map<String, ClusterData> clusterMap = namespaceClusterDataMap.get(namespace);
                if (clusterMap != null) {
                    clusterMap.forEach((clusterName, clusterData) -> {
                        if (clusterData.getUnitData() != null && clusterData.getUnitData().containsKey(unitName)) {
                            clusterData.removeInstance(node, unitName);
                            notifyClusterChange(namespace, clusterName, unitName);
                            instanceLiveTable.remove(new InetSocketAddress(node.getTransaction().getHost(),
                                node.getTransaction().getPort()));
                        }
                    });
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
            Pair<String, String> clusterUnitPair = vGroupMap.get(vGroup).get(namespace);
            String clusterName = clusterUnitPair.getKey();
            String unitName = clusterUnitPair.getValue();
            ClusterData clusterData = namespaceClusterDataMap.get(namespace).get(clusterName);
            clusterList.add(clusterData.getClusterByUnit(unitName));
        } catch (NullPointerException e) {
            LOGGER.error("no cluster mapping for vGroup: " + vGroup);
        }
        return clusterList;
    }

    public List<Node> getInstances(String namespace, String clusterName) {
        Map<String, ClusterData> clusterDataHashMap = namespaceClusterDataMap.get(namespace);
        AbstractClusterData abstractClusterData = clusterDataHashMap.get(clusterName);
        if (abstractClusterData == null) {
            LOGGER.warn("no instances in {} : {}", namespace, clusterName);
            return Collections.emptyList();
        }
        return abstractClusterData.getInstanceList();
    }

    public void instanceHeartBeatCheck() {
        for (String namespace : namespaceClusterDataMap.keySet()) {
            for (ClusterData clusterData : namespaceClusterDataMap.get(namespace).values()) {
                for (Unit unit : clusterData.getUnitData().values()) {
                    Iterator<Node> instanceIterator = unit.getNamingInstanceList().iterator();

                    while (instanceIterator.hasNext()) {
                        Node instance = instanceIterator.next();
                        InetSocketAddress inetSocketAddress = new InetSocketAddress(instance.getTransaction().getHost(),
                            instance.getTransaction().getPort());
                        long lastHeatBeatTimeStamp = instanceLiveTable.getOrDefault(inetSocketAddress, (long)0);

                        if (Math.abs(lastHeatBeatTimeStamp - System.currentTimeMillis()) > heartbeatTimeThreshold) {
                            instanceLiveTable.remove(inetSocketAddress);

                            instanceIterator.remove(); // Safe removal using iterator's remove method
                            clusterData.removeInstance(instance, unit.getUnitName());

                            notifyClusterChange(namespace, clusterData.getClusterName(), unit.getUnitName());
                            LOGGER.warn("{} instance has gone offline",
                                instance.getTransaction().getHost() + ":" + instance.getTransaction().getPort());
                        }
                    }
                }
            }
        }

    }

}
