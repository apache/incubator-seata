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
package io.seata.server.storage.redis.store;

import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.LogStore;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author funkye
 */
public class RedisTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    //region Instance

    private static volatile RedisTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static RedisTransactionStoreManager getInstance() {
        if (null == instance) {
            synchronized (RedisTransactionStoreManager.class) {
                if (null == instance) {
                    instance = new RedisTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    //endregion

    //region Constructor

    /**
     * Instantiates a new redis transaction store manager.
     */
    private RedisTransactionStoreManager() {
        //init logQueryLimit
        super.initLogQueryLimit(ConfigurationKeys.STORE_REDIS_QUERY_LIMIT);
    }

    /**
     * Instantiates a new redis transaction store manager.
     *
     * @param logStore      the log store
     * @param logQueryLimit the log query limit
     */
    private RedisTransactionStoreManager(LogStore logStore, int logQueryLimit) {
        super.logStore = logStore;
        super.logQueryLimit = logQueryLimit;
    }

    //endregion

    //region Override TransactionStoreManager

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (super.logStore != null) {
            return super.writeSession(logOperation, session);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).writeSession(logOperation, session);
            }
        }
    }

    @Override
    public GlobalSession getSession(String xid, boolean withBranchSessions) {
        if (super.logStore != null) {
            return super.getSession(xid, withBranchSessions);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).getSession(xid, withBranchSessions);
            }
        }
    }

    @Override
    public List<GlobalSession> findSession(SessionCondition sessionCondition, boolean withBranchSessions) {
        if (super.logStore != null) {
            return super.findSession(sessionCondition, withBranchSessions);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).findSession(sessionCondition, withBranchSessions);
            }
        }
    }

    //endregion

    //region Private

    /**
     * Create store manager.
     *
     * @param jedis the jedis
     * @return the store manager
     */
    private RedisTransactionStoreManager create(Jedis jedis) {
        LogStoreRedisDAO logStore = new LogStoreRedisDAO(jedis, super.logQueryLimit);
        return new RedisTransactionStoreManager(logStore, super.logQueryLimit);
    }

    //endregion
}
