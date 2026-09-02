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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class RocksDBStoreConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void testDefaultsKeepRocksDBOptionsUnset() {
        Map<String, String> values = new HashMap<>();
        values.put(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("default").toString());

        RocksDBStoreConfig config = RocksDBStoreConfig.fromConfiguration(configuration(values), false);

        Assertions.assertEquals(tempDir.resolve("default").toString(), config.getDbPath());
        Assertions.assertFalse(config.isSyncWrite());
        Assertions.assertEquals(0L, config.getBlockCacheSize());
        Assertions.assertEquals(0L, config.getWriteBufferSize());
        Assertions.assertEquals(0L, config.getDbWriteBufferSize());
        for (RocksDBColumnFamily columnFamily : RocksDBColumnFamily.values()) {
            Assertions.assertEquals(0L, config.getWriteBufferSize(columnFamily));
        }
        Assertions.assertEquals(0, config.getMaxWriteBufferNumber());
        Assertions.assertEquals(0, config.getMinWriteBufferNumberToMerge());
        Assertions.assertEquals(0, config.getMaxBackgroundJobs());
        Assertions.assertEquals(0, config.getMaxOpenFiles());
        Assertions.assertEquals(0L, config.getTargetFileSizeBase());
        Assertions.assertEquals(0, config.getLevel0FileNumCompactionTrigger());
        Assertions.assertEquals(0, config.getLevel0SlowdownWritesTrigger());
        Assertions.assertEquals(0, config.getLevel0StopWritesTrigger());
        Assertions.assertFalse(config.isEnableStatistics());
        Assertions.assertFalse(config.isOptimizeFiltersForHits());
        Assertions.assertNull(config.getCompressionType());
        Assertions.assertTrue(
                config.isEnableRangeDelete(), "enableRangeDelete defaults to true since the Direction C optimization");
        Assertions.assertFalse(config.isRangeDeleteCompactAfterDelete());
        Assertions.assertEquals(RocksDBWalSyncMode.NONE, config.getWalSyncMode());
        Assertions.assertEquals(RocksDBStoreConfig.DEFAULT_WAL_SYNC_INTERVAL_MILLIS, config.getWalSyncIntervalMillis());
        Assertions.assertEquals(RocksDBStoreConfig.DEFAULT_WAL_SYNC_WRITE_THRESHOLD, config.getWalSyncWriteThreshold());
        Assertions.assertTrue(config.isWalSyncOnShutdown());
        Assertions.assertEquals(
                RocksDBStoreConfig.DEFAULT_WAL_SYNC_SHUTDOWN_TIMEOUT_MILLIS, config.getWalSyncShutdownTimeoutMillis());
        Assertions.assertEquals(
                RocksDBStoreConfig.DEFAULT_WAL_SYNC_WARN_THRESHOLD_MILLIS, config.getWalSyncWarnThresholdMillis());
        Assertions.assertFalse(config.isPeriodicWalSyncEnabled());
    }

    @Test
    void testReadsTunableOptionsFromConfiguration() {
        Map<String, String> values = new HashMap<>();
        values.put(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("configured").toString());
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_BLOCK_CACHE_SIZE, "64KB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE, "2MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_DB_WRITE_BUFFER_SIZE, "32MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_TOTAL_WAL_SIZE, "1GB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_GLOBAL_WRITE_BUFFER_SIZE, "8MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_BRANCH_WRITE_BUFFER_SIZE, "4MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_LOCK_WRITE_BUFFER_SIZE, "1MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_INDEX_WRITE_BUFFER_SIZE, "512KB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_METADATA_WRITE_BUFFER_SIZE, "64KB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_WRITE_BUFFER_NUMBER, "3");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_MIN_WRITE_BUFFER_NUMBER_TO_MERGE, "2");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_BACKGROUND_JOBS, "4");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_OPEN_FILES, "128");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_TARGET_FILE_SIZE_BASE, "1GB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_FILE_NUM_COMPACTION_TRIGGER, "8");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_SLOWDOWN_WRITES_TRIGGER, "16");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_STOP_WRITES_TRIGGER, "24");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_ENABLE_STATISTICS, "true");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_OPTIMIZE_FILTERS_FOR_HITS, "true");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_COMPRESSION_TYPE, "no");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_ENABLE_RANGE_DELETE, "true");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_RANGE_DELETE_COMPACT_AFTER_DELETE, "true");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_MODE, "periodic");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_INTERVAL_MILLIS, "500");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_WRITE_THRESHOLD, "5000");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_ON_SHUTDOWN, "false");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_SHUTDOWN_TIMEOUT_MILLIS, "750");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_WARN_THRESHOLD_MILLIS, "200");

        RocksDBStoreConfig config = RocksDBStoreConfig.fromConfiguration(configuration(values), false);

        Assertions.assertFalse(config.isSyncWrite());
        Assertions.assertEquals(64L * 1024L, config.getBlockCacheSize());
        Assertions.assertEquals(2L * 1024L * 1024L, config.getWriteBufferSize());
        Assertions.assertEquals(32L * 1024L * 1024L, config.getDbWriteBufferSize());
        Assertions.assertEquals(1024L * 1024L * 1024L, config.getMaxTotalWalSize());
        Assertions.assertEquals(8L * 1024L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.GLOBAL_SESSION));
        Assertions.assertEquals(4L * 1024L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.BRANCH_SESSION));
        Assertions.assertEquals(1L * 1024L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.LOCK));
        Assertions.assertEquals(512L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.GLOBAL_STATUS_INDEX));
        Assertions.assertEquals(512L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.LOCK_BRANCH_INDEX));
        Assertions.assertEquals(64L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.METADATA));
        Assertions.assertEquals(3, config.getMaxWriteBufferNumber());
        Assertions.assertEquals(2, config.getMinWriteBufferNumberToMerge());
        Assertions.assertEquals(4, config.getMaxBackgroundJobs());
        Assertions.assertEquals(128, config.getMaxOpenFiles());
        Assertions.assertEquals(1024L * 1024L * 1024L, config.getTargetFileSizeBase());
        Assertions.assertEquals(8, config.getLevel0FileNumCompactionTrigger());
        Assertions.assertEquals(16, config.getLevel0SlowdownWritesTrigger());
        Assertions.assertEquals(24, config.getLevel0StopWritesTrigger());
        Assertions.assertTrue(config.isEnableStatistics());
        Assertions.assertTrue(config.isOptimizeFiltersForHits());
        Assertions.assertEquals("no", config.getCompressionType());
        Assertions.assertTrue(config.isEnableRangeDelete());
        Assertions.assertTrue(config.isRangeDeleteCompactAfterDelete());
        Assertions.assertEquals(RocksDBWalSyncMode.PERIODIC, config.getWalSyncMode());
        Assertions.assertEquals(500, config.getWalSyncIntervalMillis());
        Assertions.assertEquals(5000L, config.getWalSyncWriteThreshold());
        Assertions.assertFalse(config.isWalSyncOnShutdown());
        Assertions.assertEquals(750, config.getWalSyncShutdownTimeoutMillis());
        Assertions.assertEquals(200, config.getWalSyncWarnThresholdMillis());
        Assertions.assertTrue(config.isPeriodicWalSyncEnabled());
    }

    @Test
    void testPeriodicWalSyncIgnoredBySyncWriteEffectiveMode() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_MODE, "periodic");

        RocksDBStoreConfig config = RocksDBStoreConfig.fromConfiguration(configuration(values), true);

        Assertions.assertTrue(config.isSyncWrite());
        Assertions.assertEquals(RocksDBWalSyncMode.PERIODIC, config.getWalSyncMode());
        Assertions.assertFalse(config.isPeriodicWalSyncEnabled());
    }

    @Test
    void testRejectsNegativeOption() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_BLOCK_CACHE_SIZE, "-1");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));
    }

    @Test
    void testRejectsInvalidSizeOption() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE, "invalid");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));
    }

    @Test
    void testColumnFamilyWriteBufferFallsBackToSharedSize() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE, "2MB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_LOCK_WRITE_BUFFER_SIZE, "256KB");

        RocksDBStoreConfig config = RocksDBStoreConfig.fromConfiguration(configuration(values), false);

        Assertions.assertEquals(2L * 1024L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.GLOBAL_SESSION));
        Assertions.assertEquals(256L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.LOCK));
        Assertions.assertEquals(2L * 1024L * 1024L, config.getWriteBufferSize(RocksDBColumnFamily.GLOBAL_STATUS_INDEX));
    }

    @Test
    void testRejectsNegativeColumnFamilyWriteBufferSize() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_INDEX_WRITE_BUFFER_SIZE, "-1");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));
    }

    @Test
    void testRejectsInvalidWalSyncMode() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_MODE, "invalid");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));
    }

    @Test
    void testRejectsInvalidWalSyncThresholds() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_MODE, "periodic");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_INTERVAL_MILLIS, "0");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));

        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_INTERVAL_MILLIS, "1");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_WRITE_THRESHOLD, "0");

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));
    }

    @Test
    void testRejectsNonPositiveWalSyncShutdownTimeout() {
        Map<String, String> values = new HashMap<>();
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_SHUTDOWN_TIMEOUT_MILLIS, "0");

        StoreException exception = Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreConfig.fromConfiguration(configuration(values), false));

        Assertions.assertTrue(
                exception.getMessage().contains(ConfigurationKeys.STORE_FILE_ROCKSDB_WAL_SYNC_SHUTDOWN_TIMEOUT_MILLIS));
    }

    private Configuration configuration(Map<String, String> values) {
        Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getConfig(Mockito.anyString()))
                .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        Mockito.when(configuration.getConfig(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    return value == null ? invocation.getArgument(1) : value;
                });
        Mockito.when(configuration.getInt(Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    return value == null ? invocation.getArgument(1) : Integer.parseInt(value);
                });
        Mockito.when(configuration.getLong(Mockito.anyString(), Mockito.anyLong()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    return value == null ? invocation.getArgument(1) : Long.parseLong(value);
                });
        Mockito.when(configuration.getBoolean(Mockito.anyString(), Mockito.anyBoolean()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    return value == null ? invocation.getArgument(1) : Boolean.parseBoolean(value);
                });
        return configuration;
    }
}
