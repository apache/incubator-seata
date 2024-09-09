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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.util.SizeUnit;

import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_DESTROY_ON_SHUTDOWN;
import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_DIR;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;


/**
 * The RocksDB options builder
 *
 */
public class RocksDBOptionsFactory {
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    public static final String ROCKSDB_SUFFIX = "rocksdb";
    private static volatile DBOptions options = null;
    private static final Map<String/*namespace*/, ColumnFamilyOptions> COLUMN_FAMILY_OPTIONS_MAP = new ConcurrentHashMap<>();
    public static DBOptions getDBOptions() {
        if (options == null) {
            synchronized (RocksDBOptionsFactory.class) {
                if (options == null) {
                    options = buildDBOptions();
                }
            }
        }
        return options;
    }

    public static ColumnFamilyOptions getColumnFamilyOptionsMap(final String namespace) {
        ColumnFamilyOptions opts = COLUMN_FAMILY_OPTIONS_MAP.get(namespace);
        if (opts == null) {
            final ColumnFamilyOptions newOpts = buildColumnFamilyOptions();
            opts = COLUMN_FAMILY_OPTIONS_MAP.putIfAbsent(namespace, newOpts);
            if (opts != null) {
                newOpts.close();
            } else {
                opts = newOpts;
            }
        }
        return opts;
    }
    public static String getDBPath() {
        String dir = FILE_CONFIG.getConfig(CONFIG_STORE_DIR);
        String group = FILE_CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
        return String.join(separator, dir, group, ROCKSDB_SUFFIX);
    }

    public static boolean getDBDestroyOnShutdown() {
        return FILE_CONFIG.getBoolean(CONFIG_STORE_DESTROY_ON_SHUTDOWN, false);
    }

    private static DBOptions buildDBOptions() {
        final DBOptions options = new DBOptions();
        // If the database does not exist, create it
        options.setCreateIfMissing(true);
        // If true, missing column families will be automatically created.
        options.setCreateMissingColumnFamilies(true);
        // Retain only the latest log file
        options.setKeepLogFileNum(1);
        // Disable log file rolling based on time
        options.setLogFileTimeToRoll(0);
        // Disable log file rolling based on size
        options.setMaxLogFileSize(0);
        // Number of open files that can be used by the DB.
        options.setMaxOpenFiles(-1);
        return options;
    }

    private static ColumnFamilyOptions buildColumnFamilyOptions() {
        ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();
        // set little write buffer size, since the size of config file is small
        columnFamilyOptions.setWriteBufferSize(4 * SizeUnit.MB);
        // Set memtable prefix bloom filter to reduce memory usage.
        columnFamilyOptions.setMemtablePrefixBloomSizeRatio(0.125);
        // Set compression type
        columnFamilyOptions.setCompressionType(CompressionType.LZ4_COMPRESSION);
        // Set compaction style
        columnFamilyOptions.setCompactionStyle(CompactionStyle.LEVEL);
        // Optimize level style compaction
        columnFamilyOptions.optimizeLevelStyleCompaction();
        return columnFamilyOptions;
    }

    public static void releaseAllOptions() {
        // close all options
        if (options != null) {
            options.close();
        }
        for (final ColumnFamilyOptions opts : COLUMN_FAMILY_OPTIONS_MAP.values()) {
            if (opts != null) {
                opts.close();
            }
        }
        // help gc
        options = null;
        COLUMN_FAMILY_OPTIONS_MAP.clear();
    }

}
