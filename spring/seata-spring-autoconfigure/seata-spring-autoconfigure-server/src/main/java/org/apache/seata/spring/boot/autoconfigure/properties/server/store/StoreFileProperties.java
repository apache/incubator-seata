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
package org.apache.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_FILE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_FILE_ROCKSDB_PREFIX;

@Component
@ConfigurationProperties(prefix = STORE_FILE_PREFIX)
public class StoreFileProperties {
    private String engine = "file";
    private String dir = "sessionStore";
    private Integer maxBranchSessionSize = 16384;
    private Integer maxGlobalSessionSize = 512;
    private Integer fileWriteBufferCacheSize = 16384;
    private Integer sessionReloadReadSize = DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE;
    private String flushDiskMode = "async";

    public String getEngine() {
        return engine;
    }

    public StoreFileProperties setEngine(String engine) {
        this.engine = engine;
        return this;
    }

    public String getDir() {
        return dir;
    }

    public StoreFileProperties setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public Integer getMaxBranchSessionSize() {
        return maxBranchSessionSize;
    }

    public StoreFileProperties setMaxBranchSessionSize(Integer maxBranchSessionSize) {
        this.maxBranchSessionSize = maxBranchSessionSize;
        return this;
    }

    public Integer getMaxGlobalSessionSize() {
        return maxGlobalSessionSize;
    }

    public StoreFileProperties setMaxGlobalSessionSize(Integer maxGlobalSessionSize) {
        this.maxGlobalSessionSize = maxGlobalSessionSize;
        return this;
    }

    public Integer getFileWriteBufferCacheSize() {
        return fileWriteBufferCacheSize;
    }

    public StoreFileProperties setFileWriteBufferCacheSize(Integer fileWriteBufferCacheSize) {
        this.fileWriteBufferCacheSize = fileWriteBufferCacheSize;
        return this;
    }

    public Integer getSessionReloadReadSize() {
        return sessionReloadReadSize;
    }

    public StoreFileProperties setSessionReloadReadSize(Integer sessionReloadReadSize) {
        this.sessionReloadReadSize = sessionReloadReadSize;
        return this;
    }

    public String getFlushDiskMode() {
        return flushDiskMode;
    }

    public StoreFileProperties setFlushDiskMode(String flushDiskMode) {
        this.flushDiskMode = flushDiskMode;
        return this;
    }

    @Component
    @ConfigurationProperties(prefix = STORE_FILE_ROCKSDB_PREFIX)
    public static class RocksDB {
        private String dir = "sessionStore/rocksdb";
        private String blockCacheSize = "0";
        private String writeBufferSize = "0";
        private String dbWriteBufferSize = "0";
        private String maxTotalWalSize = "0";
        private String globalWriteBufferSize = "0";
        private String branchWriteBufferSize = "0";
        private String lockWriteBufferSize = "0";
        private String indexWriteBufferSize = "0";
        private String metadataWriteBufferSize = "0";
        private Integer maxWriteBufferNumber = 0;
        private Integer minWriteBufferNumberToMerge = 0;
        private Integer maxBackgroundJobs = 0;
        private Integer maxOpenFiles = 0;
        private String targetFileSizeBase = "0";
        private Integer level0FileNumCompactionTrigger = 0;
        private Integer level0SlowdownWritesTrigger = 0;
        private Integer level0StopWritesTrigger = 0;
        private Boolean enableStatistics = false;
        private Boolean optimizeFiltersForHits = false;
        private String compressionType;
        private Boolean enableRangeDelete = true;
        private Boolean rangeDeleteCompactAfterDelete = false;
        private Integer fullScanMaxLimit = 10000;
        private Long fullScanDeadlineMillis = 5000L;
        private Integer multiStatusScanPageSize = 256;
        private String walSyncMode = "none";
        private Integer walSyncIntervalMillis = 2000;
        private Long walSyncWriteThreshold = 10L;
        private Boolean walSyncOnShutdown = true;
        private Integer walSyncShutdownTimeoutMillis = 30000;
        private Integer walSyncWarnThresholdMillis = 1000;
        private Boolean orphanLockCleanEnabled = true;
        private Long orphanLockCleanIntervalMillis = 60000L;
        private Integer orphanLockCleanBatchLimit = 1000;
        private Integer orphanLockCleanMaxBatches = 2;
        private Long orphanLockCleanRoundSleepMillis = 100L;

        public String getDir() {
            return dir;
        }

        public RocksDB setDir(String dir) {
            this.dir = dir;
            return this;
        }

        public String getBlockCacheSize() {
            return blockCacheSize;
        }

        public RocksDB setBlockCacheSize(String blockCacheSize) {
            this.blockCacheSize = blockCacheSize;
            return this;
        }

        public String getWriteBufferSize() {
            return writeBufferSize;
        }

