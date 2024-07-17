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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.rocksdb.Options;

import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;


/**
 * The RocksDB options builder
 *
 */
public class RocksDBOptions {
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    public static final String ROCKSDB_SUFFIX = "rocksdb";

    private static volatile Options options = null;
    public static Options getOptions() {
        if (options == null){
            synchronized (RocksDBOptions.class){
                if (options == null){
                    options = buildOptions();
                }
            }
        }
        return options;
    }

    public static String getDBPath() {
        String dir = FILE_CONFIG.getConfig(CONFIG_STORE_DIR);
        String group = FILE_CONFIG.getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
        return String.join(separator, dir, group, ROCKSDB_SUFFIX);
    }

    public static boolean getDBDestroyOnShutdown() {
        return FILE_CONFIG.getBoolean(CONFIG_STORE_DESTROY_ON_SHUTDOWN, false);
    }

    private static Options buildOptions() {
        Options options = new Options();
        // If the database does not exist, create it
        options.setCreateIfMissing(true);
        // Retain only the latest log file
        options.setKeepLogFileNum(1);
        // Disable log file rolling based on time
        options.setLogFileTimeToRoll(0);
        // Disable log file rolling based on size
        options.setMaxLogFileSize(0);
        return options;
    }

}
