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

import org.apache.seata.core.protocol.ConnectionPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for storing client connection pool information received via enhanced heartbeats.
 * This cache provides thread-safe operations for storing, retrieving, and cleaning up
 * connection pool metrics from multiple clients.
 *
 * @since 2.1.0
 */
public class ConnectionPoolInfoCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolInfoCache.class);

    private final Map<String, CachedPoolInfo> cache = new ConcurrentHashMap<>();

    /**
     * Update connection pool information for a specific client.
     *
     * @param clientAddress the client address (IP:port)
     * @param poolInfo      the connection pool information to cache
     */
    void updatePoolInfo(String clientAddress, ConnectionPoolInfo poolInfo) {
        if (clientAddress == null || poolInfo == null) {
            LOGGER.warn("Cannot update pool info: clientAddress={}, poolInfo={}", clientAddress, poolInfo);
            return;
        }

        CachedPoolInfo oldInfo = cache.put(clientAddress, new CachedPoolInfo(poolInfo, System.currentTimeMillis()));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updated pool info for client {}, previous info existed: {}", clientAddress, oldInfo != null);
        }
    }

    /**
     * Get cached connection pool information for a specific client.
     *
     * @param clientAddress the client address (IP:port)
     * @return the cached connection pool information, or null if not found
     */
    ConnectionPoolInfo getPoolInfo(String clientAddress) {
        if (clientAddress == null) {
            return null;
        }

        CachedPoolInfo cached = cache.get(clientAddress);
        return cached != null ? cached.poolInfo : null;
    }

    /**
     * Get all cached connection pool information.
     *
     * @return a map of client address to connection pool information
     */
    Map<String, ConnectionPoolInfo> getAllPoolInfo() {
        Map<String, ConnectionPoolInfo> result = new ConcurrentHashMap<>();
        cache.forEach((key, value) -> result.put(key, value.poolInfo));
        return result;
    }

    private static class CachedPoolInfo {
        final ConnectionPoolInfo poolInfo;
        final long timestamp;

        CachedPoolInfo(ConnectionPoolInfo poolInfo, long timestamp) {
            this.poolInfo = poolInfo;
            this.timestamp = timestamp;
        }
    }
}
