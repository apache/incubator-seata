/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.storage.file.lock;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.StoreException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.model.LockStatus;
import io.seata.server.session.BranchSession;

import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * The type Memory locker.
 *
 * @author zhangsen
 */
public class FileLocker extends AbstractLocker {

    private static final int BUCKET_PER_TABLE = 128;

    private static final ConcurrentMap<String/* resourceId */, ConcurrentMap<String/* tableName */,
        ConcurrentMap<Integer/* bucketId */, BucketLockMap>>>
        LOCK_MAP = new ConcurrentHashMap<>();

    private static final long MAX_EMPTY_MAP_CLEAN_DELAY = 24 * 60 * 60 * 1000L;

    private static final ScheduledThreadPoolExecutor CLEAN_EMPTY_EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("CleanEmptyMap", 1));


    static {

        CLEAN_EMPTY_EXECUTOR.scheduleAtFixedRate(() -> {
            cleanEmptyMap();
        }, MAX_EMPTY_MAP_CLEAN_DELAY, MAX_EMPTY_MAP_CLEAN_DELAY / 2, TimeUnit.MILLISECONDS);
    }

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

        ConcurrentMap<BucketLockMap, Set<String>> bucketHolder = branchSession.getLockHolder();
        ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> memLockMap = CollectionUtils.computeIfAbsent(
            LOCK_MAP, resourceId, key -> new ConcurrentHashMap<>());
        boolean failFast = false;
        boolean canLock = true;
        for (RowLock lock : rowLocks) {
            String tableName = lock.getTableName();
            String pk = lock.getPk();
            ConcurrentMap<Integer, BucketLockMap> tableLockMap = CollectionUtils.computeIfAbsent(memLockMap, tableName,
                key -> new ConcurrentHashMap<>());

            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = CollectionUtils.computeIfAbsent(tableLockMap, bucketId,
                key -> new BucketLockMap());
            BranchSession previousLockBranchSession = bucketLockMap.putIfAbsent(pk, branchSession);
            if (previousLockBranchSession == null) {
                // No existing lock, and now locked by myself
                Set<String> keysInHolder = CollectionUtils.computeIfAbsent(bucketHolder, bucketLockMap,
                    key -> ConcurrentHashMap.newKeySet());
                keysInHolder.add(pk);
            } else if (previousLockBranchSession.getTransactionId() == transactionId) {
                // Locked by me before
                continue;
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
        ConcurrentMap<BucketLockMap, Set<String>> lockHolder = branchSession.getLockHolder();
        if (CollectionUtils.isEmpty(lockHolder)) {
            return true;
        }
        for (Map.Entry<BucketLockMap, Set<String>> entry : lockHolder.entrySet()) {
            BucketLockMap bucket = entry.getKey();
            Set<String> keys = entry.getValue();
            for (String key : keys) {
                // remove lock only if it locked by myself
                if (!bucket.remove(key, branchSession)) {
                    LOGGER.warn("not found the lock of {},current lock: {}",key,bucket.get(key));
                }
            }
        }
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
        ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> memLockMap = LOCK_MAP.get(resourceId);
        if (memLockMap == null) {
            return true;
        }
        for (RowLock rowLock : rowLocks) {
            String tableName = rowLock.getTableName();
            String pk = rowLock.getPk();

            ConcurrentMap<Integer, BucketLockMap> tableLockMap = memLockMap.get(tableName);
            if (tableLockMap == null) {
                continue;
            }
            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = tableLockMap.get(bucketId);
            if (bucketLockMap == null) {
                continue;
            }
            BranchSession branchSession = bucketLockMap.get(pk);
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

    private static void cleanEmptyMap() {

        for (Map.Entry<String, ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>>> resourceEntry : LOCK_MAP.entrySet()) {
            String resourceId = resourceEntry.getKey();
            ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> tableMap = resourceEntry.getValue();
            for (Map.Entry<String, ConcurrentMap<Integer, BucketLockMap>> tableEntry : tableMap.entrySet()) {
                String tableName = tableEntry.getKey();
                ConcurrentMap<Integer, BucketLockMap> bucketMap = tableEntry.getValue();
                for (Map.Entry<Integer, BucketLockMap> bucketEntry : bucketMap.entrySet()) {
                    BucketLockMap lockMap = bucketEntry.getValue();
                    if (lockMap.isEmpty() && (System.currentTimeMillis() - lockMap.getLastOperateMills() > MAX_EMPTY_MAP_CLEAN_DELAY)) {
                        bucketMap.remove(bucketEntry.getKey());
                    }
                }
                if (bucketMap.isEmpty()) {
                    tableMap.remove(tableName);
                }
            }
            if (tableMap.isEmpty()) {
                LOCK_MAP.remove(resourceId);
            }
        }
    }
    /**
     * Because bucket lock map will be key of HashMap(lockHolder), however {@link ConcurrentHashMap} overwrites
     * {@link Object##hashCode()} and {@link Object##equals(Object)}, that leads to hash key conflict in lockHolder.
     * We define a {@link BucketLockMap} to hold the ConcurrentHashMap(bucketLockMap) and replace it as key of
     * HashMap(lockHolder).
     */
    public static class BucketLockMap {
        private final ConcurrentHashMap<String/* pk */, BranchSession/* branchSession */> bucketLockMap
            = new ConcurrentHashMap<>();
        private long lastOperateMills = System.currentTimeMillis();

        public BranchSession putIfAbsent(String key, BranchSession value) {
            lastOperateMills = System.currentTimeMillis();
            return bucketLockMap.putIfAbsent(key, value);
        }

        public boolean remove(Object key, Object value) {
            lastOperateMills = System.currentTimeMillis();
            return bucketLockMap.remove(key, value);
        }

        public BranchSession get(Object key) {
            lastOperateMills = System.currentTimeMillis();
            return bucketLockMap.get(key);
        }

        public boolean isEmpty() {
            return bucketLockMap.isEmpty();
        }

        public long getLastOperateMills() {
            return lastOperateMills;
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
