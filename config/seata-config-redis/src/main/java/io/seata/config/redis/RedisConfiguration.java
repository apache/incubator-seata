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
package io.seata.config.redis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import io.seata.common.exception.RedisException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationKeys;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wangyuewen
 */
public class RedisConfiguration extends AbstractConfiguration {
    private static volatile RedisConfiguration instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    private static final String DB_KEY = "db";
    private static final String PASSWORD_KEY = "password";
    private static final String TIMEOUT_KEY = "timeout";
    private static final String CONFIG_TYPE = "redis";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String CONFIG_KEY_NAME_KEY = "keyName";
    private static final String LISTENER_ENABLED_KEY = "listenerEnabled";
    private static final String REDIS_PING_STR = "PONG";
    private static final long LISTENER_CONFIG_INTERVAL = 1000;

    private final ConcurrentMap<String, Set<ConfigurationChangeListener>> configListenersMap = new ConcurrentHashMap<>(8);
    private final Map<String, String> listenedConfigMap = new HashMap<>(8);
    private final RedisListener redisListener = new RedisListener();
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile JedisPool jedisPool;
    private String keyName;

    /**
     * Get instance of RedisConfiguration
     *
     * @return instance
     */
    public static RedisConfiguration getInstance() {
        if (null == instance) {
            synchronized (RedisConfiguration.class) {
                if (null == instance) {
                    instance = new RedisConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates a new Redis configuration.
     */
    private RedisConfiguration() {
        if (null == jedisPool) {
            synchronized (RedisConfiguration.class) {
                String serverAddr = FILE_CONFIG.getConfig(getRedisServerAddr());
                String passWord = FILE_CONFIG.getConfig(getRedisPassWord());
                String[] uri = serverAddr.split(":");
                String host = uri[0];
                int port = Integer.parseInt(uri[1]);
                int db = FILE_CONFIG.getInt(getRedisDb());
                int timeOut = FILE_CONFIG.getInt(getRedisTimeOut());
                keyName = FILE_CONFIG.getConfig(getRedisConfigKeyName());
                JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
                jedisPoolConfig.setMaxWaitMillis(DEFAULT_CONFIG_TIMEOUT);
                if (StringUtils.isNotEmpty(passWord)) {
                    jedisPool = new JedisPool(jedisPoolConfig, host, port, timeOut, passWord, db);
                } else {
                    jedisPool = new JedisPool(jedisPoolConfig, host, port, timeOut, null, db);
                }
                try (Jedis jedis = jedisPool.getResource()) {
                    if (REDIS_PING_STR.equals(jedis.ping())) {
                        LOGGER.info("redis configuration connection successful!");
                        LOGGER.info("connected database:{}", db);
                    } else {
                        throw new RuntimeException("Redis configuration connection failed!");
                    }
                } catch (JedisConnectionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String getRedisConfigKeyName() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, CONFIG_KEY_NAME_KEY);
    }

    private String getRedisTimeOut() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, TIMEOUT_KEY);
    }

    private String getRedisDb() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, DB_KEY);
    }

    private String getRedisServerAddr() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, SERVER_ADDR_KEY);
    }

    private String getRedisPassWord() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, PASSWORD_KEY);
    }

    private String getRedisListenerEnabled() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, LISTENER_ENABLED_KEY);
    }

    /**
     * Gets type name.
     *
     * @return the type name
     */
    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    /**
     * Put config boolean.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        boolean result = false;
        try (Jedis jedis = jedisPool.getResource()) {
            Long status = jedis.hset(keyName, dataId, content);
            if (status == 1 || status == 0) {
                result = true;
            }
        } catch (RedisException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Get latest config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the Latest config
     */
    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = null;
        try (Jedis jedis = jedisPool.getResource()) {
            value = jedis.hget(keyName, dataId);
        } catch (RedisException e) {
            LOGGER.error(e.getMessage());
        }
        return null == value ? defaultValue : value;
    }

    /**
     * Put config if absent boolean.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        boolean result = false;
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.hexists(keyName, dataId)) {
                Long status = jedis.hset(keyName, dataId, content);
                if (status == 1) {
                    result = true;
                }
            }
        } catch (RedisException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Remove config boolean.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        boolean result = false;
        try (Jedis jedis = jedisPool.getResource()) {
            Long status = jedis.hdel(keyName, dataId);
            if (status == 1) {
                result = true;
            }
        } catch (RedisException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Add config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || null == listener) {
            return;
        }
        configListenersMap.computeIfAbsent(dataId, value -> ConcurrentHashMap.newKeySet()).add(listener);
        listenedConfigMap.put(dataId, getConfig(dataId));
        redisListener.addListener(dataId, listener);
    }

    /**
     * Remove config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || null == listener) {
            return;
        }
        Set<ConfigurationChangeListener> configListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configListenersMap)) {
            configListeners.remove(listener);
            if (configListeners.isEmpty()) {
                configListenersMap.remove(dataId);
                listenedConfigMap.remove(dataId);
            }
        }
        listener.onShutDown();
    }

    /**
     * Gets config listeners.
     *
     * @param dataId the data id
     * @return the config listeners
     */
    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return configListenersMap.get(dataId);
    }

    /**
     * The type RedisListener
     */
    class RedisListener implements ConfigurationChangeListener {
        private final Map<String, Set<ConfigurationChangeListener>> dataIdMap = new HashMap<>();

        private final ExecutorService executor = new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("redisListener", MAX_LISTENER_THREAD));

        public synchronized void addListener(String dataId, ConfigurationChangeListener listener) {
            // only the first time add listener will trigger on process event
            if (dataIdMap.isEmpty()) {
                redisListener.onProcessEvent(new ConfigurationChangeEvent());
            }
            dataIdMap.computeIfAbsent(dataId, value -> new HashSet<>()).add(listener);
        }

        /**
         * Process.
         *
         * @param event the event
         */
        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            boolean enabled = FILE_CONFIG.getBoolean(getRedisListenerEnabled());
            while (enabled) {
                for (String dataId : dataIdMap.keySet()) {
                    try {
                        String newConfig = ConfigurationFactory.getInstance().getLatestConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT);
                        if (StringUtils.isNotBlank(newConfig)) {
                            String oldConfig = listenedConfigMap.get(dataId);
                            if (!StringUtils.equals(newConfig, oldConfig)) {
                                listenedConfigMap.put(dataId, newConfig);
                                event.setDataId(dataId).setNewValue(newConfig).setOldValue(oldConfig);

                                for (ConfigurationChangeListener listener : dataIdMap.get(dataId)) {
                                    listener.onChangeEvent(event);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("redisListener execute error, dataId :{}", dataId, e);
                    }
                }
                try {
                    Thread.sleep(LISTENER_CONFIG_INTERVAL);
                } catch (InterruptedException e) {
                    LOGGER.error("redisListener thread sleep error:{}", e.getMessage());
                }
                enabled = FILE_CONFIG.getBoolean(getRedisListenerEnabled());
            }

        }

        /**
         * Gets executor service.
         *
         * @return the executor service
         */
        @Override
        public ExecutorService getExecutorService() {
            return executor;
        }
    }
}
