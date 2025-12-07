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
package org.apache.seata.spring.boot.autoconfigure.controller;

import org.apache.seata.core.model.PoolConfigUpdateRequest;
import org.apache.seata.core.rpc.netty.DataSourceConnectionPoolCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Client-side REST controller for handling connection pool configuration updates.
 *
 */
@RestController
@RequestMapping("/client/pool")
public class ClientConnectionPoolController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionPoolController.class);

    /**
     * Update connection pool configuration.
     * This endpoint is called by the Seata server to update client-side connection pool settings.
     *
     * @param request the configuration update request containing new parameter values
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateConfig(@RequestBody PoolConfigUpdateRequest request) {
        try {
            // Validate request parameters
            if (request == null
                    || request.getPoolName() == null
                    || request.getPoolName().trim().isEmpty()) {
                LOGGER.warn("Invalid configuration update request: {}", request);
                return ResponseEntity.badRequest().body("Invalid request: poolName is required");
            }

            LOGGER.info("Received connection pool configuration update request for pool: {}", request.getPoolName());
            LOGGER.debug("Configuration update details: {}", request);

            // Validate configuration parameters
            if (!isValidConfigRequest(request)) {
                LOGGER.warn("Invalid configuration parameters in request: {}", request);
                return ResponseEntity.badRequest().body("Invalid configuration parameters");
            }

            // Apply configuration update using DataSourceConnectionPoolCollector
            boolean success = DataSourceConnectionPoolCollector.updateConfig(request.getPoolName(), request);

            if (success) {
                LOGGER.info("Successfully updated connection pool configuration for pool: {}", request.getPoolName());
                return ResponseEntity.ok("Configuration updated successfully");
            } else {
                LOGGER.warn("Failed to update connection pool configuration for pool: {}", request.getPoolName());
                return ResponseEntity.internalServerError().body("Failed to update configuration");
            }

        } catch (Exception e) {
            LOGGER.error(
                    "Error processing configuration update request for pool: {}",
                    request != null ? request.getPoolName() : "unknown",
                    e);
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Validate configuration update request parameters.
     *
     * @param request the configuration update request
     * @return true if the request contains valid parameters, false otherwise
     */
    private boolean isValidConfigRequest(PoolConfigUpdateRequest request) {
        // Check for negative values that would be invalid
        if (request.getMaxPoolSize() < 0 || request.getMinIdle() < 0 || request.getConnectionTimeout() < 0) {
            return false;
        }

        // Check for unreasonable values
        if (request.getMaxPoolSize() > 0
                && request.getMinIdle() > 0
                && request.getMinIdle() > request.getMaxPoolSize()) {
            LOGGER.warn(
                    "minIdle ({}) cannot be greater than maxPoolSize ({})",
                    request.getMinIdle(),
                    request.getMaxPoolSize());
            return false;
        }

        // Check for negative time values
        if (request.getTimeBetweenEvictionRunsMills() < 0
                || request.getMaxEvictableTimeMills() < 0
                || request.getMaxLifeTime() < 0
                || request.getKeepaliveTime() < 0) {
            return false;
        }

        return true;
    }

    /**
     * Health check endpoint to verify the controller is available.
     *
     * @return ResponseEntity with status information
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Client connection pool controller is running");
    }
}
