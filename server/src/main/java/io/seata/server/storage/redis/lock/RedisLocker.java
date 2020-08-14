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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 */
public class RedisLocker extends AbstractLocker {

    private static final Integer DEFAULT_QUERY_LIMIT = 100;

    private static final String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX = "SEATA_LOCK_XID_";

    private static final String XID = "xid";

    private static final String TRANSACTION_ID = "transactionId";

    private static final String BRANCH_ID = "branchId";

    private static final String RESOURCE_ID = "resourceId";

    private static final String TABLE_NAME = "tableName";

    private static final String PK = "pk";

    private static final String ROW_KEY = "rowKey";

    private static final String OK = "OK";

    /**
     * The query limit.
     */
    private int logQueryLimit;

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
        logQueryLimit =
                ConfigurationFactory.getInstance()
                        .getInt(ConfigurationKeys.STORE_REDIS_QUERY_LIMIT, DEFAULT_QUERY_LIMIT);
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        Set<String> successList = new HashSet<>();
        String status = OK;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> needLockDOS = convertToLockDO(rowLocks);
            if (needLockDOS.size() >= 1) {
                needLockDOS = needLockDOS.stream().
                        filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                        .collect(Collectors.toList());
            }
            List<String> needLockKeys = new ArrayList<>();
            needLockDOS.forEach(lockDO -> needLockKeys.add(buildLockKey(lockDO.getRowKey())));

            Pipeline pipeline1 = jedis.pipelined();
            needLockKeys.stream().forEachOrdered(needLockKey -> pipeline1.hget(needLockKey, XID));
            List<Object> existedXidObjs = pipeline1.syncAndReturnAll();
            List<String> existedXids = (List<String>) (List) existedXidObjs;
            Map<String, LockDO> map = new HashMap<>(needLockKeys.size(), 1);

            String needLockXid = rowLocks.get(0).getXid();
            for (int i = 0; i < needLockKeys.size(); i++) {
                String existedXid = existedXids.get(i);
                if (StringUtils.isEmpty(existedXid)) {
                    //If empty,we need to lock this row
                    map.put(needLockKeys.get(i), needLockDOS.get(i));
                } else {
                    if (!StringUtils.equals(existedXid, needLockXid)) {
                        //If not equals,means the rowkey is holding by another global transaction
                        return false;
                    }
                }
            }

            if (map.isEmpty()) {
                return true;
            }
            Pipeline pipeline = jedis.pipelined();
            List<String> readyKeys = new ArrayList<>();
            map.forEach((key, value) -> {
                Map<String, String> lockMap = new HashMap<>(8);
                lockMap.put(XID, value.getXid());
                lockMap.put(TRANSACTION_ID, value.getTransactionId().toString());
                lockMap.put(BRANCH_ID, value.getBranchId().toString());
                lockMap.put(RESOURCE_ID, value.getResourceId());
                lockMap.put(TABLE_NAME, value.getTableName());
                lockMap.put(ROW_KEY, value.getRowKey());
                lockMap.put(PK, value.getPk());
                pipeline.hmset(key, lockMap);
                readyKeys.add(key);
            });
            List<Object> results = pipeline.syncAndReturnAll();

            for (int i = 0; i < results.size(); i++) {
                String result = results.get(i).toString();
                String key = readyKeys.get(i);
                if (!OK.equalsIgnoreCase(result)) {
                    status = result;
                } else {
                    successList.add(key);
                }
            }

            //If someone has failed,all the lockkey which has been added need to be delete.
            if (!OK.equalsIgnoreCase(status)) {
                String[] rms = successList.toArray(new String[0]);
                if (rms.length > 0) {
                    Pipeline pipeline2 = jedis.pipelined();
                    Arrays.stream(rms).forEach(locKey ->
                            pipeline2.hdel(locKey, XID, TRANSACTION_ID, BRANCH_ID, RESOURCE_ID,
                                    TABLE_NAME, ROW_KEY, PK));
                    pipeline2.sync();
                }
                return false;
            } else {
                try {
                    String xidLockKey = buildXidLockKey(needLockDOS.get(0).getXid());
                    jedis.lpush(xidLockKey, readyKeys.toArray(new String[0]));
                } catch (Exception e) {
                    return false;
                }
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
            String key = buildLockKey(locks.get(i).getRowKey());
            keys[i] = key;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(locks.get(0).getXid());
            Pipeline pipeline = jedis.pipelined();
            pipeline.del(keys);
            Arrays.stream(keys).forEach(key -> pipeline.lrem(xidLockKey, 0, key));
            pipeline.sync();
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
            String lockListKey = buildXidLockKey(xid);
            Set<String> keys = lRange(jedis, lockListKey);
            if (CollectionUtils.isNotEmpty(keys)) {
                List<String> delKeys = new ArrayList<>();

                Pipeline pipelined = jedis.pipelined();
                keys.stream().forEach(key -> {
                    pipelined.hmget(key, BRANCH_ID, ROW_KEY);
                });
                List<Object> nestedArray = pipelined.syncAndReturnAll();

                nestedArray.stream().forEach(arr -> {
                    List<String> branchIdAndRowKey = (List<String>) arr;
                    String branchId = branchIdAndRowKey.get(0);
                    String rowkey = branchIdAndRowKey.get(1);
                    if (StringUtils.isNotEmpty(branchId)
                            && branchIds.contains(Long.valueOf(branchId))) {
                        delKeys.add(buildLockKey(rowkey));
                    }
                });
                if (CollectionUtils.isNotEmpty(delKeys)) {
                    Pipeline pipelined1 = jedis.pipelined();
                    delKeys.stream().forEach(delkey -> {
                        pipelined1.hdel(delkey, XID, TRANSACTION_ID, BRANCH_ID, RESOURCE_ID,
                                TABLE_NAME, ROW_KEY, PK);
                        pipelined1.lrem(lockListKey, 0, delkey);
                    });
                    pipelined1.sync();
                }
            }
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        List<Long> branchIds = new ArrayList<>();
        branchIds.add(branchId);
        return releaseLock(xid, branchIds);
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            Set<String> lockKeys = new HashSet<>();
            for (LockDO rowlock : locks) {
                lockKeys.add(buildLockKey(rowlock.getRowKey()));
            }

            String xid = rowLocks.get(0).getXid();
            Pipeline pipeline = jedis.pipelined();
            lockKeys.stream().forEach(key -> pipeline.hget(key, XID));
            List<Object> existedRowLockXid = pipeline.syncAndReturnAll();
            List<String> existedXids = (List<String>) (List) existedRowLockXid;
            return existedXids.stream().allMatch(existedXid -> xid.equals(existedXid));
        }
    }

    private Set<String> lRange(Jedis jedis, String key) {
        Set<String> keys = new HashSet<>();
        List<String> redisLockJson;
        int start = 0;
        int stop = logQueryLimit;
        do {
            redisLockJson = jedis.lrange(key, start, stop);
            keys.addAll(redisLockJson);
            start = keys.size();
            stop = start + logQueryLimit;
        } while (CollectionUtils.isNotEmpty(redisLockJson));
        return keys;
    }

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowKey;
    }

}
