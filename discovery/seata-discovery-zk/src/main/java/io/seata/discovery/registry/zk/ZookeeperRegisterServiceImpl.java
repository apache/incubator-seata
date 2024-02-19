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
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.exception.ConfigNotFoundException;
import io.seata.discovery.registry.RegistryService;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String AUTH_USERNAME = "username";
    private static final String AUTH_PASSWORD = "password";
    private static final String SESSION_TIME_OUT_KEY = "sessionTimeout";
    private static final String CONNECT_TIME_OUT_KEY = "connectTimeout";
    private static final int DEFAULT_SESSION_TIMEOUT = 6000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
        + FILE_CONFIG_SPLIT_CHAR;
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_REGISTRY + ZK_PATH_SPLIT_CHAR + REGISTRY_TYPE
        + ZK_PATH_SPLIT_CHAR;
    private static final String ROOT_PATH_WITHOUT_SUFFIX = ZK_PATH_SPLIT_CHAR + FILE_ROOT_REGISTRY + ZK_PATH_SPLIT_CHAR
        + REGISTRY_TYPE;
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<IZkChildListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Object> CLUSTER_LOCK = new ConcurrentHashMap<>();

    private static final int REGISTERED_PATH_SET_SIZE = 1;
    private static final Set<String> REGISTERED_PATH_SET = Collections.synchronizedSet(new HashSet<>(REGISTERED_PATH_SET_SIZE));

    private ZookeeperRegisterServiceImpl() {
    }

    static ZookeeperRegisterServiceImpl getInstance() {
        if (instance == null) {
            synchronized (ZookeeperRegisterServiceImpl.class) {
                if (instance == null) {
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
        if (cluster == null) {
            return;
        }

        String path = ROOT_PATH + cluster;
        if (!getClientInstance().exists(path)) {
            getClientInstance().createPersistent(path);
        }
        getClientInstance().subscribeChildChanges(path, listener);
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    @Override
    public void unsubscribe(String cluster, IZkChildListener listener) throws Exception {
        if (cluster == null) {
            return;
        }
        String path = ROOT_PATH + cluster;
        if (getClientInstance().exists(path)) {
            getClientInstance().unsubscribeChildChanges(path, listener);

            List<IZkChildListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
            if (subscribeList != null) {
                List<IZkChildListener> newSubscribeList = subscribeList.stream()
                        .filter(eventListener -> !eventListener.equals(listener))
                        .collect(Collectors.toList());
                LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
            }
        }

    }

    /**
     * @param key the key
     * @return the socket address list
     * @throws Exception the exception
     */
    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);

        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }

        return doLookup(clusterName);
    }

    // visible for test.
    List<InetSocketAddress> doLookup(String clusterName) throws Exception {
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            Object lock = CLUSTER_LOCK.putIfAbsent(clusterName, new Object());
            if (null == lock) {
                lock = CLUSTER_LOCK.get(clusterName);
            }
            synchronized (lock) {
                if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
                    boolean exist = getClientInstance().exists(ROOT_PATH + clusterName);
                    if (!exist) {
                        return null;
                    }

                    List<String> childClusterPath = getClientInstance().getChildren(ROOT_PATH + clusterName);
                    refreshClusterAddressMap(clusterName, childClusterPath);
                    subscribeCluster(clusterName);
                }
            }
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
                if (zkClient == null) {
                    zkClient = buildZkClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIME_OUT_KEY, DEFAULT_SESSION_TIMEOUT),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIME_OUT_KEY, DEFAULT_CONNECT_TIMEOUT),
                        FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + AUTH_USERNAME),
                        FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + AUTH_PASSWORD));
                }
            }
        }
        return zkClient;
    }

    // visible for test.
    private ZkClient buildZkClient(String address, int sessionTimeout, int connectTimeout,String... authInfo) {
        ZkClient zkClient = new ZkClient(address, sessionTimeout, connectTimeout);
        if (authInfo != null && authInfo.length == 2) {
            if (!StringUtils.isBlank(authInfo[0]) && !StringUtils.isBlank(authInfo[1])) {
                StringBuilder auth = new StringBuilder(authInfo[0]).append(":").append(authInfo[1]);
                zkClient.addAuthInfo("digest", auth.toString().getBytes());
            }
        }
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
            LISTENER_SERVICE_MAP.clear();
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
        String clusterConfigName = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER);
        return FILE_CONFIG.getConfig(clusterConfigName);
    }

    private String getRegisterPathByPath(InetSocketAddress address) {
        return ROOT_PATH + getClusterName() + ZK_PATH_SPLIT_CHAR + NetUtil.toStringAddress(address);
    }
}
