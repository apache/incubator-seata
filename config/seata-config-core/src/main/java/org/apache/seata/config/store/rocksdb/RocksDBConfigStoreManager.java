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
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.*;
import org.apache.seata.config.store.AbstractConfigStoreManager;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.Constants.DEFAULT_STORE_GROUP;


/**
 * The RocksDB config store manager
 *
 */
public class RocksDBConfigStoreManager extends AbstractConfigStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBConfigStoreManager.class);
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String DB_PATH = RocksDBOptions.getDBPath();
    private static final String DEFAULT_GROUP = DEFAULT_STORE_GROUP;
    private static String CURRENT_GROUP;
    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final Options DB_OPTIONS = RocksDBOptions.getOptions();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String, Set<ConfigurationChangeListener>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>(
            MAP_INITIAL_CAPACITY);

    //====================================NON COMMON FILED===================================
    private static volatile RocksDBConfigStoreManager instance;
    private final RocksDB rocksdb;
    private static final List<String> prefixList = Arrays.asList(FILE_ROOT_PREFIX_CONFIG, FILE_ROOT_PREFIX_REGISTRY, SERVER_PREFIX,
            STORE_PREFIX, METRICS_PREFIX, TRANSPORT_PREFIX);

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
        this.rocksdb = RocksDBFactory.getInstance(DB_PATH, DB_OPTIONS);
        CURRENT_GROUP = FILE_CONFIG.getConfig(CONFIG_STORE_GROUP, DEFAULT_GROUP);
        maybeNeedLoadOriginConfig();
        LOGGER.info("RocksDBConfigStoreManager initialized successfully");
    }

    /**
     * load origin config if first startup
     */
    private void maybeNeedLoadOriginConfig() {
        if (isEmpty(CURRENT_GROUP)){
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
                // filter all seata related configs
                if (prefixList.stream().anyMatch(k::startsWith)) {
                    seataConfigs.put(k, v);
                }
            });
            putAll(CURRENT_GROUP, seataConfigs);
        }
    }


    private Map<String, Object> getConfigMap(String group) throws RocksDBException{
        lock.readLock().lock();
        try {
            group = StringUtils.isEmpty(group)? CURRENT_GROUP : group;
            byte[] value = rocksdb.get(group.getBytes(DEFAULT_CHARSET));
            String configStr = value != null ? new String(value, DEFAULT_CHARSET) : null;
            return convertConfigStr2Map(configStr);
        }finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public String get(String group, String key) {
        lock.readLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(group);
            return configMap.get(key) != null ? configMap.get(key).toString() : null;
        }catch (RocksDBException e) {
            LOGGER.error("Failed to get value for key: " + key, e);
        }finally {
            lock.readLock().unlock();
        }
        return null;
    }

    @Override
    public Map<String, Object> getAll(String group) {
        try {
            return getConfigMap(group);
        }catch (RocksDBException e) {
            LOGGER.error("Failed to get all configs", e);
        }
        return null;
    }

    @Override
    public Boolean put(String group, String key, Object value) {
        lock.writeLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(group);
            configMap.put(key, value);
            String configStr = convertConfig2Str(configMap);
            rocksdb.put(group.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            notifyConfigChange(group, new ConfigurationChangeEvent(group, configStr));
            // LOGGER.info("put {} = {} in group: {}", key, value, group);
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to put value for key: " + key, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Boolean delete(String group, String key) {
        lock.writeLock().lock();
        try {
            Map<String, Object> configMap = getConfigMap(group);
            configMap.remove(key);
            String configStr = convertConfig2Str(configMap);
            rocksdb.put(group.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            notifyConfigChange(group, new ConfigurationChangeEvent(group, configStr));
            // LOGGER.info("delete {} in group: {}", key, group);
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to delete value for key: " + key, e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Boolean putAll(String group, Map<String, Object> configMap) {
        lock.writeLock().lock();
        try{
            String configStr = convertConfig2Str(configMap);
            rocksdb.put(group.getBytes(DEFAULT_CHARSET), configStr.getBytes(DEFAULT_CHARSET));
            notifyConfigChange(group, new ConfigurationChangeEvent(group, configStr));
            return true;
        }catch (RocksDBException e){
            LOGGER.error("Failed to put all configs", e);
        }finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Boolean deleteAll(String group) {
        lock.writeLock().lock();
        try {
            rocksdb.delete(group.getBytes(DEFAULT_CHARSET));
            notifyConfigChange(group, new ConfigurationChangeEvent(group, null));
            return true;
        } catch (RocksDBException e) {
            LOGGER.error("Failed to clear all configs", e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Map<String, Object> getConfigMap() {
        lock.readLock().lock();
        HashMap<String, Object> configMap = new HashMap<>();
        try (RocksIterator iterator = rocksdb.newIterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                String key = new String(iterator.key(), DEFAULT_CHARSET);
                String value = new String(iterator.value(), DEFAULT_CHARSET);
                configMap.put(key, value);
            }
            return configMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Boolean putConfigMap(Map<String, Object> configMap) {
        lock.writeLock().lock();
        try (WriteBatch batch = new WriteBatch(); WriteOptions writeOptions = new WriteOptions()) {
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                batch.put(entry.getKey().getBytes(DEFAULT_CHARSET), entry.getValue().toString().getBytes(DEFAULT_CHARSET));
            }
            rocksdb.write(writeOptions, batch);
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String group = entry.getKey();
                String configStr = (String) entry.getValue();
                notifyConfigChange(group, new ConfigurationChangeEvent(group, configStr));
            }
            return true;
        } catch (RocksDBException e) {
            LOGGER.error("Failed to put values for multiple keys", e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Boolean clearData() {
        lock.writeLock().lock();
        try (WriteBatch batch = new WriteBatch(); WriteOptions writeOptions = new WriteOptions();
             RocksIterator iterator = rocksdb.newIterator()) {
            Set<String> groupSet = new HashSet<>();
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                batch.delete(iterator.key());
                groupSet.add(new String(iterator.key(), DEFAULT_CHARSET));
            }
            rocksdb.write(writeOptions, batch);
            for (String group : groupSet) {
                notifyConfigChange(group, new ConfigurationChangeEvent(group, null));
            }
            return true;
        } catch (RocksDBException e) {
            LOGGER.error("Failed to clear the database", e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public Boolean isEmpty(String group) {
        return CollectionUtils.isEmpty(getAll(group));
    }

    // todo server关闭时需要被调用
    @Override
    public void shutdown() {
        RocksDBFactory.close();
        if (RocksDBOptions.getDBDestroyOnShutdown()) {
            destroy();
        }
        LOGGER.info("RocksDBConfigStoreManager has shutdown");
    }

    @Override
    public void destroy() {
        RocksDBFactory.destroy(DB_PATH, DB_OPTIONS);
        LOGGER.info("DB destroyed, the db path is: {}.", DB_PATH);
    }


    @Override
    public void addConfigListener(String group, String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, k -> ConcurrentHashMap.newKeySet())
                .add(listener);
    }

    @Override
    public void removeConfigListener(String group, String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configChangeListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configChangeListeners)) {
            configChangeListeners.remove(listener);
        }
    }


    private void notifyConfigChange(String dataId, ConfigurationChangeEvent event) {
        Set<ConfigurationChangeListener> configChangeListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configChangeListeners)) {
            configChangeListeners.forEach(listener -> listener.onChangeEvent(event));
        }
    }
}
