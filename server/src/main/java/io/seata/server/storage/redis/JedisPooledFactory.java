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
package io.seata.server.storage.redis;

import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author funkye
 */
public class JedisPooledFactory {

    private static volatile JedisPool jedisPool = null;
    private static String host = "127.0.0.1";
    private static int port = 6379;
    private static int minConn = 1;
    private static int maxConn = 1;
    private static int dataBase = 0;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    /**
     * get the RedisPool instance (singleton)
     * 
     * @return redisPool
     */
    public static JedisPool getJedisPoolInstance() {
        if (jedisPool == null) {
            synchronized (JedisPooledFactory.class) {
                String password = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD);
                if (StringUtils.isBlank(password)) {
                    password = null;
                }
                if (jedisPool == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, minConn));
                    poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, maxConn));
                    jedisPool =
                        new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_HOST, host),
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_PORT, port), 60000, password,
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE, dataBase));
                }
            }
        }

        return jedisPool;
    }

    /**
     * get an instance of Jedis (connection) from the connection pool
     * 
     * @return jedis
     */
    public static Jedis getJedisInstance() {
        return getJedisPoolInstance().getResource();
    }

}
