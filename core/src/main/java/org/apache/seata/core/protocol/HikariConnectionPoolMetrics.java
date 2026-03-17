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

/**
 * HikariCP connection pool metrics for detailed monitoring.
 *
 */
public class HikariConnectionPoolMetrics implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final double MAX_ACCEPTABLE_TIMEOUT_RATE = 0.1;

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
     * Connection acquisition time (ms)
     */
    private long connectionAcquired;

    /**
     * Connection timeout rate (0.0–1.0)
     */
    private double connectionTimeoutRate;

    /**
     * Leak detection threshold (ms)
     */
    private long leakDetectionThreshold;

    /**
     * Max lifetime (ms)
     */
    private long maxLifeTime;

    /**
     * Keepalive time (ms)
     */
    private long keepaliveTime;

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

    public HikariConnectionPoolMetrics() {
        this.timestamp = System.currentTimeMillis();
    }

    public HikariConnectionPoolMetrics(String poolName) {
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

    public long getMaxLifeTime() {
        return maxLifeTime;
    }

    public void setMaxLifeTime(long maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public long getKeepaliveTime() {
        return keepaliveTime;
    }

    public void setKeepaliveTime(long keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
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

    /**
     * Calculate connection pool utilization
     */
    public double getUtilizationRate() {
        return maxPoolSize > 0 ? (double) activeConnections / maxPoolSize : 0.0;
    }

    /**
     * Check whether the connection pool is healthy
     */
    public boolean isHealthy() {
        return activeConnections >= 0
                && totalConnections >= activeConnections
                && totalConnections <= maxPoolSize
                && connectionTimeoutRate < MAX_ACCEPTABLE_TIMEOUT_RATE;
    }

    @Override
    public String toString() {
        return "HikariConnectionPoolMetrics{" + "poolName='"
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
                + idleTimeout + ", connectionAcquired="
                + connectionAcquired + ", connectionTimeoutRate="
                + connectionTimeoutRate + ", leakDetectionThreshold="
                + leakDetectionThreshold + ", maxLifeTime="
                + maxLifeTime + ", keepaliveTime="
                + keepaliveTime + ", utilizationRate="
                + getUtilizationRate() + ", isHealthy="
                + isHealthy() + ", timestamp="
                + timestamp + '}';
    }
}
