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

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.ConnectionPoolMetricsMessage;
import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Processor for handling connection pool metrics messages on server side.
 */
public class ServerConnectionPoolMetricsProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConnectionPoolMetricsProcessor.class);

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        Object body = rpcMessage.getBody();
        if (!(body instanceof ConnectionPoolMetricsMessage)) {
            LOGGER.warn("Unexpected message for metrics processor: {}", body);
            return;
        }
        ConnectionPoolMetricsMessage metricsMessage = (ConnectionPoolMetricsMessage) body;
        handleConnectionPoolMetrics(metricsMessage);
    }

    private void handleConnectionPoolMetrics(ConnectionPoolMetricsMessage metricsMessage) {
        LOGGER.info("received connection pool metrics from {}", metricsMessage.getApplicationId());
        List<DruidConnectionPoolMetrics> druidMetrics = metricsMessage.getDruidMetrics();
        if (druidMetrics != null && !druidMetrics.isEmpty()) {
            LOGGER.info("druid metrics: {}", druidMetrics.size());
            druidMetrics.stream().filter(Objects::nonNull).forEach(druidConnectionPoolMetrics -> {
                ConnectionPoolInfoCache.getInstance()
                        .processDruidData(
                                druidConnectionPoolMetrics.getPoolName(),
                                druidConnectionPoolMetrics,
                                metricsMessage.getClientUrl());
            });
        }
        List<HikariConnectionPoolMetrics> hikariMetrics = metricsMessage.getHikariMetrics();
        if (hikariMetrics != null && !hikariMetrics.isEmpty()) {
            LOGGER.info("hikari metrics: {}", hikariMetrics.size());
            hikariMetrics.stream().filter(Objects::nonNull).forEach(hikariConnectionPoolMetrics -> {
                ConnectionPoolInfoCache.getInstance()
                        .processHikariCPData(
                                hikariConnectionPoolMetrics.getPoolName(),
                                hikariConnectionPoolMetrics,
                                metricsMessage.getClientUrl());
            });
        }
    }
}
