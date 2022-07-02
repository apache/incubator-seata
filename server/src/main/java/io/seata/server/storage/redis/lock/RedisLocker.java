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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.model.LockStatus;
import io.seata.core.store.LockDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.LuaParser;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import static io.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static io.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX;
import static io.seata.core.constants.RedisKeyConstants.DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX;
import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;
import static io.seata.server.storage.redis.LuaParser.LuaErrorStatus;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 * @author conghuhu
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

    private static final String STATUS = "status";

    private static final String ROW_KEY = "rowKey";

    private static final String LUA_PREFIX = "lua/redislocker/";

    private static final String ACQUIRE_LOCK_LUA_FILE_NAME = LUA_PREFIX + "acquireRedisLock.lua";

    private static final String RELEASE_LOCK_LUA_FILE_NAME = LUA_PREFIX + "releaseRedisLock.lua";

    private static final String UPDATE_LOCK_LUA_FILE_NAME = LUA_PREFIX + "updateLockStatus.lua";

    private static final String LOCKABLE_LUA_FILE_NAME = LUA_PREFIX + "isLockable.lua";

    /**
     * key filename
     * value LOCK_SHA_SCRIPT
     */
    private static final Map<String, String> LOCK_SHA_MAP = new HashMap<>(4);

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
        if (LOCK_SHA_MAP.isEmpty()) {
            loadLuaFile(ACQUIRE_LOCK_LUA_FILE_NAME, "acquire lock");
            loadLuaFile(RELEASE_LOCK_LUA_FILE_NAME, "release lock");
            loadLuaFile(UPDATE_LOCK_LUA_FILE_NAME, "update lock");
            loadLuaFile(LOCKABLE_LUA_FILE_NAME, "lockable");
        }
    }

    private void loadLuaFile(String fileName, String mode) {
        try {
            LOCK_SHA_MAP.putAll(LuaParser.getEvalShaMapFromFile(fileName));
        } catch (IOException e) {
            // if it fails to read the file, pipeline mode is used
            if (LOCK_SHA_MAP.get(fileName) != null) {
                LOCK_SHA_MAP.remove(fileName);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("redis locker: {} use pipeline mode", mode);
            }
        }
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
            String luaSHA = LOCK_SHA_MAP.get(ACQUIRE_LOCK_LUA_FILE_NAME);
            if (luaSHA != null) {
                return acquireLockByLua(jedis, rowLocks, luaSHA);
            } else {
                return acquireLockByPipeline(jedis, rowLocks, autoCommit, skipCheckLock);
            }
        }
    }

    private boolean acquireLockByPipeline(Jedis jedis, List<RowLock> rowLocks, boolean autoCommit,
        boolean skipCheckLock) {
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

    private boolean acquireLockByLua(Jedis jedis, List<RowLock> rowLocks, String luaSHA) {
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<LockDO> needLockDOs = rowLocks.stream()
            .map(this::convertToLockDO)
            .filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
            .collect(Collectors.toList());
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>();
        int size = needLockDOs.size();
        args.add(String.valueOf(size));
        // args index 2 placeholder
        args.add(null);
        args.add(needLockXid);
        for (LockDO lockDO : needLockDOs) {
            keys.add(buildLockKey(lockDO.getRowKey()));
            args.add(lockDO.getTransactionId().toString());
            args.add(lockDO.getBranchId().toString());
            args.add(lockDO.getResourceId());
            args.add(lockDO.getTableName());
            args.add(lockDO.getRowKey());
            args.add(lockDO.getPk());
        }
        String xidLockKey = buildXidLockKey(needLockXid);
        StringJoiner lockKeysString = new StringJoiner(ROW_LOCK_KEY_SPLIT_CHAR);
        needLockDOs.stream().map(lockDO -> buildLockKey(lockDO.getRowKey())).forEach(lockKeysString::add);

        keys.add(xidLockKey);
        keys.add(branchId.toString());
        args.add(lockKeysString.toString());
        // reset args index 2
        args.set(1, String.valueOf(args.size()));

        String result = (String) jedis.evalsha(luaSHA, keys, args);

        LuaParser.LuaResult luaResult = LuaParser.getObjectFromJson(result, LuaParser.LuaResult.class);

        // luaResult.getData() : xIdOwnLock
        if (luaResult.getSuccess() && luaResult.getData().equals(needLockXid)) {
            return true;
        } else {
            if (LuaErrorStatus.ANOTHER_ROLLBACKING.equals(luaResult.getStatus())) {
                // if a global lock is found in the Rollbacking state,the fail-fast code is returned directly.
                throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
            } else if (LuaErrorStatus.ANOTHER_HOLDING.equals(luaResult.getStatus())) {
                // means the rowKey is holding by another global transaction
                logGlobalLockConflictInfo(needLockXid, keys.get(0), luaResult.getData());
            }
            return false;
        }
    }

    private void logGlobalLockConflictInfo(String needLockXid, String lockKey, String xIdOwnLock) {
        LOGGER.info("tx:[{}] acquire Global lock failed. Global lock on [{}] is holding by xid {}", needLockXid, lockKey, xIdOwnLock);
    }

    @Override
    public boolean releaseLock(String xid) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            String luaSHA = LOCK_SHA_MAP.get(RELEASE_LOCK_LUA_FILE_NAME);
            if (luaSHA != null) {
                return doReleaseLockByLua(xidLockKey, null, luaSHA, jedis);
            } else {
                return doReleaseLockByPipeline(xidLockKey, null, jedis);
            }
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        if (branchId == null) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            String luaSHA = LOCK_SHA_MAP.get(RELEASE_LOCK_LUA_FILE_NAME);
            if (luaSHA != null) {
                return doReleaseLockByLua(xidLockKey, branchId, luaSHA, jedis);
            } else {
                return doReleaseLockByPipeline(xidLockKey, branchId, jedis);
            }
        }
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
            String luaSHA = LOCK_SHA_MAP.get(LOCKABLE_LUA_FILE_NAME);
            if (luaSHA != null) {
                return doIsLockableByLua(lockKeys, xid, luaSHA, jedis);
            } else {
                return doIsLockableByPipeline(lockKeys, xid, jedis);
            }
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            String luaSHA = LOCK_SHA_MAP.get(UPDATE_LOCK_LUA_FILE_NAME);
            if (luaSHA != null) {
                doUpdateLockStatusByLua(xidLockKey, lockStatus, luaSHA, jedis);
            } else {
                doUpdateLockStatusByPipeline(xidLockKey, lockStatus, jedis);
            }
        }
    }

    private boolean doIsLockableByLua(Set<String> lockKeys, String xid, String luaSHA, Jedis jedis) {
        ArrayList<String> keys = new ArrayList<>();
        keys.add(String.valueOf(lockKeys.size()));
        keys.addAll(lockKeys);
        ArrayList<String> args = new ArrayList<>();
        args.add(xid);
        String res = (String) jedis.evalsha(luaSHA, keys, args);
        return "true".equals(res);
    }

    private boolean doIsLockableByPipeline(Set<String> lockKeys, String xid, Jedis jedis) {
        try (Pipeline pipeline = jedis.pipelined()) {
            lockKeys.forEach(key -> pipeline.hget(key, XID));
            List<String> existedXids = (List<String>) (List) pipeline.syncAndReturnAll();
            return existedXids.stream().allMatch(existedXid -> existedXid == null || xid.equals(existedXid));
        }
    }

    private void doUpdateLockStatusByLua(String xidLockKey, LockStatus lockStatus, String luaSHA, Jedis jedis) {
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>();
        keys.add(xidLockKey);
        keys.add(STATUS);
        args.add(String.valueOf(lockStatus.getCode()));
        jedis.evalsha(luaSHA, keys, args);
    }

    private void doUpdateLockStatusByPipeline(String xidLockKey, LockStatus lockStatus, Jedis jedis) {
        Map<String, String> branchAndLockKeys = jedis.hgetAll(xidLockKey);
        if (CollectionUtils.isEmpty(branchAndLockKeys)) {
            return;
        }
        try (Pipeline pipeline = jedis.pipelined()) {
            if (CollectionUtils.isEmpty(branchAndLockKeys)) {
                return;
            }
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

    private boolean doReleaseLockByPipeline(String xidLockKey, Long branchId, Jedis jedis) {
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

    private boolean doReleaseLockByLua(String xidLockKey, Long branchId, String luaSHA, Jedis jedis) {
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>();
        keys.add(xidLockKey);
        Optional.ofNullable(branchId).ifPresent(val -> keys.add(String.valueOf(val)));
        jedis.evalsha(luaSHA, keys, args);
        return true;
    }

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + rowKey;
    }

}
