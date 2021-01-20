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
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import static io.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 */
public class RedisLocker extends AbstractLocker {

    private static final Integer SUCCEED = 1;

    private static final Integer FAILED = 0;

    private static final String DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX = "SEATA_ROW_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX = "SEATA_GLOBAL_LOCK";

    private static final String XID = "xid";

    private static final String TRANSACTION_ID = "transactionId";

    private static final String BRANCH_ID = "branchId";

    private static final String RESOURCE_ID = "resourceId";

    private static final String TABLE_NAME = "tableName";

    private static final String PK = "pk";

    private static final String ROW_KEY = "rowKey";

    private static final String ACQUIRE_LOCK = "local array = {}; local result; local keySize = ARGV[1]; local argSize = ARGV[2];" +
            // Loop through all keys to see if they can be used , when a key is not available, exit
            "for i= 1, keySize do result = redis.call('HGET',KEYS[i],'xid');" +
            "if (not result) then array[i]='no' else if (result ~= ARGV[3]) then return 0 else array[i]= 'yes' end end end " +
            "for i =1, keySize do if(array[i] == 'no') then " +
            "redis.call('HMSET',KEYS[i],'xid',ARGV[(i-1)*7+4]" +
            ",'transactionId',ARGV[(i-1)*7+5]" +
            ",'branchId',ARGV[(i-1)*7+6]" +
            ",'resourceId',ARGV[(i-1)*7+7]" +
            ",'tableName',ARGV[(i-1)*7+8]" +
            ",'rowKey',ARGV[(i-1)*7+9]" +
            ",'pk',ARGV[(i-1)*7+10])" +
            " end end " +
            "redis.call('HSET',KEYS[(keySize+1)],KEYS[(keySize+2)],ARGV[(argSize+0)]); return 1";

    private static String ACQUIRE_LOCK_SHA;

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            // TODO
            String acquireLockLuaByFile = null;
            if (acquireLockLuaByFile != null) {
                ACQUIRE_LOCK_SHA = jedis.scriptLoad(acquireLockLuaByFile);
            } else {
                ACQUIRE_LOCK_SHA = jedis.scriptLoad(ACQUIRE_LOCK);
            }
        }
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> needLockDOs = rowLocks.stream()
                    .map(rowLock -> convertToLockDO(rowLock))
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
                args.add(lockDO.getXid());
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
            long result = (long)jedis.evalsha(ACQUIRE_LOCK_SHA, keys, args);
            return SUCCEED == result;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        String currentXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<LockDO> needReleaseLocks = convertToLockDO(rowLocks);
        String[] needReleaseKeys = new String[needReleaseLocks.size()];
        for (int i = 0; i < needReleaseLocks.size(); i ++) {
            needReleaseKeys[i] = buildLockKey(needReleaseLocks.get(i).getRowKey());
        }

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Pipeline pipelined = jedis.pipelined();
            pipelined.del(needReleaseKeys);
            pipelined.hdel(buildXidLockKey(currentXid), branchId.toString());
            pipelined.sync();
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

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        List<Long> branchIds = new ArrayList<>();
        branchIds.add(branchId);
        return releaseLock(xid, branchIds);
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
            Pipeline pipeline = jedis.pipelined();
            lockKeys.forEach(key -> pipeline.hget(key, XID));
            List<String> existedXids = (List<String>) (List) pipeline.syncAndReturnAll();
            return existedXids.stream().allMatch(existedXid -> existedXid == null || xid.equals(existedXid));
        }
    }

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + rowKey;
    }

}
