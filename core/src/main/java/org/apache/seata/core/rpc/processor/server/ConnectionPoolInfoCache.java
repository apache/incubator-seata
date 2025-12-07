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
package org.apache.seata.core.rpc.processor.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.PoolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Cache for storing client connection pool information.
 * connection pool metrics from multiple clients.
 *
 */
public class ConnectionPoolInfoCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolInfoCache.class);

    public static final Cache<String, HikariConnectionPoolMetrics> hikariCPCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    public static final Cache<String, DruidConnectionPoolMetrics> druidCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    public static final Cache<String, PoolType> poolTypeCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    public static final Cache<String, String> clientUrlCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private ConnectionPoolInfoCache() {}

    public static ConnectionPoolInfoCache getInstance() {
        return ConnectionPoolInfoCacheHolder.INSTANCE;
    }

    private static class ConnectionPoolInfoCacheHolder {
        private static final ConnectionPoolInfoCache INSTANCE = new ConnectionPoolInfoCache();
    }

    /**
     * Process HikariCP connection pool information for a specific client.
     */
    public void processHikariCPData(String poolName, HikariConnectionPoolMetrics hikariMetrics, String clientUrl) {
        if (poolName == null || hikariMetrics == null) {
            LOGGER.warn("Cannot put hikari pool info: poolName={}, hikariMetrics={}", poolName, hikariMetrics);
            return;
        }
        hikariCPCache.put(poolName, hikariMetrics);
        poolTypeCache.put(poolName, PoolType.HikariCP);
        clientUrlCache.put(poolName, clientUrl);
    }

    /**
     * Process Druid connection pool information for a specific client.
     */
    public void processDruidData(String poolName, DruidConnectionPoolMetrics druidMetrics, String clientUrl) {
        if (poolName == null || druidMetrics == null) {
            LOGGER.warn("Cannot put druid pool info: poolName={}, druidMetrics={}", poolName, druidMetrics);
            return;
        }
        druidCache.put(poolName, druidMetrics);
        poolTypeCache.put(poolName, PoolType.Druid);
        clientUrlCache.put(poolName, clientUrl);
    }

    public List<HikariConnectionPoolMetrics> getAllHikariCP() {
        return new ArrayList<>(hikariCPCache.asMap().values());
    }

    public List<DruidConnectionPoolMetrics> getAllDruid() {
        return new ArrayList<>(druidCache.asMap().values());
    }

    public String getClientUrl(String poolName) {
        return clientUrlCache.getIfPresent(poolName);
    }

    /**
     * Refresh connection pool configuration
     */
    public void refresh(PoolConfigUpdateRequest request) {
        LOGGER.info("{}", request);
        String poolName = request.getPoolName();
        PoolType poolType = poolTypeCache.getIfPresent(poolName);
        if (poolType == PoolType.HikariCP) {
            HikariConnectionPoolMetrics hikariCP = hikariCPCache.getIfPresent(poolName);
            if (Objects.isNull(hikariCP)) {
                return;
            }
            hikariCP.setMaxPoolSize(request.getMaxPoolSize());
            hikariCP.setMinIdle(request.getMinIdle());
            hikariCP.setConnectionTimeout(request.getConnectionTimeout());
            hikariCP.setMaxLifeTime(request.getMaxLifeTime());
            hikariCP.setKeepaliveTime(request.getKeepaliveTime());
        } else if (poolType == PoolType.Druid) {
            DruidConnectionPoolMetrics druid = druidCache.getIfPresent(poolName);
            if (Objects.isNull(druid)) {
                return;
            }
            druid.setMaxPoolSize(request.getMaxPoolSize());
            druid.setMinIdle(request.getMinIdle());
            druid.setConnectionTimeout(request.getConnectionTimeout());
            druid.setTimeBetweenEvictionRunsMills(request.getTimeBetweenEvictionRunsMills());
            druid.setMaxEvictableTimeMills(request.getMaxEvictableTimeMills());
        }
        LOGGER.info("Connection pool config refresh successfully");
    }
}
