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

package io.seata.server.lock.redis;

import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.fiftyonred.mock_jedis.MockJedis;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.RowLock;
import io.seata.server.session.BranchSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

/**
 * @author funkye
 */
public class RedisLockManagerForTest {

    private static final String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";

    @Test
    public void acquireLock() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:786756");
        branchSession.setTransactionId(123543465);
        branchSession.setBranchId(5756678);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:13,14;t2:11,12");
        List<RowLock> locks = collectRowLocks(branchSession);

        try (Jedis jedis = new MockJedis("test")) {
            for (RowLock lock : locks) {
                String key = DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey();
                Long status = jedis.setnx(key, JSON.toJSONString(branchSession));
                Assertions.assertTrue(status == 1);
                break;
            }
        }
    }

    @Test
    public void unLock() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56867");
        branchSession.setTransactionId(1236765);
        branchSession.setBranchId(204565);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:3,4;t2:4,5");
        List<RowLock> locks = collectRowLocks(branchSession);
        try (Jedis jedis = new MockJedis("test")) {
            for (RowLock lock : locks) {
                String key = DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey();
                Long status = jedis.del(key);
                break;
            }
        }
    }

    @Test
    public void isLockable() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56877898");
        branchSession.setTransactionId(245686786);
        branchSession.setBranchId(467568);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:8,7;t2:1,2");
        List<RowLock> locks = collectRowLocks(branchSession);
        try (Jedis jedis = new MockJedis("test")) {
            for (RowLock lock : locks) {
                String key = DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey();
                long status = jedis.setnx(key, JSON.toJSONString(lock));
                Assertions.assertTrue(status == 1);
                break;
            }
            BranchSession branchSession2 = new BranchSession();
            branchSession2.setXid("abc-123:56877898");
            branchSession2.setTransactionId(245686786);
            branchSession2.setBranchId(1242354576);
            branchSession2.setResourceId("abcss");
            branchSession2.setLockKey("t1:8");
            locks = collectRowLocks(branchSession);
            for (RowLock lock : locks) {
                String key = DEFAULT_REDIS_SEATA_LOCK_PREFIX + lock.getRowKey();
                Long status = jedis.del(key);
                Assertions.assertTrue(status == 1);
                break;
            }
        }
    }

    /**
     * Collect row locks list.`
     *
     * @param branchSession
     *            the branch session
     * @return the list
     */
    protected List<RowLock> collectRowLocks(BranchSession branchSession) {
        List<RowLock> locks = new ArrayList<>();
        if (branchSession == null || StringUtils.isBlank(branchSession.getLockKey())) {
            return locks;
        }
        String xid = branchSession.getXid();
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        String lockKey = branchSession.getLockKey();

        return collectRowLocks(lockKey, resourceId, xid, transactionId, branchSession.getBranchId());
    }

    /**
     * Collect row locks list.
     *
     * @param lockKey
     *            the lock key
     * @param resourceId
     *            the resource id
     * @param xid
     *            the xid
     * @param transactionId
     *            the transaction id
     * @param branchID
     *            the branch id
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId,
        Long branchID) {
        List<RowLock> locks = new ArrayList<RowLock>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if (StringUtils.isBlank(mergedPKs)) {
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if (pks == null || pks.length == 0) {
                return locks;
            }
            for (String pk : pks) {
                if (StringUtils.isNotBlank(pk)) {
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(xid);
                    rowLock.setTransactionId(transactionId);
                    rowLock.setBranchId(branchID);
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(resourceId);
                    locks.add(rowLock);
                }
            }
        }
        return locks;
    }
}
