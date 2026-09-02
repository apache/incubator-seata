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
package org.apache.seata.server.storage.rocksdb.lock;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.lock.AbstractLockManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBLocalLocks;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;

import java.util.Arrays;

/**
 * RocksDB lock manager for file store engine.
 */
@LoadLevel(name = "rocksdb", scope = Scope.PROTOTYPE)
public class RocksDBLockManager extends AbstractLockManager {

    private final RocksDBLocker locker;

    public RocksDBLockManager() {
        this(RocksDBStoreEngineFactory.getInstance());
    }

    public RocksDBLockManager(RocksDBStoreEngine storeEngine) {
        this(storeEngine, RocksDBLocker.DEFAULT_LOCK_INDEX_SCAN_BATCH_SIZE);
    }

    /**
     * Creates a lock manager with a bounded lock-index streaming batch size.
     *
     * <p>This is primarily useful for controlled performance experiments. Production callers should use the
     * default constructor unless they have independently validated a different value for their workload.
     */
    public RocksDBLockManager(RocksDBStoreEngine storeEngine, int lockIndexScanBatchSize) {
        this.locker = new RocksDBLocker(storeEngine, new RocksDBLocalLocks(), lockIndexScanBatchSize);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        try {
            return getLocker().releaseLock(branchSession.getXid(), branchSession.getBranchId());
        } catch (Exception t) {
            LOGGER.error("unLock error, xid {}, branchId:{}", branchSession.getXid(), branchSession.getBranchId(), t);
            return false;
        }
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        try {
            return getLocker().releaseLock(globalSession.getXid());
        } catch (Exception t) {
            LOGGER.error("unLock globalSession error, xid:{}", globalSession.getXid(), t);
            return false;
        }
    }

    public int cleanOrphanLocks() {
        return locker.cleanOrphanLocks();
    }

    public CleanOrphanLocksResult cleanOrphanLocks(int limit) {
        return locker.cleanOrphanLocks(limit);
    }

    public CleanOrphanLocksResult cleanOrphanLocks(byte[] seekKey, int limit) {
        return locker.cleanOrphanLocks(seekKey, limit);
    }

    public boolean wasLastShutdownClean() {
        return locker.wasLastShutdownClean();
    }

    public CleanOrphanLocksResult cleanOrphanLocksBatches(int batchLimit, int maxBatches) {
        return cleanOrphanLocksBatches(null, batchLimit, maxBatches, 0L);
    }

    public CleanOrphanLocksResult cleanOrphanLocksBatches(byte[] seekKey, int batchLimit, int maxBatches) {
        return cleanOrphanLocksBatches(seekKey, batchLimit, maxBatches, 0L);
    }

    /**
     * Clean orphan locks in batches with optional deadline protection.
     *
     * @param seekKey      starting position (null for beginning)
     * @param batchLimit   max entries per batch
     * @param maxBatches   max number of batches to execute
     * @param deadlineNanos absolute nanoTime deadline; 0 means no deadline
     * @return aggregated result
     */
    public CleanOrphanLocksResult cleanOrphanLocksBatches(
            byte[] seekKey, int batchLimit, int maxBatches, long deadlineNanos) {
        if (batchLimit <= 0) {
            throw new IllegalArgumentException("batchLimit must be positive");
        }
        if (maxBatches <= 0) {
            throw new IllegalArgumentException("maxBatches must be positive");
        }
        int cleaned = 0;
        int scanned = 0;
        int batches = 0;
        boolean limitReached = false;
        boolean deadlineReached = false;
        byte[] cursor = CleanOrphanLocksResult.copy(seekKey);
        for (int i = 0; i < maxBatches; i++) {
            if (deadlineNanos > 0 && System.nanoTime() >= deadlineNanos) {
                deadlineReached = true;
                break;
            }
            CleanOrphanLocksResult result = cleanOrphanLocks(cursor, batchLimit);
            batches++;
            cleaned += result.getCleaned();
            scanned += result.getScanned();
            limitReached = result.isLimitReached();
            cursor = result.getNextSeekKey();
            if (!limitReached) {
                break;
            }
        }
        return new CleanOrphanLocksResult(cleaned, scanned, limitReached, deadlineReached, cursor, batches);
    }

    @Override
    protected Locker getLocker(BranchSession branchSession) {
        return locker;
    }

    public static class CleanOrphanLocksResult {
        private final int cleaned;
        private final int scanned;
        private final boolean limitReached;
        private final boolean deadlineReached;
        private final byte[] nextSeekKey;
        private final int batches;

        CleanOrphanLocksResult(int cleaned, int scanned, boolean limitReached, byte[] nextSeekKey) {
            this(cleaned, scanned, limitReached, false, nextSeekKey, 1);
        }

        CleanOrphanLocksResult(int cleaned, int scanned, boolean limitReached, byte[] nextSeekKey, int batches) {
            this(cleaned, scanned, limitReached, false, nextSeekKey, batches);
        }

        CleanOrphanLocksResult(
                int cleaned,
                int scanned,
                boolean limitReached,
                boolean deadlineReached,
                byte[] nextSeekKey,
                int batches) {
            this.cleaned = cleaned;
            this.scanned = scanned;
            this.limitReached = limitReached;
            this.deadlineReached = deadlineReached;
            this.nextSeekKey = copy(nextSeekKey);
            this.batches = batches;
        }

        public int getCleaned() {
            return cleaned;
        }

        public int getScanned() {
            return scanned;
        }

        public boolean isLimitReached() {
            return limitReached;
        }

        public boolean isDeadlineReached() {
            return deadlineReached;
        }

        public boolean isTruncated() {
            return limitReached || deadlineReached;
        }

        public byte[] getNextSeekKey() {
            return copy(nextSeekKey);
        }

        public int getBatches() {
            return batches;
        }

        private static byte[] copy(byte[] value) {
            return value == null ? null : Arrays.copyOf(value, value.length);
        }
    }
}
