package io.seata.namingserver.manager;


import io.seata.common.thread.NamedThreadFactory;
import io.seata.discovery.registry.namingserver.NamingInstance;
import io.seata.namingserver.ClusterDataFactory;
import io.seata.namingserver.ClusterNotFoundException;
import io.seata.namingserver.listener.ClusterChangeEvent;
import io.seata.namingserver.pojo.AbstractClusterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class NamingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingManager.class);
    private final HashMap<InetSocketAddress, Long> instanceLiveTable;
    private final HashMap<String/* namespace */, HashMap<String/* clusterName */,AbstractClusterData>> NamespaceClusterDataMap;
    private final long HEARTBEAT_TIME_THRESHOLD = 30 * 1000;
    protected final ScheduledExecutorService heartBeatCheckService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("heartBeatCheckExcuter", 1, true));


    @Autowired
    private ApplicationContext applicationContext;


    public NamingManager() {
        this.instanceLiveTable = new HashMap<>();
        this.NamespaceClusterDataMap=new HashMap<>();
        //start heartbeat check
        this.heartBeatCheckService.scheduleAtFixedRate(() -> {
            try {
                instanceHeartBeatCheck();
            } catch (Exception e) {
                LOGGER.error("Heart Beat Check Exception", e);
            }
        }, 0, HEARTBEAT_TIME_THRESHOLD, TimeUnit.MILLISECONDS);
    }

    public void addvGroup(String namespace,String clusterName, String vGroup) {
        HashMap<String, AbstractClusterData> clusterDataHashMap = NamespaceClusterDataMap.get(namespace);
        if (clusterDataHashMap != null) {
            AbstractClusterData clusterData = clusterDataHashMap.get(clusterName);
            if (clusterData != null) {
                clusterData.addvGroup(vGroup);
            } else {
                throw new ClusterNotFoundException(clusterName + ": Cluster not found!");
            }
        } else {
            LOGGER.error("Namespace not found:"+namespace);
        }

    }

    public void removeGroup(String namespace,String clusterName, String vGroup) {
        HashMap<String, AbstractClusterData> clusterDataHashMap = NamespaceClusterDataMap.get(namespace);
        if (clusterDataHashMap != null) {
            AbstractClusterData clusterData = clusterDataHashMap.get(clusterName);
            if (clusterData != null) {
                clusterData.removeGroup(vGroup);
            } else {
                throw new ClusterNotFoundException(clusterName + ": Cluster not found!");
            }
        } else {
            LOGGER.error("Namespace not found:"+namespace);
        }
    }

    public void registerInstance(NamingInstance namingInstance, String namespace) {
        try {
            HashMap<String,AbstractClusterData> clusterDataHashMap = NamespaceClusterDataMap.computeIfAbsent(namespace, k -> new HashMap<>());
            String clusterName = namingInstance.getClusterName();


            //add instance in cluster
            AbstractClusterData clusterData = clusterDataHashMap.computeIfAbsent(clusterName, k -> ClusterDataFactory.createClusterData((String) namingInstance.getMetadata().get("cluster-type")));
            clusterData.registerInstance(namingInstance);

            instanceLiveTable.put(new InetSocketAddress(namingInstance.getIp(), namingInstance.getPort()), System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("Instance registered failed!");
        }
    }


    public void unregisterInstance(NamingInstance namingInstance, String namespace) {
        try {
            HashMap<String,AbstractClusterData> clusterDataHashMap = NamespaceClusterDataMap.computeIfAbsent(namespace, k -> new HashMap<>());
            String clusterName = namingInstance.getClusterName();
            if (!clusterDataHashMap.containsKey(clusterName)) {
                LOGGER.warn("The cluster" + clusterName + " is not present in the namespace " + namespace);
                return;
            }

            //remove instance in cluster
            AbstractClusterData clusterData = clusterDataHashMap.get(clusterName);
            clusterData.removeInstance(namingInstance);

            instanceLiveTable.remove(new InetSocketAddress(namingInstance.getIp(), namingInstance.getPort()));
        } catch (Exception e) {
            LOGGER.error("Instance unregistered failed!");
        }
    }


    public List<NamingInstance> getInstanceListByVgroup(String vGroup, String namespace) {
        HashMap<String,AbstractClusterData> clusterDataHashMap = NamespaceClusterDataMap.computeIfAbsent(namespace, k -> new HashMap<>());
        //find cluster mapped for vgroup
        AbstractClusterData cluster=null;
        for (AbstractClusterData clusterData:clusterDataHashMap.values()) {
            if (clusterData.hasVgroup(vGroup)) {
                cluster = clusterData;
                break;
            }
        }
        if (cluster == null) {
            throw new ClusterNotFoundException("No cluster mapping for vgroup: " + vGroup);
        }
        return cluster.getInstanceListByVgroup(vGroup);
    }

    public void instanceHeartBeatCheck() {
        List<AbstractClusterData> clusterDataList = NamespaceClusterDataMap.values().stream()
                .flatMap(clusterDataMap -> clusterDataMap.values().stream())
                .collect(Collectors.toList());

        for (AbstractClusterData clusterData : clusterDataList) {
            Iterator<NamingInstance> instanceIterator = clusterData.getInstanceList().iterator();

            while (instanceIterator.hasNext()) {
                NamingInstance instance = instanceIterator.next();
                InetSocketAddress inetSocketAddress = new InetSocketAddress(instance.getIp(), instance.getPort());
                long lastHeatBeatTimeStamp = instanceLiveTable.get(inetSocketAddress);

                if (Math.abs(lastHeatBeatTimeStamp - System.currentTimeMillis()) > HEARTBEAT_TIME_THRESHOLD) {
                    instanceLiveTable.remove(inetSocketAddress);

                    instanceIterator.remove();
                    clusterData.removeInstance(instance);
                    for (String vGroup : clusterData.getVgroups()) {
                        applicationContext.publishEvent(new ClusterChangeEvent(this, vGroup, System.currentTimeMillis()));
                    }
                    LOGGER.warn("{} instance has gone offline", instance.getIp() + ":" + instance.getPort());
                }
            }

        }

    }

}
