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
import org.apache.seata.common.XID;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.storage.file.FlushDiskMode;
import org.apache.seata.server.store.StoreConfig;

import java.util.Locale;
import java.util.Objects;

import static java.io.File.separator;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * RocksDB store configuration.
 */
public class RocksDBStoreConfig {

    private static final long DEFAULT_LONG_OPTION = 0L;
    private static final int DEFAULT_INT_OPTION = 0;

    private final String dbPath;
    private final boolean syncWrite;
    private final long blockCacheSize;
    private final long writeBufferSize;
    private final int maxWriteBufferNumber;
    private final int minWriteBufferNumberToMerge;
    private final int maxBackgroundJobs;
    private final int maxOpenFiles;
    private final long targetFileSizeBase;
    private final int level0FileNumCompactionTrigger;
    private final int level0SlowdownWritesTrigger;
    private final int level0StopWritesTrigger;
    private final boolean enableStatistics;
    private final boolean optimizeFiltersForHits;
    private final String compressionType;
    private final boolean enableRangeDelete;
    private final boolean rangeDeleteCompactAfterDelete;

    public RocksDBStoreConfig(String dbPath, boolean syncWrite) {
        this(dbPath, syncWrite, false);
    }

    public RocksDBStoreConfig(String dbPath, boolean syncWrite, boolean enableRangeDelete) {
        this(
                dbPath,
                syncWrite,
                DEFAULT_LONG_OPTION,
                DEFAULT_LONG_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_LONG_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_INT_OPTION,
                DEFAULT_INT_OPTION,
                false,
                false,
                null,
                enableRangeDelete,
                false);
    }

    public RocksDBStoreConfig(
            String dbPath,
            boolean syncWrite,
            long blockCacheSize,
            long writeBufferSize,
            int maxWriteBufferNumber,
            int minWriteBufferNumberToMerge,
            int maxBackgroundJobs,
            int maxOpenFiles,
            long targetFileSizeBase,
            int level0FileNumCompactionTrigger,
            int level0SlowdownWritesTrigger,
            int level0StopWritesTrigger,
            boolean enableStatistics,
            boolean optimizeFiltersForHits,
            String compressionType) {
        this(
                dbPath,
                syncWrite,
                blockCacheSize,
                writeBufferSize,
                maxWriteBufferNumber,
                minWriteBufferNumberToMerge,
                maxBackgroundJobs,
                maxOpenFiles,
                targetFileSizeBase,
                level0FileNumCompactionTrigger,
                level0SlowdownWritesTrigger,
                level0StopWritesTrigger,
                enableStatistics,
                optimizeFiltersForHits,
                compressionType,
                false,
                false);
    }

    public RocksDBStoreConfig(
            String dbPath,
            boolean syncWrite,
            long blockCacheSize,
            long writeBufferSize,
            int maxWriteBufferNumber,
            int minWriteBufferNumberToMerge,
            int maxBackgroundJobs,
            int maxOpenFiles,
            long targetFileSizeBase,
            int level0FileNumCompactionTrigger,
            int level0SlowdownWritesTrigger,
            int level0StopWritesTrigger,
            boolean enableStatistics,
            boolean optimizeFiltersForHits,
            String compressionType,
            boolean enableRangeDelete) {
        this(
                dbPath,
                syncWrite,
                blockCacheSize,
                writeBufferSize,
                maxWriteBufferNumber,
                minWriteBufferNumberToMerge,
                maxBackgroundJobs,
                maxOpenFiles,
                targetFileSizeBase,
                level0FileNumCompactionTrigger,
                level0SlowdownWritesTrigger,
                level0StopWritesTrigger,
                enableStatistics,
                optimizeFiltersForHits,
                compressionType,
                enableRangeDelete,
                false);
    }

