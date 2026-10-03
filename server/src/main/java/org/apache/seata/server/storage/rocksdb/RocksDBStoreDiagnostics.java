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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable diagnostics snapshot for the RocksDB file store engine.
 */
public class RocksDBStoreDiagnostics {

    public static final String ESTIMATE_LIVE_DATA_SIZE = "rocksdb.estimate-live-data-size";
    public static final String TOTAL_SST_FILES_SIZE = "rocksdb.total-sst-files-size";
    public static final String ESTIMATE_PENDING_COMPACTION_BYTES = "rocksdb.estimate-pending-compaction-bytes";
    public static final String CUR_SIZE_ACTIVE_MEM_TABLE = "rocksdb.cur-size-active-mem-table";
    public static final String CUR_SIZE_ALL_MEM_TABLES = "rocksdb.cur-size-all-mem-tables";
    public static final String NUM_LIVE_VERSIONS = "rocksdb.num-live-versions";
    public static final String ESTIMATE_NUM_KEYS = "rocksdb.estimate-num-keys";
    public static final String NUM_FILES_AT_LEVEL0 = "rocksdb.num-files-at-level0";
    public static final String NUM_FILES_AT_LEVEL1 = "rocksdb.num-files-at-level1";
    public static final String NUM_FILES_AT_LEVEL2 = "rocksdb.num-files-at-level2";
    public static final String NUM_FILES_AT_LEVEL3 = "rocksdb.num-files-at-level3";
    public static final String NUM_FILES_AT_LEVEL4 = "rocksdb.num-files-at-level4";
    public static final String NUM_FILES_AT_LEVEL5 = "rocksdb.num-files-at-level5";
    public static final String NUM_FILES_AT_LEVEL6 = "rocksdb.num-files-at-level6";
    public static final String NUM_IMMUTABLE_MEM_TABLE = "rocksdb.num-immutable-mem-table";
    public static final String MEM_TABLE_FLUSH_PENDING = "rocksdb.mem-table-flush-pending";
    public static final String COMPACTION_PENDING = "rocksdb.compaction-pending";
    public static final String SIZE_ALL_MEM_TABLES = "rocksdb.size-all-mem-tables";
    public static final String ESTIMATE_TABLE_READERS_MEM = "rocksdb.estimate-table-readers-mem";
    public static final String BACKGROUND_ERRORS = "rocksdb.background-errors";
    public static final String NUM_RUNNING_COMPACTIONS = "rocksdb.num-running-compactions";
    public static final String NUM_RUNNING_FLUSHES = "rocksdb.num-running-flushes";
    public static final String ACTUAL_DELAYED_WRITE_RATE = "rocksdb.actual-delayed-write-rate";
    public static final String IS_WRITE_STOPPED = "rocksdb.is-write-stopped";

    public static final String BLOCK_CACHE_USAGE = "block-cache-usage";
    public static final String BLOCK_CACHE_PINNED_USAGE = "block-cache-pinned-usage";
    public static final String BLOCK_CACHE_CAPACITY = "block-cache-capacity";

    private final String dbPath;
    private final int formatVersion;
    private final String rocksDBVersion;
    private final boolean syncWrite;
    private final boolean closed;
    private final String tuningSummary;
    private final Map<String, Long> properties;
    private final Map<RocksDBColumnFamily, Map<String, Long>> columnFamilyProperties;
    private final List<String> errors;
    private final long blockCacheUsage;
    private final long blockCachePinnedUsage;
    private final long blockCacheCapacity;
    private final RocksDBWalSyncStats walSyncStats;

    public RocksDBStoreDiagnostics(
            String dbPath,
            int formatVersion,
            String rocksDBVersion,
            boolean syncWrite,
            boolean closed,
            String tuningSummary,
            Map<String, Long> properties,
            Map<RocksDBColumnFamily, Map<String, Long>> columnFamilyProperties,
            List<String> errors) {
        this(
                dbPath,
                formatVersion,
                rocksDBVersion,
                syncWrite,
                closed,
                tuningSummary,
                properties,
                columnFamilyProperties,
                errors,
                0L,
                0L,
                0L,
                RocksDBWalSyncStats.NONE);
    }

