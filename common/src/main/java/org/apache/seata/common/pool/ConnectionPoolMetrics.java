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
package org.apache.seata.common.pool;

import org.apache.seata.common.monitor.SqlExecutionEntry;

import java.util.List;

public class ConnectionPoolMetrics {
    private String serviceName;

    // common filed (supported by both Druid & HikariCP)
    private int activeConnections;
    private int idleConnections;
    private int totalConnections;
    private int maxPoolSize;
    private int minIdle;
    private int waitThreadCount;
    private int connectionTimeout;
    private long validationTimeout;
    private boolean autoCommit;
    private long idleTimeout;

    // Druid specific fields
    private long executeCount;
    private long errorCount;
    private long commitCount;
    private long rollbackCount;
    private long logicConnectCount;
    private List<SqlExecutionEntry> slowSqlList;
    private List<SqlExecutionEntry> sqlExecutionRecord;
    private long[] transactionHistogramValues;
    private long[] transactionHistogramRanges;

    // HikariCP specific fields
    private long connectionAcquiredNanos; // optional: reserved for Micrometer
    private double connectionTimeoutRate; // reserved for Micrometer
    private long leakDetectionThreshold;
    private String poolName;
    private long timestamp;

    public ConnectionPoolMetrics() {
    }

    private ConnectionPoolMetrics(Builder builder) {
        this.serviceName = builder.serviceName;
        this.activeConnections = builder.activeConnections;
        this.idleConnections = builder.idleConnections;
        this.totalConnections = builder.totalConnections;
        this.maxPoolSize = builder.maxPoolSize;
        this.minIdle = builder.minIdle;
        this.waitThreadCount = builder.waitThreadCount;
        this.connectionTimeout = builder.connectionTimeout;
        this.validationTimeout = builder.validationTimeout;
        this.autoCommit = builder.autoCommit;
        this.idleTimeout = builder.idleTimeout;
        this.executeCount = builder.executeCount;
        this.errorCount = builder.errorCount;
        this.commitCount = builder.commitCount;
        this.rollbackCount = builder.rollbackCount;
        this.logicConnectCount = builder.logicConnectCount;
        this.slowSqlList = builder.slowSqlList;
        this.sqlExecutionRecord = builder.sqlExecutionRecord;
        this.transactionHistogramValues = builder.transactionHistogramValues;
        this.transactionHistogramRanges = builder.transactionHistogramRanges;
        this.connectionAcquiredNanos = builder.connectionAcquiredNanos;
        this.connectionTimeoutRate = builder.connectionTimeoutRate;
        this.leakDetectionThreshold = builder.leakDetectionThreshold;
        this.poolName = builder.poolName;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceName;
        private int activeConnections;
        private int idleConnections;
        private int totalConnections;
        private int maxPoolSize;
        private int minIdle;
        private int waitThreadCount;
        private int connectionTimeout; // unit: ms
        private long validationTimeout; // unit: ms
        private boolean autoCommit;
        private long idleTimeout;

        private long executeCount;
        private long errorCount;
        private long commitCount;
        private long rollbackCount;
        private long logicConnectCount;
        private List<SqlExecutionEntry> slowSqlList;
        private List<SqlExecutionEntry> sqlExecutionRecord;
        private long[] transactionHistogramValues;
        private long[] transactionHistogramRanges;

        private long connectionAcquiredNanos;
        private double connectionTimeoutRate;
        private long leakDetectionThreshold;
        private String poolName;
        private long timestamp;


        public Builder serviceName(String val) {
            this.serviceName = val;
            return this;
        }

        public Builder activeConnections(int val) {
            this.activeConnections = val;
            return this;
        }

        public Builder idleConnections(int val) {
            this.idleConnections = val;
            return this;
        }

        public Builder totalConnections(int val) {
            this.totalConnections = val;
            return this;
        }

        public Builder maxPoolSize(int val) {
            this.maxPoolSize = val;
            return this;
        }

        public Builder minIdle(int val) {
            this.minIdle = val;
            return this;
        }

        public Builder waitThreadCount(int val) {
            this.waitThreadCount = val;
            return this;
        }

        public Builder connectionTimeout(int val) {
            this.connectionTimeout = val;
            return this;
        }

        public Builder validationTimeout(long val) {
            this.validationTimeout = val;
            return this;
        }

        public Builder autoCommit(boolean val) {
            this.autoCommit = val;
            return this;
        }

        public Builder idleTimeout(long val) {
            this.idleTimeout = val;
            return this;
        }

        public Builder executeCount(long val) {
            this.executeCount = val;
            return this;
        }

        public Builder errorCount(long val) {
            this.errorCount = val;
            return this;
        }

        public Builder commitCount(long val) {
            this.commitCount = val;
            return this;
        }

        public Builder rollbackCount(long val) {
            this.rollbackCount = val;
            return this;
        }

        public Builder logicConnectCount(long val) {
            this.logicConnectCount = val;
            return this;
        }

        public Builder slowSqlList(List<SqlExecutionEntry> val) {
            this.slowSqlList = val;
            return this;
        }

        public Builder sqlExecutionRecords(List<SqlExecutionEntry> val) {
            this.sqlExecutionRecord = val;
            return this;
        }

        public Builder transactionHistogramValues(long[] val) {
            this.transactionHistogramValues = val;
            return this;
        }

        public Builder transactionHistogramRanges(long[] val) {
            this.transactionHistogramRanges = val;
            return this;
        }

        public Builder connectionAcquiredNanos(long val) {
            this.connectionAcquiredNanos = val;
            return this;
        }

        public Builder connectionTimeoutRate(double val) {
            this.connectionTimeoutRate = val;
            return this;
        }

        public Builder leakDetectionThreshold(long val) {
            this.leakDetectionThreshold = val;
            return this;
        }

        public Builder poolName(String val) {
            this.poolName = val;
            return this;
        }

        public Builder timestamp(long val) {
            this.timestamp = val;
            return this;
        }

        public ConnectionPoolMetrics build() {
            return new ConnectionPoolMetrics(this);
        }
    }

    public String getServiceName() {
        return serviceName;
    }


    public int getActiveConnections() {
        return activeConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getWaitThreadCount() {
        return waitThreadCount;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getExecuteCount() {
        return executeCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    public long getLogicConnectCount() {
        return logicConnectCount;
    }

    public List<SqlExecutionEntry> getSlowSqlList() {
        return slowSqlList;
    }
    public List<SqlExecutionEntry> getSqlExecutionRecord() {
        return sqlExecutionRecord;
    }

    public long[] getTransactionHistogramValues() {
        return transactionHistogramValues;
    }

    public long[] getTransactionHistogramRanges() {
        return transactionHistogramRanges;
    }


    public long getConnectionAcquiredNanos() {
        return connectionAcquiredNanos;
    }

    public double getConnectionTimeoutRate() {
        return connectionTimeoutRate;
    }

    public long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public String getPoolName() {
        return poolName;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
