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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.NamingServerConstants;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.entity.bo.ClusterBO;
import org.apache.seata.namingserver.entity.bo.NamespaceBO;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.entity.pojo.ClusterData;
import org.apache.seata.namingserver.entity.vo.monitor.ClusterVO;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import static org.apache.seata.common.NamingServerConstants.CONSTANT_GROUP;

@Component
public class NamingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingManager.class);
    private final ConcurrentMap<InetSocketAddress, Long> instanceLiveTable;
    private volatile LoadingCache<String/* VGroup */, ConcurrentMap<String/* namespace */,NamespaceBO>> vGroupMap;
    private final ConcurrentMap<String/* namespace */,
        ConcurrentMap<String/* clusterName */, ClusterData>> namespaceClusterDataMap;

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
        this.namespaceClusterDataMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        this.vGroupMap = Caffeine.newBuilder()
                .expireAfterAccess(10000, TimeUnit.MILLISECONDS) // 设置过期时间
                .maximumSize(Integer.MAX_VALUE) // 设置最大缓存条目数
                .removalListener(new RemovalListener<Object, Object>() {

                    @Override
                    public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
                        System.err.println("out " + key + " " + value);
                    }
                })
                .build(k -> new ConcurrentHashMap<>());
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

        vGroupMap.asMap().forEach((vGroup, namespaceMap) -> {
            NamespaceBO namespaceBO = namespaceMap.get(namespace);
            if (namespaceBO != null) {
                namespaceBO.getClusterMap().forEach((clusterName, clusterBO) -> {
                    ClusterVO clusterVO = clusterVOHashMap.get(clusterName);
                    if (clusterVO != null) {
                        clusterVO.addMapping(vGroup);
                    }
                });
            }
        });
        return new ArrayList<>(clusterVOHashMap.values());
    }

    public Result<String> createGroup(String namespace, String vGroup, String clusterName, String unitName) {
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

            try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.doGet(httpUrl, params, header, 3000)) {
                if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                    return new Result<>(String.valueOf(closeableHttpResponse.getStatusLine().getStatusCode()),
                        "add vGroup in new cluster failed");
                }
            } catch (IOException e) {
                LOGGER.warn("add vGroup in new cluster failed");
                return new Result<>("500", "add vGroup in new cluster failed");
            }
        }
        addGroup(namespace,clusterName,unitName,vGroup);
        return new Result<>("200", "add vGroup successfully!");
    }

    public Result<String> removeGroup(String namespace, String clusterName,String vGroup, String unitName) {
        List<Cluster> clusterList = getClusterListByVgroup(vGroup, namespace);
        for (Cluster cluster : clusterList) {
            if (!StringUtils.equals(clusterName, cluster.getClusterName())) continue;
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
                        HttpClientUtil.doGet(httpUrl, params, header, 3000)) {
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

    public void addGroup(String namespace, String clusterName, String unitName, String vGroup) {
        try {
            ClusterBO clusterBO = vGroupMap.get(vGroup, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(namespace, k -> new NamespaceBO()).getCluster(clusterName);
            if (clusterBO != null && !clusterBO.getUnitNames().contains(unitName)) {
                clusterBO.addUnit(unitName);
                NamespaceBO namespaceBO = vGroupMap.getIfPresent(vGroup).get(namespace);
                namespaceBO.removeOldCluster(clusterName);
                applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
            }
        } catch (Exception e) {
            LOGGER.error("change vGroup mapping failed:{}", vGroup, e);
        }
    }

    public void notifyClusterChange(String namespace, String clusterName, String unitName, long term) {
        vGroupMap.asMap().forEach((vGroup, namespaceMap) -> {
            Optional.ofNullable(namespaceMap.get(namespace))
                .flatMap(namespaceBO -> Optional.ofNullable(namespaceBO.getCluster(clusterName)))
                .ifPresent(clusterBO -> {
                    Set<String> units = clusterBO.getUnitNames();
                    if (StringUtils.isBlank(unitName) || units.contains(unitName)) {
                        applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, term));
                    }
                });
        });
    }

    public boolean registerInstance(NamingServerNode node, String namespace, String clusterName, String unitName) {
        try {
            Map<String, ClusterData> clusterDataHashMap =
                namespaceClusterDataMap.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

            // add instance in cluster
            // create cluster when there is no cluster in clusterDataHashMap
            ClusterData clusterData = clusterDataHashMap.computeIfAbsent(clusterName,
                key -> new ClusterData(clusterName, (String)node.getMetadata().get("cluster-type")));

            // if extended metadata includes vgroup mapping relationship, add it in clusterData
            Optional.ofNullable(node.getMetadata().get(CONSTANT_GROUP)).ifPresent(mappingObj -> {
                if (mappingObj instanceof Map) {
                    Map<String, String> vGroups = (Map<String, String>) mappingObj;
                    vGroups.forEach((k, v) -> {
                        // In non-raft mode, a unit is one-to-one with a node, and the unitName is stored on the node.
                        // In raft mode, the unitName is equal to the raft-group, so the node's unitName cannot be used.
                        addGroup(namespace, clusterName, StringUtils.isBlank(v) ? unitName : v, k);
                    });
                }
            });
            boolean hasChanged = clusterData.registerInstance(node, unitName);
            if (hasChanged) {
                notifyClusterChange(namespace, clusterName, unitName,node.getTerm());
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

    public boolean unregisterInstance(String namespace, String clusterName, String unitName, NamingServerNode node) {
        try {
            Map<String, ClusterData> clusterMap = namespaceClusterDataMap.get(namespace);
            if (clusterMap != null) {
                ClusterData clusterData = clusterMap.get(clusterName);
                if (clusterData.getUnitData() != null && clusterData.getUnitData().containsKey(unitName)) {
                    clusterData.removeInstance(node, unitName);
                    Object vgroupMap = node.getMetadata().get(CONSTANT_GROUP);
                    if (vgroupMap instanceof Map) {
                        ((Map<String, Object>) vgroupMap).forEach((group, realUnitName) -> vGroupMap.get(group, k -> new ConcurrentHashMap<>())
                                .get(namespace).getCluster(clusterName).remove(realUnitName == null ? unitName : realUnitName));
                    }
                    notifyClusterChange(namespace, clusterName, unitName, node.getTerm());
                    instanceLiveTable.remove(
                        new InetSocketAddress(node.getTransaction().getHost(), node.getTransaction().getPort()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Instance unregistered failed!", e);
            return false;
        }
        return true;
    }

    public List<Cluster> getClusterListByVgroup(String vGroup, String namespace) {
        // find the cluster where the transaction group is located
        HashMap<String/* VGroup */, ConcurrentMap<String/* namespace */,NamespaceBO>> concurrentVgroupMap = new HashMap<>(vGroupMap.asMap());
        ConcurrentMap<String/* namespace */, NamespaceBO> vgroupNamespaceMap = concurrentVgroupMap.get(vGroup);
        List<Cluster> clusterList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(vgroupNamespaceMap)) {
            NamespaceBO namespaceBO = vgroupNamespaceMap.get(namespace);
            ConcurrentMap<String/* clusterName */, ClusterData> clusterDataMap = namespaceClusterDataMap.get(namespace);
            if (namespaceBO != null && !CollectionUtils.isEmpty(clusterDataMap)) {
                clusterList.addAll(namespaceBO.getCluster(clusterDataMap));
            }
        }
        return clusterList;
    }

    public List<Node> getInstances(String namespace, String clusterName) {
        Map<String, ClusterData> clusterDataHashMap = namespaceClusterDataMap.get(namespace);
        ClusterData clusterData = clusterDataHashMap.get(clusterName);
        if (clusterData == null) {
            LOGGER.warn("no instances in {} : {}", namespace, clusterName);
            return Collections.emptyList();
        }
        return clusterData.getInstanceList();
    }

    public void instanceHeartBeatCheck() {
        for (String namespace : namespaceClusterDataMap.keySet()) {
            for (ClusterData clusterData : namespaceClusterDataMap.get(namespace).values()) {
                for (Unit unit : clusterData.getUnitData().values()) {
                    List<NamingServerNode> removeList = new ArrayList<>();
                    for (NamingServerNode instance : unit.getNamingInstanceList()) {
                        InetSocketAddress inetSocketAddress = new InetSocketAddress(instance.getTransaction().getHost(),
                            instance.getTransaction().getPort());
                        long lastHeatBeatTimeStamp = instanceLiveTable.getOrDefault(inetSocketAddress, (long)0);
                        if (Math.abs(lastHeatBeatTimeStamp - System.currentTimeMillis()) > heartbeatTimeThreshold) {
                            instanceLiveTable.remove(inetSocketAddress);
                            removeList.add(instance);
                        }
                    }
                    if (!CollectionUtils.isEmpty(removeList)) {
                        unit.getNamingInstanceList().removeAll(removeList);
                        for (NamingServerNode instance : removeList) {
                            clusterData.removeInstance(instance, unit.getUnitName());
                            Object vgoupMap = instance.getMetadata().get(CONSTANT_GROUP);
                            if (vgoupMap instanceof Map) {
                                ((Map<String, Object>)vgoupMap).forEach((group, unitName) -> {
                                    ClusterBO clusterBO =
                                        vGroupMap.get(group, k -> new ConcurrentHashMap<>()).get(namespace).getCluster(clusterData.getClusterName());
                                    Set<String> units = clusterBO.getUnitNames();
                                    if (units != null) {
                                        units.remove(unitName == null ? instance.getUnit() : unitName);
                                    }
                                });
                            }

                            LOGGER.warn("{} instance has gone offline",
                                instance.getTransaction().getHost() + ":" + instance.getTransaction().getPort());
                        }
                        notifyClusterChange(namespace, clusterData.getClusterName(), unit.getUnitName(),-1);
                    }
                }
            }
        }
    }

    public Result<String> changeGroup(String namespace, String vGroup, String clusterName, String unitName) {
        ConcurrentMap<String, NamespaceBO> namespaceMap =
            new ConcurrentHashMap<>(vGroupMap.get(vGroup));
        createGroup(namespace, vGroup, clusterName, unitName);
        AtomicReference<Result<String>> result = new AtomicReference<>();
        namespaceMap.forEach((currentNamespace, namespaceBO) -> namespaceBO.getClusterMap().forEach((currentCluster, clusterBO) -> {
            for (String currentUnitName : clusterBO.getUnitNames()) {
                if (StringUtils.isBlank(unitName)) {
                    if (StringUtils.equalsIgnoreCase(clusterName, currentCluster)) {
                        continue;
                    }
                    result.set(removeGroup(currentNamespace, currentCluster, vGroup, unitName));
                } else {
                    if (!StringUtils.equalsIgnoreCase(unitName, currentUnitName)) {
                        result.set(removeGroup(currentNamespace, currentCluster, vGroup, unitName));
                    }
                }
            }
        }));
        return Optional.ofNullable(result.get()).orElseGet(() -> new Result<>("200", "change vGroup successfully!"));
    }

}