    public RocksDBStoreDiagnostics(
            String dbPath,
            int formatVersion,
            String rocksDBVersion,
            boolean syncWrite,
            boolean closed,
            String tuningSummary,
            Map<String, Long> properties,
            Map<RocksDBColumnFamily, Map<String, Long>> columnFamilyProperties,
            List<String> errors,
            long blockCacheUsage,
            long blockCachePinnedUsage,
            long blockCacheCapacity) {
        this(
                dbPath,
                formatVersion,
                rocksDBVersion,
                syncWrite,
                closed,
                tuningSummary,
                properties,
                columnFamilyProperties,
                errors,
                blockCacheUsage,
                blockCachePinnedUsage,
                blockCacheCapacity,
                RocksDBWalSyncStats.NONE);
    }

    public RocksDBStoreDiagnostics(
            String dbPath,
            int formatVersion,
            String rocksDBVersion,
            boolean syncWrite,
            boolean closed,
            String tuningSummary,
            Map<String, Long> properties,
            Map<RocksDBColumnFamily, Map<String, Long>> columnFamilyProperties,
            List<String> errors,
            long blockCacheUsage,
            long blockCachePinnedUsage,
            long blockCacheCapacity,
            RocksDBWalSyncStats walSyncStats) {
        this.dbPath = dbPath;
        this.formatVersion = formatVersion;
        this.rocksDBVersion = rocksDBVersion;
        this.syncWrite = syncWrite;
        this.closed = closed;
        this.tuningSummary = tuningSummary;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.columnFamilyProperties = unmodifiableColumnFamilyProperties(columnFamilyProperties);
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.blockCacheUsage = blockCacheUsage;
        this.blockCachePinnedUsage = blockCachePinnedUsage;
        this.blockCacheCapacity = blockCacheCapacity;
        this.walSyncStats = walSyncStats == null ? RocksDBWalSyncStats.NONE : walSyncStats;
    }

    public String getDbPath() {
        return dbPath;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public String getRocksDBVersion() {
        return rocksDBVersion;
    }

    public boolean isSyncWrite() {
        return syncWrite;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getTuningSummary() {
        return tuningSummary;
    }

    public Map<String, Long> getProperties() {
        return properties;
    }

    public long getProperty(String property) {
        return properties.getOrDefault(property, 0L);
    }

    public Map<RocksDBColumnFamily, Map<String, Long>> getColumnFamilyProperties() {
        return columnFamilyProperties;
    }

    public long getColumnFamilyProperty(RocksDBColumnFamily columnFamily, String property) {
        Map<String, Long> properties = columnFamilyProperties.get(columnFamily);
        return properties == null ? 0L : properties.getOrDefault(property, 0L);
    }

    public List<String> getErrors() {
        return errors;
    }

    public long getBlockCacheUsage() {
        return blockCacheUsage;
    }

    public long getBlockCachePinnedUsage() {
        return blockCachePinnedUsage;
    }

    public long getBlockCacheCapacity() {
        return blockCacheCapacity;
    }

    public boolean isBlockCacheEnabled() {
        return blockCacheCapacity > 0;
    }

    public RocksDBWalSyncStats getWalSyncStats() {
        return walSyncStats;
    }

    private static Map<RocksDBColumnFamily, Map<String, Long>> unmodifiableColumnFamilyProperties(
            Map<RocksDBColumnFamily, Map<String, Long>> source) {
        Map<RocksDBColumnFamily, Map<String, Long>> result = new EnumMap<>(RocksDBColumnFamily.class);
        for (Map.Entry<RocksDBColumnFamily, Map<String, Long>> entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }
}
