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
package org.apache.seata.server.cluster.raft.snapshot.config;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.store.ConfigStoreManager;
import org.apache.seata.config.store.ConfigStoreManagerProvider;
import org.apache.seata.config.store.rocksdb.RocksDBConfigStoreManager;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_TYPE;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_TYPE;


public class ConfigSnapshotFile implements Serializable, StoreSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSnapshotFile.class);

    private static final long serialVersionUID = 1452307567830545914L;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private static ConfigStoreManager configStoreManager;
    String group;

    String fileName = "config";

    public ConfigSnapshotFile(String group) {
        this.group = group;
        String dbType = FILE_CONFIG.getConfig(CONFIG_STORE_TYPE, DEFAULT_DB_TYPE);
        configStoreManager = EnhancedServiceLoader.load(ConfigStoreManagerProvider.class, Objects.requireNonNull(dbType), false).provide();
    }

    @Override
    public Status save(SnapshotWriter writer) {
        Map<String, Map<String, Object>> configMap = configStoreManager.getConfigMap();
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(configMap);
        raftSnapshot.setType(RaftSnapshot.SnapshotType.config);
        LOGGER.info("groupId: {}, config size: {}", group, configMap.size());
        String path = new StringBuilder(writer.getPath()).append(File.separator).append(fileName).toString();
        try {
            if (save(raftSnapshot, path)) {
                if (writer.addFile(fileName)) {
                    return Status.OK();
                } else {
                    return new Status(RaftError.EIO, "Fail to add file to writer");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Fail to save groupId: {} snapshot {}", group, path, e);
        }
        return new Status(RaftError.EIO, "Fail to save groupId: " + group + " snapshot %s", path);
    }

    @Override
    public boolean load(SnapshotReader reader) {
        if (reader.getFileMeta(fileName) == null) {
            LOGGER.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        String path = new StringBuilder(reader.getPath()).append(File.separator).append(fileName).toString();
        try {
            LOGGER.info("on snapshot load start index: {}", reader.load().getLastIncludedIndex());
            Map<String, Map<String, Object>> configMap = (Map<String, Map<String, Object>>)load(path);
            ConfigStoreManager configStoreManager = RocksDBConfigStoreManager.getInstance();
            configStoreManager.clearData();
            configStoreManager.putConfigMap(configMap);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("on snapshot load end index: {}", reader.load().getLastIncludedIndex());
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("fail to load snapshot from {}", path, e);
            return false;
        }
    }
}
