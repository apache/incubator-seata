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
package org.apache.seata.server.controller;

import org.apache.seata.core.model.ApiResponse;
import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.protocol.PoolType;
import org.apache.seata.server.metrics.ConnectionPoolService;
import org.apache.seata.server.metrics.vo.ConnectionPoolConfigVO;
import org.apache.seata.server.metrics.vo.ConnectionPoolMetricsVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Server-side REST controller for accessing client connection pool information.
 * Provides REST endpoints to query cached connection pool metrics
 *
 */
@RestController
@RequestMapping("/api/pool")
@CrossOrigin(
        origins = {"http://127.0.0.1:30000", "http://127.0.0.1:8081"},
        allowCredentials = "true")
@ConditionalOnProperty(name = "seata.enableConnectionPoolMetrics", havingValue = "true")
public class ConnectionPoolController {

    private final ConnectionPoolService connectionPoolService;

    public ConnectionPoolController(ConnectionPoolService connectionPoolService) {
        this.connectionPoolService = connectionPoolService;
    }

    /**
     * Get connection pool metrics by pool type
     *
     * @return list of connection pool metrics
     */
    @GetMapping("/metrics/type/{poolType}")
    public ApiResponse<List<ConnectionPoolMetricsVO>> getMetricsByType(@PathVariable("poolType") PoolType poolType) {
        return ApiResponse.success(connectionPoolService.getMetricsByType(poolType));
    }

    /**
     * Get connection pool metrics for all services
     *
     * @return list of all connection pool metrics
     */
    @GetMapping("/metrics")
    public ApiResponse<List<ConnectionPoolMetricsVO>> getAllMetrics() {
        return ApiResponse.success(connectionPoolService.getAllMetrics());
    }

    /**
     * Get connection pool configuration by pool type
     *
     * @return list of connection pool configuration
     */
    @GetMapping("/config/type/{poolType}")
    public ApiResponse<List<ConnectionPoolConfigVO>> getConfigByType(@PathVariable("poolType") PoolType poolType) {
        return ApiResponse.success(connectionPoolService.getConfigByType(poolType));
    }

    /**
     * Get connection pool configuration for all services
     *
     * @return list of all connection pool configuration
     */
    @GetMapping("/config")
    public ApiResponse<List<ConnectionPoolConfigVO>> getAllConfig() {
        return ApiResponse.success(connectionPoolService.getAllConfig());
    }

    /**
     * Update connection pool configuration
     *
     * @param poolName connection pool name
     * @param request  configuration update request
     * @return operation result
     */
    @PutMapping("/config/{poolName}")
    public ApiResponse<Boolean> updateConfig(
            @PathVariable("poolName") String poolName, @RequestBody @Validated PoolConfigUpdateRequest request) {
        try {
            if (poolName == null || request == null) {
                return ApiResponse.of(-1, "Invalid request parameters", false);
            }
            if (!isValidConfigRequest(request)) {
                return ApiResponse.of(-1, "Invalid configuration parameters", false);
            }
            request.setPoolName(poolName);
            boolean success = connectionPoolService.updateConfig(poolName, request);
            if (success) {
                return ApiResponse.success(true);
            } else {
                return ApiResponse.of(-1, "Failed to update configuration for service: " + poolName, false);
            }
        } catch (Exception e) {
            return ApiResponse.of(-1, "Error updating configuration: " + e.getMessage(), false);
        }
    }

    /**
     * Validate the validity of configuration update request parameters
     *
     * @param request configuration update request
     * @return true if parameters are valid, false otherwise
     */
    private boolean isValidConfigRequest(PoolConfigUpdateRequest request) {
        // Check for negative values
        if (request.getMaxPoolSize() < 0 || request.getMinIdle() < 0 || request.getConnectionTimeout() < 0) {
            return false;
        }
        // Check for unreasonable values: minIdle cannot be greater than maxPoolSize
        if (request.getMaxPoolSize() > 0
                && request.getMinIdle() > 0
                && request.getMinIdle() > request.getMaxPoolSize()) {
            return false;
        }
        // Check for negative values in time-related parameters
        return request.getTimeBetweenEvictionRunsMills() >= 0
                && request.getMaxEvictableTimeMills() >= 0
                && request.getMaxLifeTime() >= 0
                && request.getKeepaliveTime() >= 0;
    }
}
