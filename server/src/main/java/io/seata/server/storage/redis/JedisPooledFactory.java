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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.exception.RedisException;
import io.seata.common.util.ConfigTools;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @author funkye
 */
public class JedisPooledFactory {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(JedisPooledFactory.class);

    private static volatile JedisPoolAbstract jedisPool = null;

    private static final String HOST = "127.0.0.1";

    private static final int PORT = 6379;

    private static final int MINCONN = 1;

    private static final int MAXCONN = 10;

    private static final int MAXTOTAL = 100;

    private static final int DATABASE = 0;

    private static final int SENTINEL_HOST_NUMBER = 3;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    /**
     * get the RedisPool instance (singleton)
     * 
     * @return redisPool
     */
    public static JedisPoolAbstract getJedisPoolInstance(JedisPoolAbstract... jedisPools) {
        if (jedisPool == null) {
            synchronized (JedisPooledFactory.class) {
                if (jedisPool == null) {
                    JedisPoolAbstract tempJedisPool = null;
                    if (jedisPools != null && jedisPools.length > 0) {
                        tempJedisPool = jedisPools[0];
                    } else {
                        String password = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD);
                        if (StringUtils.isBlank(password)) {
                            password = null;
                        } else {
                            String publicKey = CONFIGURATION.getConfig(ConfigurationKeys.STORE_PUBLIC_KEY);
                            if (StringUtils.isNotBlank(publicKey)) {
                                try {
                                    password = ConfigTools.publicDecrypt(password, publicKey);
                                } catch (Exception e) {
                                    LOGGER.error("decryption failed,please confirm whether the ciphertext and secret key are correct! error msg: {}", e.getMessage());
                                }
                            }
                        }
                        JedisPoolConfig poolConfig = new JedisPoolConfig();
                        poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, MINCONN));
                        poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, MAXCONN));
                        poolConfig.setMaxTotal(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_TOTAL, MAXTOTAL));
                        String mode = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_MODE,ConfigurationKeys.REDIS_SINGLE_MODE);
                        if (mode.equals(ConfigurationKeys.REDIS_SENTINEL_MODE)) {
                            String masterName = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_SENTINEL_MASTERNAME);
                            if (StringUtils.isBlank(masterName)) {
                                throw new RedisException("The masterName is null in redis sentinel mode");
                            }
                            Set<String> sentinels = new HashSet<>(SENTINEL_HOST_NUMBER);
                            String[] sentinelHosts = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_SENTINEL_HOST).split(",");
                            Arrays.asList(sentinelHosts).forEach(sentinelHost -> sentinels.add(sentinelHost));
                            tempJedisPool = new JedisSentinelPool(masterName, sentinels, poolConfig, 60000, password, CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE, DATABASE));
                        } else if (mode.equals(ConfigurationKeys.REDIS_SINGLE_MODE)) {
                            String host = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_SINGLE_HOST);
                            host = StringUtils.isBlank(host) ? CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_HOST, HOST) : host;
                            int port = CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_SINGLE_PORT);
                            port = port == 0 ? CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_PORT, PORT) : port;
                            tempJedisPool = new JedisPool(poolConfig, host, port, 60000, password, CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE, DATABASE));
                        } else {
                            throw new RedisException("Configuration error of redis cluster mode");
                        }
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("initialization of the build redis connection pool is complete");
                    }
                    jedisPool = tempJedisPool;
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
