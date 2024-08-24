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
package org.apache.seata.config.zk;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.AbstractConfiguration;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeType;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.processor.ConfigProcessor;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static org.apache.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;
import static org.apache.seata.config.ConfigurationKeys.SEATA_FILE_ROOT_CONFIG;

/**
 * The type Zookeeper configuration.
 */
public class ZookeeperConfiguration extends AbstractConfiguration {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfiguration.class);

    private static final String CONFIG_TYPE = "zk";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + SEATA_FILE_ROOT_CONFIG;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    private static final String CONNECT_TIMEOUT_KEY = "connectTimeout";
    private static final String AUTH_USERNAME = "username";
    private static final String AUTH_PASSWORD = "password";
    private static final String SERIALIZER_KEY = "serializer";
    private static final String CONFIG_PATH_KEY = "nodePath";
    private static final int THREAD_POOL_NUM = 1;
    private static final int DEFAULT_SESSION_TIMEOUT = 6000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final String DEFAULT_CONFIG_PATH = ROOT_PATH + "/seata.properties";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE
        + FILE_CONFIG_SPLIT_CHAR;
    private static final ExecutorService CONFIG_EXECUTOR = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
        Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        new NamedThreadFactory("ZKConfigThread", THREAD_POOL_NUM));
    private static volatile CuratorFramework zkClient;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, NodeCacheListenerImpl>> CONFIG_LISTENERS_MAP
        = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    private static volatile Properties seataConfig = new Properties();
    static final Charset CHARSET = StandardCharsets.UTF_8;
    private static Map<String, CuratorCache> nodeCacheMap = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Zookeeper configuration.
     */
    @SuppressWarnings("lgtm[java/unsafe-double-checked-locking-init-order]")
    public ZookeeperConfiguration() {
        if (zkClient == null) {
            synchronized (ZookeeperConfiguration.class) {
                if (zkClient == null) {
                    String serverAddr = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY);
                    int sessionTimeout = FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIMEOUT_KEY, DEFAULT_SESSION_TIMEOUT);
                    int connectTimeout = FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT);
                    CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                        .connectString(serverAddr)
                        .retryPolicy(new RetryNTimes(1, 1000))
                        .connectionTimeoutMs(connectTimeout)
                        .sessionTimeoutMs(sessionTimeout);
                    String username = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + AUTH_USERNAME);
                    String password = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + AUTH_PASSWORD);
                    if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
                        StringBuilder auth = new StringBuilder(username).append(":").append(password);
                        builder.authorization("digest", auth.toString().getBytes());
                    }
                    zkClient = builder.build();
                    zkClient.start();
                }
            }
            if (!checkExists(ROOT_PATH)) {
                createPersistent(ROOT_PATH);
            }
            initSeataConfig();
        }
    }

    public void createPersistent(String path) {
        try {
            zkClient.create().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            LOGGER.warn("ZNode " + path + " already exists.", e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public boolean checkExists(String path) {
        try {
            if (zkClient.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = seataConfig.getProperty(dataId);
        if (value != null) {
            return value;
        }
        FutureTask<String> future = new FutureTask<>(() -> {
            String path = buildPath(dataId);
            if (!checkExists(path)) {
                LOGGER.warn("config {} is not existed, return defaultValue {} ",
                    dataId, defaultValue);
                return defaultValue;
            }
            String value1 = readData(path);
            return StringUtils.isNullOrEmpty(value1) ? defaultValue : value1;
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("getConfig {} error or timeout, return defaultValue {}, exception:{} ",
                dataId, defaultValue, e.getMessage());
            return defaultValue;
        }
    }

    public String readData(String path) {
        try {
            byte[] dataBytes = zkClient.getData().forPath(path);
            return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, CHARSET);
        } catch (KeeperException.NoNodeException e) {
            // ignore NoNode Exception.
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        if (!seataConfig.isEmpty()) {
            seataConfig.setProperty(dataId, content);
            createPersistent(getConfigPath(), getSeataConfigStr());
            return true;
        }

        FutureTask<Boolean> future = new FutureTask<>(() -> {
            String path = buildPath(dataId);
            if (!checkExists(path)) {
                createPersistent(path, content);
            } else {
                createPersistent(path, content);
            }
            return true;
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("putConfig {}, value: {} is error or timeout, exception: {}",
                dataId, content, e.getMessage());
            return false;
        }
    }

    public String buildPath(String dataId) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        return path;
    }

    protected void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            zkClient.create().forPath(path, dataBytes);
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

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        if (!seataConfig.isEmpty()) {
            seataConfig.remove(dataId);
            createPersistent(getConfigPath(), getSeataConfigStr());
            return true;
        }

        FutureTask<Boolean> future = new FutureTask<>(() -> {
            String path = buildPath(dataId);
            return deletePath(path);
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("removeConfig {} is error or timeout, exception:{}", dataId, e.getMessage());
            return false;
        }

    }

    protected boolean deletePath(String path) {
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(path);
            return true;
        } catch (KeeperException.NoNodeException ignored) {
            return true;
        } catch (Exception e) {
            LOGGER.error("deletePath {} is error or timeout", path, e);
            return false;
        }
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        String path = buildPath(dataId);
        if (!seataConfig.isEmpty()) {
            NodeCacheListenerImpl zkListener = new NodeCacheListenerImpl(dataId, listener);
            CuratorCacheListener.builder().forAll(zkListener).build();
            CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>())
                .put(listener, zkListener);
            return;
        }
        if (checkExists(path)) {
            NodeCacheListenerImpl zkListener = new NodeCacheListenerImpl(path, listener);
            CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>())
                .put(listener, zkListener);
            addDataListener(path, zkListener);
        }
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configChangeListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configChangeListeners)) {
            String path = buildPath(dataId);
            if (checkExists(path)) {
                for (ConfigurationChangeListener entry : configChangeListeners) {
                    if (listener.equals(entry)) {
                        NodeCacheListenerImpl zkListener = null;
                        Map<ConfigurationChangeListener, NodeCacheListenerImpl> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
                        if (configListeners != null) {
                            zkListener = configListeners.get(listener);
                            configListeners.remove(entry);
                        }
                        if (zkListener != null) {
                            removeDataListener(path, zkListener);
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        ConcurrentMap<ConfigurationChangeListener, NodeCacheListenerImpl> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            return configListeners.keySet();
        } else {
            return null;
        }
    }

    private void initSeataConfig() {
        String configPath = getConfigPath();
        String config = readData(configPath);
        if (StringUtils.isNotBlank(config)) {
            try {
                seataConfig = ConfigProcessor.processConfig(config, getZkDataType());
            } catch (IOException e) {
                LOGGER.error("init config properties error", e);
            }
            addDataListener(configPath, new NodeCacheListenerImpl(configPath, null));
        }
    }

    private static String getConfigPath() {
        return FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + CONFIG_PATH_KEY, DEFAULT_CONFIG_PATH);
    }

    private static String getZkDataType() {
        return ConfigProcessor.resolverConfigDataType(getConfigPath());
    }

    private static String getSeataConfigStr() {
        StringBuilder sb = new StringBuilder();

        Enumeration<?> enumeration = seataConfig.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String property = seataConfig.getProperty(key);
            sb.append(key).append("=").append(property).append("\n");
        }

        return sb.toString();
    }

    public static class NodeCacheListenerImpl implements CuratorCacheListener {
        private String path;
        private ConfigurationChangeListener listener;

        public NodeCacheListenerImpl(String path, ConfigurationChangeListener listener) {
            this.path = path;
            this.listener = listener;
        }

        @Override
        public void event(Type type, ChildData oldData, ChildData data) {

            String o;
            if (type == Type.NODE_DELETED) {
                o = "";
            } else {
                o = new String(data.getData());
            }
            if (path.equals(getConfigPath())) {
                Properties seataConfigNew = new Properties();
                if (StringUtils.isNotBlank(o.toString())) {
                    try {
                        seataConfigNew = ConfigProcessor.processConfig(o.toString(), getZkDataType());

                    } catch (IOException e) {
                        LOGGER.error("load config properties error", e);
                        return;
                    }
                }

                for (Map.Entry<String, ConcurrentMap<ConfigurationChangeListener, NodeCacheListenerImpl>> entry : CONFIG_LISTENERS_MAP.entrySet()) {
                    String listenedDataId = entry.getKey();
                    String propertyOld = seataConfig.getProperty(listenedDataId, "");
                    String propertyNew = seataConfigNew.getProperty(listenedDataId, "");
                    if (!propertyOld.equals(propertyNew)) {
                        ConfigurationChangeEvent event = new ConfigurationChangeEvent()
                            .setDataId(listenedDataId)
                            .setNewValue(propertyNew)
                            .setChangeType(ConfigurationChangeType.MODIFY);

                        ConcurrentMap<ConfigurationChangeListener, NodeCacheListenerImpl> configListeners = entry.getValue();
                        for (ConfigurationChangeListener configListener : configListeners.keySet()) {
                            configListener.onProcessEvent(event);
                        }
                    }
                }
                seataConfig = seataConfigNew;

                return;
            } else {
                if (type == Type.NODE_DELETED) {
                    // Node is deleted.
                    String dataId = path.replaceFirst(ROOT_PATH + ZK_PATH_SPLIT_CHAR, "");
                    ConfigurationChangeEvent event = new ConfigurationChangeEvent().setDataId(dataId).setChangeType(
                        ConfigurationChangeType.DELETE);
                    listener.onProcessEvent(event);
                } else {
                    // Node is changed.
                    String dataId = path.replaceFirst(ROOT_PATH + ZK_PATH_SPLIT_CHAR, "");
                    ConfigurationChangeEvent event = new ConfigurationChangeEvent().setDataId(dataId).setNewValue(o.toString())
                        .setChangeType(ConfigurationChangeType.MODIFY);
                    listener.onProcessEvent(event);
                }
            }
        }
    }

    protected void addDataListener(String path, NodeCacheListenerImpl nodeCacheListener) {
        try {
            CuratorCache nodeCache = CuratorCache.build(zkClient, path);
            if (nodeCacheMap.putIfAbsent(path, nodeCache) != null) {
                return;
            }
            nodeCache.listenable().addListener(nodeCacheListener);
            nodeCache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Add nodeCache listener for path:" + path, e);
        }
    }

    protected void removeDataListener(String path, NodeCacheListenerImpl nodeCacheListener) {
        CuratorCache nodeCache = nodeCacheMap.get(path);
        if (nodeCache != null) {
            nodeCache.listenable().removeListener(nodeCacheListener);
        }
        nodeCacheListener.listener = null;
    }
}
