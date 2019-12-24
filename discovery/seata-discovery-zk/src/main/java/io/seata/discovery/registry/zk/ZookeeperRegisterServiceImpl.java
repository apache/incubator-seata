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
package io.seata.discovery.registry.zk;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.seata.common.Constants.IP_PORT_SPLIT_CHAR;

/**
 * zookeeper path as /registry/zk/
 *
 * @author crazier.huang
 */
public class ZookeeperRegisterServiceImpl implements RegistryService<IZkChildListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegisterServiceImpl.class);

    private static volatile ZookeeperRegisterServiceImpl instance;
    private static volatile ZkClient zkClient;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
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

    private static final int REGISTERED_PATH_SET_SIZE = 1;
    private static final Set<String> REGISTERED_PATH_SET = Collections.synchronizedSet(new HashSet<>(REGISTERED_PATH_SET_SIZE));

    private ZookeeperRegisterServiceImpl() {
    }

    static ZookeeperRegisterServiceImpl getInstance() {
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
        doRegister(path);
    }

    private boolean doRegister(String path) {
        if (checkExists(path)) {
            return false;
        }
        createParentIfNotPresent(path);
        getClientInstance().createEphemeral(path, true);
        REGISTERED_PATH_SET.add(path);
        return true;
    }

    private void createParentIfNotPresent(String path) {
        int i = path.lastIndexOf('/');
        if (i > 0) {
            String parent = path.substring(0, i);
            if (!checkExists(parent)) {
                getClientInstance().createPersistent(parent);
            }
        }
    }

    private boolean checkExists(String path) {
        return getClientInstance().exists(path);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

        String path = getRegisterPathByPath(address);
        getClientInstance().delete(path);
        REGISTERED_PATH_SET.remove(path);
    }

    @Override
    public void subscribe(String cluster, IZkChildListener listener) throws Exception {
        if (null == cluster) {
            return;
        }

        String path = ROOT_PATH + cluster;
        if (!getClientInstance().exists(path)) {
            getClientInstance().createPersistent(path);
        }
        getClientInstance().subscribeChildChanges(path, listener);
        LISTENER_SERVICE_MAP.putIfAbsent(cluster, new CopyOnWriteArrayList<>());
        LISTENER_SERVICE_MAP.get(cluster).add(listener);
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

        return doLookup(clusterName);
    }

    // visible for test.
    List<InetSocketAddress> doLookup(String clusterName) throws Exception {
        boolean exist = getClientInstance().exists(ROOT_PATH + clusterName);
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

    @Override
    public void close() throws Exception {
        getClientInstance().close();
    }

    private ZkClient getClientInstance() {
        if (zkClient == null) {
            synchronized (ZookeeperRegisterServiceImpl.class) {
                if (null == zkClient) {
                    zkClient = buildZkClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIME_OUT_KEY),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIME_OUT_KEY));
                }
            }
        }
        return zkClient;
    }

    // visible for test.
    ZkClient buildZkClient(String address, int sessionTimeout, int connectTimeout) {
        ZkClient zkClient = new ZkClient(address, sessionTimeout, connectTimeout);
        if (!zkClient.exists(ROOT_PATH_WITHOUT_SUFFIX)) {
            zkClient.createPersistent(ROOT_PATH_WITHOUT_SUFFIX, true);
        }
        zkClient.subscribeStateChanges(new IZkStateListener() {

            @Override
            public void handleStateChanged(Watcher.Event.KeeperState keeperState) throws Exception {
                //ignore
            }

            @Override
            public void handleNewSession() throws Exception {
                recover();
            }

            @Override
            public void handleSessionEstablishmentError(Throwable throwable) throws Exception {
                //ignore
            }
        });
        return zkClient;
    }

    private void recover() throws Exception {
        // recover Server
        if (!REGISTERED_PATH_SET.isEmpty()) {
            REGISTERED_PATH_SET.forEach(this::doRegister);
        }
        // recover client
        if (!LISTENER_SERVICE_MAP.isEmpty()) {
            Map<String, List<IZkChildListener>> listenerMap = new HashMap<>(LISTENER_SERVICE_MAP);
            for (Map.Entry<String, List<IZkChildListener>> listenerEntry : listenerMap.entrySet()) {
                List<IZkChildListener> iZkChildListeners = listenerEntry.getValue();
                if (CollectionUtils.isEmpty(iZkChildListeners)) {
                    continue;
                }
                for (IZkChildListener listener : iZkChildListeners) {
                    subscribe(listenerEntry.getKey(), listener);
                }
            }
        }
    }

    private void subscribeCluster(String cluster) throws Exception {
        subscribe(cluster, (parentPath, currentChilds) -> {
            String clusterName = parentPath.replace(ROOT_PATH, "");
            if (CollectionUtils.isEmpty(currentChilds) && CLUSTER_ADDRESS_MAP.get(clusterName) != null) {
                CLUSTER_ADDRESS_MAP.remove(clusterName);
            } else if (!CollectionUtils.isEmpty(currentChilds)) {
                refreshClusterAddressMap(clusterName, currentChilds);
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

    private String getRegisterPathByPath(InetSocketAddress address) {
        return ROOT_PATH + getClusterName() + ZK_PATH_SPLIT_CHAR + NetUtil.toStringAddress(address);
    }
}
