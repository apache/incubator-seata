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
package org.apache.seata.core.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Connection pool metrics message.
 * Carries Druid/Hikari metrics from client to server.
 */
public class ConnectionPoolMetricsMessage extends AbstractMessage {

    private static final long serialVersionUID = 1L;

    /**
     * client application ID
     */
    private String applicationId;

    /**
     * client URL (address:port)
     */
    private String clientUrl;

    /**
     * HikariCP metrics
     */
    private List<HikariConnectionPoolMetrics> hikariMetrics = new ArrayList<>();

    /**
     * Druid metrics
     */
    private List<DruidConnectionPoolMetrics> druidMetrics = new ArrayList<>();

    /**
     * sequence number
     */
    private long sequenceNumber;

    /**
     * collect timestamp
     */
    private long timestamp;

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_CONNECTION_POOL_METRICS;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public void setClientUrl(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public List<HikariConnectionPoolMetrics> getHikariMetrics() {
        return hikariMetrics;
    }

    public void setHikariMetrics(List<HikariConnectionPoolMetrics> hikariMetrics) {
        this.hikariMetrics = hikariMetrics;
    }

    public List<DruidConnectionPoolMetrics> getDruidMetrics() {
        return druidMetrics;
    }

    public void setDruidMetrics(List<DruidConnectionPoolMetrics> druidMetrics) {
        this.druidMetrics = druidMetrics;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConnectionPoolMetricsMessage{" + "applicationId='"
                + applicationId + '\'' + ", clientUrl='"
                + clientUrl + '\'' + ", hikariMetrics="
                + hikariMetrics + ", druidMetrics="
                + druidMetrics + ", sequenceNumber="
                + sequenceNumber + ", timestamp="
                + timestamp + '}';
    }
}
