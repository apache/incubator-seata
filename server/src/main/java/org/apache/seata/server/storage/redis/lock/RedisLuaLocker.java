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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.LambdaUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.apache.seata.server.storage.redis.LuaParser;

import static org.apache.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 */
public class RedisLuaLocker extends RedisLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLuaLocker.class);

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

    public RedisLuaLocker() {
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
    public boolean acquireLock(List<RowLock> rowLocks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }

        String luaSHA = LOCK_SHA_MAP.get(ACQUIRE_LOCK_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.acquireLock(rowLocks, autoCommit, skipCheckLock);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String needLockXid = rowLocks.get(0).getXid();
            Long branchId = rowLocks.get(0).getBranchId();
            List<LockDO> needLockDOs = rowLocks.stream()
                .map(this::convertToLockDO)
                .filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                .collect(Collectors.toList());
            List<String> keys = new ArrayList<>();
            List<String> args = new ArrayList<>();
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

            String result = (String)LuaParser.jedisEvalSha(jedis, luaSHA, ACQUIRE_LOCK_LUA_FILE_NAME, keys, args);

            LuaParser.LuaResult luaResult = LuaParser.getObjectFromJson(result, LuaParser.LuaResult.class);

            // luaResult.getData() : xIdOwnLock
            if (luaResult.getSuccess() && luaResult.getData().equals(needLockXid)) {
                return true;
            } else {
                if (LuaParser.LuaErrorStatus.ANOTHER_ROLLBACKING.equals(luaResult.getStatus())) {
                    // if a global lock is found in the Rollbacking state,the fail-fast code is returned directly.
                    throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
                } else if (LuaParser.LuaErrorStatus.ANOTHER_HOLDING.equals(luaResult.getStatus())) {
                    // means the rowKey is holding by another global transaction
                    logGlobalLockConflictInfo(needLockXid, keys.get(0), luaResult.getData());
                }
                return false;
            }
        }
    }

    @Override
    public boolean releaseLock(String xid) {
        String luaSHA = LOCK_SHA_MAP.get(RELEASE_LOCK_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.releaseLock(xid);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            List<String> keys = new ArrayList<>();
            List<String> args = Collections.emptyList();
            keys.add(xidLockKey);
            LuaParser.jedisEvalSha(jedis, luaSHA, RELEASE_LOCK_LUA_FILE_NAME, keys, args);
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        if (branchId == null) {
            return true;
        }
        String luaSHA = LOCK_SHA_MAP.get(RELEASE_LOCK_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.releaseLock(xid, branchId);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            List<String> keys = new ArrayList<>();
            List<String> args = Collections.emptyList();
            keys.add(xidLockKey);
            keys.add(String.valueOf(branchId));
            LuaParser.jedisEvalSha(jedis, luaSHA, RELEASE_LOCK_LUA_FILE_NAME, keys, args);
            return true;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        String luaSHA = LOCK_SHA_MAP.get(LOCKABLE_LUA_FILE_NAME);
        if (luaSHA == null) {
            return super.isLockable(rowLocks);
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            Set<String> lockKeys = new HashSet<>();
            for (LockDO rowlock : locks) {
                lockKeys.add(buildLockKey(rowlock.getRowKey()));
            }
            String xid = rowLocks.get(0).getXid();
            List<String> keys = new ArrayList<>();
            keys.add(String.valueOf(lockKeys.size()));
            keys.addAll(lockKeys);
            List<String> args = new ArrayList<>();
            args.add(xid);
            String res = (String)LuaParser.jedisEvalSha(jedis, luaSHA, LOCKABLE_LUA_FILE_NAME, keys, args);
            return "true".equals(res);
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        String luaSHA = LOCK_SHA_MAP.get(UPDATE_LOCK_LUA_FILE_NAME);
        if (luaSHA == null) {
            super.updateLockStatus(xid, lockStatus);
            return;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            List<String> keys = new ArrayList<>();
            List<String> args = new ArrayList<>();
            keys.add(xidLockKey);
            keys.add(STATUS);
            args.add(String.valueOf(lockStatus.getCode()));
            LuaParser.jedisEvalSha(jedis, luaSHA, UPDATE_LOCK_LUA_FILE_NAME, keys, args);
        }
    }
}
