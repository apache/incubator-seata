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
package org.apache.seata.server.storage.rocksdb.index;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Secondary index manager for RocksDB global sessions.
 */
public class RocksDBIndexManager {

    public static final int INDEX_VERSION = 2;
    public static final String INDEX_VERSION_KEY = "index_version";
    public static final String INDEX_BUILD_STATUS_KEY = "index_build_status";
    public static final String INDEX_BUILD_STATUS_IN_PROGRESS = "in_progress";
    public static final String INDEX_BUILD_STATUS_COMPLETED = "completed";

    private static final byte[] EMPTY_PREFIX = new byte[0];

    private final RocksDBStoreEngine storeEngine;

    public RocksDBIndexManager(RocksDBStoreEngine storeEngine) {
        this.storeEngine = storeEngine;
    }

    public void ensureReady() {
        String version = getMetadata(INDEX_VERSION_KEY);
        String status = getMetadata(INDEX_BUILD_STATUS_KEY);
        if (version != null) {
            int parsedVersion = parseIndexVersion(version);
            if (parsedVersion > INDEX_VERSION) {
                throw new StoreException("unsupported RocksDB index version:" + parsedVersion);
            }
        }
        if (Integer.toString(INDEX_VERSION).equals(version) && INDEX_BUILD_STATUS_COMPLETED.equals(status)) {
            return;
        }
        rebuildFromGlobalSessions();
    }

