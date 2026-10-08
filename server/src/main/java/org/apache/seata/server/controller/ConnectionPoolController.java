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

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.core.protocol.ConnectionPoolInfo;
import org.apache.seata.core.rpc.netty.DataSourceConnectionPoolCollector;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for accessing client connection pool information.
 * Provides REST endpoints to query cached connection pool metrics
 * received from clients via enhanced heartbeats.
 *
 * @since 2.1.0
 */
@RestController
@RequestMapping("/api/v1/connection-pool")
public class ConnectionPoolController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolController.class);

    private NettyRemotingServer nettyRemotingServer;

    /**
     * Get connection pool information for all clients.
     *
     * @return Result containing all client connection pool information
     */
    @GetMapping("/info")
    public ResponseEntity<SingleResult<Map<String, Object>>> getAllConnectionPoolInfo() {
        SingleResult<Map<String, Object>> result = new SingleResult<>("0", "成功");

        try {
            Map<String, ConnectionPoolInfo> allPoolInfo = nettyRemotingServer.getAllClientPoolInfo();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("totalClients", allPoolInfo.size());
            responseData.put("clients", allPoolInfo);
            responseData.put("timestamp", System.currentTimeMillis());

            result.setData(responseData);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrieved connection pool info for {} clients", allPoolInfo.size());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to retrieve connection pool information", e);
            result.setCode("500");
            result.setMessage("Failed to retrieve connection pool information: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get connection pool information for a specific client.
     *
     * @param clientAddress the client address (IP:port)
     * @return SingleResult containing the client's connection pool information
     */
    @GetMapping("/info/{clientAddress}")
    public ResponseEntity<SingleResult<Map<String, Object>>> getConnectionPoolInfo(@PathVariable String clientAddress) {
        SingleResult<Map<String, Object>> result = new SingleResult<>("0", "成功");

        try {
            // Decode the client address (replace underscores with colons for IP:port format)
            String decodedAddress = clientAddress.replace("_", ":");

            ConnectionPoolInfo poolInfo = nettyRemotingServer.getClientPoolInfo(decodedAddress);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("clientAddress", decodedAddress);
            responseData.put("poolInfo", poolInfo);
            responseData.put("timestamp", System.currentTimeMillis());

            result.setData(responseData);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrieved connection pool info for client: {}", decodedAddress);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to retrieve connection pool information for client: {}", clientAddress, e);
            result.setCode("500");
            result.setMessage("Failed to retrieve connection pool information: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    public void setNettyRemotingServer(NettyRemotingServer nettyRemotingServer) {
        this.nettyRemotingServer = nettyRemotingServer;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Object>> getAllConnectionPools() {
        List<Object> result = DataSourceConnectionPoolCollector.collectAllPoolMetrics();
        return ResponseEntity.ok(result);
    }
}
