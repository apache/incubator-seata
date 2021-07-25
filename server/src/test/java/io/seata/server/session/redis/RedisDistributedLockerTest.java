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

import org.junit.jupiter.api.Assertions;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;
import java.io.IOException;

import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.distributed.DistributedLockerFactory;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.redis.JedisPooledFactory;

/**
 * @description redis distributed lock test
 *
 * ！！！！！Please use a true redis server to run the following test case ！！！！！
 * The redis mock framework(jedis-mock) can not support set(lockKey, lockValue, SetParams.setParams().nx())，the nx() will not effective.
 * So we can not use mock to test.
 * We need a true redis server to test!
 * ！！！！！Please use a true redis server to run the following test case ！！！！！
 *
 * @author  zhongxiang.wang
 */
public class RedisDistributedLockerTest {

    private static JedisPoolAbstract jedisPoolInstance;
    private String retryRollbacking = "RetryRollbacking";
    private String retryCommiting = "RetryCommiting";
    private String lockValue = "127.1.1.1:9081";
    private static DistributedLocker distributedLocker;

//    @BeforeAll
    public static void start() throws IOException {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        jedisPoolInstance = JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6379, 60000));
        distributedLocker = DistributedLockerFactory.getDistributedLocker(StoreMode.REDIS.getName());
    }

//    @Test
    public void test_acquireScheduledLock_success() {
        boolean acquire = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue, 60000l));
        Assertions.assertEquals(true,acquire);
        String lockValueExisted = jedisPoolInstance.getResource().get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);
        boolean release = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue,null));
        Assertions.assertEquals(true,release);
        Assertions.assertNull(jedisPoolInstance.getResource().get(retryRollbacking));
    }

//    @Test
    public void test_acquireScheduledLock_success_() {
        SessionHolder.init(StoreMode.REDIS.getName());
        boolean accquire = SessionHolder.acquireDistributedLock(retryRollbacking);
        Assertions.assertEquals(true,accquire);
        String lockValueExisted = jedisPoolInstance.getResource().get(retryRollbacking);
        Assertions.assertEquals("null:0",lockValueExisted);
        boolean release = SessionHolder.releaseDistributedLock(retryRollbacking);
        Assertions.assertEquals(true,release);
        Assertions.assertNull(jedisPoolInstance.getResource().get(retryRollbacking));
    }

//    @Test
    public void test_acquireLock_concurrent() {
        //acquire the lock success
        boolean accquire = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue, 60000l));
        Assertions.assertEquals(true,accquire);
        String lockValueExisted = jedisPoolInstance.getResource().get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);

        // concurrent acquire
       for(int i = 0;i < 1000;i++){
           boolean b = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + i, 60000l));
           Assertions.assertEquals(false,b);
       }

       //release the lock
       boolean release = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue ,null));
       Assertions.assertEquals(true,release);
       Assertions.assertNull(jedisPoolInstance.getResource().get(retryRollbacking));

       // other acquire the lock success
       boolean c = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 1, 60000l));
       Assertions.assertEquals(true,c);

        //other2 acquire the lock failed
        boolean d = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 2, 60000l));
        Assertions.assertEquals(false,d);

       //sleep 60s
        try {
            Thread.sleep(60*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //other2 acquire the lock
        boolean e = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 2, 60000l));
        Assertions.assertEquals(true,e);

        //clear
        boolean f = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue + 2,null));
    }

//    @Test
    public void test_acquireLock_false() {
        String set = jedisPoolInstance.getResource().set(retryCommiting, lockValue);
        Assertions.assertEquals("OK",set);
        boolean acquire = distributedLocker.acquireLock(new DistributedLockDO(retryCommiting, lockValue, 60000l));
        Assertions.assertEquals(false,acquire);
    }
}
