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

import com.github.microwww.redis.RedisServer;
import io.seata.common.XID;
import io.seata.common.loader.EnhancedServiceLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.distributed.DistributedLockerFactory;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.redis.JedisPooledFactory;

/**
 * @description redis distributed lock test
 *
 * @author zhongxiang.wang funkye
 */
@SpringBootTest
public class RedisDistributedLockerTest {

    private String retryRollbacking = "RetryRollbacking";
    private String retryCommiting = "RetryCommiting";
    private String lockValue = "127.1.1.1:9081";
    private static DistributedLocker distributedLocker;
    private static RedisServer server = null;
    private static Jedis jedis;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        server = new RedisServer();
        server.listener("127.0.0.1", 6789);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6789, 60000));
        EnhancedServiceLoader.unload(DistributedLocker.class);
        DistributedLockerFactory.cleanLocker();
        distributedLocker = DistributedLockerFactory.getDistributedLocker(StoreMode.REDIS.getName());
        jedis = JedisPooledFactory.getJedisInstance();
    }

    @Test
    public void test_acquireScheduledLock_success() {
        boolean acquire = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue, 60000L));
        Assertions.assertTrue(acquire);
        String lockValueExisted = jedis.get(retryRollbacking);
        Assertions.assertEquals(lockValue, lockValueExisted);
        boolean release = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue, null));
        Assertions.assertTrue(release);
        Assertions.assertNull(jedis.get(retryRollbacking));
    }

    @Test
    public void test_acquireScheduledLock_success_() throws UnknownHostException {
        SessionHolder.init(StoreMode.REDIS.getName());
        boolean accquire = SessionHolder.acquireDistributedLock(retryRollbacking);
        Assertions.assertTrue(accquire);
        String lockValueExisted = jedis.get(retryRollbacking);
        Assertions.assertEquals(XID.getIpAddressAndPort(), lockValueExisted);
        boolean release = SessionHolder.releaseDistributedLock(retryRollbacking);
        Assertions.assertTrue(release);
        Assertions.assertNull(jedis.get(retryRollbacking));
    }

    @Test
    public void test_acquireLock_concurrent() {
        //acquire the lock success
        boolean accquire = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue, 60000l));
        Assertions.assertEquals(true,accquire);
        String lockValueExisted = jedis.get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);

        // concurrent acquire
       for(int i = 0;i < 1000;i++){
           boolean b = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + i, 60000l));
           Assertions.assertEquals(false,b);
       }

       //release the lock
       boolean release = distributedLocker.releaseLock(new DistributedLockDO(retryRollbacking, lockValue ,null));
       Assertions.assertEquals(true,release);
       Assertions.assertNull(jedis.get(retryRollbacking));

       // other acquire the lock success
       boolean c = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 1, 60000l));
        Assertions.assertTrue(c);

        //other2 acquire the lock failed
        boolean d = distributedLocker.acquireLock(new DistributedLockDO(retryRollbacking, lockValue + 2, 60000l));
        Assertions.assertFalse(d);

       //sleep 60s
        try {
            Thread.sleep(60*1000);
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
        Assertions.assertEquals(false,acquire);
    }

    @AfterAll
    public static void after() throws IOException {
        DistributedLockerFactory.cleanLocker();
        EnhancedServiceLoader.unload(DistributedLocker.class);
        DistributedLockerFactory.getDistributedLocker(StoreMode.FILE.getName());
        jedis.close();
        server.close();
        server = null;
    }

}
