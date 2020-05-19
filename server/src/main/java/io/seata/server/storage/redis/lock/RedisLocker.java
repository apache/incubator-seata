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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.alibaba.fastjson.JSON;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;

/**
 * @author funkye
 */
public class RedisLocker extends AbstractLocker {

    private static final Integer DEFAULT_SECONDS = 30;

    private static final String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX = "SEATA_LOCK_XID_";

    /**
     * Instantiates a new Redis locker.
     *
     */
    public RedisLocker() {}

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
                String key = getLockKey(lock.getRowKey());
                status = jedis.setnx(key, JSON.toJSONString(lock));
                if (status == 1) {
                    successList.add(key);
                    jedis.lpush(getXidLockKey(lock.getXid()), key);
                    jedis.expire(key, DEFAULT_SECONDS);
                } else {
                    break;
                }
            }
            if (status != 1) {
                String[] rms = successList.toArray(new String[successList.size()]);
                if (rms.length > 0) {
                    jedis.del(rms);
                }
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
            String key = getLockKey(locks.get(i).getRowKey());
            keys[i] = key;
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
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String lockListKey = getXidLockKey(xid);
            Set<String> keys = lRange(jedis, lockListKey);
            if (null != keys && keys.size() > 0) {
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    LockDO lock = JSON.parseObject(jedis.get(key), LockDO.class);
                    for (int i = 0; i < branchIds.size(); i++) {
                        if (null != lock && Objects.equals(lock.getBranchId(), branchIds.get(i))) {
                            jedis.del(key);
                            jedis.lrem(lockListKey, 0, key);
                            it.remove();
                        }
                    }
                }
                if (keys.size() == 0) {
                    jedis.del(lockListKey);
                }
            }
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String lockListKey = getXidLockKey(xid);
            Set<String> keys = lRange(jedis, lockListKey);
            if (null != keys && keys.size() > 0) {
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    LockDO lock = JSON.parseObject(jedis.get(key), LockDO.class);
                    if (null != lock && Objects.equals(branchId, lock.getBranchId())) {
                        jedis.del(key);
                        jedis.lrem(lockListKey, 0, key);
                        it.remove();
                    }
                }
                if (keys.size() == 0) {
                    jedis.del(lockListKey);
                }
            }
            return true;
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
                String rowlockJson = jedis.get(getLockKey(rowlock.getRowKey()));
                if (StringUtils.isNotBlank(rowlockJson)) {
                    LockDO lock = JSON.parseObject(rowlockJson, LockDO.class);
                    if (null != lock && !Objects.equals(lock.getXid(), rowlock.getXid())) {
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
     * @param locks
     *            the locks
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

    private Set<String> lRange(Jedis jedis, String Key) {
        Set<String> keys = new HashSet<>();
        List<String> redisLockJson = null;
        int start = 0;
        int stop = 100;
        for (;;) {
            redisLockJson = jedis.lrange(Key, start, stop);
            if (null != redisLockJson) {
                keys.addAll(redisLockJson);
                start = keys.size();
                stop = start + 100;
            } else {
                break;
            }
        }
        return keys;
    }

    private String getXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX + xid;
    }

    private String getLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowKey;
    }

}
