package io.seata.server.storage.redis;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisPooledFactory {

    private static volatile JedisPool jedisPool = null;
    private static String host = "127.0.0.1";
    private static int port = 6379;
    private static String password = null;
    private static int minConn = 1;
    private static int maxConn = 1;
    private static int dataBase = 0;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();
    /**
     * get the RedisPool instance (singleton)
     * @return redisPool
     */
    public static JedisPool getJedisPoolInstance() {
        if (jedisPool == null) {
            synchronized (JedisPooledFactory.class) {
                if (jedisPool == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, minConn));
                    poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, maxConn));
                    jedisPool =
                        new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_HOST, host),
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_PORT, port), 60000,
                            CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD, password),
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE,dataBase));
                }
            }
        }

        return jedisPool;
    }

    /**
     * get an instance of Jedis (connection) from the connection pool
     * @return jedis
     */
    public static Jedis getJedisInstance() {
        return getJedisPoolInstance().getResource();
    }


}
