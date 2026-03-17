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
 * Druid connection pool metrics for detailed monitoring.
 */
public class DruidConnectionPoolMetrics implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final double MAX_ACCEPTABLE_ERROR_RATE = 0.05;
    private static final double MAX_ACCEPTABLE_ROLLBACK_RATE = 0.1;

    /**
     * Active connections
     */
    private int activeConnections;

    /**
     * Idle connections
     */
    private int idleConnections;

    /**
     * Total connections
     */
    private int totalConnections;

    /**
     * Max pool size
     */
    private int maxPoolSize;

    /**
     * Min idle connections
     */
    private int minIdle;

    /**
     * Threads awaiting connection
     */
    private int waitThreadCount;

    /**
     * Connection timeout (ms)
     */
    private int connectionTimeout;

    /**
     * Validation timeout (ms)
     */
    private long validationTimeout;

    /**
     * Auto-commit status
     */
    private boolean autoCommit;

    /**
     * Idle timeout (ms)
     */
    private long idleTimeout;

    /**
     * Execute count
     */
    private long executeCount;

    /**
     * Error count
     */
    private long errorCount;

    /**
     * Commit count
     */
    private long commitCount;

    /**
     * Rollback count
     */
    private long rollbackCount;

    /**
     * Logical connections count
     */
    private long logicConnectCount;

    /**
     * Eviction run interval (ms)
     */
    private long timeBetweenEvictionRunsMills;

    /**
     * Max evictable idle time (ms)
     */
    private long maxEvictableTimeMills;

    /**
     * Pool name
     */
    private String poolName;

    /**
     * JDBC URL
     */
    private String jdbcUrl;

    /**
     * DataSource class name
     */
    private String dataSourceClassName;

    /**
     * Metrics collection timestamp
     */
    private long timestamp;

    /**
     * Pool creation time
     */
    private long poolCreatedTime;

    /**
     * Max wait time (ms)
     */
    private long maxWaitTime;

    /**
     * Physical connections created
     */
    private long physicalConnectCount;

    /**
     * Physical connections closed
     */
    private long physicalCloseCount;

    /**
     * Transaction histogram values
     */
    private long[] transactionHistogramValues;

    /**
     * Transaction histogram ranges
     */
    private long[] transactionHistogramRanges;

    /**
     * Slow SQL list (records exceeding threshold)
     */
    private List<SqlExecutionEntry> slowSqlList = new ArrayList<>();

    /**
     * SQL execution record list (recent executions)
     */
    private List<SqlExecutionEntry> sqlExecutionRecord = new ArrayList<>();

    public DruidConnectionPoolMetrics() {
        this.timestamp = System.currentTimeMillis();
    }

    public DruidConnectionPoolMetrics(String poolName) {
        this();
        this.poolName = poolName;
    }

    // Getters and setters
    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(int idleConnections) {
        this.idleConnections = idleConnections;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getWaitThreadCount() {
        return waitThreadCount;
    }

    public void setWaitThreadCount(int waitThreadCount) {
        this.waitThreadCount = waitThreadCount;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getExecuteCount() {
        return executeCount;
    }

    public void setExecuteCount(long executeCount) {
        this.executeCount = executeCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(long commitCount) {
        this.commitCount = commitCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    public void setRollbackCount(long rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public long getLogicConnectCount() {
        return logicConnectCount;
    }

    public void setLogicConnectCount(long logicConnectCount) {
        this.logicConnectCount = logicConnectCount;
    }

    public long getTimeBetweenEvictionRunsMills() {
        return timeBetweenEvictionRunsMills;
    }

    public void setTimeBetweenEvictionRunsMills(long timeBetweenEvictionRunsMills) {
        this.timeBetweenEvictionRunsMills = timeBetweenEvictionRunsMills;
    }

    public long getMaxEvictableTimeMills() {
        return maxEvictableTimeMills;
    }

    public void setMaxEvictableTimeMills(long maxEvictableTimeMills) {
        this.maxEvictableTimeMills = maxEvictableTimeMills;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDataSourceClassName() {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
        this.dataSourceClassName = dataSourceClassName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getPoolCreatedTime() {
        return poolCreatedTime;
    }

    public void setPoolCreatedTime(long poolCreatedTime) {
        this.poolCreatedTime = poolCreatedTime;
    }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public long getPhysicalConnectCount() {
        return physicalConnectCount;
    }

    public void setPhysicalConnectCount(long physicalConnectCount) {
        this.physicalConnectCount = physicalConnectCount;
    }

    public long getPhysicalCloseCount() {
        return physicalCloseCount;
    }

    public void setPhysicalCloseCount(long physicalCloseCount) {
        this.physicalCloseCount = physicalCloseCount;
    }

    public long[] getTransactionHistogramValues() {
        return transactionHistogramValues;
    }

    public void setTransactionHistogramValues(long[] transactionHistogramValues) {
        this.transactionHistogramValues = transactionHistogramValues;
    }

    public long[] getTransactionHistogramRanges() {
        return transactionHistogramRanges;
    }

    public void setTransactionHistogramRanges(long[] transactionHistogramRanges) {
        this.transactionHistogramRanges = transactionHistogramRanges;
    }

    public List<SqlExecutionEntry> getSlowSqlList() {
        return slowSqlList;
    }

    public void setSlowSqlList(List<SqlExecutionEntry> slowSqlList) {
        this.slowSqlList = slowSqlList;
    }

    public List<SqlExecutionEntry> getSqlExecutionRecord() {
        return sqlExecutionRecord;
    }

    public void setSqlExecutionRecord(List<SqlExecutionEntry> sqlExecutionRecord) {
        this.sqlExecutionRecord = sqlExecutionRecord;
    }

    /**
     * Calculate connection pool utilization
     */
    public double getUtilizationRate() {
        return maxPoolSize > 0 ? (double) activeConnections / maxPoolSize : 0.0;
    }

    /**
     * Calculate error rate
     */
    public double getErrorRate() {
        return executeCount > 0 ? (double) errorCount / executeCount : 0.0;
    }

    /**
     * Calculate rollback rate
     */
    public double getRollbackRate() {
        long totalTransactions = commitCount + rollbackCount;
        return totalTransactions > 0 ? (double) rollbackCount / totalTransactions : 0.0;
    }

    /**
     * Check whether the connection pool is healthy
     */
    public boolean isHealthy() {
        return activeConnections >= 0
                && totalConnections >= activeConnections
                && totalConnections <= maxPoolSize
                && getErrorRate() < MAX_ACCEPTABLE_ERROR_RATE
                && getRollbackRate() < MAX_ACCEPTABLE_ROLLBACK_RATE;
    }

    /**
     * Get connection pool uptime (ms)
     */
    public long getPoolUptime() {
        return poolCreatedTime > 0 ? timestamp - poolCreatedTime : 0;
    }

    @Override
    public String toString() {
        return "DruidConnectionPoolMetrics{" + "poolName='"
                + poolName + '\'' + ", activeConnections="
                + activeConnections + ", idleConnections="
                + idleConnections + ", totalConnections="
                + totalConnections + ", maxPoolSize="
                + maxPoolSize + ", minIdle="
                + minIdle + ", waitThreadCount="
                + waitThreadCount + ", connectionTimeout="
                + connectionTimeout + ", validationTimeout="
                + validationTimeout + ", autoCommit="
                + autoCommit + ", idleTimeout="
                + idleTimeout + ", executeCount="
                + executeCount + ", errorCount="
                + errorCount + ", commitCount="
                + commitCount + ", rollbackCount="
                + rollbackCount + ", logicConnectCount="
                + logicConnectCount + ", timeBetweenEvictionRunsMills="
                + timeBetweenEvictionRunsMills + ", maxEvictableTimeMills="
                + maxEvictableTimeMills + ", utilizationRate="
                + getUtilizationRate() + ", errorRate="
                + getErrorRate() + ", rollbackRate="
                + getRollbackRate() + ", isHealthy="
                + isHealthy() + ", poolUptime="
                + getPoolUptime() + ", timestamp="
                + timestamp + '}';
    }
}
