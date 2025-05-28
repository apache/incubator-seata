/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.config.redis;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.AbstractConfiguration;
import org.apache.seata.config.ConfigFuture;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;

/**
 * The type Redis configuration
 */
public class RedisConfiguration extends AbstractConfiguration {
    private static volatile RedisConfiguration instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    private static final String DEFAULT_GROUP = "SEATA_GROUP";
    private static final String CONFIG_TYPE = "redis";
    private static final String REDIS_FILEKEY_PREFIX = "config.redis.";

    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile Properties seataConfig = new Properties();
    private static volatile JedisPool jedisPool;

    private static final ConcurrentMap<String, Set<RedisListener>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>();

    private final ExecutorService subscribeExecutor;

    /**
     * Get instance of RedisConfiguration
     *
     * @return instance
     */
    public static RedisConfiguration getInstance() {
        if (instance == null) {
            synchronized (RedisConfiguration.class) {
                if (instance == null) {
                    instance = new RedisConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates Redis configuration
     */
    private RedisConfiguration() {
        initRedisPool();
        initSeataConfig();

        int threadPoolNum = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "thread-pool-num", 50);

        this.subscribeExecutor = new ThreadPoolExecutor(
                threadPoolNum,
                threadPoolNum,
                Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("redis-subscribe-executor", threadPoolNum));

        // Register the JVM shutdown hook to release resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdownExecutorService(subscribeExecutor);

                if (jedisPool != null && !jedisPool.isClosed()) {
                    jedisPool.close();
                }
            } catch (Exception e) {
                LOGGER.error("Error while shutting down Redis configuration: {}", e.getMessage());
            }
        }));
    }

    /**
     * Get latest config with timeout and default value
     */
    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = seataConfig.getProperty(dataId);
        if (value != null) {
            return value;
        }

        // Time in ConfigFuture, if time out, get() would return the default value
        ConfigFuture configFuture =
                new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET, timeoutMills);

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.hget(getConfigKey(), dataId);
            configFuture.setResult(result != null ? result : defaultValue);
        } catch (Exception e) {
            LOGGER.error("Failed to get config from Redis: {}", e.getMessage());
            configFuture.setResult(defaultValue);
        }

        return (String) configFuture.get();
    }

    /**
     * Write config regardless of whether the configuration already exists
     */
    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);

        try (Jedis jedis = jedisPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
            pipeline.hset(getConfigKey(), dataId, content);
            pipeline.publish(
                    getConfigChannelKey() + ":" + dataId,
                    buildConfigChangeMessage(ConfigOperation.PUT, dataId, content));
            pipeline.sync();

            seataConfig.setProperty(dataId, content);
            configFuture.setResult(Boolean.TRUE);
        } catch (Exception e) {
            LOGGER.error("Failed to put config to Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    /**
     * Write config only when the configuration does not exist
     */
    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (!seataConfig.isEmpty() && seataConfig.containsKey(dataId)) {
            return true;
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT, timeoutMills);

        try (Jedis jedis = jedisPool.getResource()) {
            String configKey = getConfigKey();
            Long result = jedis.hsetnx(configKey, dataId, content);

            if (result == 1) {
                jedis.publish(
                        getConfigChannelKey() + ":" + dataId,
                        buildConfigChangeMessage(ConfigOperation.PUT, dataId, content));
                seataConfig.setProperty(dataId, content);
                configFuture.setResult(Boolean.TRUE);
            } else {
                // If HSETNX returns 0, the key-field already exists in the Redis
                String existingContent = jedis.hget(configKey, dataId);
                seataConfig.setProperty(dataId, existingContent);
                configFuture.setResult(Boolean.TRUE);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to put config if absent to Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    /**
     * Remove config with timeout
     */
    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);

        try (Jedis jedis = jedisPool.getResource();
             Pipeline pipeline = jedis.pipelined()) {
            pipeline.hdel(getConfigKey(), dataId);
            pipeline.publish(
                    getConfigChannelKey() + ":" + dataId,
                    buildConfigChangeMessage(ConfigOperation.REMOVE, dataId, ""));
            pipeline.sync();

            seataConfig.remove(dataId);
            configFuture.setResult(Boolean.TRUE);
        } catch (Exception e) {
            LOGGER.error("Failed to remove config from Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        try {
            RedisListener redisListener = new RedisListener(dataId, listener);

            subscribeExecutor.execute(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(redisListener, getConfigChannelKey() + ":" + dataId);
                } catch (Exception e) {
                    LOGGER.error("Failed to subscribe Redis channel: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to add Redis listener: {}", e.getMessage());
        }
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        Set<RedisListener> redisListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (redisListeners != null) {
            redisListeners.removeIf(redisListener -> {
                if (redisListener.getTargetListener().equals(listener)) {
                    redisListener.unsubscribe();
                    return true;
                }
                return false;
            });

            if (redisListeners.isEmpty()) {
                CONFIG_LISTENERS_MAP.remove(dataId);
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        Set<RedisListener> redisListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (redisListeners != null) {
            return redisListeners.stream().map(RedisListener::getTargetListener).collect(Collectors.toSet());
        }

        return null;
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    /**
     * RedisListener, handles Redis publish events for configuration changes
     */
    public static class RedisListener extends JedisPubSub {
        private final String dataId;
        private final ConfigurationChangeListener listener;
        private volatile boolean running = true;

        public RedisListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        @Override
        public void onMessage(String channel, String message) {
            if (!running) {
                return;
            }

            String[] parts = message.split("-", 3);
            if (parts.length >= 2) {
                try {
                    ConfigOperation operation = ConfigOperation.valueOf(parts[0].toUpperCase());
                    String dataId = parts[1];
                    String content = parts.length > 2 ? parts[2] : null;

                    switch (operation) {
                        case PUT:
                            if (content != null) {
                                seataConfig.setProperty(dataId, content);
                            }
                            break;
                        case REMOVE:
                            seataConfig.remove(dataId);
                            content = null;
                            break;
                        default:
                            break;
                    }

                    ConfigurationChangeEvent event = new ConfigurationChangeEvent()
                            .setDataId(dataId)
                            .setNewValue(content)
                            .setNamespace(DEFAULT_GROUP);
                    listener.onProcessEvent(event);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid operation in Redis message: {}", parts[0]);
                }
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            CONFIG_LISTENERS_MAP
                    .computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                    .add(this);
        }

        public ConfigurationChangeListener getTargetListener() {
            return this.listener;
        }

        public void unsubscribe() {
            running = false;
            super.unsubscribe();
        }
    }

    private void shutdownExecutorService(ExecutorService executorService) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Initialize Redis connection pool with configuration parameters
     */
    private void initRedisPool() {
        GenericObjectPoolConfig redisConfig = new GenericObjectPoolConfig();
        redisConfig.setTestOnBorrow(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-borrow", true));
        redisConfig.setTestOnReturn(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-return", false));
        redisConfig.setTestWhileIdle(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-while-idle", false));

        String serverAddr = fileConfig.getConfig(REDIS_FILEKEY_PREFIX + "server-addr");
        String[] serverArr = NetUtil.splitIPPortStr(serverAddr);
        String host = serverArr[0];
        int port = Integer.parseInt(serverArr[1]);

        String password = fileConfig.getConfig(REDIS_FILEKEY_PREFIX + "password");
        int database = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "db", 0);

        int maxIdle = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-idle", 0);
        if (maxIdle > 0) {
            redisConfig.setMaxIdle(maxIdle);
        }
        int minIdle = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "min-idle", 0);
        if (minIdle > 0) {
            redisConfig.setMinIdle(minIdle);
        }
        int maxActive = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-active", 0);
        if (maxActive > 0) {
            redisConfig.setMaxTotal(maxActive);
        }
        int maxTotal = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-total", 0);
        if (maxTotal > 0) {
            redisConfig.setMaxTotal(maxTotal);
        }
        int maxWait = fileConfig.getInt(
                REDIS_FILEKEY_PREFIX + "max-wait", fileConfig.getInt(REDIS_FILEKEY_PREFIX + "timeout", 0));
        if (maxWait > 0) {
            redisConfig.setMaxWaitMillis(maxWait);
        }
        int numTestsPerEvictionRun = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "num-tests-per-eviction-run", 0);
        if (numTestsPerEvictionRun > 0) {
            redisConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
        int timeBetweenEvictionRunsMillis =
                fileConfig.getInt(REDIS_FILEKEY_PREFIX + "time-between-eviction-runs-millis", 0);
        if (timeBetweenEvictionRunsMillis > 0) {
            redisConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
        int minEvictableIdleTimeMillis = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "min-evictable-idle-time-millis", 0);
        if (minEvictableIdleTimeMillis > 0) {
            redisConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }

        if (StringUtils.isNullOrEmpty(password)) {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, database);
        } else {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, password, database);
        }
    }

    /**
     * Initialize Seata configuration from Redis, loads all configurations into local cache
     */
    private void initSeataConfig() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> configMap = jedis.hgetAll(getConfigKey());
            if (!configMap.isEmpty()) {
                configMap.forEach((key, value) -> seataConfig.setProperty(key, value));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to init Redis config: {}", e.getMessage());
        }
    }

    /**
     * Build configuration change message for Redis pub/sub
     */
    private String buildConfigChangeMessage(ConfigOperation operation, String dataId, String content) {
        // like: put-dataId-content
        return String.format("%s-%s-%s", operation.name().toLowerCase(), dataId, content);
    }

    private String getConfigKey() {
        // like: seata:config:SEATA_GROUP
        return "seata:config:" + DEFAULT_GROUP;
    }

    private String getConfigChannelKey() {
        // like: seata:config:channel:SEATA_GROUP
        return "seata:config:channel:" + DEFAULT_GROUP;
    }
}
