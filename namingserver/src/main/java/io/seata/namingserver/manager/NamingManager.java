package io.seata.namingserver.manager;


import io.seata.common.metadata.Cluster;
import io.seata.common.metadata.Node;
import io.seata.common.metadata.Unit;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.namingserver.listener.ClusterChangeEvent;
import io.seata.namingserver.pojo.AbstractClusterData;
import io.seata.namingserver.pojo.ClusterData;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@Component
public class NamingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingManager.class);
    private final HashMap<InetSocketAddress, Long> instanceLiveTable;
    private final HashMap<String/* VGroup */, HashMap<String/* namespace */, Pair<String/* clusterName */, String/* unitName */>>> VGroupMap;
    private final HashMap<String/* namespace */, HashMap<String/* clusterName */, ClusterData>> NamespaceClusterDataMap;
    private final long HEARTBEAT_TIME_THRESHOLD = 30 * 1000;
    protected final ScheduledExecutorService heartBeatCheckService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("heartBeatCheckExcuter", 1, true));


    @Autowired
    private ApplicationContext applicationContext;


    public NamingManager() {
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
        }, 0, HEARTBEAT_TIME_THRESHOLD, TimeUnit.MILLISECONDS);
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

            // 内部HashMap的遍历
            for (Map.Entry<String, Pair<String, String>> innerEntry : namespaceMap.entrySet()) {
                String namespace1 = innerEntry.getKey();
                Pair<String, String> pair = innerEntry.getValue();
                String clusterName1 = pair.getKey();
                String unitName1 = pair.getValue();
                if (namespace1.equals(namespace)
                        && clusterName1.equals(clusterName)
                        && (unitName1 == null || unitName1.equals(unitName))) {
                    applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
                }
            }
        }

    }

    public void registerInstance(Node node, String namespace, String clusterName, String unitName) {
        try {
            HashMap<String, ClusterData> clusterDataHashMap = NamespaceClusterDataMap.computeIfAbsent(namespace, k -> new HashMap<>());

            // add instance in cluster
            ClusterData clusterData = clusterDataHashMap.computeIfAbsent(clusterName, k -> new ClusterData(clusterName, (String) node.getMetadata().get("cluster-type")));
            clusterData.registerInstance(node, unitName);

            // if extended metadata includes vgroup mapping relationship, add it in clusterData
            Object mappingObj = node.getMetadata().get("vGroup");

            if (mappingObj instanceof HashMap) {
                HashMap<String, Object> mapping = (HashMap<String, Object>) mappingObj;
                mapping.forEach((vGroup, unitObj) -> {
                            changeGroup(namespace, clusterName, (String) unitObj, vGroup);
                        }
                );
            }
            notifyClusterChange(namespace, clusterName, unitName);
            instanceLiveTable.put(new InetSocketAddress(node.getIp(), node.getPort()), System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("Instance registered failed!", e);
        }
    }


    public void unregisterInstance(String unitName, Node node) {
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
                        instanceLiveTable.remove(new InetSocketAddress(node.getIp(), node.getPort()));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Instance unregistered failed!");
        }
    }


    public List<Cluster> getClusterListByVgroup(String vGroup, String namespace) {
        //找到事务分组所在的集群
        List<Cluster> clusterList = new ArrayList<>();
        try {
            Pair<String, String> clusterUnitPair = VGroupMap.get(vGroup).get(namespace);
            String clusterName = clusterUnitPair.getKey();
            String unitName = clusterUnitPair.getValue();
            ClusterData clusterData = NamespaceClusterDataMap.get(namespace).get(clusterName);
            clusterList.add(clusterData.getClusterByUnit(unitName));
        } catch (NullPointerException e) {
            LOGGER.warn("no cluster mapping for vGroup: {}", vGroup);
        }
        return clusterList;
    }

    public List<InetSocketAddress> getInstances(String namespace, String clusterName) {
        HashMap<String, ClusterData> clusterDataHashMap = NamespaceClusterDataMap.get(namespace);
        AbstractClusterData abstractClusterData = clusterDataHashMap.get(clusterName);
        if (abstractClusterData == null) {
            LOGGER.warn("no instances in {} : {}", namespace, clusterName);
            return Collections.EMPTY_LIST;
        }
        return abstractClusterData.getInstanceList().stream()
                .map(namingInstance -> new InetSocketAddress(namingInstance.getIp(), namingInstance.getPort()))
                .collect(Collectors.toList());
    }

    public void instanceHeartBeatCheck() {
        for (String namespace : NamespaceClusterDataMap.keySet()) {
            for (ClusterData clusterData : NamespaceClusterDataMap.get(namespace).values()) {
                for (Unit unit : clusterData.getUnitData().values()) {
                    Iterator<Node> instanceIterator = unit.getNamingInstanceList().iterator();

                    while (instanceIterator.hasNext()) {
                        Node instance = instanceIterator.next();
                        InetSocketAddress inetSocketAddress = new InetSocketAddress(instance.getIp(), instance.getPort());
                        long lastHeatBeatTimeStamp = instanceLiveTable.get(inetSocketAddress);

                        if (Math.abs(lastHeatBeatTimeStamp - System.currentTimeMillis()) > HEARTBEAT_TIME_THRESHOLD) {
                            instanceLiveTable.remove(inetSocketAddress);

                            instanceIterator.remove();  // Safe removal using iterator's remove method
                            clusterData.removeInstance(instance, unit.getUnitName());

                            notifyClusterChange(namespace, clusterData.getClusterName(), unit.getUnitName());
                            LOGGER.warn("{} instance has gone offline", instance.getIp() + ":" + instance.getPort());
                        }
                    }
                }
            }
        }


    }

}
