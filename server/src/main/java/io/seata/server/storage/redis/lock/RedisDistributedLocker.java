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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;
import io.seata.server.storage.redis.JedisPooledFactory;

/**
 * @description: Redis distributed lock
 * @author: zhongxiang.wang
 * @date: 2021-03-02 11:34
 */
public class RedisDistributedLocker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLocker.class);
    private static final String SUCCESS = "OK";
    /**
     * Acquire the distributed lock
     *
     * @param lockKey    the lock key
     * @param lockValue  the lock value
     * @param expireTime the expireTime,to prevent the dead lock when current TC who acquired the lock has down
     * @return
     */
    public static boolean acquireScheduledLock(String lockKey, String lockValue, Integer expireTime) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            //Don't need retry,if can't acquire the lock,let the other get the lock
            String result = jedis.set(lockKey, lockValue, SetParams.setParams().nx().ex(expireTime));
            if (SUCCESS.equalsIgnoreCase(result)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("The {} acquired the {} distributed lock failed.", lockValue, lockKey, ex);
            return false;
        }
    }

    /**
     * Release the distributed lock
     *
     * @param lockKey   the lock key
     * @param lockValue the lock value
     * @return
     */
    public static boolean releaseScheduleLock(String lockKey, String lockValue) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.watch(lockKey);
            //Check the value to prevent release the other's lock
            if (lockValue.equals(jedis.get(lockKey))) {
                Transaction multi = jedis.multi();
                multi.del(lockKey);
                multi.exec();
                return true;
            }
            //The lock hold by others,If other one get the lock,we release lock success too as for current lockKey
            jedis.unwatch();
            return true;
        } catch (Exception ex) {
            LOGGER.error("The {} release the {} distributed lock failed.", lockValue, lockKey, ex);
            return false;
        }
    }

}
