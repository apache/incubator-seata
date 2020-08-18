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

import static io.seata.common.Constants.SEMICOLON;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 */
public class RedisLocker extends AbstractLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLocker.class);

    private static final Integer DEFAULT_QUERY_LIMIT = 100;

    private static final Integer SUCCEED = 1;

    private static final Integer FAILED = 0;

    private static final String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX = "SEATA_LOCK_XID_";

    private static final String XID = "xid";

    private static final String TRANSACTION_ID = "transactionId";

    private static final String BRANCH_ID = "branchId";

    private static final String RESOURCE_ID = "resourceId";

    private static final String TABLE_NAME = "tableName";

    private static final String PK = "pk";

    private static final String ROW_KEY = "rowKey";


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
            return true;
        }
        Integer status = SUCCEED;
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();

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
            needLockKeys.stream().forEachOrdered(
                    needLockKey -> pipeline1.hmget(needLockKey, XID, TABLE_NAME, PK, BRANCH_ID));
            List<Object> existedObjs = pipeline1.syncAndReturnAll();
            List<List<String>> existedLockInfos = (List<List<String>>) (List) existedObjs;
            Map<String, LockDO> needAddLock = new HashMap<>(needLockKeys.size(), 1);

            for (int i = 0; i < needLockKeys.size(); i++) {
                List<String> existedLockInfo = existedLockInfos.get(i);
                if (CollectionUtils.isEmpty(existedLockInfo) ||
                        existedLockInfo.stream().allMatch(info -> info == null)) {
                    //If empty,we need to lock this row
                    needAddLock.put(needLockKeys.get(i), needLockDOS.get(i));
                } else {
                    if (!StringUtils.equals(existedLockInfo.get(0), needLockXid)) {
                        //If not equals,means the rowkey is holding by another global transaction
                        LOGGER.error(
                                "Acquire lock failed,Global lock on [{}:{}] is holding by xid {} branchId {}",
                                existedLockInfo.get(1), existedLockInfo.get(2),
                                existedLockInfo.get(0), existedLockInfo.get(3));
                        return false;
                    }
                }
            }

            if (needAddLock.isEmpty()) {
                return true;
            }
            Pipeline pipeline = jedis.pipelined();
            List<String> readyKeys = new ArrayList<>();
            needAddLock.forEach((key, value) -> {
                pipeline.hsetnx(key, XID, value.getXid());
                pipeline.hsetnx(key, TRANSACTION_ID, value.getTransactionId().toString());
                pipeline.hsetnx(key, BRANCH_ID, value.getBranchId().toString());
                pipeline.hsetnx(key, RESOURCE_ID, value.getResourceId());
                pipeline.hsetnx(key, TABLE_NAME, value.getTableName());
                pipeline.hsetnx(key, ROW_KEY, value.getRowKey());
                pipeline.hsetnx(key, PK, value.getPk());
                readyKeys.add(key);
            });
            List<Object> results = pipeline.syncAndReturnAll();
            List<Integer> results2 = (List<Integer>) (List) results;
            List<List<Integer>> partitions = Lists.partition(results2, 7);

            Set<String> successSet = new HashSet<>();
            for (int i = 0; i < partitions.size(); i++) {
                String key = readyKeys.get(i);
                if (partitions.get(i).contains(FAILED)) {
                    status = FAILED;
                } else {
                    successSet.add(key);
                }
            }

            //If someone has failed,all the lockkey which has been added need to be delete.
            if (FAILED.equals(status)) {
                if (successSet.size() > 0) {
                    Pipeline pipeline2 = jedis.pipelined();
                    successSet.forEach(locKey ->
                            pipeline2.hdel(locKey, XID, TRANSACTION_ID, BRANCH_ID, RESOURCE_ID,
                                    TABLE_NAME, ROW_KEY, PK));
                    pipeline2.sync();
                }
                return false;
            }
            String xidLockKey = buildXidLockKey(needLockXid);
            StringJoiner lockKeysString = new StringJoiner(SEMICOLON);
            needLockKeys.stream().forEach(lockKey -> lockKeysString.add(lockKey));
            jedis.hsetnx(xidLockKey, branchId.toString(), lockKeysString.toString());
            return true;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        String currentXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<String> needReleaseKeys = new ArrayList<>(rowLocks.size());
        List<LockDO> needReleaseLocks = convertToLockDO(rowLocks);
        needReleaseLocks.stream()
                .forEach(lockDO -> needReleaseKeys.add(buildLockKey(lockDO.getRowKey())));

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Pipeline pipelined = jedis.pipelined();
            needReleaseKeys.stream()
                    .forEach(key -> pipelined.hmget(key, XID, TABLE_NAME, PK, BRANCH_ID));
            List<Object> existedObjs = pipelined.syncAndReturnAll();
            List<List<String>> existedLockInfos = (List<List<String>>) (List) existedObjs;

            for (int i = 0; i < existedLockInfos.size(); i++) {
                List<String> existedLockInfo = existedLockInfos.get(i);
                if (CollectionUtils.isNotEmpty(existedLockInfo) ||
                        existedLockInfo.stream().allMatch(info -> info != null)) {
                    if (!StringUtils.equals(currentXid, existedLockInfo.get(0))) {
                        LOGGER.error(
                                "Release lock failed,Global lock on [{}:{}] is holding by xid {} branchId {}",
                                existedLockInfo.get(1), existedLockInfo.get(2),
                                existedLockInfo.get(0), existedLockInfo.get(3));
                        return false;
                    }
                }
            }

            Pipeline pipelined1 = jedis.pipelined();
            needReleaseKeys.stream().forEach(key ->
                    pipelined1.hdel(key, XID, TRANSACTION_ID, BRANCH_ID, RESOURCE_ID,
                            TABLE_NAME, ROW_KEY, PK));
            pipelined1.hdel(buildXidLockKey(currentXid), branchId.toString());
            pipelined1.sync();
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            String[] branchIdsArray = new String[branchIds.size()];
            for (int i = 0; i < branchIds.size(); i++) {
                branchIdsArray[i] = branchIds.get(i).toString();
            }
            List<String> rowKeys = jedis.hmget(xidLockKey, branchIdsArray);

            if (CollectionUtils.isNotEmpty(rowKeys)) {
                Pipeline pipelined = jedis.pipelined();
                pipelined.hdel(xidLockKey, branchIdsArray);
                rowKeys.stream().forEach(rowKeyStr -> {
                    if (StringUtils.isNotEmpty(rowKeyStr)) {
                        if (rowKeyStr.contains(SEMICOLON)) {
                            String[] keys = rowKeyStr.split(SEMICOLON);
                            Arrays.asList(keys).stream().forEach(rowKey -> {
                                if (StringUtils.isNotEmpty(rowKey)) {
                                    pipelined.hdel(rowKey, XID, TRANSACTION_ID, BRANCH_ID,
                                            RESOURCE_ID, TABLE_NAME, ROW_KEY, PK);
                                }
                            });
                        } else {
                            pipelined.hdel(rowKeyStr, XID, TRANSACTION_ID, BRANCH_ID, RESOURCE_ID,
                                    TABLE_NAME, ROW_KEY, PK);
                        }

                    }
                });
                pipelined.sync();
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

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowKey;
    }

}
