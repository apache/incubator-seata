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
package io.seata.server.storage.redis.lock;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;

import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.session.BranchSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;

public class RedisLocker extends AbstractLocker {

    private static Integer DEFAULT_SECONDS = 30;

    private static String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";
    /**
     * The Branch session.
     */
    protected BranchSession branchSession = null;

    /**
     * Instantiates a new Memory locker.
     *
     * @param branchSession
     *            the branch session
     */
    public RedisLocker(BranchSession branchSession) {
        this.branchSession = branchSession;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        List<String> successList = new ArrayList<>();
        long status = 0;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            for (LockDO lock : locks) {
                status = jedis.setnx(DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey(), JSON.toJSONString(lock));
                if (status == 1) {
                    successList.add(DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey());
                    jedis.expire(DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey(), DEFAULT_SECONDS);
                } else {
                    break;
                }
            }
            if (status != 1) {
                jedis.del(successList.toArray(new String[successList.size()]));
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        String[] keys = new String[rowLocks.size()];
        List<LockDO> locks = convertToLockDO(rowLocks);
        for (int i = 0; i < locks.size(); i++) {
            keys[i] = DEFAULT_REDIS_SEATA_LOCK_PREFIX + locks.get(i).getRowKey();
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(keys);
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            // no lock
            return true;
        }
        try {
            return lockStore.unLock(xid, branchIds);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}, branchIds:{}", xid, CollectionUtils.toString(branchIds), t);
            return false;
        }
    }
    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            for (LockDO rowlock : locks) {
                String rowlockJson = jedis.get(DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowlock.getRowKey());
                if (StringUtils.isNotBlank(rowlockJson)) {
                    LockDO lock = JSON.parseObject(rowlockJson, LockDO.class);
                    if (!lock.getXid().equals(rowlock.getXid())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    /**
     * Convert to lock do list.
     *
     * @param locks the locks
     * @return the list
     */
    protected List<LockDO> convertToLockDO(List<RowLock> locks) {
        List<LockDO> lockDOs = new ArrayList<>();
        if (CollectionUtils.isEmpty(locks)) {
            return lockDOs;
        }
        for (RowLock rowLock : locks) {
            LockDO lockDO = new LockDO();
            lockDO.setBranchId(rowLock.getBranchId());
            lockDO.setPk(rowLock.getPk());
            lockDO.setResourceId(rowLock.getResourceId());
            lockDO.setRowKey(getRowKey(rowLock.getResourceId(), rowLock.getTableName(), rowLock.getPk()));
            lockDO.setXid(rowLock.getXid());
            lockDO.setTransactionId(rowLock.getTransactionId());
            lockDO.setTableName(rowLock.getTableName());
            lockDOs.add(lockDO);
        }
        return lockDOs;
    }

}
