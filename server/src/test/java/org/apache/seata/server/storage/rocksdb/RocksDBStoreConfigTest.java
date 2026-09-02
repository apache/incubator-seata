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
        Assertions.assertFalse(config.isEnableRangeDelete());
        Assertions.assertFalse(config.isRangeDeleteCompactAfterDelete());
    }

    @Test
    void testReadsTunableOptionsFromConfiguration() {
        Map<String, String> values = new HashMap<>();
        values.put(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("configured").toString());
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_BLOCK_CACHE_SIZE, "64KB");
        values.put(ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE, "2MB");
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

        RocksDBStoreConfig config = RocksDBStoreConfig.fromConfiguration(configuration(values), true);

        Assertions.assertTrue(config.isSyncWrite());
        Assertions.assertEquals(64L * 1024L, config.getBlockCacheSize());
        Assertions.assertEquals(2L * 1024L * 1024L, config.getWriteBufferSize());
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
        Mockito.when(configuration.getBoolean(Mockito.anyString(), Mockito.anyBoolean()))
                .thenAnswer(invocation -> {
                    String value = values.get(invocation.getArgument(0));
                    return value == null ? invocation.getArgument(1) : Boolean.parseBoolean(value);
                });
        return configuration;
    }
}
