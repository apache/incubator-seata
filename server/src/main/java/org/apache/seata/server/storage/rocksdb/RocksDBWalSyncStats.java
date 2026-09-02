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

/**
 * Immutable WAL sync statistics snapshot for RocksDB file mode.
 */
public class RocksDBWalSyncStats {

    public static final RocksDBWalSyncStats NONE =
            new RocksDBWalSyncStats(RocksDBWalSyncMode.NONE, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, null);

    private final RocksDBWalSyncMode mode;
    private final long syncCount;
    private final long syncFailureCount;
    private final long lastSyncTimeMillis;
    private final long lastSyncCostMillis;
    private final long avgSyncCostMillis;
    private final long maxSyncCostMillis;
    private final long unsyncedWriteRequests;
    private final long maxUnsyncedWriteRequests;
    private final long unsyncedMillis;
    private final long maxUnsyncedMillis;
    private final long latestSequenceNumber;
    private final long lastSyncedSequenceNumber;
    private final String lastSyncError;

    public RocksDBWalSyncStats(
            RocksDBWalSyncMode mode,
            long syncCount,
            long syncFailureCount,
            long lastSyncTimeMillis,
            long lastSyncCostMillis,
            long avgSyncCostMillis,
            long maxSyncCostMillis,
            long unsyncedWriteRequests,
            long maxUnsyncedWriteRequests,
            long unsyncedMillis,
            long maxUnsyncedMillis,
            long latestSequenceNumber,
            long lastSyncedSequenceNumber,
            String lastSyncError) {
        this.mode = mode == null ? RocksDBWalSyncMode.NONE : mode;
        this.syncCount = syncCount;
        this.syncFailureCount = syncFailureCount;
        this.lastSyncTimeMillis = lastSyncTimeMillis;
        this.lastSyncCostMillis = lastSyncCostMillis;
        this.avgSyncCostMillis = avgSyncCostMillis;
        this.maxSyncCostMillis = maxSyncCostMillis;
        this.unsyncedWriteRequests = unsyncedWriteRequests;
        this.maxUnsyncedWriteRequests = maxUnsyncedWriteRequests;
        this.unsyncedMillis = unsyncedMillis;
        this.maxUnsyncedMillis = maxUnsyncedMillis;
        this.latestSequenceNumber = latestSequenceNumber;
        this.lastSyncedSequenceNumber = lastSyncedSequenceNumber;
        this.lastSyncError = lastSyncError;
    }

    public RocksDBWalSyncMode getMode() {
        return mode;
    }

    public long getSyncCount() {
        return syncCount;
    }

    public long getSyncFailureCount() {
        return syncFailureCount;
    }

    public long getLastSyncTimeMillis() {
        return lastSyncTimeMillis;
    }

    public long getLastSyncCostMillis() {
        return lastSyncCostMillis;
    }

    public long getAvgSyncCostMillis() {
        return avgSyncCostMillis;
    }

    public long getMaxSyncCostMillis() {
        return maxSyncCostMillis;
    }

    public long getUnsyncedWriteRequests() {
        return unsyncedWriteRequests;
    }

    public long getMaxUnsyncedWriteRequests() {
        return maxUnsyncedWriteRequests;
    }

    public long getUnsyncedMillis() {
        return unsyncedMillis;
    }

    public long getMaxUnsyncedMillis() {
        return maxUnsyncedMillis;
    }

    public long getLatestSequenceNumber() {
        return latestSequenceNumber;
    }

    public long getLastSyncedSequenceNumber() {
        return lastSyncedSequenceNumber;
    }

    public String getLastSyncError() {
        return lastSyncError;
    }
}