    public RocksDBStoreConfig(
            String dbPath,
            boolean syncWrite,
            long blockCacheSize,
            long writeBufferSize,
            int maxWriteBufferNumber,
            int minWriteBufferNumberToMerge,
            int maxBackgroundJobs,
            int maxOpenFiles,
            long targetFileSizeBase,
            int level0FileNumCompactionTrigger,
            int level0SlowdownWritesTrigger,
            int level0StopWritesTrigger,
            boolean enableStatistics,
            boolean optimizeFiltersForHits,
            String compressionType,
            boolean enableRangeDelete,
            boolean rangeDeleteCompactAfterDelete) {
        this.dbPath = dbPath;
        this.syncWrite = syncWrite;
        this.blockCacheSize = nonNegative(blockCacheSize, ConfigurationKeys.STORE_FILE_ROCKSDB_BLOCK_CACHE_SIZE);
        this.writeBufferSize = nonNegative(writeBufferSize, ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE);
        this.maxWriteBufferNumber =
                nonNegative(maxWriteBufferNumber, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_WRITE_BUFFER_NUMBER);
        this.minWriteBufferNumberToMerge = nonNegative(
                minWriteBufferNumberToMerge, ConfigurationKeys.STORE_FILE_ROCKSDB_MIN_WRITE_BUFFER_NUMBER_TO_MERGE);
        this.maxBackgroundJobs =
                nonNegative(maxBackgroundJobs, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_BACKGROUND_JOBS);
        this.maxOpenFiles = nonNegative(maxOpenFiles, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_OPEN_FILES);
        this.targetFileSizeBase =
                nonNegative(targetFileSizeBase, ConfigurationKeys.STORE_FILE_ROCKSDB_TARGET_FILE_SIZE_BASE);
        this.level0FileNumCompactionTrigger = nonNegative(
                level0FileNumCompactionTrigger,
                ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_FILE_NUM_COMPACTION_TRIGGER);
        this.level0SlowdownWritesTrigger = nonNegative(
                level0SlowdownWritesTrigger, ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_SLOWDOWN_WRITES_TRIGGER);
        this.level0StopWritesTrigger =
                nonNegative(level0StopWritesTrigger, ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_STOP_WRITES_TRIGGER);
        this.enableStatistics = enableStatistics;
        this.optimizeFiltersForHits = optimizeFiltersForHits;
        this.compressionType = StringUtils.isBlank(compressionType) ? null : compressionType.trim();
        this.enableRangeDelete = enableRangeDelete;
        this.rangeDeleteCompactAfterDelete = rangeDeleteCompactAfterDelete;
    }

    public static RocksDBStoreConfig fromConfiguration() {
        Configuration config = ConfigurationFactory.getInstance();
        return fromConfiguration(config, StoreConfig.getFlushDiskMode() == FlushDiskMode.SYNC_MODEL);
    }

