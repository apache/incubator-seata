package io.seata.server.store.redis;

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
     * 获取RedisPool实例（单例）
     * @return RedisPool实例
     */
    public static JedisPool getJedisPoolInstance() {
        if (jedisPool == null) {
            synchronized (JedisPooledFactory.class) {
                if (jedisPool == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, minConn));
                    poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, maxConn));
                    poolConfig.setTestOnBorrow(true);
                    jedisPool =
                        new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_HOST, host),
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_PORT, port), 60000,
                            CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD, password),
                            CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE));
                }
            }
        }

        return jedisPool;
    }

    /**
     * 从连接池中获取一个 Jedis 实例（连接）
     * @return Jedis 实例
     */
    public static Jedis getJedisInstance() {
        return getJedisPoolInstance().getResource();
    }

    /**
     * 将Jedis对象（连接）归还连接池
     * @param jedis 连接对象
     */
    public static void release(Jedis jedis) {
        if (jedis != null) {
            jedis.close();  // 已废弃，推荐使用jedis.close()方法
        }
    }

}
