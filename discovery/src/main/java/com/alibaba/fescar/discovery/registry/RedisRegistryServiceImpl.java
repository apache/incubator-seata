/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kl on 2019/2/19.
 * Content :
 */
public class RedisRegistryServiceImpl implements RegistryService<RedisListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRegistryServiceImpl.class);
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String REDIS_FILEKEY_PREFIX = "registry.redis.";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String REGISTRY_CLUSTER_KEY = "cluster";
    private static String REGISTRY_CLUSTER_VALUE;
    private static final String REDIS_DB = "db";
    private static final String REDIS_PASSWORD = "password";
    private final int RENEWAL_TIME = 60 * 1000;//一分钟
    private static final ConcurrentMap<String, List<RedisListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static volatile RedisRegistryServiceImpl instance;
    private static volatile JedisPool jedisPool;
    private static volatile AtomicBoolean renew = new AtomicBoolean();
    private ExecutorService renewExecutor = Executors.newCachedThreadPool();

    private RedisRegistryServiceImpl() {
        Configuration fescarConfig = ConfigurationFactory.getInstance();
        this.REGISTRY_CLUSTER_VALUE = fescarConfig.getConfig(REDIS_FILEKEY_PREFIX + REGISTRY_CLUSTER_KEY, DEFAULT_CLUSTER);
        String password = fescarConfig.getConfig(getRedisPasswordFileKey());
        String serverAddr = fescarConfig.getConfig(getRedisAddrFileKey());
        String host = serverAddr.split(":")[0];
        int port = Integer.valueOf(serverAddr.split(":")[1]);
        int db = fescarConfig.getInt(getRedisDbFileKey());
        GenericObjectPoolConfig redisConfig = new GenericObjectPoolConfig();
        redisConfig.setTestOnBorrow(fescarConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test.on.borrow", true));
        redisConfig.setTestOnReturn(fescarConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test.on.return", false));
        redisConfig.setTestWhileIdle(fescarConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test.while.idle", false));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.idle", 0) > 0)
            redisConfig.setMaxIdle(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.idle", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "min.idle", 0) > 0)
            redisConfig.setMinIdle(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "min.idle", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.active", 0) > 0)
            redisConfig.setMaxTotal(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.active", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.total", 0) > 0)
            redisConfig.setMaxTotal(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.total", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.wait", fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "timeout", 0)) > 0)
            redisConfig.setMaxWaitMillis(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "max.wait", fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "timeout", 0)));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "num.tests.per.eviction.run", 0) > 0)
            redisConfig.setNumTestsPerEvictionRun(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "num.tests.per.eviction.run", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "time.between.eviction.runs.millis", 0) > 0)
            redisConfig.setTimeBetweenEvictionRunsMillis(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "time.between.eviction.runs.millis", 0));
        if (fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "min.evictable.idle.time.millis", 0) > 0)
            redisConfig.setMinEvictableIdleTimeMillis(fescarConfig.getInt(REDIS_FILEKEY_PREFIX + "min.evictable.idle.time.millis", 0));
        if (StringUtils.isEmpty(password)) {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, db);
        } else {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, password, db);
        }
    }

    public static RedisRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (RedisRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new RedisRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        Jedis jedis = jedisPool.getResource();
        String serverAddr = NetUtil.toStringAddress(address);
        jedis.hset(getRedisRegistryKey(), serverAddr, ManagementFactory.getRuntimeMXBean().getName());
        jedis.publish(getRedisRegistryKey(), serverAddr + "-" + RedisListener.REGISTER);
    }
    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        Jedis jedis = jedisPool.getResource();
        String serverAddr = NetUtil.toStringAddress(address);
        jedis.hdel(getRedisRegistryKey(), serverAddr);
        jedis.publish(getRedisRegistryKey(), serverAddr + "-" + RedisListener.UN_REGISTER);
    }

    @Override
    public void subscribe(String cluster, RedisListener listener) throws Exception {
        String redis_registry_key = REDIS_FILEKEY_PREFIX + cluster;
        LISTENER_SERVICE_MAP.putIfAbsent(cluster, new ArrayList<>());
        LISTENER_SERVICE_MAP.get(cluster).add(listener);
        renewExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Jedis jedis = jedisPool.getResource();
                    jedis.subscribe(new NotifySub(LISTENER_SERVICE_MAP.get(cluster)), redis_registry_key);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void unsubscribe(String cluster, RedisListener listener) throws Exception {
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        String clusterName = config.getConfig(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key);
        if (null == clusterName) {
            return null;
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            Jedis jedis = jedisPool.getResource();
            Map<String, String> instances = jedis.hgetAll(getRedisRegistryKey());
            if (null != instances) {
                List<InetSocketAddress> newAddressList = new ArrayList<>();
                for (Map.Entry<String, String> instance : instances.entrySet()) {
                    String serverAddr = instance.getKey();
                    newAddressList.add(NetUtil.toInetSocketAddress(serverAddr));
                }
                CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
            }
        }
        subscribe(clusterName, new RedisListener() {
            @Override
            public void onEvent(String msg) {
                String serverAddr = msg.split("-")[0];
                String eventType = msg.split("-")[1];
                switch (eventType) {
                    case RedisListener.REGISTER:
                        CLUSTER_ADDRESS_MAP.get(clusterName).add(NetUtil.toInetSocketAddress(serverAddr));
                        break;
                    case RedisListener.UN_REGISTER:
                        CLUSTER_ADDRESS_MAP.get(clusterName).remove(NetUtil.toInetSocketAddress(serverAddr));
                        break;
                }
            }
        });
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    private class NotifySub extends JedisPubSub {

        private final List<RedisListener> redisListeners;

        public NotifySub(List<RedisListener> redisListeners) {
            this.redisListeners = redisListeners;
        }

        @Override
        public void onMessage(String key, String msg) {
            for (RedisListener listener : redisListeners) {
                listener.onEvent(msg);
            }
        }
    }

    private static String getRedisRegistryKey(){
        return REDIS_FILEKEY_PREFIX + REGISTRY_CLUSTER_VALUE;
    }

    private static String getRedisAddrFileKey() {
        return REDIS_FILEKEY_PREFIX + PRO_SERVER_ADDR_KEY;
    }

    private static String getRedisPasswordFileKey() {
        return REDIS_FILEKEY_PREFIX + REDIS_PASSWORD;
    }

    private static String getRedisDbFileKey() {
        return REDIS_FILEKEY_PREFIX + REDIS_DB;
    }
}
