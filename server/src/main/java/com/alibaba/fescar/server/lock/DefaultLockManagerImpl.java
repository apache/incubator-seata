/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.server.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.server.session.BranchSession;

import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default lock manager.
 */
public class DefaultLockManagerImpl implements LockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLockManagerImpl.class);

    private static final int BUCKET_PER_TABLE = 128;

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>>> LOCK_MAP = new ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>>>();

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>> dbLockMap = LOCK_MAP.get(resourceId);
        if (dbLockMap == null) {
            LOCK_MAP.putIfAbsent(resourceId, new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>>());
            dbLockMap = LOCK_MAP.get(resourceId);
        }
        ConcurrentHashMap<Map<String, Long>, Set<String>> bucketHolder = branchSession.getLockHolder();

        String lockKey = branchSession.getLockKey();
        if(StringUtils.isEmpty(lockKey)) {
            return true;
        }

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                branchSession.unlock();
                throw new ShouldNeverHappenException("Wrong format of LOCK KEYS: " + branchSession.getLockKey());
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            ConcurrentHashMap<Integer, Map<String, Long>> tableLockMap = dbLockMap.get(tableName);
            if (tableLockMap == null) {
                dbLockMap.putIfAbsent(tableName, new ConcurrentHashMap<Integer, Map<String, Long>>());
                tableLockMap = dbLockMap.get(tableName);
            }
            String[] pks = mergedPKs.split(",");
            for (String pk : pks) {
                int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
                Map<String, Long> bucketLockMap = tableLockMap.get(bucketId);
                if (bucketLockMap == null) {
                    tableLockMap.putIfAbsent(bucketId, new HashMap<String, Long>());
                    bucketLockMap = tableLockMap.get(bucketId);
                }
                synchronized (bucketLockMap) {
                    Long lockingTransactionId = bucketLockMap.get(pk);
                    if (lockingTransactionId == null) {
                        // No existing lock
                        bucketLockMap.put(pk, transactionId);
                        Set<String> keysInHolder = bucketHolder.get(bucketLockMap);
                        if (keysInHolder == null) {
                            bucketHolder.putIfAbsent(bucketLockMap, new ConcurrentSet<String>());
                            keysInHolder = bucketHolder.get(bucketLockMap);
                        }
                        keysInHolder.add(pk);

                    } else if (lockingTransactionId.longValue() == transactionId) {
                        // Locked by me
                        continue;
                    } else {
                        LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + lockingTransactionId);
                        branchSession.unlock(); // Release all acquired locks.
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean isLockable(long transactionId, String resourceId, String lockKey) throws TransactionException {
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>> dbLockMap = LOCK_MAP.get(resourceId);
        if (dbLockMap == null) {
            return true;
        }
        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                throw new ShouldNeverHappenException("Wrong format of LOCK KEYS: " + lockKey);
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            ConcurrentHashMap<Integer, Map<String, Long>> tableLockMap = dbLockMap.get(tableName);
            if (tableLockMap == null) {
                continue;
            }
            String[] pks = mergedPKs.split(",");
            for (String pk : pks) {
                int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
                Map<String, Long> bucketLockMap = tableLockMap.get(bucketId);
                if (bucketLockMap == null) {
                    continue;
                }
                Long lockingTransactionId = bucketLockMap.get(pk);
                if (lockingTransactionId == null || lockingTransactionId.longValue() == transactionId) {
                    // Locked by me
                    continue;
                } else {
                    LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + lockingTransactionId);
                    return false;
                }
            }
        }
        return true;
    }
}
