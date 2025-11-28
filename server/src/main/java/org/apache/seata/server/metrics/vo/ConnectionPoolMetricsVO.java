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
package org.apache.seata.server.metrics.vo;

import org.apache.seata.core.protocol.DruidConnectionPoolMetrics;
import org.apache.seata.core.protocol.HikariConnectionPoolMetrics;
import org.apache.seata.core.protocol.PoolType;
import org.apache.seata.core.protocol.SqlExecutionEntry;

import java.io.Serializable;
import java.util.List;

/**
 * ConnectionPoolMetricsVO
 */
public class ConnectionPoolMetricsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String poolName;

    private PoolType poolType;

    // common filed (supported by both Druid & HikariCP)
    /**
     * Current active connections
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
     * Maximum pool size
     */
    private int maxPoolSize;
    /**
     * Minimum idle connections
     */
    private int minIdle;
    /**
     * Waiting thread count
     */
    private int waitThreadCount;
    /**
     * Connection timeout
     */
    private int connectionTimeout;
    /**
     * Validation timeout
     */
    private long validationTimeout;
    /**
     * Auto commit
     */
    private boolean autoCommit;
    /**
     * Idle timeout
     */
    private long idleTimeout;

    // Druid specific fields
    /**
     * SQL execution count
     */
    private long executeCount;
    /**
     * SQL error count
     */
    private long errorCount;
    /**
     * Transaction commit count
     */
    private long commitCount;
    /**
     * Transaction rollback count
     */
    private long rollbackCount;
    /**
     * Logic connection count
     */
    private long logicConnectCount;
    /**
     * Slow SQL list
     */
    private List<SqlExecutionEntry> slowSqlList;
    /**
     * SQL execution record
     */
    private List<SqlExecutionEntry> sqlExecutionRecord;
    /**
     * Transaction histogram values
     */
    private long[] transactionHistogramValues;
    /**
     * Transaction histogram ranges
     */
    private long[] transactionHistogramRanges;

    // HikariCP specific fields
    /**
     * Connection acquired time
     */
    private long connectionAcquired;
    /**
     * Connection timeout rate
     */
    private double connectionTimeoutRate;
    /**
     * Leak detection threshold
     */
    private long leakDetectionThreshold;
    /**
     * Timestamp
     */
    private long timestamp;

    private ConnectionPoolMetricsVO() {}

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public PoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(PoolType poolType) {
        this.poolType = poolType;
    }

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

    public long getConnectionAcquired() {
        return connectionAcquired;
    }

    public void setConnectionAcquired(long connectionAcquired) {
        this.connectionAcquired = connectionAcquired;
    }

    public double getConnectionTimeoutRate() {
        return connectionTimeoutRate;
    }

    public void setConnectionTimeoutRate(double connectionTimeoutRate) {
        this.connectionTimeoutRate = connectionTimeoutRate;
    }

    public long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convert HikariCP DTO to ConnectionPoolMetrics
     */
    public static ConnectionPoolMetricsVO of(HikariConnectionPoolMetrics hikariCP) {
        ConnectionPoolMetricsVO metrics = new ConnectionPoolMetricsVO();
        metrics.setPoolName(hikariCP.getPoolName());
        metrics.setPoolType(PoolType.HikariCP);
        metrics.setActiveConnections(hikariCP.getActiveConnections());
        metrics.setIdleConnections(hikariCP.getIdleConnections());
        metrics.setTotalConnections(hikariCP.getTotalConnections());
        metrics.setMaxPoolSize(hikariCP.getMaxPoolSize());
        metrics.setMinIdle(hikariCP.getMinIdle());
        metrics.setWaitThreadCount(hikariCP.getWaitThreadCount());
        metrics.setConnectionTimeout(hikariCP.getConnectionTimeout());
        metrics.setValidationTimeout(hikariCP.getValidationTimeout());
        metrics.setAutoCommit(hikariCP.isAutoCommit());
        metrics.setIdleTimeout(hikariCP.getIdleTimeout());
        metrics.setConnectionAcquired(hikariCP.getConnectionAcquired());
        metrics.setConnectionTimeoutRate(hikariCP.getConnectionTimeoutRate());
        metrics.setLeakDetectionThreshold(hikariCP.getLeakDetectionThreshold());
        metrics.setPoolName(hikariCP.getPoolName());
        metrics.setTimestamp(hikariCP.getTimestamp());
        return metrics;
    }

    /**
     * Convert Druid DTO to ConnectionPoolMetrics
     */
    public static ConnectionPoolMetricsVO of(DruidConnectionPoolMetrics druid) {
        ConnectionPoolMetricsVO metrics = new ConnectionPoolMetricsVO();
        metrics.setPoolName(druid.getPoolName());
        metrics.setPoolType(PoolType.Druid);
        metrics.setActiveConnections(druid.getActiveConnections());
        metrics.setIdleConnections(druid.getIdleConnections());
        metrics.setTotalConnections(druid.getTotalConnections());
        metrics.setMaxPoolSize(druid.getMaxPoolSize());
        metrics.setMinIdle(druid.getMinIdle());
        metrics.setWaitThreadCount(druid.getWaitThreadCount());
        metrics.setConnectionTimeout(druid.getConnectionTimeout());
        metrics.setValidationTimeout(druid.getValidationTimeout());
        metrics.setAutoCommit(druid.isAutoCommit());
        metrics.setIdleTimeout(druid.getIdleTimeout());
        metrics.setExecuteCount(druid.getExecuteCount());
        metrics.setErrorCount(druid.getErrorCount());
        metrics.setCommitCount(druid.getCommitCount());
        metrics.setRollbackCount(druid.getRollbackCount());
        metrics.setLogicConnectCount(druid.getLogicConnectCount());
        metrics.setSlowSqlList(druid.getSlowSqlList());
        metrics.setSqlExecutionRecord(druid.getSqlExecutionRecord());
        metrics.setTransactionHistogramValues(druid.getTransactionHistogramValues());
        metrics.setTransactionHistogramRanges(druid.getTransactionHistogramRanges());
        return metrics;
    }
}
