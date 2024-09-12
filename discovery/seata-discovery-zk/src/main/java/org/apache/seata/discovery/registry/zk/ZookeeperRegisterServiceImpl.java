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
package org.apache.seata.discovery.registry.zk;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.CuratorCacheListenerBuilder;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zookeeper path as /registry/zk/
 */
public class ZookeeperRegisterServiceImpl implements RegistryService<CuratorCacheListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegisterServiceImpl.class);
    static final Charset CHARSET = StandardCharsets.UTF_8;
    private static volatile ZookeeperRegisterServiceImpl instance;
    private static volatile CuratorFramework zkClient;
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
    private static final ConcurrentMap<String, List<CuratorCacheListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Object> CLUSTER_LOCK = new ConcurrentHashMap<>();
    private static Map<String, CuratorCache> nodeCacheMap = new ConcurrentHashMap<>();

    private static final int REGISTERED_PATH_SET_SIZE = 1;
    private static final Set<String> REGISTERED_PATH_SET = Collections.synchronizedSet(new HashSet<>(REGISTERED_PATH_SET_SIZE));

    private String transactionServiceGroup;

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
        createEphemeral(path, Boolean.TRUE.toString());
        REGISTERED_PATH_SET.add(path);
        return true;
    }

    private void createParentIfNotPresent(String path) {
        int i = path.lastIndexOf('/');
        if (i > 0) {
            String parent = path.substring(0, i);
            if (!checkExists(parent)) {
                createPersistent(parent);
            }
        }
    }

    private boolean checkExists(String path) {
        try {
            if (getClientInstance().checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

        String path = getRegisterPathByPath(address);
        deletePath(path);
        REGISTERED_PATH_SET.remove(path);
    }

    @Override
    public void subscribe(String cluster, CuratorCacheListener listener) throws Exception {
        if (cluster == null) {
            return;
        }
        String path = ROOT_PATH + cluster;
        if (!checkExists(path)) {
            createPersistent(path);
        }
        subscribeChildChanges(path, listener);
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new CopyOnWriteArrayList<>())
            .add(listener);
    }

    private void subscribeChildChanges(String path, CuratorCacheListener listener) {
        CuratorCache nodeCache = CuratorCache.build(zkClient, path);
        if (nodeCacheMap.putIfAbsent(path, nodeCache) != null) {
            return;
        }
        nodeCache.listenable().addListener(listener);
        nodeCache.start();

    }

    private void unsubscribeChildChanges(String path, CuratorCacheListener listener) {
        CuratorCache nodeCache = nodeCacheMap.get(path);
        if (nodeCache != null) {
            nodeCache.listenable().removeListener(listener);
        }
    }

    @Override
    public void unsubscribe(String cluster, CuratorCacheListener listener) throws Exception {
        if (cluster == null) {
            return;
        }
        String path = ROOT_PATH + cluster;
        if (checkExists(path)) {
            unsubscribeChildChanges(path, listener);
            List<CuratorCacheListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
            if (subscribeList != null) {
                List<CuratorCacheListener> newSubscribeList = subscribeList.stream()
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
        transactionServiceGroup = key;
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
                    boolean exist = checkExists(ROOT_PATH + clusterName);
                    if (!exist) {
                        return null;
                    }

                    List<String> childClusterPath = getClientInstance().getChildren().forPath(ROOT_PATH + clusterName);
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

    private CuratorFramework getClientInstance() {
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
    CuratorFramework buildZkClient(String address, int sessionTimeout, int connectTimeout, String... authInfo) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(address)
            .retryPolicy(new RetryNTimes(1, 1000))
            .connectionTimeoutMs(connectTimeout)
            .sessionTimeoutMs(sessionTimeout);
        if (authInfo != null && authInfo.length == 2) {
            if (!StringUtils.isBlank(authInfo[0]) && !StringUtils.isBlank(authInfo[1])) {
                StringBuilder auth = new StringBuilder(authInfo[0]).append(":").append(authInfo[1]);
                builder.authorization("digest", auth.toString().getBytes());
            }
        }
        zkClient = builder.build();
        zkClient.start();

        if (!checkExists(ROOT_PATH_WITHOUT_SUFFIX)) {
            createPersistent(ROOT_PATH_WITHOUT_SUFFIX, Boolean.TRUE.toString());
        }
        subscribeStateChanges();
        return zkClient;
    }

    private void subscribeStateChanges() {
        getClientInstance().getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if (newState == ConnectionState.RECONNECTED || newState == ConnectionState.CONNECTED) {
                    try {
                        recover();
                    } catch (Exception e) {
                        LOGGER.error("handleNewSession error", e);
                    }
                } else {
                    LOGGER.error("stateChanged error, newState:{}", newState);
                }
            }
        });
    }

    private void recover() throws Exception {
        // recover Server
        if (!REGISTERED_PATH_SET.isEmpty()) {
            REGISTERED_PATH_SET.forEach(this::doRegister);
        }
        // recover client
        if (!LISTENER_SERVICE_MAP.isEmpty()) {
            Map<String, List<CuratorCacheListener>> listenerMap = new HashMap<>(LISTENER_SERVICE_MAP);
            LISTENER_SERVICE_MAP.clear();
            for (Map.Entry<String, List<CuratorCacheListener>> listenerEntry : listenerMap.entrySet()) {
                List<CuratorCacheListener> iZkChildListeners = listenerEntry.getValue();
                if (CollectionUtils.isEmpty(iZkChildListeners)) {
                    continue;
                }
                for (CuratorCacheListener listener : iZkChildListeners) {
                    subscribe(listenerEntry.getKey(), listener);
                }
            }
        }
    }

    private void subscribeCluster(String cluster) throws Exception {
        String path = ROOT_PATH + cluster;
        CuratorCacheListenerBuilder builder = CuratorCacheListener.builder();
        CuratorCacheListener listener = builder.forPathChildrenCache(path, getClientInstance(), new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                List<String> currentChilds = getClientInstance().getChildren().forPath(path);
                if (CollectionUtils.isEmpty(currentChilds) && CLUSTER_ADDRESS_MAP.get(cluster) != null) {
                    CLUSTER_ADDRESS_MAP.remove(cluster);
                } else if (!CollectionUtils.isEmpty(currentChilds)) {
                    ZookeeperRegisterServiceImpl.this.refreshClusterAddressMap(cluster, currentChilds);
                }
            }
        }).build();

        subscribe(cluster, listener);
    }

    private void refreshClusterAddressMap(String clusterName, List<String> instances) {
        List<InetSocketAddress> newAddressList = new ArrayList<>();
        if (instances == null) {
            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
            return;
        }
        for (String path : instances) {
            try {
                String[] ipAndPort = NetUtil.splitIPPortStr(path);
                newAddressList.add(new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
            } catch (Exception e) {
                LOGGER.warn("The cluster instance info is error, instance info:{}", path);
            }
        }
        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);

        removeOfflineAddressesIfNecessary(transactionServiceGroup, clusterName, newAddressList);
    }

    private String getClusterName() {
        String clusterConfigName = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER);
        return FILE_CONFIG.getConfig(clusterConfigName);
    }

    private String getRegisterPathByPath(InetSocketAddress address) {
        return ROOT_PATH + getClusterName() + ZK_PATH_SPLIT_CHAR + NetUtil.toStringAddress(address);
    }

    protected void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            zkClient.create().creatingParentsIfNeeded().forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
            try {
                zkClient.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void createPersistent(String path) {
        try {
            zkClient.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            // ignore
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void createEphemeral(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            getClientInstance().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
            try {
                getClientInstance().setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected boolean deletePath(String path) {
        try {
            getClientInstance().delete().deletingChildrenIfNeeded().forPath(path);
            return true;
        } catch (KeeperException.NoNodeException ignored) {
            return true;
        } catch (Exception e) {
            LOGGER.error("deletePath {} is error or timeout", path, e);
            return false;
        }
    }

    @VisibleForTesting
    CuratorFramework getZkClient() {
        return zkClient;
    }
}
