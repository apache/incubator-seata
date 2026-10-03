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

import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.IdConstants;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.server.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * RocksDB file store metrics binder.
 */
final class RocksDBStoreMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBStoreMetrics.class);
    private static final String METRIC_NAME = "seata.store.rocksdb";
    private static final String STORE_KEY = "store";
    private static final String STORE_VALUE_FILE = "file";
    private static final String METRIC_KEY = "metric";

    private static volatile RocksDBStoreEngine engine;

    private RocksDBStoreMetrics() {}

    static void tryRegister(RocksDBStoreEngine engine) {
        Registry registry = MetricsManager.get().getRegistry();
        if (registry == null) {
            return;
        }
        register(registry, engine);
    }

    static void register(Registry registry, RocksDBStoreEngine engine) {
        if (registry == null || engine == null) {
            return;
        }
        RocksDBStoreMetrics.engine = engine;
        try {
            gauge(registry, "estimateLiveDataSizeBytes", () -> diagnostics()
                    .getProperty(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE));
            gauge(registry, "totalSstFilesSizeBytes", () -> diagnostics()
                    .getProperty(RocksDBStoreDiagnostics.TOTAL_SST_FILES_SIZE));
            gauge(registry, "pendingCompactionBytes", () -> diagnostics()
                    .getProperty(RocksDBStoreDiagnostics.ESTIMATE_PENDING_COMPACTION_BYTES));
            gauge(registry, "activeMemTableBytes", () -> diagnostics()
                    .getProperty(RocksDBStoreDiagnostics.CUR_SIZE_ACTIVE_MEM_TABLE));
            gauge(registry, "allMemTablesBytes", () -> diagnostics()
                    .getProperty(RocksDBStoreDiagnostics.CUR_SIZE_ALL_MEM_TABLES));
            gauge(registry, "liveVersions", () -> diagnostics().getProperty(RocksDBStoreDiagnostics.NUM_LIVE_VERSIONS));
            gauge(registry, "globalSessionEstimateKeys", () -> diagnostics()
                    .getColumnFamilyProperty(
                            RocksDBColumnFamily.GLOBAL_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS));
            gauge(registry, "branchSessionEstimateKeys", () -> diagnostics()
                    .getColumnFamilyProperty(
                            RocksDBColumnFamily.BRANCH_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS));
            gauge(registry, "lockEstimateKeys", () -> diagnostics()
                    .getColumnFamilyProperty(RocksDBColumnFamily.LOCK, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS));
            gauge(registry, "blockCacheUsageBytes", () -> {
                RocksDBStoreEngine current = engine;
                return current == null ? 0L : current.getBlockCacheUsage();
            });
            gauge(registry, "blockCachePinnedUsageBytes", () -> {
                RocksDBStoreEngine current = engine;
                return current == null ? 0L : current.getBlockCachePinnedUsage();
            });
            gauge(registry, "blockCacheCapacityBytes", () -> {
                RocksDBStoreEngine current = engine;
                return current == null ? 0L : current.getBlockCacheCapacity();
            });
        } catch (Exception e) {
            LOGGER.warn("register RocksDB file store metrics failed", e);
        }
    }

    static void unregister(RocksDBStoreEngine engine) {
        if (engine == null || RocksDBStoreMetrics.engine == engine) {
            RocksDBStoreMetrics.engine = null;
        }
    }

    private static void gauge(Registry registry, String metric, Supplier<Long> supplier) {
        registry.getGauge(id(metric), () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                LOGGER.debug("collect RocksDB file store metric failed, metric:{}", metric, e);
                return 0L;
            }
        });
    }

    private static RocksDBStoreDiagnostics diagnostics() {
        RocksDBStoreEngine current = engine;
        return current == null ? RocksDBStoreEngine.closedDiagnostics() : current.diagnostics();
    }

    private static Id id(String metric) {
        return new Id(METRIC_NAME)
                .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC)
                .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_GAUGE)
                .withTag(STORE_KEY, STORE_VALUE_FILE)
                .withTag(METRIC_KEY, metric);
    }
}
