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
package org.apache.seata.server.storage.redis.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.LambdaUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import static org.apache.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static org.apache.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX;
import static org.apache.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX;
import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * The redis lock store operation
 *
 */
public class RedisLocker extends AbstractLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLocker.class);

    private static final Integer SUCCEED = 1;

    private static final Integer FAILED = 0;

    private static final String XID = "xid";

    private static final String TRANSACTION_ID = "transactionId";

    private static final String BRANCH_ID = "branchId";

    private static final String RESOURCE_ID = "resourceId";

    private static final String TABLE_NAME = "tableName";

    private static final String PK = "pk";

    protected static final String STATUS = "status";

    private static final String ROW_KEY = "rowKey";

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        return acquireLock(rowLocks, true, false);
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String needLockXid = rowLocks.get(0).getXid();
            Long branchId = rowLocks.get(0).getBranchId();
            List<LockDO> needLockDOS = convertToLockDO(rowLocks);
            if (needLockDOS.size() > 1) {
                needLockDOS =
                    needLockDOS.stream().filter(LambdaUtils.distinctByKey(LockDO::getRowKey)).collect(Collectors.toList());
            }
            List<String> needLockKeys = new ArrayList<>();
            needLockDOS.forEach(lockDO -> needLockKeys.add(buildLockKey(lockDO.getRowKey())));
            Map<String, LockDO> needAddLock = new HashMap<>(needLockKeys.size(), 1);

            if (!skipCheckLock) {
                Pipeline pipeline1 = jedis.pipelined();
                needLockKeys.stream().forEachOrdered(needLockKey -> {
                    pipeline1.hget(needLockKey, XID);
                    if (!autoCommit) {
                        pipeline1.hget(needLockKey, STATUS);
                    }
                });
                List<List<String>> existedLockInfos =
                    Lists.partition((List<String>) (List) pipeline1.syncAndReturnAll(), autoCommit ? 1 : 2);

                // When the local transaction and the global transaction are enabled,
                // the branch registration fails to acquire the global lock,
                // the lock holder is in the second-stage rollback,
                // and the branch registration fails to be retried quickly,
                // because the retry with the local transaction does not release the database lock ,
                // resulting in a two-phase rollback wait.
                // Therefore, if a global lock is found in the Rollbacking state,
                // the fail-fast code is returned directly.
                if (!autoCommit) {
                    boolean hasRollBackingLock = existedLockInfos.parallelStream().anyMatch(
                        result -> StringUtils.equals(result.get(1), String.valueOf(LockStatus.Rollbacking.getCode())));
                    if (hasRollBackingLock) {
                        throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
                    }
                }

                // The logic is executed here, there must be a lock without Rollbacking status when autoCommit equals false
                for (int i = 0; i < needLockKeys.size(); i++) {
                    List<String> results = existedLockInfos.get(i);
                    String existedLockXid = CollectionUtils.isEmpty(results) ? null : existedLockInfos.get(i).get(0);
                    if (StringUtils.isEmpty(existedLockXid)) {
                        // If empty,we need to lock this row
                        needAddLock.put(needLockKeys.get(i), needLockDOS.get(i));
                    } else {
                        if (!StringUtils.equals(existedLockXid, needLockXid)) {
                            // If not equals,means the rowkey is holding by another global transaction
                            logGlobalLockConflictInfo(needLockXid, needLockKeys.get(i), existedLockXid);
                            return false;
                        }
                    }
                }
                if (needAddLock.isEmpty()) {
                    return true;
                }
            }

            Pipeline pipeline = jedis.pipelined();
            List<String> readyKeys = new ArrayList<>(needAddLock.keySet());
            needAddLock.forEach((key, value) -> {
                pipeline.hsetnx(key, XID, value.getXid());
                pipeline.hsetnx(key, TRANSACTION_ID, value.getTransactionId().toString());
                pipeline.hsetnx(key, BRANCH_ID, value.getBranchId().toString());
                pipeline.hset(key, ROW_KEY, value.getRowKey());
                pipeline.hset(key, RESOURCE_ID, value.getResourceId());
                pipeline.hset(key, TABLE_NAME, value.getTableName());
                pipeline.hset(key, PK, value.getPk());
            });
            List<Integer> results = (List<Integer>) (List) pipeline.syncAndReturnAll();
            List<List<Integer>> partitions = Lists.partition(results, 7);

            ArrayList<String> success = new ArrayList<>(partitions.size());
            Integer status = SUCCEED;
            for (int i = 0; i < partitions.size(); i++) {
                if (Objects.equals(partitions.get(i).get(0), FAILED)) {
                    status = FAILED;
                } else {
                    success.add(readyKeys.get(i));
                }
            }

            // If someone has failed,all the lockkey which has been added need to be delete.
            if (FAILED.equals(status)) {
                if (success.size() > 0) {
                    jedis.del(success.toArray(new String[0]));
                }
                return false;
            }
            String xidLockKey = buildXidLockKey(needLockXid);
            StringJoiner lockKeysString = new StringJoiner(ROW_LOCK_KEY_SPLIT_CHAR);
            needLockKeys.forEach(lockKeysString::add);
            jedis.hset(xidLockKey, branchId.toString(), lockKeysString.toString());
            return true;
        }
    }

    protected void logGlobalLockConflictInfo(String needLockXid, String lockKey, String xIdOwnLock) {
        LOGGER.info("tx:[{}] acquire Global lock failed. Global lock on [{}] is holding by xid {}", needLockXid, lockKey, xIdOwnLock);
    }

    @Override
    public boolean releaseLock(String xid) {
        return doReleaseLock(xid, null);
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        if (branchId == null) {
            return true;
        }
        return doReleaseLock(xid, branchId);
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            Set<String> lockKeys = new HashSet<>();
            for (LockDO rowlock : locks) {
                lockKeys.add(buildLockKey(rowlock.getRowKey()));
            }

            String xid = rowLocks.get(0).getXid();
            try (Pipeline pipeline = jedis.pipelined()) {
                lockKeys.forEach(key -> pipeline.hget(key, XID));
                List<String> existedXids = (List<String>) (List) pipeline.syncAndReturnAll();
                return existedXids.stream().allMatch(existedXid -> existedXid == null || xid.equals(existedXid));
            }
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            Map<String, String> branchAndLockKeys = jedis.hgetAll(xidLockKey);
            if (CollectionUtils.isEmpty(branchAndLockKeys)) {
                return;
            }
            try (Pipeline pipeline = jedis.pipelined()) {
                branchAndLockKeys.values()
                    .forEach(k -> {
                        if (StringUtils.isNotEmpty(k)) {
                            if (k.contains(ROW_LOCK_KEY_SPLIT_CHAR)) {
                                String[] keys = k.split(ROW_LOCK_KEY_SPLIT_CHAR);
                                for (String key : keys) {
                                    pipeline.hset(key, STATUS, String.valueOf(lockStatus.getCode()));
                                }
                            } else {
                                pipeline.hset(k, STATUS, String.valueOf(lockStatus.getCode()));
                            }
                        }
                    });
                pipeline.sync();
            }
        }
    }

    private boolean doReleaseLock(String xid, Long branchId) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            final List<String> rowKeys = new ArrayList<>();
            if (null == branchId) {
                Map<String, String> rowKeyMap = jedis.hgetAll(xidLockKey);
                rowKeyMap.forEach((branch, rowKey) -> rowKeys.add(rowKey));
            } else {
                rowKeys.add(jedis.hget(xidLockKey, branchId.toString()));
            }
            if (CollectionUtils.isNotEmpty(rowKeys)) {
                Pipeline pipelined = jedis.pipelined();
                if (null == branchId) {
                    pipelined.del(xidLockKey);
                } else {
                    pipelined.hdel(xidLockKey, branchId.toString());
                }
                rowKeys.forEach(rowKeyStr -> {
                    if (StringUtils.isNotEmpty(rowKeyStr)) {
                        if (rowKeyStr.contains(ROW_LOCK_KEY_SPLIT_CHAR)) {
                            String[] keys = rowKeyStr.split(ROW_LOCK_KEY_SPLIT_CHAR);
                            pipelined.del(keys);
                        } else {
                            pipelined.del(rowKeyStr);
                        }
                    }
                });
                pipelined.sync();
            }
            return true;
        }
    }

    protected String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid;
    }

    protected String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + rowKey;
    }

}
