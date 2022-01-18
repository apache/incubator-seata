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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;
import io.seata.config.ConfigurationChangeListener;
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
    private static final String DEFAULT_CONFIG_KEY_NAME = "seataConfig";
    private static final String REDIS_PING_STR = "PONG";
    private static String keyName;
    private static final int MAP_INITIAL_CAPACITY = 16;
    private static final ConcurrentMap<String,String> CONFIG_MAP = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile JedisPool jedisPool;

    /**
     * Get instance of NacosConfiguration
     *
     * @return instance
     */
    public static RedisConfiguration getInstance() {
        if (null == instance){
            synchronized (RedisConfiguration.class){
                if (null == instance){
                    instance = new RedisConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates a new Redis configuration.
     */
    private RedisConfiguration(){
        if (null == jedisPool){
            synchronized (RedisConfiguration.class){
                String serverAddr = StringUtils.isNotBlank(System.getProperty(SERVER_ADDR_KEY))?System.getProperty(SERVER_ADDR_KEY) : FILE_CONFIG.getConfig(getRedisServerAddr());
                String passWord = StringUtils.isNotBlank(System.getProperty(PASSWORD_KEY))?System.getProperty(PASSWORD_KEY) : FILE_CONFIG.getConfig(getRedisPassWord());
                String[] uri = serverAddr.split(":");
                String host = uri[0];
                int port = Integer.parseInt(uri[1]);
                int db = StringUtils.isNotBlank(System.getProperty(DB_KEY))? Integer.parseInt(System.getProperty(DB_KEY)) : FILE_CONFIG.getInt(getRedisDb());
                int timeOut = StringUtils.isNotBlank(System.getProperty(TIMEOUT_KEY))? Integer.parseInt(System.getProperty(TIMEOUT_KEY)) : FILE_CONFIG.getInt(getRedisTimeOut());
                if (StringUtils.isNotEmpty(passWord)){
                    jedisPool = new JedisPool(new JedisPoolConfig(),host,port,timeOut,passWord,db);
                } else {
                    jedisPool = new JedisPool(new JedisPoolConfig(),host,port,timeOut,null,db);
                }
                keyName = StringUtils.isNotBlank(System.getProperty(CONFIG_KEY_NAME_KEY))?System.getProperty(CONFIG_KEY_NAME_KEY) : FILE_CONFIG.getConfig(getRedisConfigKeyName());
                if (StringUtils.isEmpty(keyName)){
                    keyName = DEFAULT_CONFIG_KEY_NAME;
                }
                initSeataConfig();
            }
        }
    }



    private static void initSeataConfig() {
        try(Jedis jedis = jedisPool.getResource()) {
            if (REDIS_PING_STR.equals(jedis.ping())){
                LOGGER.info("redis configuration connection successful!");
                CONFIG_MAP.clear();
                Map<String, String> configMap = jedis.hgetAll(keyName);
                CONFIG_MAP.putAll(configMap);
            }
        }
    }

    private static String getRedisConfigKeyName() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,ConfigurationKeys.FILE_ROOT_CONFIG,CONFIG_TYPE,CONFIG_KEY_NAME_KEY);
    }

    private static String getRedisTimeOut() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,ConfigurationKeys.FILE_ROOT_CONFIG,CONFIG_TYPE,TIMEOUT_KEY);
    }

    private static String getRedisDb() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,ConfigurationKeys.FILE_ROOT_CONFIG,CONFIG_TYPE,DB_KEY);
    }

    private static String getRedisServerAddr() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,ConfigurationKeys.FILE_ROOT_CONFIG,CONFIG_TYPE,SERVER_ADDR_KEY);
    }
    private static String getRedisPassWord(){
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,ConfigurationKeys.FILE_ROOT_CONFIG,CONFIG_TYPE,PASSWORD_KEY);
    }

    /**
     * Gets type name.
     *
     * @return the type name
     */
    @Override
    public String getTypeName() {
        LOGGER.info("调用了getTypeName");
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
        LOGGER.info("调用了putConfig  dataId={}  content={}   timeoutMills={}",dataId,content,timeoutMills);
        boolean result = false;
        try(Jedis jedis = jedisPool.getResource()) {
            Long status = jedis.hset(keyName, dataId, content);
            if (status == 1 || status == 0){
                initSeataConfig();
                result = true;
            }
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
        LOGGER.info("调用了getLatestConfig  dataId={}  defaultValue={}   timeoutMills={}",dataId,defaultValue,timeoutMills);
        String value = CONFIG_MAP.get(dataId);
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
        LOGGER.info("调用了putConfigIfAbsent  dataId={}  content={}   timeoutMills={}",dataId,content,timeoutMills);
        return false;
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
        LOGGER.info("调用了removeConfig  dataId={}  timeoutMills={}",dataId,timeoutMills);
        boolean result = false;
        try(Jedis jedis = jedisPool.getResource()) {
            Long status = jedis.hdel(keyName, dataId);
            if (status == 1){
                initSeataConfig();
                result = true;
            }
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
        LOGGER.info("调用了addConfigListener  dataId={}  listener={}",dataId,listener);
    }

    /**
     * Remove config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        LOGGER.info("调用了removeConfigListener  dataId={}  listener={}",dataId,listener);
    }

    /**
     * Gets config listeners.
     *
     * @param dataId the data id
     * @return the config listeners
     */
    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        LOGGER.info("调用了getConfigListeners  dataId="+dataId);
        return null;
    }
}
