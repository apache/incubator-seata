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
package org.apache.seata.server.storage.file.lock;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;

import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * The type Memory locker.
 *
 */
public class FileLocker extends AbstractLocker {

    private static final int BUCKET_PER_TABLE = 128;

    private static final ConcurrentMap<String/* resourceId */, ConcurrentMap<String/* tableName */,
        ConcurrentMap<Integer/* bucketId */, BucketLockMap>>>
        LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * The Branch session.
     */
    protected BranchSession branchSession;

    /**
     * Instantiates a new Memory locker.
     *
     * @param branchSession the branch session
     */
    public FileLocker(BranchSession branchSession) {
        this.branchSession = branchSession;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        return acquireLock(rowLocks, true, false);
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        Map<BucketLockMap, Set<String>> bucketHolder = branchSession.getLockHolder();
        Map<String, ConcurrentMap<Integer, BucketLockMap>> dbLockMap = CollectionUtils.computeIfAbsent(
            LOCK_MAP, resourceId, key -> new ConcurrentHashMap<>(8));
        boolean failFast = false;
        boolean canLock = true;
        for (RowLock lock : rowLocks) {
            String tableName = lock.getTableName();
            String pk = lock.getPk();
            ConcurrentMap<Integer, BucketLockMap> tableLockMap = CollectionUtils.computeIfAbsent(dbLockMap, tableName,
                key -> new ConcurrentHashMap<>(8));

            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = CollectionUtils.computeIfAbsent(tableLockMap, bucketId,
                key -> new BucketLockMap());
            BranchSession previousLockBranchSession = bucketLockMap.get().putIfAbsent(pk, branchSession);
            if (previousLockBranchSession == null) {
                // No existing lock, and now locked by myself
                Set<String> keysInHolder = CollectionUtils.computeIfAbsent(bucketHolder, bucketLockMap,
                    key -> ConcurrentHashMap.newKeySet());
                keysInHolder.add(pk);
            } else if (previousLockBranchSession.getTransactionId() == transactionId) {
                // Locked by me before
            } else {
                LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + previousLockBranchSession.getBranchId());
                try {
                    // Release all acquired locks.
                    branchSession.unlock();
                } catch (TransactionException e) {
                    throw new FrameworkException(e);
                }
                if (!autoCommit && previousLockBranchSession.getLockStatus() == LockStatus.Rollbacking) {
                    failFast = true;
                    break;
                }
                if (canLock) {
                    canLock = false;
                    if (autoCommit) {
                        break;
                    }
                }
            }
        }
        if (failFast) {
            throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
        }
        return canLock;
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLock) {
        if (CollectionUtils.isEmpty(rowLock)) {
            //no lock
            return true;
        }
        Map<BucketLockMap, Set<String>> lockHolder = branchSession.getLockHolder();
        if (CollectionUtils.isEmpty(lockHolder)) {
            return true;
        }
        lockHolder.forEach((bucket, keys) -> {
            for (String key : keys) {
                // remove lock only if it locked by myself
                bucket.get().remove(key, branchSession);
            }
        });
        lockHolder.clear();
        return true;
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        Long transactionId = rowLocks.get(0).getTransactionId();
        String resourceId = rowLocks.get(0).getResourceId();
        ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> dbLockMap = LOCK_MAP.get(resourceId);
        if (dbLockMap == null) {
            return true;
        }
        for (RowLock rowLock : rowLocks) {
            String tableName = rowLock.getTableName();
            String pk = rowLock.getPk();

            ConcurrentMap<Integer, BucketLockMap> tableLockMap = dbLockMap.get(tableName);
            if (tableLockMap == null) {
                continue;
            }
            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = tableLockMap.get(bucketId);
            if (bucketLockMap == null) {
                continue;
            }
            BranchSession branchSession = bucketLockMap.get().get(pk);
            Long lockingTransactionId = branchSession != null ? branchSession.getTransactionId() : null;
            if (lockingTransactionId == null || lockingTransactionId.longValue() == transactionId) {
                // Locked by me
                continue;
            } else {
                LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + lockingTransactionId);
                return false;
            }
        }
        return true;
    }


    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
    }

    @Override
    public void cleanAllLocks() {
        LOCK_MAP.clear();
    }

    /**
     * Because bucket lock map will be key of HashMap(lockHolder), however {@link ConcurrentHashMap} overwrites
     * {@link Object#hashCode()} and {@link Object#equals(Object)}, that leads to hash key conflict in lockHolder.
     * We define a {@link BucketLockMap} to hold the ConcurrentHashMap(bucketLockMap) and replace it as key of
     * HashMap(lockHolder).
     */
    public static class BucketLockMap {
        private final ConcurrentHashMap<String/* pk */, BranchSession/* branchSession */> bucketLockMap
            = new ConcurrentHashMap<>();

        ConcurrentHashMap<String, BranchSession> get() {
            return bucketLockMap;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }
}