        public RocksDB setWriteBufferSize(String writeBufferSize) {
            this.writeBufferSize = writeBufferSize;
            return this;
        }

        public String getDbWriteBufferSize() {
            return dbWriteBufferSize;
        }

        public RocksDB setDbWriteBufferSize(String dbWriteBufferSize) {
            this.dbWriteBufferSize = dbWriteBufferSize;
            return this;
        }

        public String getMaxTotalWalSize() {
            return maxTotalWalSize;
        }

        public RocksDB setMaxTotalWalSize(String maxTotalWalSize) {
            this.maxTotalWalSize = maxTotalWalSize;
            return this;
        }

        public String getGlobalWriteBufferSize() {
            return globalWriteBufferSize;
        }

        public RocksDB setGlobalWriteBufferSize(String globalWriteBufferSize) {
            this.globalWriteBufferSize = globalWriteBufferSize;
            return this;
        }

        public String getBranchWriteBufferSize() {
            return branchWriteBufferSize;
        }

        public RocksDB setBranchWriteBufferSize(String branchWriteBufferSize) {
            this.branchWriteBufferSize = branchWriteBufferSize;
            return this;
        }

        public String getLockWriteBufferSize() {
            return lockWriteBufferSize;
        }

        public RocksDB setLockWriteBufferSize(String lockWriteBufferSize) {
            this.lockWriteBufferSize = lockWriteBufferSize;
            return this;
        }

        public String getIndexWriteBufferSize() {
            return indexWriteBufferSize;
        }

        public RocksDB setIndexWriteBufferSize(String indexWriteBufferSize) {
            this.indexWriteBufferSize = indexWriteBufferSize;
            return this;
        }

        public String getMetadataWriteBufferSize() {
            return metadataWriteBufferSize;
        }

        public RocksDB setMetadataWriteBufferSize(String metadataWriteBufferSize) {
            this.metadataWriteBufferSize = metadataWriteBufferSize;
            return this;
        }

        public Integer getMaxWriteBufferNumber() {
            return maxWriteBufferNumber;
        }

        public RocksDB setMaxWriteBufferNumber(Integer maxWriteBufferNumber) {
            this.maxWriteBufferNumber = maxWriteBufferNumber;
            return this;
        }

        public Integer getMinWriteBufferNumberToMerge() {
            return minWriteBufferNumberToMerge;
        }

        public RocksDB setMinWriteBufferNumberToMerge(Integer minWriteBufferNumberToMerge) {
            this.minWriteBufferNumberToMerge = minWriteBufferNumberToMerge;
            return this;
        }

        public Integer getMaxBackgroundJobs() {
            return maxBackgroundJobs;
        }

        public RocksDB setMaxBackgroundJobs(Integer maxBackgroundJobs) {
            this.maxBackgroundJobs = maxBackgroundJobs;
            return this;
        }

        public Integer getMaxOpenFiles() {
            return maxOpenFiles;
        }

        public RocksDB setMaxOpenFiles(Integer maxOpenFiles) {
            this.maxOpenFiles = maxOpenFiles;
            return this;
        }

        public String getTargetFileSizeBase() {
            return targetFileSizeBase;
        }

        public RocksDB setTargetFileSizeBase(String targetFileSizeBase) {
            this.targetFileSizeBase = targetFileSizeBase;
            return this;
        }

        public Integer getLevel0FileNumCompactionTrigger() {
            return level0FileNumCompactionTrigger;
        }

        public RocksDB setLevel0FileNumCompactionTrigger(Integer level0FileNumCompactionTrigger) {
            this.level0FileNumCompactionTrigger = level0FileNumCompactionTrigger;
            return this;
        }

        public Integer getLevel0SlowdownWritesTrigger() {
            return level0SlowdownWritesTrigger;
        }

        public RocksDB setLevel0SlowdownWritesTrigger(Integer level0SlowdownWritesTrigger) {
            this.level0SlowdownWritesTrigger = level0SlowdownWritesTrigger;
            return this;
        }

        public Integer getLevel0StopWritesTrigger() {
            return level0StopWritesTrigger;
        }

        public RocksDB setLevel0StopWritesTrigger(Integer level0StopWritesTrigger) {
            this.level0StopWritesTrigger = level0StopWritesTrigger;
            return this;
        }

        public Boolean getEnableStatistics() {
            return enableStatistics;
        }

        public RocksDB setEnableStatistics(Boolean enableStatistics) {
            this.enableStatistics = enableStatistics;
            return this;
        }

        public Boolean getOptimizeFiltersForHits() {
            return optimizeFiltersForHits;
        }

        public RocksDB setOptimizeFiltersForHits(Boolean optimizeFiltersForHits) {
            this.optimizeFiltersForHits = optimizeFiltersForHits;
            return this;
        }

        public String getCompressionType() {
            return compressionType;
        }

        public RocksDB setCompressionType(String compressionType) {
            this.compressionType = compressionType;
            return this;
        }

