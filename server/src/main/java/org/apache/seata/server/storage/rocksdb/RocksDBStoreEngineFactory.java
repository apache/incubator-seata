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
package org.apache.seata.server.storage.rocksdb;

import org.apache.seata.common.exception.StoreException;

/**
 * Shared RocksDB store engine factory.
 */
public final class RocksDBStoreEngineFactory {

    private static volatile RocksDBStoreEngine ENGINE;

    private RocksDBStoreEngineFactory() {}

    public static RocksDBStoreEngine getInstance() {
        return getInstance(RocksDBStoreConfig.fromConfiguration());
    }

    public static RocksDBStoreEngine getInstance(RocksDBStoreConfig config) {
        if (ENGINE == null) {
            synchronized (RocksDBStoreEngineFactory.class) {
                if (ENGINE == null) {
                    ENGINE = RocksDBStoreEngine.open(config);
                }
            }
        }
        if (!ENGINE.getConfig().getDbPath().equals(config.getDbPath())) {
            throw new StoreException("RocksDB file store engine already opened with path:"
                    + ENGINE.getConfig().getDbPath() + ", requested path:" + config.getDbPath());
        }
        if (ENGINE.getConfig().isSyncWrite() != config.isSyncWrite()) {
            throw new StoreException("RocksDB file store engine already opened with syncWrite:"
                    + ENGINE.getConfig().isSyncWrite() + ", requested syncWrite:" + config.isSyncWrite());
        }
        if (!ENGINE.getConfig().equals(config)) {
            throw new StoreException("RocksDB file store engine already opened with options:"
                    + ENGINE.getConfig().tuningSummary() + ", requested options:" + config.tuningSummary());
        }
        return ENGINE;
    }

    public static void destroy() {
        synchronized (RocksDBStoreEngineFactory.class) {
            if (ENGINE != null) {
                ENGINE.close();
                ENGINE = null;
            }
        }
    }
}
