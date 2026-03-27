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
package org.apache.seata.core.rpc.processor.client;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.protocol.ConnectionPoolMetricsMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.DataSourceConnectionPoolCollector;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Client-side processor for connection pool metrics.
 * Collects and sends pool metrics to server.
 */
public class ClientConnectionPoolMetricsProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionPoolMetricsProcessor.class);
    private static final long REPORT_INTERVAL_MS = 10000;

    private volatile long lastReportTime = System.currentTimeMillis();
    private volatile boolean enableConnectionPoolMetrics;
    private volatile int httpPort;
    private final AtomicLong sequence = new AtomicLong(0);
    private final String applicationId;

    public ClientConnectionPoolMetricsProcessor(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setEnableConnectionPoolMetrics(boolean enableConnectionPoolMetrics) {
        this.enableConnectionPoolMetrics = enableConnectionPoolMetrics;
    }

    public void setHttpPort(int port) {
        this.httpPort = port;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("client received metrics processor message: {}", rpcMessage.getBody());
        }
    }

    /**
     * Create a metrics message to send to server.
     */
    public Object createMetricsMessage() {
        ConnectionPoolMetricsMessage msg = new ConnectionPoolMetricsMessage();
        msg.setApplicationId(applicationId);
        msg.setClientUrl(getClientHttpUrl());
        msg.setDruidMetrics(DataSourceConnectionPoolCollector.collectAllDruidPoolMetrics());
        msg.setHikariMetrics(DataSourceConnectionPoolCollector.collectAllHikariPoolMetrics());
        msg.setSequenceNumber(sequence.incrementAndGet());
        lastReportTime = System.currentTimeMillis();
        msg.setTimestamp(lastReportTime);
        return msg;
    }

    /**
     * Whether metrics should be reported now.
     */
    public boolean shouldReportPoolInfo() {
        return enableConnectionPoolMetrics && (System.currentTimeMillis() - lastReportTime) >= REPORT_INTERVAL_MS;
    }

    /**
     * Get client HTTP URL (address:port).
     * Priority: override by setter -> system properties/env -> default 8080
     */
    private String getClientHttpUrl() {
        try {
            String localIp = NetUtil.getLocalIp();
            int httpPort = getHttpPortFromConfig();
            return localIp + ":" + httpPort;
        } catch (Exception e) {
            LOGGER.debug("Failed to get HTTP port from config, fallback to default", e);
            return NetUtil.getLocalIp() + ":8080";
        }
    }

    /**
     * Get HTTP port from configuration.
     * Checks: override -> server.port property -> environment variable -> default 8080
     */
    private int getHttpPortFromConfig() {
        // Override from upper layer (preferred)
        if (httpPort > 0) {
            return httpPort;
        }

        // Try to get from Spring Boot configuration
        String portStr = System.getProperty("server.port");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid server.port system property: {}", portStr);
            }
        }

        // Try environment variable
        portStr = System.getenv("SERVER_PORT");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid SERVER_PORT environment variable: {}", portStr);
            }
        }

        // Default port
        return 8080;
    }
}