    static RocksDBStoreConfig fromConfiguration(Configuration config, boolean syncWrite) {
        String configuredDir = config.getConfig(ConfigurationKeys.STORE_FILE_ROCKSDB_DIR);
        String dbPath = configuredDir;
        if (StringUtils.isBlank(dbPath)) {
            dbPath = config.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
                    + separator
                    + XID.getPort()
                    + separator
                    + "rocksdb";
        }
        return new RocksDBStoreConfig(
                dbPath,
                syncWrite,
                sizeOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_BLOCK_CACHE_SIZE),
                sizeOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_WRITE_BUFFER_SIZE),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_WRITE_BUFFER_NUMBER),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_MIN_WRITE_BUFFER_NUMBER_TO_MERGE),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_BACKGROUND_JOBS),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_MAX_OPEN_FILES),
                sizeOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_TARGET_FILE_SIZE_BASE),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_FILE_NUM_COMPACTION_TRIGGER),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_SLOWDOWN_WRITES_TRIGGER),
                intOption(config, ConfigurationKeys.STORE_FILE_ROCKSDB_LEVEL0_STOP_WRITES_TRIGGER),
                config.getBoolean(ConfigurationKeys.STORE_FILE_ROCKSDB_ENABLE_STATISTICS, false),
                config.getBoolean(ConfigurationKeys.STORE_FILE_ROCKSDB_OPTIMIZE_FILTERS_FOR_HITS, false),
                config.getConfig(ConfigurationKeys.STORE_FILE_ROCKSDB_COMPRESSION_TYPE),
                config.getBoolean(ConfigurationKeys.STORE_FILE_ROCKSDB_ENABLE_RANGE_DELETE, false),
                config.getBoolean(ConfigurationKeys.STORE_FILE_ROCKSDB_RANGE_DELETE_COMPACT_AFTER_DELETE, false));
    }

    public String getDbPath() {
        return dbPath;
    }

    public boolean isSyncWrite() {
        return syncWrite;
    }

    public long getBlockCacheSize() {
        return blockCacheSize;
    }

    public long getWriteBufferSize() {
        return writeBufferSize;
    }

    public int getMaxWriteBufferNumber() {
        return maxWriteBufferNumber;
    }

    public int getMinWriteBufferNumberToMerge() {
        return minWriteBufferNumberToMerge;
    }

    public int getMaxBackgroundJobs() {
        return maxBackgroundJobs;
    }

    public int getMaxOpenFiles() {
        return maxOpenFiles;
    }

    public long getTargetFileSizeBase() {
        return targetFileSizeBase;
    }

    public int getLevel0FileNumCompactionTrigger() {
        return level0FileNumCompactionTrigger;
    }

    public int getLevel0SlowdownWritesTrigger() {
        return level0SlowdownWritesTrigger;
    }

    public int getLevel0StopWritesTrigger() {
        return level0StopWritesTrigger;
    }

    public boolean isEnableStatistics() {
        return enableStatistics;
    }

    public boolean isOptimizeFiltersForHits() {
        return optimizeFiltersForHits;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public boolean isEnableRangeDelete() {
        return enableRangeDelete;
    }

    public boolean isRangeDeleteCompactAfterDelete() {
        return rangeDeleteCompactAfterDelete;
    }

    public String tuningSummary() {
        return "blockCacheSize="
                + blockCacheSize
                + ", writeBufferSize="
                + writeBufferSize
                + ", maxWriteBufferNumber="
                + maxWriteBufferNumber
                + ", minWriteBufferNumberToMerge="
                + minWriteBufferNumberToMerge
                + ", maxBackgroundJobs="
                + maxBackgroundJobs
                + ", maxOpenFiles="
                + maxOpenFiles
                + ", targetFileSizeBase="
                + targetFileSizeBase
                + ", level0FileNumCompactionTrigger="
                + level0FileNumCompactionTrigger
                + ", level0SlowdownWritesTrigger="
                + level0SlowdownWritesTrigger
                + ", level0StopWritesTrigger="
                + level0StopWritesTrigger
                + ", enableStatistics="
                + enableStatistics
                + ", optimizeFiltersForHits="
                + optimizeFiltersForHits
                + ", compressionType="
                + compressionType
                + ", enableRangeDelete="
                + enableRangeDelete
                + ", rangeDeleteCompactAfterDelete="
                + rangeDeleteCompactAfterDelete;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RocksDBStoreConfig)) {
            return false;
        }
        RocksDBStoreConfig that = (RocksDBStoreConfig) o;
        return syncWrite == that.syncWrite
                && blockCacheSize == that.blockCacheSize
                && writeBufferSize == that.writeBufferSize
                && maxWriteBufferNumber == that.maxWriteBufferNumber
                && minWriteBufferNumberToMerge == that.minWriteBufferNumberToMerge
                && maxBackgroundJobs == that.maxBackgroundJobs
                && maxOpenFiles == that.maxOpenFiles
                && targetFileSizeBase == that.targetFileSizeBase
                && level0FileNumCompactionTrigger == that.level0FileNumCompactionTrigger
                && level0SlowdownWritesTrigger == that.level0SlowdownWritesTrigger
                && level0StopWritesTrigger == that.level0StopWritesTrigger
                && enableStatistics == that.enableStatistics
                && optimizeFiltersForHits == that.optimizeFiltersForHits
                && enableRangeDelete == that.enableRangeDelete
                && rangeDeleteCompactAfterDelete == that.rangeDeleteCompactAfterDelete
                && Objects.equals(dbPath, that.dbPath)
                && Objects.equals(compressionType, that.compressionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dbPath,
                syncWrite,
                blockCacheSize,
                writeBufferSize,
                maxWriteBufferNumber,
                minWriteBufferNumberToMerge,
                maxBackgroundJobs,
                maxOpenFiles,
                targetFileSizeBase,
                level0FileNumCompactionTrigger,
                level0SlowdownWritesTrigger,
                level0StopWritesTrigger,
                enableStatistics,
                optimizeFiltersForHits,
                compressionType,
                enableRangeDelete,
                rangeDeleteCompactAfterDelete);
    }

    private static int intOption(Configuration config, String key) {
        return nonNegative(config.getInt(key, DEFAULT_INT_OPTION), key);
    }

    private static long sizeOption(Configuration config, String key) {
        String value = config.getConfig(key);
        if (StringUtils.isBlank(value)) {
            return DEFAULT_LONG_OPTION;
        }
        return nonNegative(parseSize(value, key), key);
    }

    private static long parseSize(String rawValue, String key) {
        String value = rawValue.trim().toUpperCase(Locale.ROOT);
        long multiplier = 1L;
        if (value.endsWith("KB")) {
            multiplier = 1024L;
            value = value.substring(0, value.length() - 2).trim();
        } else if (value.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            value = value.substring(0, value.length() - 2).trim();
        } else if (value.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            value = value.substring(0, value.length() - 2).trim();
        } else if (value.endsWith("B")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        try {
            return Math.multiplyExact(Long.parseLong(value), multiplier);
        } catch (ArithmeticException | NumberFormatException e) {
            throw new StoreException(e, "invalid RocksDB size config, key:" + key + ", value:" + rawValue);
        }
    }

    private static int nonNegative(int value, String key) {
        if (value < 0) {
            throw new StoreException("RocksDB config must be non-negative, key:" + key + ", value:" + value);
        }
        return value;
    }

    private static long nonNegative(long value, String key) {
        if (value < 0) {
            throw new StoreException("RocksDB config must be non-negative, key:" + key + ", value:" + value);
        }
        return value;
    }
}
