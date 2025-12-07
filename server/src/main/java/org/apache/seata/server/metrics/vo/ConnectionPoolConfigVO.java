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

import java.io.Serializable;

/**
 * ConnectionPoolConfigVO
 */
public class ConnectionPoolConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String poolName;

    private PoolType poolType;

    // General field
    /**
     * Maximum pool size
     */
    private int maxPoolSize;
    /**
     * Minimum idle connections
     */
    private int minIdle;
    /**
     * Connection timeout
     */
    private int connectionTimeout;

    // field for Druid
    /**
     * Execution interval of the detection thread for closing idle connections
     */
    private long timeBetweenEvictionRunsMills;
    /**
     * Maximum survival time of a connection in the pool
     */
    private long maxEvictableTimeMills;

    // field for HikariCP
    /**
     * Maximum lifetime of a connection
     */
    private long maxLifeTime;
    /**
     * Keepalive heartbeat interval
     */
    private long keepaliveTime;

    /**
     * Last update timestamp
     */
    private long timestamp;

    public ConnectionPoolConfigVO() {}

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

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convert HikariCP DTO to ConnectionPoolConfig
     */
    public static ConnectionPoolConfigVO of(HikariConnectionPoolMetrics hikariCP) {
        ConnectionPoolConfigVO config = new ConnectionPoolConfigVO();
        config.setPoolName(hikariCP.getPoolName());
        config.setPoolType(PoolType.HikariCP);
        config.setMaxPoolSize(hikariCP.getMaxPoolSize());
        config.setMinIdle(hikariCP.getMinIdle());
        config.setConnectionTimeout(hikariCP.getConnectionTimeout());
        config.setMaxLifeTime(hikariCP.getMaxLifeTime());
        config.setKeepaliveTime(hikariCP.getKeepaliveTime());
        config.setTimestamp(hikariCP.getTimestamp());
        return config;
    }

    /**
     * Convert Druid DTO to ConnectionPoolConfig
     */
    public static ConnectionPoolConfigVO of(DruidConnectionPoolMetrics druid) {
        ConnectionPoolConfigVO config = new ConnectionPoolConfigVO();
        config.setPoolName(druid.getPoolName());
        config.setPoolType(PoolType.Druid);
        config.setMaxPoolSize(druid.getMaxPoolSize());
        config.setMinIdle(druid.getMinIdle());
        config.setConnectionTimeout(druid.getConnectionTimeout());
        config.setTimeBetweenEvictionRunsMills(druid.getTimeBetweenEvictionRunsMills());
        config.setMaxEvictableTimeMills(druid.getMaxEvictableTimeMills());
        config.setTimestamp(druid.getTimestamp());
        return config;
    }
}
