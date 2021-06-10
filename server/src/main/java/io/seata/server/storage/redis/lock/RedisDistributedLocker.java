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

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.server.storage.redis.JedisPooledFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;

/**
 * @description Redis distributed lock
 * @author  zhongxiang.wang
 */
@LoadLevel(name = "redis", scope = Scope.SINGLETON)
public class RedisDistributedLocker implements DistributedLocker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLocker.class);
    private static final String SUCCESS = "OK";

    /**
     * Acquire the distributed lock
     *
     * @param distributedLockDO
     * @return
     */
    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            //Don't need retry,if can't acquire the lock,let the other get the lock
            String result = jedis.set(distributedLockDO.getLockKey(), distributedLockDO.getLockValue(), SetParams.setParams().nx().px(distributedLockDO.getExpireTime()));
            if (SUCCESS.equalsIgnoreCase(result)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("The {} acquired the {} distributed lock failed.", distributedLockDO.getLockValue(), distributedLockDO.getLockKey(), ex);
            return false;
        }
    }


    /**
     * Release the distributed lock
     *
     * @param distributedLockDO
     * @return
     */
    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        String lockKey = distributedLockDO.getLockKey();
        String lockValue = distributedLockDO.getLockValue();
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
