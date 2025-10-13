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
package org.apache.seata.core.rpc.netty;

import org.apache.seata.core.protocol.ConnectionPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Connection pool monitor for collecting pool statistics and health information.
 *
 * @since 2.1.0
 */
public class ConnectionPoolMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionPoolMonitor.class);

    private final String applicationId;
    private final String transactionServiceGroup;

    public ConnectionPoolMonitor(String applicationId, String transactionServiceGroup) {
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Collect current connection pool information
     */
    public ConnectionPoolInfo collectPoolInfo() {
        ConnectionPoolInfo poolInfo = new ConnectionPoolInfo(applicationId, transactionServiceGroup);
        try {
            List<Object> dataSourceMetrics = DataSourceConnectionPoolCollector.collectAllPoolMetrics();
            poolInfo.setDataSourceMetrics(dataSourceMetrics);
            LOGGER.debug("Collected connection pool information: {}", poolInfo);
        } catch (Exception e) {
            LOGGER.warn("Failed to collect connection pool information", e);
        }
        return poolInfo;
    }
}