    public void rebuildFromGlobalSessions() {
        putMetadata(INDEX_BUILD_STATUS_KEY, INDEX_BUILD_STATUS_IN_PROGRESS);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, EMPTY_PREFIX);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX, EMPTY_PREFIX);
        storeEngine.deleteByPrefix(RocksDBColumnFamily.TRANSACTION_ID_INDEX, EMPTY_PREFIX);

        WriteBatch[] batch = new WriteBatch[] {new WriteBatch()};
        int[] count = new int[] {0};
        try {
            storeEngine.scanByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, EMPTY_PREFIX, (key, value) -> {
                putGlobalIndexes(batch[0], decodeGlobalSession(value));
                count[0]++;
                if (count[0] >= 1024) {
                    storeEngine.write(batch[0]);
                    batch[0].close();
                    batch[0] = new WriteBatch();
                    count[0] = 0;
                }
            });
            if (count[0] > 0) {
                storeEngine.write(batch[0]);
            }
        } finally {
            batch[0].close();
        }

        try (WriteBatch metadataBatch = new WriteBatch()) {
            storeEngine.put(
                    metadataBatch,
                    RocksDBColumnFamily.METADATA,
                    bytes(INDEX_VERSION_KEY),
                    bytes(Integer.toString(INDEX_VERSION)));
            storeEngine.put(
                    metadataBatch,
                    RocksDBColumnFamily.METADATA,
                    bytes(INDEX_BUILD_STATUS_KEY),
                    bytes(INDEX_BUILD_STATUS_COMPLETED));
            storeEngine.write(metadataBatch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB index metadata failed");
        }
    }

    /**
     * Atomically rebuild global secondary indexes after a bounded preflight scan.
     */
    public void rebuildFromGlobalSessionsAtomically(int maxRepairEntries) {
        if (maxRepairEntries <= 0) {
            throw new IllegalArgumentException("maxRepairEntries must be positive");
        }
        ensureRepairEntryLimit(maxRepairEntries);
        try (WriteBatch batch = new WriteBatch()) {
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.GLOBAL_STATUS_INDEX, EMPTY_PREFIX);
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX, EMPTY_PREFIX);
            storeEngine.deleteByPrefix(batch, RocksDBColumnFamily.TRANSACTION_ID_INDEX, EMPTY_PREFIX);
            Set<Long> transactionIds = new HashSet<>();
            storeEngine.scanByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, EMPTY_PREFIX, (key, value) -> {
                GlobalSession globalSession = decodeGlobalSession(value);
                if (!Arrays.equals(key, RocksDBKeyCodec.encodeXid(globalSession.getXid()))) {
                    throw new StoreException("global session key does not match payload xid:" + globalSession.getXid());
                }
                if (!transactionIds.add(globalSession.getTransactionId())) {
                    throw new StoreException("duplicate global transaction id:" + globalSession.getTransactionId());
                }
                putGlobalIndexes(batch, globalSession);
            });
            storeEngine.put(
                    batch,
                    RocksDBColumnFamily.METADATA,
                    bytes(INDEX_VERSION_KEY),
                    bytes(Integer.toString(INDEX_VERSION)));
            storeEngine.put(
                    batch,
                    RocksDBColumnFamily.METADATA,
                    bytes(INDEX_BUILD_STATUS_KEY),
                    bytes(INDEX_BUILD_STATUS_COMPLETED));
            storeEngine.write(batch);
        } catch (RocksDBException e) {
            throw new StoreException(e, "write RocksDB rebuilt indexes failed");
        }
    }

    public void putGlobalIndexes(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        byte[] xidValue = bytes(globalSession.getXid());
        storeEngine.put(
                batch,
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                RocksDBKeyCodec.encodeGlobalStatusIndex(
                        globalSession.getStatus(), globalSession.getBeginTime(), globalSession.getXid()),
                xidValue);
        if (globalSession.getStatus() == GlobalStatus.Begin) {
            storeEngine.put(
                    batch,
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            timeoutDeadlineMillis(globalSession), globalSession.getXid()),
                    xidValue);
        }
        storeEngine.put(
                batch,
                RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                RocksDBKeyCodec.encodeTransactionIdIndex(globalSession.getTransactionId()),
                xidValue);
    }

    public void deleteGlobalIndexes(WriteBatch batch, GlobalSession globalSession) throws RocksDBException {
        storeEngine.delete(
                batch,
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                RocksDBKeyCodec.encodeGlobalStatusIndex(
                        globalSession.getStatus(), globalSession.getBeginTime(), globalSession.getXid()));
        if (globalSession.getStatus() == GlobalStatus.Begin) {
            storeEngine.delete(
                    batch,
                    RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                    RocksDBKeyCodec.encodeGlobalTimeoutIndex(
                            timeoutDeadlineMillis(globalSession), globalSession.getXid()));
        }
        storeEngine.delete(
                batch,
                RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                RocksDBKeyCodec.encodeTransactionIdIndex(globalSession.getTransactionId()));
    }

    public String findXidByTransactionId(long transactionId) {
        byte[] value = storeEngine.get(
                RocksDBColumnFamily.TRANSACTION_ID_INDEX, RocksDBKeyCodec.encodeTransactionIdIndex(transactionId));
        return value == null ? null : string(value);
    }

    public List<String> scanXidsByStatus(GlobalStatus status) {
        List<String> xids = new ArrayList<>();
        scanXidsByStatus(status, xids::add);
        return xids;
    }

    public void scanXidsByStatus(GlobalStatus status, Consumer<String> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        storeEngine.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                RocksDBKeyCodec.encodeGlobalStatusPrefix(status),
                (key, value) -> consumer.accept(string(value)));
    }

    public StatusScanResult scanXidsByStatus(GlobalStatus status, long maxBeginTimeInclusive, int limit) {
        return scanXidsByStatus(status, 0L, maxBeginTimeInclusive, null, limit);
    }

    public StatusScanResult scanXidsByStatus(
            GlobalStatus status, long minBeginTimeInclusive, long maxBeginTimeInclusive, byte[] cursor, int limit) {
        List<StatusIndexEntry> entries = new ArrayList<>();
        if (maxBeginTimeInclusive < minBeginTimeInclusive) {
            return new StatusScanResult(entries, new RocksDBStoreEngine.ScanStats(0, 0, false), null);
        }
        byte[] prefix = RocksDBKeyCodec.encodeGlobalStatusPrefix(status);
        byte[] seekKey = cursor == null
                ? RocksDBKeyCodec.encodeGlobalStatusSeekKey(status, minBeginTimeInclusive)
                : Arrays.copyOf(cursor, cursor.length);
        byte[][] lastReturnedKey = new byte[1][];
        RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                seekKey,
                prefix,
                limit,
                (key, value) -> RocksDBKeyCodec.extractBeginTimeFromStatusIndexKey(key) <= maxBeginTimeInclusive,
                (key, value) -> {
                    entries.add(new StatusIndexEntry(
                            status, string(value), RocksDBKeyCodec.extractBeginTimeFromStatusIndexKey(key)));
                    lastReturnedKey[0] = Arrays.copyOf(key, key.length);
                });
        byte[] nextCursor =
                stats.isLimitReached() && lastReturnedKey[0] != null ? nextSeekKey(lastReturnedKey[0]) : null;
        return new StatusScanResult(entries, stats, nextCursor);
    }

    public TimeoutScanResult scanXidsByTimeoutDeadline(long maxDeadlineMillisInclusive, byte[] cursor, int limit) {
        List<TimeoutIndexEntry> entries = new ArrayList<>();
        byte[] seekKey =
                cursor == null ? RocksDBKeyCodec.encodeGlobalTimeoutSeekKey(0L) : Arrays.copyOf(cursor, cursor.length);
        byte[][] lastReturnedKey = new byte[1][];
        RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
                seekKey,
                EMPTY_PREFIX,
                limit,
                (key, value) -> RocksDBKeyCodec.extractDeadlineFromTimeoutIndexKey(key) <= maxDeadlineMillisInclusive,
                (key, value) -> {
                    entries.add(new TimeoutIndexEntry(
                            string(value), RocksDBKeyCodec.extractDeadlineFromTimeoutIndexKey(key)));
                    lastReturnedKey[0] = Arrays.copyOf(key, key.length);
                });
        byte[] nextCursor =
                stats.isLimitReached() && lastReturnedKey[0] != null ? nextSeekKey(lastReturnedKey[0]) : null;
        return new TimeoutScanResult(entries, stats, nextCursor);
    }

    public static long timeoutDeadlineMillis(GlobalSession globalSession) {
        long beginTime = globalSession.getBeginTime();
        int timeout = globalSession.getTimeout();
        if (timeout > 0 && beginTime > Long.MAX_VALUE - timeout) {
            return Long.MAX_VALUE;
        }
        if (timeout < 0 && beginTime < Long.MIN_VALUE - timeout) {
            return Long.MIN_VALUE;
        }
        return beginTime + timeout;
    }

    public static class StatusIndexEntry {
        private final GlobalStatus status;
        private final String xid;
        private final long beginTime;

        StatusIndexEntry(GlobalStatus status, String xid, long beginTime) {
            this.status = status;
            this.xid = xid;
            this.beginTime = beginTime;
        }

        public GlobalStatus getStatus() {
            return status;
        }

        public String getXid() {
            return xid;
        }

        public long getBeginTime() {
            return beginTime;
        }
    }

    public static class TimeoutIndexEntry {
        private final String xid;
        private final long deadlineMillis;

        TimeoutIndexEntry(String xid, long deadlineMillis) {
            this.xid = xid;
            this.deadlineMillis = deadlineMillis;
        }

        public String getXid() {
            return xid;
        }

        public long getDeadlineMillis() {
            return deadlineMillis;
        }
    }

    public static class TimeoutScanResult {
        private final List<TimeoutIndexEntry> entries;
        private final int rowsScanned;
        private final int rowsReturned;
        private final boolean limitReached;
        private final byte[] nextCursor;

        TimeoutScanResult(List<TimeoutIndexEntry> entries, RocksDBStoreEngine.ScanStats scanStats, byte[] nextCursor) {
            this.entries = entries;
            this.rowsScanned = scanStats.getRowsScanned();
            this.rowsReturned = scanStats.getRowsReturned();
            this.limitReached = scanStats.isLimitReached();
            this.nextCursor = nextCursor == null ? null : Arrays.copyOf(nextCursor, nextCursor.length);
        }

        public List<TimeoutIndexEntry> getEntries() {
            return entries;
        }

        public int getRowsScanned() {
            return rowsScanned;
        }

        public int getRowsReturned() {
            return rowsReturned;
        }

        public boolean isLimitReached() {
            return limitReached;
        }

        public byte[] getNextCursor() {
            return nextCursor == null ? null : Arrays.copyOf(nextCursor, nextCursor.length);
        }
    }

    public static class StatusScanResult {
        private final List<StatusIndexEntry> entries;
        private final int rowsScanned;
        private final int rowsReturned;
        private final boolean limitReached;
        private final byte[] nextCursor;

        StatusScanResult(List<StatusIndexEntry> entries, RocksDBStoreEngine.ScanStats scanStats) {
            this(entries, scanStats, null);
        }

        StatusScanResult(List<StatusIndexEntry> entries, RocksDBStoreEngine.ScanStats scanStats, byte[] nextCursor) {
            this.entries = entries;
            this.rowsScanned = scanStats.getRowsScanned();
            this.rowsReturned = scanStats.getRowsReturned();
            this.limitReached = scanStats.isLimitReached();
            this.nextCursor = nextCursor == null ? null : Arrays.copyOf(nextCursor, nextCursor.length);
        }

        public List<String> getXids() {
            List<String> xids = new ArrayList<>(entries.size());
            for (StatusIndexEntry entry : entries) {
                xids.add(entry.getXid());
            }
            return xids;
        }

        public List<StatusIndexEntry> getEntries() {
            return entries;
        }

        public int getRowsScanned() {
            return rowsScanned;
        }

        public int getRowsReturned() {
            return rowsReturned;
        }

        public boolean isLimitReached() {
            return limitReached;
        }

        public byte[] getNextCursor() {
            return nextCursor == null ? null : Arrays.copyOf(nextCursor, nextCursor.length);
        }
    }

    private GlobalSession decodeGlobalSession(byte[] value) {
        RocksDBValueCodec.DecodedValue decodedValue = RocksDBValueCodec.decode(value);
        if (decodedValue.getType() != RocksDBValueCodec.ValueType.GLOBAL_SESSION) {
            throw new StoreException("unexpected RocksDB value type for global session:" + decodedValue.getType());
        }
        GlobalSession globalSession = new GlobalSession(null, null, null, 0, true);
        globalSession.decode(decodedValue.getPayload());
        return globalSession;
    }

    private void ensureRepairEntryLimit(int maxRepairEntries) {
        int checkedEntries = 0;
        for (RocksDBColumnFamily columnFamily : new RocksDBColumnFamily[] {
            RocksDBColumnFamily.GLOBAL_SESSION,
            RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
            RocksDBColumnFamily.GLOBAL_TIMEOUT_INDEX,
            RocksDBColumnFamily.TRANSACTION_ID_INDEX
        }) {
            int remaining = maxRepairEntries - checkedEntries;
            RocksDBStoreEngine.ScanStats stats = storeEngine.scanByPrefix(
                    columnFamily, EMPTY_PREFIX, EMPTY_PREFIX, remaining + 1, null, (key, value) -> {});
            checkedEntries += stats.getRowsReturned();
            if (checkedEntries > maxRepairEntries) {
                throw new StoreException("RocksDB repair exceeds maxRepairEntries:" + maxRepairEntries);
            }
        }
    }

    private int parseIndexVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new StoreException(e, "invalid RocksDB index version metadata:" + version);
        }
    }

    private String getMetadata(String key) {
        byte[] value = storeEngine.get(RocksDBColumnFamily.METADATA, bytes(key));
        return value == null ? null : string(value);
    }

    private void putMetadata(String key, String value) {
        storeEngine.put(RocksDBColumnFamily.METADATA, bytes(key), bytes(value));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    private static byte[] nextSeekKey(byte[] key) {
        byte[] next = Arrays.copyOf(key, key.length + 1);
        next[key.length] = 0;
        return next;
    }
}