        public Boolean getEnableRangeDelete() {
            return enableRangeDelete;
        }

        public RocksDB setEnableRangeDelete(Boolean enableRangeDelete) {
            this.enableRangeDelete = enableRangeDelete;
            return this;
        }

        public Boolean getRangeDeleteCompactAfterDelete() {
            return rangeDeleteCompactAfterDelete;
        }

        public RocksDB setRangeDeleteCompactAfterDelete(Boolean rangeDeleteCompactAfterDelete) {
            this.rangeDeleteCompactAfterDelete = rangeDeleteCompactAfterDelete;
            return this;
        }

        public Integer getFullScanMaxLimit() {
            return fullScanMaxLimit;
        }

        public RocksDB setFullScanMaxLimit(Integer fullScanMaxLimit) {
            this.fullScanMaxLimit = fullScanMaxLimit;
            return this;
        }

        public Long getFullScanDeadlineMillis() {
            return fullScanDeadlineMillis;
        }

        public RocksDB setFullScanDeadlineMillis(Long fullScanDeadlineMillis) {
            this.fullScanDeadlineMillis = fullScanDeadlineMillis;
            return this;
        }

        public Integer getMultiStatusScanPageSize() {
            return multiStatusScanPageSize;
        }

        public RocksDB setMultiStatusScanPageSize(Integer multiStatusScanPageSize) {
            this.multiStatusScanPageSize = multiStatusScanPageSize;
            return this;
        }

        public String getWalSyncMode() {
            return walSyncMode;
        }

        public RocksDB setWalSyncMode(String walSyncMode) {
            this.walSyncMode = walSyncMode;
            return this;
        }

        public Integer getWalSyncIntervalMillis() {
            return walSyncIntervalMillis;
        }

        public RocksDB setWalSyncIntervalMillis(Integer walSyncIntervalMillis) {
            this.walSyncIntervalMillis = walSyncIntervalMillis;
            return this;
        }

        public Long getWalSyncWriteThreshold() {
            return walSyncWriteThreshold;
        }

        public RocksDB setWalSyncWriteThreshold(Long walSyncWriteThreshold) {
            this.walSyncWriteThreshold = walSyncWriteThreshold;
            return this;
        }

        public Boolean getWalSyncOnShutdown() {
            return walSyncOnShutdown;
        }

        public RocksDB setWalSyncOnShutdown(Boolean walSyncOnShutdown) {
            this.walSyncOnShutdown = walSyncOnShutdown;
            return this;
        }

        public Integer getWalSyncShutdownTimeoutMillis() {
            return walSyncShutdownTimeoutMillis;
        }

        public RocksDB setWalSyncShutdownTimeoutMillis(Integer walSyncShutdownTimeoutMillis) {
            this.walSyncShutdownTimeoutMillis = walSyncShutdownTimeoutMillis;
            return this;
        }

        public Integer getWalSyncWarnThresholdMillis() {
            return walSyncWarnThresholdMillis;
        }

        public RocksDB setWalSyncWarnThresholdMillis(Integer walSyncWarnThresholdMillis) {
            this.walSyncWarnThresholdMillis = walSyncWarnThresholdMillis;
            return this;
        }

        public Boolean getOrphanLockCleanEnabled() {
            return orphanLockCleanEnabled;
        }

        public RocksDB setOrphanLockCleanEnabled(Boolean orphanLockCleanEnabled) {
            this.orphanLockCleanEnabled = orphanLockCleanEnabled;
            return this;
        }

        public Long getOrphanLockCleanIntervalMillis() {
            return orphanLockCleanIntervalMillis;
        }

        public RocksDB setOrphanLockCleanIntervalMillis(Long orphanLockCleanIntervalMillis) {
            this.orphanLockCleanIntervalMillis = orphanLockCleanIntervalMillis;
            return this;
        }

        public Integer getOrphanLockCleanBatchLimit() {
            return orphanLockCleanBatchLimit;
        }

        public RocksDB setOrphanLockCleanBatchLimit(Integer orphanLockCleanBatchLimit) {
            this.orphanLockCleanBatchLimit = orphanLockCleanBatchLimit;
            return this;
        }

        public Integer getOrphanLockCleanMaxBatches() {
            return orphanLockCleanMaxBatches;
        }

        public RocksDB setOrphanLockCleanMaxBatches(Integer orphanLockCleanMaxBatches) {
            this.orphanLockCleanMaxBatches = orphanLockCleanMaxBatches;
            return this;
        }

        public Long getOrphanLockCleanRoundSleepMillis() {
            return orphanLockCleanRoundSleepMillis;
        }

        public RocksDB setOrphanLockCleanRoundSleepMillis(Long orphanLockCleanRoundSleepMillis) {
            this.orphanLockCleanRoundSleepMillis = orphanLockCleanRoundSleepMillis;
            return this;
        }
    }
}
