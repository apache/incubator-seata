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
package io.seata.discovery.registry.redis;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.exception.ConfigNotFoundException;
import io.seata.discovery.registry.RegistryHeartBeats;
import io.seata.discovery.registry.RegistryService;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * The type Redis registry service.
 *
 * @author kl @kailing.pub
 */
public class RedisRegistryServiceImpl implements RegistryService<RedisListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRegistryServiceImpl.class);
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String REDIS_FILEKEY_PREFIX = "registry.redis.";
    private static final String REGISTRY_TYPE = "redis";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String REGISTRY_CLUSTER_KEY = "cluster";
    private String clusterName;
    private static final String REDIS_DB = "db";
    private static final String REDIS_PASSWORD = "password";
    private static final ConcurrentMap<String, List<RedisListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Set<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static volatile RedisRegistryServiceImpl instance;
    private static volatile JedisPool jedisPool;

    // redis registry key live 5 seconds, auto refresh key every 2 seconds
    private static final int KEY_TTL = 5;
    private static final long KEY_REFRESH_PERIOD = 2000L;

    private ScheduledExecutorService threadPoolExecutorForSubscribe = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("RedisRegistryService-subscribe", 1));
    private ScheduledExecutorService threadPoolExecutorForUpdateMap = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("RedisRegistryService-updateClusterAddrMap", 1));

    private RedisRegistryServiceImpl() {
        Configuration seataConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;
        this.clusterName = seataConfig.getConfig(REDIS_FILEKEY_PREFIX + REGISTRY_CLUSTER_KEY, DEFAULT_CLUSTER);
        String password = seataConfig.getConfig(getRedisPasswordFileKey());
        String serverAddr = seataConfig.getConfig(getRedisAddrFileKey());
        String[] serverArr = serverAddr.split(":");
        String host = serverArr[0];
        int port = Integer.parseInt(serverArr[1]);
        int db = seataConfig.getInt(getRedisDbFileKey());
        GenericObjectPoolConfig redisConfig = new GenericObjectPoolConfig();
        redisConfig.setTestOnBorrow(seataConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-borrow", true));
        redisConfig.setTestOnReturn(seataConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-return", false));
        redisConfig.setTestWhileIdle(seataConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-while-idle", false));
        int maxIdle = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "max-idle", 0);
        if (maxIdle > 0) {
            redisConfig.setMaxIdle(maxIdle);
        }
        int minIdle = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "min-idle", 0);
        if (minIdle > 0) {
            redisConfig.setMinIdle(minIdle);
        }
        int maxActive = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "max-active", 0);
        if (maxActive > 0) {
            redisConfig.setMaxTotal(maxActive);
        }
        int maxTotal = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "max-total", 0);
        if (maxTotal > 0) {
            redisConfig.setMaxTotal(maxTotal);
        }
        int maxWait = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "max-wait",
                seataConfig.getInt(REDIS_FILEKEY_PREFIX + "timeout", 0));
        if (maxWait > 0) {
            redisConfig.setMaxWaitMillis(maxWait);
        }
        int numTestsPerEvictionRun = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "num-tests-per-eviction-run", 0);
        if (numTestsPerEvictionRun > 0) {
            redisConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
        int timeBetweenEvictionRunsMillis = seataConfig.getInt(
                REDIS_FILEKEY_PREFIX + "time-between-eviction-runs-millis", 0);
        if (timeBetweenEvictionRunsMillis > 0) {
            redisConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
        int minEvictableIdleTimeMillis = seataConfig.getInt(REDIS_FILEKEY_PREFIX + "min-evictable-idle-time-millis",
                0);
        if (minEvictableIdleTimeMillis > 0) {
            redisConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }
        if (StringUtils.isNullOrEmpty(password)) {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, db);
        } else {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, password, db);
        }
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static RedisRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (RedisRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new RedisRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) {
        NetUtil.validAddress(address);
        doRegisterOrExpire(address, true);
        RegistryHeartBeats.addHeartBeat(REGISTRY_TYPE, address, KEY_REFRESH_PERIOD, this::doRegisterOrExpire);
    }

    private void doRegisterOrExpire(InetSocketAddress address) {
        doRegisterOrExpire(address, false);
    }

    private void doRegisterOrExpire(InetSocketAddress address, boolean publish) {
        String serverAddr = NetUtil.toStringAddress(address);
        String key = getRedisRegistryKey() + "_" + serverAddr;  // key = registry.redis.${cluster}_ip:port
        try (Jedis jedis = jedisPool.getResource(); Pipeline pipelined = jedis.pipelined()) {
            pipelined.setex(key, KEY_TTL, ManagementFactory.getRuntimeMXBean().getName());
            if (publish) {
                pipelined.publish(getRedisRegistryKey(), serverAddr + "-" + RedisListener.REGISTER);
            }
            pipelined.sync();
        }
    }

    @Override
    public void unregister(InetSocketAddress address) {
        NetUtil.validAddress(address);
        String serverAddr = NetUtil.toStringAddress(address);
        try (Jedis jedis = jedisPool.getResource(); Pipeline pipelined = jedis.pipelined()) {
            pipelined.hdel(getRedisRegistryKey(), serverAddr);
            pipelined.publish(getRedisRegistryKey(), serverAddr + "-" + RedisListener.UN_REGISTER);
            pipelined.sync();
        }
    }

    @Override
    public void subscribe(String cluster, RedisListener listener) {
        String redisRegistryKey = REDIS_FILEKEY_PREFIX + cluster;
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>())
                .add(listener);


        threadPoolExecutorForUpdateMap.scheduleAtFixedRate(() -> {
            try {
                try (Jedis jedis = jedisPool.getResource()) {
                    // try update Map every 2s
                    updateClusterAddressMap(jedis, redisRegistryKey, cluster);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, 0, KEY_REFRESH_PERIOD, TimeUnit.MILLISECONDS);

        threadPoolExecutorForSubscribe.scheduleAtFixedRate(() -> {
            try {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(new NotifySub(LISTENER_SERVICE_MAP.get(cluster)), redisRegistryKey);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unsubscribe(String cluster, RedisListener listener) {
    }

    @Override
    public List<InetSocketAddress> lookup(String key) {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }
        return lookupByCluster(clusterName);
    }

    // default visible for test
    List<InetSocketAddress> lookupByCluster(String clusterName) {
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            String redisRegistryKey = REDIS_FILEKEY_PREFIX + clusterName;
            try (Jedis jedis = jedisPool.getResource()) {
                updateClusterAddressMap(jedis, redisRegistryKey, clusterName);
            }
            subscribe(clusterName, msg -> {
                String[] msgr = msg.split("-");
                String serverAddr = msgr[0];
                String eventType = msgr[1];
                switch (eventType) {
                    case RedisListener.REGISTER:
                        CLUSTER_ADDRESS_MAP.get(clusterName).add(NetUtil.toInetSocketAddress(serverAddr));
                        break;
                    case RedisListener.UN_REGISTER:
                        CLUSTER_ADDRESS_MAP.get(clusterName).remove(NetUtil.toInetSocketAddress(serverAddr));
                        break;
                    default:
                        throw new ShouldNeverHappenException("unknown redis msg:" + msg);
                }
            });
        }
        return new ArrayList<>(CLUSTER_ADDRESS_MAP.getOrDefault(clusterName, Collections.emptySet()));
    }

    @Override
    public void close() {
        threadPoolExecutorForSubscribe.shutdown();
        threadPoolExecutorForUpdateMap.shutdown();
        RegistryHeartBeats.close();
        jedisPool.destroy();
    }

    private static class NotifySub extends JedisPubSub {

        private final List<RedisListener> redisListeners;

        /**
         * Instantiates a new Notify sub.
         *
         * @param redisListeners the redis listeners
         */
        NotifySub(List<RedisListener> redisListeners) {
            this.redisListeners = redisListeners;
        }

        @Override
        public void onMessage(String key, String msg) {
            for (RedisListener listener : redisListeners) {
                try {
                    listener.onEvent(msg);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    private void updateClusterAddressMap(Jedis jedis, String redisRegistryKey, String clusterName) {
        ScanParams scanParams = new ScanParams();
        scanParams.count(10);
        scanParams.match(redisRegistryKey + "_*");
        String cursor = ScanParams.SCAN_POINTER_START;
        Set<InetSocketAddress> newAddressSet = new HashSet<>();
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            cursor = scanResult.getCursor();
            List<String> instances = scanResult.getResult();
            if (instances != null && !instances.isEmpty()) {
                // key = registry.redis.${cluster}_ip:port
                Set<InetSocketAddress> part = instances.stream().map(key -> {
                    String[] split = key.split("_");
                    return NetUtil.toInetSocketAddress(split[1]);
                }).collect(Collectors.toSet());

                newAddressSet.addAll(part);
            }
        } while (!cursor.equals(ScanParams.SCAN_POINTER_START));

        if (!newAddressSet.equals(CLUSTER_ADDRESS_MAP.get(clusterName))) {
            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressSet);
        }
    }

    private String getRedisRegistryKey() {
        return REDIS_FILEKEY_PREFIX + clusterName;
    }

    private String getRedisAddrFileKey() {
        return REDIS_FILEKEY_PREFIX + PRO_SERVER_ADDR_KEY;
    }

    private String getRedisPasswordFileKey() {
        return REDIS_FILEKEY_PREFIX + REDIS_PASSWORD;
    }

    private String getRedisDbFileKey() {
        return REDIS_FILEKEY_PREFIX + REDIS_DB;
    }

}
