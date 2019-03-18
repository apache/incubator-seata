/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.discovery.registry;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fescar.common.Constants.IP_PORT_SPLIT_CHAR;

/**
 * zookeeper path as /registry/zk/
 *
 * @author crazier.huang
 * @date 2019/2/15
 */
public class ZookeeperRegisterServiceImpl implements RegistryService<IZkChildListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegisterServiceImpl.class);

    private static volatile ZookeeperRegisterServiceImpl instance;
    private static volatile ZkClient zkClient;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String REGISTRY_TYPE = "zk";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String SESSION_TIME_OUT_KEY = "session.timeout";
    private static final String CONNECT_TIME_OUT_KEY = "connect.timeout";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
        + FILE_CONFIG_SPLIT_CHAR;
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_REGISTRY + ZK_PATH_SPLIT_CHAR + REGISTRY_TYPE
        + ZK_PATH_SPLIT_CHAR;
    private static final String ROOT_PATH_WITHOUT_SUFFIX = ZK_PATH_SPLIT_CHAR + FILE_ROOT_REGISTRY + ZK_PATH_SPLIT_CHAR
        + REGISTRY_TYPE;
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<IZkChildListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();

    private ZookeeperRegisterServiceImpl() {}

    public static ZookeeperRegisterServiceImpl getInstance() {
        if (null == instance) {
            synchronized (ZookeeperRegisterServiceImpl.class) {
                if (null == instance) {
                    instance = new ZookeeperRegisterServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

        String path = getRegisterPathByPath(address);
        getClientInstance().createPersistent(path, true);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

        String path = getRegisterPathByPath(address);
        getClientInstance().delete(path);
    }

    @Override
    public void subscribe(String cluster, IZkChildListener listener) throws Exception {
        if (null == cluster) {
            return;
        }

        String path = ROOT_PATH + cluster;
        if (getClientInstance().exists(path)) {
            getClientInstance().subscribeChildChanges(path, listener);
            LISTENER_SERVICE_MAP.putIfAbsent(cluster, new ArrayList<>());
            LISTENER_SERVICE_MAP.get(cluster).add(listener);
        }
    }

    @Override
    public void unsubscribe(String cluster, IZkChildListener listener) throws Exception {
        if (null == cluster) {
            return;
        }
        String path = ROOT_PATH + cluster;
        if (getClientInstance().exists(path)) {
            getClientInstance().unsubscribeChildChanges(path, listener);

            List<IZkChildListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
            if (null != subscribeList) {
                List<IZkChildListener> newSubscribeList = new ArrayList<>();
                for (IZkChildListener eventListener : subscribeList) {
                    if (!eventListener.equals(listener)) {
                        newSubscribeList.add(eventListener);
                    }
                }
                LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
            }
        }

    }

    /**
     * @param key the key
     * @return
     * @throws Exception
     */
    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);

        if (null == clusterName) {
            return null;
        }

        Boolean exist = getClientInstance().exists(ROOT_PATH + clusterName);
        if (!exist) {
            return null;
        }

        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            List<String> childClusterPath = getClientInstance().getChildren(ROOT_PATH + clusterName);
            refreshClusterAddressMap(clusterName, childClusterPath);
            subscribeCluster(clusterName);
        }

        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    private ZkClient getClientInstance() {
        if (zkClient == null) {
            zkClient = new ZkClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY),
                FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIME_OUT_KEY),
                FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIME_OUT_KEY));
            if (!zkClient.exists(ROOT_PATH_WITHOUT_SUFFIX)) {
                zkClient.createPersistent(ROOT_PATH_WITHOUT_SUFFIX, true);
            }
        }
        return zkClient;
    }

    private void subscribeCluster(String clusterName) throws Exception {
        subscribe(clusterName, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                String clusterName = parentPath.replace(ROOT_PATH, "");
                if (CollectionUtils.isEmpty(currentChilds) && CLUSTER_ADDRESS_MAP.get(clusterName) != null) {
                    CLUSTER_ADDRESS_MAP.remove(clusterName);
                } else if (!CollectionUtils.isEmpty(currentChilds)) {
                    refreshClusterAddressMap(clusterName, currentChilds);
                }
            }
        });
    }

    private void refreshClusterAddressMap(String clusterName, List<String> instances) {
        List<InetSocketAddress> newAddressList = new ArrayList<>();
        if (instances == null) {
            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
            return;
        }
        for (String path : instances) {
            try {
                String[] ipAndPort = path.split(IP_PORT_SPLIT_CHAR);
                newAddressList.add(new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
            } catch (Exception e) {
                LOGGER.warn("The cluster instance info is error, instance info:{}", path);
            }
        }
        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
    }

    private String getClusterName() {
        String clusterConfigName = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName);
    }

    private String getServiceGroup(String key) {
        Configuration configuration = ConfigurationFactory.getInstance();
        String clusterNameKey = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        return configuration.getConfig(clusterNameKey);
    }

    private String getRegisterPathByPath(InetSocketAddress address) {
        return ROOT_PATH + getClusterName() + ZK_PATH_SPLIT_CHAR + NetUtil.toStringAddress(address);
    }
}
