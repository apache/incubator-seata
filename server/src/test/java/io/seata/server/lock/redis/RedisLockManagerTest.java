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

import com.github.fppt.jedismock.RedisServer;
import io.seata.common.exception.StoreException;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.model.LockStatus;
import io.seata.server.lock.LockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.lock.RedisLockManager;
import io.seata.server.storage.redis.lock.RedisLocker;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author funkye
 * @author conghuhu
 */
@SpringBootTest
public class RedisLockManagerTest {
    static RedisServer server = null;
    static LockManager lockManager = null;

    static Jedis jedis = null;

    /**
     * because of mock redis server can not run lua script,
     * if you want to test lua mode, please modify application.yaml and config your redis instance info.
     *
     * @param context
     * @throws IOException
     */
    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        server = RedisServer.newRedisServer(6789);
        server.start();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        jedis = JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6789, 60000)).getResource();
        lockManager = new RedisLockManagerForTest();
    }

    @Test
    public void acquireLock() throws TransactionException {
        BranchSession branchSession = getBranchSession();
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
        Assertions.assertTrue(lockManager.releaseLock(branchSession));
    }

    /**
     * test in lua mode, a global lock is found in the Rollbacking state, fast failure
     */
    @Test
    public void acquireLockByLuaFastFailure() throws TransactionException {
        BranchSession branchLockSession = getBranchSession();
        Assertions.assertTrue(lockManager.acquireLock(branchLockSession));
        lockManager.updateLockStatus(branchLockSession.getXid(), LockStatus.Rollbacking);

        BranchSession branchSession = getBranchSession();
        Assertions.assertThrows(StoreException.class, () -> {
            lockManager.acquireLock(branchSession, false, false);
        });
        Assertions.assertTrue(lockManager.releaseLock(branchLockSession));
    }

    /**
     * test in lua mode, other hold the lock
     */
    @Test
    public void acquireLockByLuaHolding() throws TransactionException {
        BranchSession branchLockSession = getBranchSession();
        Assertions.assertTrue(lockManager.acquireLock(branchLockSession));

        BranchSession branchSession = getBranchSession();
        branchSession.setXid("abc-123:786754");

        Assertions.assertFalse(lockManager.acquireLock(branchSession));
        Assertions.assertTrue(lockManager.releaseLock(branchLockSession));
    }

    @Test
    public void unLock() throws TransactionException {
        BranchSession branchSession = getBranchSession();
        Assertions.assertTrue(lockManager.releaseLock(branchSession));
    }

    @Test
    public void isLockable() throws TransactionException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56877898");
        branchSession.setTransactionId(245686786);
        branchSession.setBranchId(467568);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:8,7;t2:1,2");
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
        BranchSession branchSession2 = new BranchSession();
        branchSession2.setXid("abc-123:56877898");
        branchSession2.setTransactionId(245686786);
        branchSession2.setBranchId(1242354576);
        branchSession2.setResourceId("abcss");
        branchSession2.setLockKey("t1:8");
        Assertions.assertTrue(lockManager.isLockable(branchSession2.getXid(), branchSession2.getResourceId(),
            branchSession2.getLockKey()));
        Assertions.assertTrue(lockManager.releaseLock(branchSession));
    }

    @Test
    public void updateLockStatus() throws TransactionException {
        BranchSession branchSession = getBranchSession();
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
        lockManager.updateLockStatus(branchSession.getXid(), LockStatus.Rollbacking);
        Assertions.assertTrue(lockManager.releaseLock(branchSession));
    }

    @AfterAll
    public static void after() {
        server.stop();
        server = null;
    }

    public static class RedisLockManagerForTest extends RedisLockManager {

        public RedisLockManagerForTest() {
        }

        @Override
        public Locker getLocker(BranchSession branchSession) {
            return new RedisLocker();
        }
    }

    private BranchSession getBranchSession() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:786756");
        branchSession.setTransactionId(123543465);
        branchSession.setBranchId(5756678);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:13,14;t2:11,12");
        return branchSession;
    }
}
