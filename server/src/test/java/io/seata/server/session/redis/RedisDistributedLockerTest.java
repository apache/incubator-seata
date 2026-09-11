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
package io.seata.server.session.redis;

import java.io.IOException;

import io.seata.common.XID;
import io.seata.common.loader.EnhancedServiceLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.server.lock.distributed.DistributedLockerFactory;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.StoreConfig.SessionMode;
import static io.seata.server.store.StoreConfig.StoreMode;

/**
 * @description redis distributed lock test
 *
 * @author zhongxiang.wang
 * @author funkye
 */
@SpringBootTest
public class RedisDistributedLockerTest {

    private String retryRollbacking = "RetryRollbacking";
    private String retryRollbacking2 = "RetryRollbacking2";
    private String retryCommiting = "RetryCommiting";
    private String lockValue = "127.1.1.1:9081";
    private static DistributedLocker distributedLocker;
    private static Jedis jedis;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        EnhancedServiceLoader.unload(DistributedLocker.class);
        MockRedisServer.getInstance();
        DistributedLockerFactory.cleanLocker();
        distributedLocker = DistributedLockerFactory.getDistributedLocker(StoreMode.REDIS.getName());
        jedis = JedisPooledFactory.getJedisInstance();
    }

    @AfterAll
    public static void after() throws IOException {
        EnhancedServiceLoader.unload(DistributedLocker.class);
        DistributedLockerFactory.cleanLocker();
        DistributedLockerFactory.getDistributedLocker(StoreMode.FILE.getName());
        jedis.close();
    }

    @Test
    public void test_acquireScheduledLock_success() {
        String lockKey = retryRollbacking;

        boolean acquire = distributedLocker.acquireLock(new DistributedLockDO(lockKey, lockValue, 60000L));
        Assertions.assertTrue(acquire);
        String lockValueExisted = jedis.get(lockKey);
        Assertions.assertEquals(lockValue, lockValueExisted);
        boolean release = distributedLocker.releaseLock(new DistributedLockDO(lockKey, lockValue, null));
        Assertions.assertTrue(release);
        Assertions.assertNull(jedis.get(lockKey));
    }

    @Test
    public void test_acquireScheduledLock_success_() {
        String lockKey = retryRollbacking2;
        SessionHolder.init(SessionMode.REDIS);

        boolean accquire = SessionHolder.acquireDistributedLock(lockKey);
        Assertions.assertTrue(accquire);
        String lockValueExisted = jedis.get(lockKey);
        Assertions.assertEquals(XID.getIpAddressAndPort(), lockValueExisted);
        boolean release = SessionHolder.releaseDistributedLock(lockKey);
        Assertions.assertTrue(release);
        Assertions.assertNull(jedis.get(lockKey));
    }

    @Test
    public void test_acquireLock_concurrent() {
        //acquire the lock success
        boolean accquire = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue, 60000l));
        Assertions.assertTrue(accquire);
        String lockValueExisted = jedis.get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);

        // concurrent acquire
       for(int i = 0;i < 10;i++){
           boolean b = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + i, 60000l));
           Assertions.assertFalse(b);
       }

       //release the lock
       boolean release = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue ,null));
       Assertions.assertTrue(release);
       Assertions.assertNull(jedis.get(retryRollbacking));

       // other acquire the lock success
       boolean c = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 1, 2000L));
        Assertions.assertTrue(c);

        //other2 acquire the lock failed
        boolean d = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 2, 2000L));
        Assertions.assertFalse(d);

       //sleep 60s
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //other2 acquire the lock
        boolean e = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 2, 60000l));
        Assertions.assertTrue(e);

        //clear
        boolean f = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue + 2,null));
    }

    @Test
    public void test_acquireLock_false() {
        String set = jedis.set(retryCommiting, lockValue);
        Assertions.assertEquals("OK",set);
        boolean acquire = distributedLocker.acquireLock(new DistributedLockDO(retryCommiting, lockValue, 60000l));
        Assertions.assertFalse(acquire);
    }

}
