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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection pool information for heartbeat monitoring.
 *
 * @since 2.1.0
 */
public class ConnectionPoolInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Client application ID
     */
    private String applicationId;

    /**
     * Transaction service group
     */
    private String transactionServiceGroup;

    /**
     * DataSource connection pool metrics (HikariCP/Druid)
     */
    private List<Object> dataSourceMetrics = new ArrayList<>();

    /**
     * Timestamp when this info was collected
     */
    private long timestamp;

    public ConnectionPoolInfo() {
        this.timestamp = System.currentTimeMillis();
    }

    public ConnectionPoolInfo(String applicationId, String transactionServiceGroup) {
        this();
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    public List<Object> getDataSourceMetrics() {
        return dataSourceMetrics;
    }

    public void setDataSourceMetrics(List<Object> dataSourceMetrics) {
        this.dataSourceMetrics = dataSourceMetrics;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConnectionPoolInfo{" + "applicationId='"
                + applicationId + '\'' + ", transactionServiceGroup='"
                + transactionServiceGroup + '\'' + ", dataSourceMetrics="
                + (dataSourceMetrics != null ? dataSourceMetrics.size() + " metrics" : "null") + ", timestamp="
                + timestamp + '}';
    }
}
