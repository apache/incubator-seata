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
package org.apache.seata.server.metrics;

import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.PoolType;
import org.apache.seata.core.rpc.processor.server.ConnectionPoolInfoCache;
import org.apache.seata.server.common.HttpClient;
import org.apache.seata.server.metrics.vo.ConnectionPoolConfigVO;
import org.apache.seata.server.metrics.vo.ConnectionPoolMetricsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Connection pool service class
 * This class provides connection pool metrics and configuration management functionality.
 */
public class ConnectionPoolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolService.class);

    private final HttpClient httpClient;

    public ConnectionPoolService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get connection pool metrics by pool type
     */
    public List<ConnectionPoolMetricsVO> getMetricsByType(PoolType poolType) {
        if (Objects.equals(poolType, PoolType.Druid)) {
            List<DruidConnectionPoolMetrics> druids =
                    ConnectionPoolInfoCache.getInstance().getAllDruid();
            return druids.stream().map(ConnectionPoolMetricsVO::of).collect(Collectors.toList());
        } else if (Objects.equals(poolType, PoolType.HikariCP)) {
            List<HikariConnectionPoolMetrics> hikariCPs =
                    ConnectionPoolInfoCache.getInstance().getAllHikariCP();
            return hikariCPs.stream().map(ConnectionPoolMetricsVO::of).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get connection pool metrics for all services
     */
    public List<ConnectionPoolMetricsVO> getAllMetrics() {
        List<DruidConnectionPoolMetrics> druids =
                ConnectionPoolInfoCache.getInstance().getAllDruid();
        List<HikariConnectionPoolMetrics> hikariCPs =
                ConnectionPoolInfoCache.getInstance().getAllHikariCP();
        List<ConnectionPoolMetricsVO> metrics = new ArrayList<>(druids.size() + hikariCPs.size());
        druids.forEach(druidDTO -> metrics.add(ConnectionPoolMetricsVO.of(druidDTO)));
        hikariCPs.forEach(hikariCPDTO -> metrics.add(ConnectionPoolMetricsVO.of(hikariCPDTO)));
        LOGGER.info("getAllMetrics: {}", metrics.size());
        return metrics;
    }

    /**
     * Get connection pool configuration by pool type
     */
    public List<ConnectionPoolConfigVO> getConfigByType(PoolType poolType) {
        if (Objects.equals(poolType, PoolType.Druid)) {
            List<DruidConnectionPoolMetrics> druids =
                    ConnectionPoolInfoCache.getInstance().getAllDruid();
            return druids.stream().map(ConnectionPoolConfigVO::of).collect(Collectors.toList());
        } else if (Objects.equals(poolType, PoolType.HikariCP)) {
            List<HikariConnectionPoolMetrics> hikariCPs =
                    ConnectionPoolInfoCache.getInstance().getAllHikariCP();
            return hikariCPs.stream().map(ConnectionPoolConfigVO::of).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get connection pool configuration for all services
     */
    public List<ConnectionPoolConfigVO> getAllConfig() {
        List<DruidConnectionPoolMetrics> druids =
                ConnectionPoolInfoCache.getInstance().getAllDruid();
        List<HikariConnectionPoolMetrics> hikariCPs =
                ConnectionPoolInfoCache.getInstance().getAllHikariCP();
        List<ConnectionPoolConfigVO> configs = new ArrayList<>(druids.size() + hikariCPs.size());
        druids.forEach(druidDTO -> configs.add(ConnectionPoolConfigVO.of(druidDTO)));
        hikariCPs.forEach(hikariCPDTO -> configs.add(ConnectionPoolConfigVO.of(hikariCPDTO)));
        LOGGER.info("getAllConfig: {}", configs.size());
        return configs;
    }

    /**
     * Notify downstream service to update connection pool configuration
     *
     * @param poolName service name
     * @param request  configuration update request
     */
    public boolean updateConfig(String poolName, PoolConfigUpdateRequest request) {
        String clientUrl = ConnectionPoolInfoCache.getInstance().getClientUrl(poolName);
        return httpClient.notifyDownstreamService(poolName, clientUrl, request);
    }
}
