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
package org.apache.seata.config.store.rocksdb;


import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.*;
import org.apache.seata.config.store.AbstractConfigStoreManager;
import org.apache.seata.config.store.ConfigStoreManager;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.Constants.DEFAULT_STORE_DATA_ID;
import static org.apache.seata.common.Constants.DEFAULT_STORE_NAMESPACE;



/**
 * The RocksDB config store manager
 *
 */
public class RocksDBConfigStoreManager extends AbstractConfigStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBConfigStoreManager.class);
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String DB_PATH = RocksDBOptionsFactory.getDBPath();
    private static final String DEFAULT_NAMESPACE = DEFAULT_STORE_NAMESPACE;
    private static final String DEFAULT_DATA_ID = DEFAULT_STORE_DATA_ID;
    private static String CURRENT_DATA_ID;
    private static String CURRENT_NAMESPACE;
    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final DBOptions DB_OPTIONS = RocksDBOptionsFactory.getDBOptions();
    private final Map<String, ReentrantReadWriteLock> LOCK_MAP = new ConcurrentHashMap<>();
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String/*namespace*/, Map<String/*dataId*/, Set<ConfigurationChangeListener>>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>(
            MAP_INITIAL_CAPACITY);

    //====================================NON COMMON FILED===================================
    private static volatile RocksDBConfigStoreManager instance;
    private RocksDB rocksdb;
    private final Map<String, ColumnFamilyHandle> columnFamilyHandleMap = new ConcurrentHashMap<>();
    private final String VERSION_COLUMN_FAMILY = "config_version";
    private static final List<String> prefixList = Arrays.asList(FILE_ROOT_PREFIX_CONFIG, FILE_ROOT_PREFIX_REGISTRY, SERVER_PREFIX, CLIENT_PREFIX, SERVICE_PREFIX,
            STORE_PREFIX, METRICS_PREFIX, TRANSPORT_PREFIX, LOG_PREFIX, TCC_PREFIX);


    public static RocksDBConfigStoreManager getInstance() {
        if (instance == null) {
            synchronized (RocksDBConfigStoreManager.class) {
                if (instance == null) {
                    instance = new RocksDBConfigStoreManager();
                }
            }
        }
        return instance;
    }

    public RocksDBConfigStoreManager() {
        super();
        CURRENT_NAMESPACE = FILE_CONFIG.getConfig(CONFIG_STORE_NAMESPACE, DEFAULT_NAMESPACE);
        CURRENT_DATA_ID = FILE_CONFIG.getConfig(CONFIG_STORE_DATA_ID, DEFAULT_DATA_ID);
        openRocksDB();
        maybeNeedLoadOriginConfig();
        LOGGER.info("RocksDBConfigStoreManager initialized successfully");
    }

    private void openRocksDB(){
        final List<ColumnFamilyHandle> handles = new ArrayList<>();
        final List<ColumnFamilyDescriptor> descriptors = new ArrayList<>();
        try (final Options options = new Options()){
            List<byte[]> cfs = RocksDB.listColumnFamilies(options, DB_PATH);
            for (byte[] cf : cfs) {
                String namespace = new String(cf);
                descriptors.add(new ColumnFamilyDescriptor(cf, RocksDBOptionsFactory.getColumnFamilyOptionsMap(namespace)));
            }
            // create default column family and config version column family
            if (CollectionUtils.isEmpty(descriptors)) {
                descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, RocksDBOptionsFactory.getColumnFamilyOptionsMap(new String(RocksDB.DEFAULT_COLUMN_FAMILY))));
                descriptors.add(new ColumnFamilyDescriptor(VERSION_COLUMN_FAMILY.getBytes(DEFAULT_CHARSET), RocksDBOptionsFactory.getColumnFamilyOptionsMap(VERSION_COLUMN_FAMILY)));
            }
            this.rocksdb = RocksDBFactory.getInstance(DB_PATH, DB_OPTIONS, descriptors, handles);
            for (ColumnFamilyHandle handle : handles) {
                columnFamilyHandleMap.put(new String(handle.getName()), handle);
            }
        }catch (RocksDBException e){
            LOGGER.error("open rocksdb error", e);
        }
    }

    private ColumnFamilyHandle getOrCreateColumnFamilyHandle(String namespace) throws RocksDBException{
        ColumnFamilyHandle handle = columnFamilyHandleMap.get(namespace);
        if (handle == null) {
            synchronized (RocksDBConfigStoreManager.class) {
                handle = columnFamilyHandleMap.get(namespace);
                if (handle == null) {
                    handle = rocksdb.createColumnFamily(new ColumnFamilyDescriptor(
                            namespace.getBytes(DEFAULT_CHARSET), RocksDBOptionsFactory.getColumnFamilyOptionsMap(namespace)));
                    columnFamilyHandleMap.put(namespace, handle);
                }
            }
        }
        return handle;
    }

    /**
     * load origin config if first startup
     */
    private void maybeNeedLoadOriginConfig() {
        if (isEmpty(CURRENT_NAMESPACE, CURRENT_DATA_ID)){
            Map<String, Object> configs = new HashMap<>();
            Map<String, Object> seataConfigs = new HashMap<>();
            String pathDataId = String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                    ConfigurationKeys.FILE_ROOT_CONFIG, FILE_TYPE, NAME_KEY);
            String name = FILE_CONFIG.getConfig(pathDataId);
            // create FileConfiguration for read file.conf
            Optional<FileConfiguration> originFileInstance = Optional.ofNullable(new FileConfiguration(name));
            originFileInstance
                    .ifPresent(fileConfiguration -> configs.putAll(fileConfiguration.getFileConfig().getAllConfig()));
            configs.forEach((k, v) -> {
                if (v instanceof String) {
                    if (StringUtils.isEmpty((String)v)) {
                        return;
                    }
                }
                // compatible with the config under Spring Boot
                if (k.startsWith(SEATA_FILE_PREFIX_ROOT_CONFIG)) {
                    k = k.substring(SEATA_FILE_PREFIX_ROOT_CONFIG.length());
                }
                // filter all seata related configs
                if (prefixList.stream().anyMatch(k::startsWith)) {
                    seataConfigs.put(k, v);
                }
            });
            putAll(CURRENT_NAMESPACE, CURRENT_DATA_ID, seataConfigs);
            LOGGER.info("Load initialization configuration file sucessfully in namespace: {}, dataId: {}", CURRENT_NAMESPACE, CURRENT_DATA_ID);
        }
    }

    /**
     * Acquire lock of the given namespace
     * @param namespace
     */
    private ReentrantReadWriteLock acquireLock(String namespace) {
        return LOCK_MAP.computeIfAbsent(namespace, k -> new ReentrantReadWriteLock());
    }

    /**
     * Get config map of the given namespace and dataId
     * @param namespace
     * @param dataId
     * @return
     * @throws RocksDBException
     */
    private Map<String, Object> getConfigMap(String namespace, String dataId) throws RocksDBException{
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.readLock().lock();
        try {
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            // the column family not exist, return empty map
            if (handle == null) {
                return new HashMap<>();
            }
            byte[] value = rocksdb.get(handle, dataId.getBytes(DEFAULT_CHARSET));
            String configStr = value != null ? new String(value, DEFAULT_CHARSET) : null;
            return ConfigStoreManager.convertConfigStr2Map(configStr);
        }finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the config value of the given namespace and dataId
     * @param namespace
     * @param dataId
     * @param key
     * @return
     */
    @Override
    public String get(String namespace, String dataId, String key) {
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.readLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(namespace, dataId);
            return configMap.get(key) != null ? configMap.get(key).toString() : null;
        }catch (RocksDBException e) {
            LOGGER.error("Failed to get value for key: " + key, e);
        }finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * Get all config items of the given namespace and dataId
     * @param namespace
     * @param dataId
     * @return
     */
    @Override
    public Map<String, Object> getAll(String namespace, String dataId) {
        try {
            return getConfigMap(namespace, dataId);
        }catch (RocksDBException e) {
            LOGGER.error("Failed to get all configs", e);
        }
        return null;
    }

    /**
     * Put a config item to the given namespace and dataId
     * @param namespace
     * @param dataId
     * @param key
     * @param value
     * @return
     */
    @Override
    public Boolean put(String namespace, String dataId, String key, Object value) {
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.writeLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(namespace, dataId);
            configMap.put(key, value);
            String configStr = ConfigStoreManager.convertConfig2Str(configMap);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            rocksdb.put(handle, dataId.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            updateConfigVersion(namespace, dataId);
            notifyConfigChange(namespace, dataId, new ConfigurationChangeEvent(namespace, dataId, configStr));
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to put value for key: " + key, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * Delete a config item with the given key from the given namespace and dataId
     * @param namespace
     * @param dataId
     * @param key
     * @return
     */
    @Override
    public Boolean delete(String namespace, String dataId, String key) {
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.writeLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(namespace, dataId);
            configMap.remove(key);
            String configStr = ConfigStoreManager.convertConfig2Str(configMap);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            rocksdb.put(handle, dataId.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            updateConfigVersion(namespace, dataId);
            notifyConfigChange(namespace, dataId, new ConfigurationChangeEvent(namespace, dataId, configStr));
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to delete value for key: " + key, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * Put all config items into the given namespace and dataId
     * @param namespace
     * @param dataId
     * @param configMap
     * @return
     */
    @Override
    public Boolean putAll(String namespace, String dataId, Map<String, Object> configMap) {
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.writeLock().lock();
        try{
            String configStr = ConfigStoreManager.convertConfig2Str(configMap);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            rocksdb.put(handle, dataId.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            updateConfigVersion(namespace, dataId);
            notifyConfigChange(namespace, dataId, new ConfigurationChangeEvent(namespace, dataId, configStr));
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to put all configs", e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * Delete all config items in the given namespace and dataId
     * @param namespace
     * @param dataId
     * @return
     */
    @Override
    public Boolean deleteAll(String namespace, String dataId) {
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.writeLock().lock();
        try {
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            rocksdb.delete(handle, dataId.getBytes(DEFAULT_CHARSET));
            deleteConfigVersion(namespace, dataId);
            notifyConfigChange(namespace, dataId, new ConfigurationChangeEvent(namespace, dataId, null));
            return true;
        } catch (RocksDBException e) {
            LOGGER.error("Failed to clear all configs", e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }


    /**
     * Get all key-values pairs in all namespaces, mainly used for backup or snapshot
     * @return Map(namespace -> Map(dataId -> value))
     */
    @Override
    public Map<String, Map<String, Object>> getConfigMap() {
        Map<String, Map<String, Object>> configMap = new HashMap<>();
        for (String namespace : columnFamilyHandleMap.keySet()) {
            HashMap<String, Object> configs = new HashMap<>();
            ReentrantReadWriteLock lock = acquireLock(namespace);
            lock.readLock().lock();
            RocksIterator iterator = null;
            try {
                ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
                iterator = rocksdb.newIterator(handle);
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    String key = new String(iterator.key(), DEFAULT_CHARSET);
                    String value = new String(iterator.value(), DEFAULT_CHARSET);
                    configs.put(key, value);
                }
                configMap.put(namespace, configs);
            } catch (RocksDBException e) {
                LOGGER.error("Failed to get configMap in namespace : {}", namespace, e);
            } finally {
                if (iterator != null) {
                    iterator.close();
                }
                lock.readLock().unlock();
            }
        }
        return configMap;
    }

    /**
     * Put all key-value pairs into the specified column family, mainly used for backup or snapshot
     * @param configMap Map(namespace -> Map(dataId -> value))
     * @return
     */
    @Override
    public Boolean putConfigMap(Map<String, Map<String, Object>> configMap) {
        try (WriteBatch batch = new WriteBatch(); WriteOptions writeOptions = new WriteOptions()) {
            for (Map.Entry<String, Map<String, Object>> entry : configMap.entrySet()) {
                String namespace = entry.getKey();
                Map<String, Object> configs = entry.getValue();
                ReentrantReadWriteLock lock = acquireLock(namespace);
                lock.writeLock().lock();
                try {
                    ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
                    for (Map.Entry<String, Object> nsEntry : configs .entrySet()) {
                        batch.put(handle, nsEntry.getKey().getBytes(DEFAULT_CHARSET), nsEntry.getValue().toString().getBytes(DEFAULT_CHARSET));
                    }
                }catch (RocksDBException e){
                    LOGGER.error("Failed to put configMap in namespace : {}", namespace, e);
                }finally {
                    lock.writeLock().unlock();
                }
            }
            rocksdb.write(writeOptions, batch);
            for (Map.Entry<String, Map<String, Object>> entry : configMap.entrySet()) {
                String namespace = entry.getKey();
                Map<String, Object> configs = entry.getValue();
                for (Map.Entry<String, Object> kv : configs.entrySet()) {
                    String dataId = kv.getKey();
                    updateConfigVersion(namespace, dataId);
                    notifyConfigChange(namespace, dataId, new ConfigurationChangeEvent(namespace, kv.getKey(), kv.getValue().toString()));
                }
            }
            return true;
        }catch (RocksDBException e) {
            LOGGER.error("Failed to put all configMap", e);
            return false;
        }
    }

    /**
     * Empty all data in rocksdb, i.e. delete all key-value pairs in all column family
     * @return
     */
    @Override
    public Boolean clearData() {
        Map<String, Set<String>> clearDataMap = new HashMap<>();
        try (WriteBatch batch = new WriteBatch(); WriteOptions writeOptions = new WriteOptions()) {
            for (ColumnFamilyHandle handle : columnFamilyHandleMap.values()) {
                String namespace = new String(handle.getName());
                ReentrantReadWriteLock lock = acquireLock(namespace);
                lock.writeLock().lock();
                HashSet<String> deleteKeySet = new HashSet<>();
                try(RocksIterator iterator = rocksdb.newIterator(handle)) {
                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        batch.delete(handle, iterator.key());
                        deleteKeySet.add(new String(iterator.key()));
                    }
                    clearDataMap.put(namespace, deleteKeySet);
                }finally {
                    lock.writeLock().unlock();
                }
            }
            rocksdb.write(writeOptions, batch);
            for (Map.Entry<String, Set<String>> entry : clearDataMap.entrySet()) {
                String namespace = entry.getKey();
                for (String key : entry.getValue()) {
                    deleteConfigVersion(namespace, key);
                    notifyConfigChange(namespace, key, new ConfigurationChangeEvent(namespace, key, null));
                }
            }
            return true;
        }catch (RocksDBException e) {
            LOGGER.error("Failed to clear all data in rocksdb", e);
            return false;
        }
    }

    /**
     * Check whether the config data exists in the given namespace and dataId
     * @param namespace
     * @param dataId
     * @return
     */
    @Override
    public Boolean isEmpty(String namespace, String dataId) {
        return CollectionUtils.isEmpty(getAll(namespace, dataId));
    }

    /**
     * Get all namespaces in current rocksdb instance
     * @return
     */
    @Override
    public List<String> getAllNamespaces() {
        return columnFamilyHandleMap.keySet().stream()
                .filter(namespace -> !namespace.equals(VERSION_COLUMN_FAMILY))
                .collect(Collectors.toList());
    }

    /**
     * Get all dataIds in the given namespace
     * @param namespace
     * @return
     */
    @Override
    public List<String> getAllDataIds(String namespace) {
        if (StringUtils.isEmpty(namespace) || !columnFamilyHandleMap.containsKey(namespace)) {
            return Collections.emptyList();
        }
        List<String> dataIds = new ArrayList<>();
        ReentrantReadWriteLock lock = acquireLock(namespace);
        lock.readLock().lock();
        RocksIterator iterator = null;
        try {
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(namespace);
            iterator = rocksdb.newIterator(handle);
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                String dataId = new String(iterator.key(), DEFAULT_CHARSET);
                dataIds.add(dataId);
            }
        }catch (RocksDBException e) {
            LOGGER.error("Failed to get all dataIds in namespace: {}", namespace, e);
        }finally {
            if (iterator != null) {
                iterator.close();
            }
            lock.readLock().unlock();
        }
        return dataIds;
    }

    /**
     * Get the config version in the given namespace and dataId
     * @param namespace
     * @param dataId
     * @return
     */
    @Override
    public Long getConfigVersion(String namespace, String dataId) {
        ReentrantReadWriteLock lock = acquireLock(VERSION_COLUMN_FAMILY);
        lock.readLock().lock();
        try {
            String configVersionKey = getConfigVersionKey(namespace, dataId);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(VERSION_COLUMN_FAMILY);
            byte[] value = rocksdb.get(handle, configVersionKey.getBytes(DEFAULT_CHARSET));
            return value != null ? NumberUtils.bytesToLong(value) : null;
        }catch (RocksDBException | IllegalArgumentException e) {
            LOGGER.error("Failed to get config version in namespace: {} and dataId: {}", namespace, dataId, e);
        }finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * Put the config version in the given namespace and dataId
     * @param namespace
     * @param dataId
     * @param version
     * @return
     */
    @Override
    public Boolean putConfigVersion(String namespace, String dataId, Long version) {
        ReentrantReadWriteLock lock = acquireLock(VERSION_COLUMN_FAMILY);
        lock.writeLock().lock();
        try {
            String configVersionKey = getConfigVersionKey(namespace, dataId);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(VERSION_COLUMN_FAMILY);
            rocksdb.put(handle, configVersionKey.getBytes(DEFAULT_CHARSET), NumberUtils.longToBytes(version));
            return true;
        }catch (RocksDBException | IllegalArgumentException e) {
            LOGGER.error("Failed to put config version in namespace: {} and dataId: {}", namespace, dataId, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * Delete the config version in the given namespace and dataId when the config data is deleted.
     * @param namespace
     * @param dataId
     * @return
     */
    @Override
    public Boolean deleteConfigVersion(String namespace, String dataId) {
        ReentrantReadWriteLock lock = acquireLock(VERSION_COLUMN_FAMILY);
        lock.writeLock().lock();
        try {
            String configVersionKey = getConfigVersionKey(namespace, dataId);
            ColumnFamilyHandle handle = getOrCreateColumnFamilyHandle(VERSION_COLUMN_FAMILY);
            rocksdb.delete(handle, configVersionKey.getBytes(DEFAULT_CHARSET));
            return true;
        }catch (RocksDBException | IllegalArgumentException e) {
            LOGGER.error("Failed to put config version in namespace: {} and dataId: {}", namespace, dataId, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    private String getConfigVersionKey(String namespace, String dataId) {
        if (StringUtils.isEmpty(namespace) || StringUtils.isEmpty(dataId)) {
            throw new IllegalArgumentException("Invalid config namespace or dataId");
        }
        return namespace + "_" + dataId;
    }

    @Override
    public void shutdown() {
        synchronized (RocksDBConfigStoreManager.class){
            // 1. close all handles
            for (ColumnFamilyHandle handle : columnFamilyHandleMap.values()) {
                if (handle != null) {
                    handle.close();
                }
            }
            // 2. close options
            RocksDBOptionsFactory.releaseAllOptions();
            // 3. close db
            RocksDBFactory.close();
            // 4. destroy db if needed
            if (RocksDBOptionsFactory.getDBDestroyOnShutdown()) {
                destroy();
            }
            // 5. help gc
            columnFamilyHandleMap.clear();
            this.rocksdb = null;
            LOGGER.info("RocksDBConfigStoreManager has shutdown");
        }
    }

    @Override
    public void destroy() {
        RocksDBFactory.destroy(DB_PATH);
        LOGGER.info("DB destroyed, the db path is: {}.", DB_PATH);
    }


    @Override
    public void addConfigListener(String namespace, String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Map<String, Set<ConfigurationChangeListener>> listenerMap = CONFIG_LISTENERS_MAP.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
        listenerMap.computeIfAbsent(dataId, k -> ConcurrentHashMap.newKeySet())
                .add(listener);
    }

    @Override
    public void removeConfigListener(String namespace, String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        // dataId -> listener
        Map<String, Set<ConfigurationChangeListener>> listenerMap = CONFIG_LISTENERS_MAP.get(namespace);
        if (CollectionUtils.isNotEmpty(listenerMap)) {
            Set<ConfigurationChangeListener> configChangeListeners = listenerMap.get(dataId);
            if (CollectionUtils.isNotEmpty(configChangeListeners)) {
                configChangeListeners.remove(listener);
            }
        }
    }

    private void updateConfigVersion(String namespace, String dataId) {
        Long version = getConfigVersion(namespace, dataId);
        if (version == null) {
            version = 0L;
        }
        putConfigVersion(namespace, dataId, version + 1);
    }


    private void notifyConfigChange(String namespace, String dataId, ConfigurationChangeEvent event) {
        Map<String, Set<ConfigurationChangeListener>> listenerMap = CONFIG_LISTENERS_MAP.get(namespace);
        if (CollectionUtils.isNotEmpty(listenerMap)) {
            Set<ConfigurationChangeListener> configChangeListeners = listenerMap.get(dataId);
            if (CollectionUtils.isNotEmpty(configChangeListeners)) {
                configChangeListeners.forEach(listener -> listener.onChangeEvent(event));
            }
        }
    }
}
