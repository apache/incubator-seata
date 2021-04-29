package io.seata.server.session.redis;

import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.lock.RedisDistributedLocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;

/**
 * @description: redis distributed lock test
 * ！！！！！warn！！！！！
 * The redis mock framework(jedis-mock) can not support set(lockKey, lockValue, SetParams.setParams().nx())，the nx() will not effective.
 * So we can not use mock to test.
 * We need a true redis server to test!
 * ！！！！！warn！！！！！
 * @author: zhongxiang.wang
 * @date: 2021-04-28 21:58
 */
public class RedisDistributedLockerTest {

    private static JedisPoolAbstract jedisPoolInstance;
    private String retryRollbacking = "RetryRollbacking";
    private String retryCommiting = "RetryCommiting";
    private String lockValue = "127.1.1.1:9081";

    @BeforeAll
    public static void start() throws IOException {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        jedisPoolInstance = JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6379, 60000));
    }

    @Test
    public void test_acquireScheduledLock_success() {
        boolean accquire = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue, 60);
        Assertions.assertEquals(true,accquire);
        String lockValueExisted = jedisPoolInstance.getResource().get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);
        boolean release = RedisDistributedLocker.releaseScheduleLock(retryRollbacking, lockValue);
        Assertions.assertEquals(true,release);
        Assertions.assertNull(jedisPoolInstance.getResource().get(retryRollbacking));
    }

    @Test
    public void test_acquireScheduledLock_concurrent() {
        //acquire the lock success
        boolean accquire = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue, 60);
        Assertions.assertEquals(true,accquire);
        String lockValueExisted = jedisPoolInstance.getResource().get(retryRollbacking);
        Assertions.assertEquals(lockValue,lockValueExisted);

        // concurrent acquire
       for(int i = 0;i < 1000;i++){
           boolean b = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue + i, 60);
           Assertions.assertEquals(false,b);
       }

       //release the lock
       boolean release = RedisDistributedLocker.releaseScheduleLock(retryRollbacking, lockValue);
       Assertions.assertEquals(true,release);
       Assertions.assertNull(jedisPoolInstance.getResource().get(retryRollbacking));

       // other acquire the lock success
       boolean c = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue + 1, 60);
       Assertions.assertEquals(true,c);

        //other2 acquire the lock failed
        boolean d = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue + 2, 60);
        Assertions.assertEquals(false,d);

       //sleep 60s
        try {
            Thread.sleep(60*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //other2 acquire the lock
        boolean e = RedisDistributedLocker.acquireScheduledLock(retryRollbacking, lockValue + 2, 60);
        Assertions.assertEquals(true,e);

        //clear
        boolean f = RedisDistributedLocker.releaseScheduleLock(retryRollbacking, lockValue + 2);
    }

    @Test
    public void test_acquireScheduledLock_false() {
        String set = jedisPoolInstance.getResource().set(retryCommiting, lockValue);
        Assertions.assertEquals("OK",set);
        boolean acquire = RedisDistributedLocker.acquireScheduledLock(retryCommiting, lockValue, 60);
        Assertions.assertEquals(false,acquire);
    }
}
