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
package io.seata.server.lock.memory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.XID;
import io.netty.util.internal.ConcurrentSet;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.store.LockDO;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The memory lock manager.
 *
 * @author sharajava
 */
@LoadLevel(name="memory")
public class MemoryLockManagerImpl extends AbstractLockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLockManagerImpl.class);

    private static final int BUCKET_PER_TABLE = 128;

    private static final
    ConcurrentHashMap<String/* resourceId */,
            ConcurrentHashMap<String/* tableName */,
                    ConcurrentHashMap<Integer/* bucketId */,
                            Map<String/* pk */, Long/* transactionId */>>>>
            LOCK_MAP = new ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>>>();

    /**
     * Instantiates a new Memory lock manager.
     */
    public MemoryLockManagerImpl(){
    }

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            //no lock
            return true;
        }
        //get locks of branch
        List<LockDO> locks = collectRowLocks(branchSession);
        if(CollectionUtils.isEmpty(locks)){
            //no lock
            return true;
        }
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        ConcurrentHashMap<Map<String, Long>, Set<String>> bucketHolder = branchSession.getLockHolder();
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>> dbLockMap = LOCK_MAP.get(resourceId);
        if (dbLockMap == null) {
            LOCK_MAP.putIfAbsent(resourceId, new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Map<String, Long>>>());
            dbLockMap = LOCK_MAP.get(resourceId);
        }

        for(LockDO lock : locks){
            String tableName = lock.getTableName();
            String pk = lock.getPk();
            ConcurrentHashMap<Integer, Map<String, Long>> tableLockMap = dbLockMap.get(tableName);
            if (tableLockMap == null) {
                dbLockMap.putIfAbsent(tableName, new ConcurrentHashMap<Integer, Map<String, Long>>());
                tableLockMap = dbLockMap.get(tableName);
            }
            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            Map<String, Long> bucketLockMap = tableLockMap.get(bucketId);
            if (bucketLockMap == null) {
                tableLockMap.putIfAbsent(bucketId, new ConcurrentHashMap<String, Long>());
                bucketLockMap = tableLockMap.get(bucketId);
            }
            synchronized (bucketLockMap) {
                Long lockingTransactionId = bucketLockMap.get(pk);
                if (lockingTransactionId == null) {
                    //No existing lock
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
        return true;
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        Long transactionId = XID.getTransactionId(xid);
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

    @Override
    public boolean unLock(BranchSession branchSession) throws TransactionException {
        ConcurrentHashMap<Map<String, Long>, Set<String>> lockHolder = branchSession.getLockHolder();
        if (lockHolder.size() == 0) {
            return true;
        }
        Iterator<Map.Entry<Map<String, Long>, Set<String>>> it = lockHolder.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Map<String, Long>, Set<String>> entry = it.next();
            Map<String, Long> bucket = entry.getKey();
            Set<String> keys = entry.getValue();
            synchronized (bucket) {
                for (String key : keys) {
                    Long v = bucket.get(key);
                    if (v == null) {
                        continue;
                    }
                    if (v.longValue() == branchSession.getTransactionId()) {
                        bucket.remove(key);
                    }
                }
            }
        }
        lockHolder.clear();
        return true;
    }

    @Override
    public void cleanAllLocks() throws TransactionException {
        LOCK_MAP.clear();
    }

}
