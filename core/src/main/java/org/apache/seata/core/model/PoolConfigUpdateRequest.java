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
package org.apache.seata.core.model;

import java.io.Serializable;

/**
 * The type Pool config update request.
 */
public class PoolConfigUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String poolName;
    private int maxPoolSize;
    private int minIdle;
    private int connectionTimeout;

    // field for Druid
    private long timeBetweenEvictionRunsMills;
    private long maxEvictableTimeMills;

    // field for HikariCP
    private long maxLifeTime;
    private long keepaliveTime;

    public PoolConfigUpdateRequest() {}

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
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

    @Override
    public String toString() {
        return "PoolConfigUpdateRequest{" + "poolName='"
                + poolName + '\'' + ", maxPoolSize="
                + maxPoolSize + ", minIdle="
                + minIdle + ", connectionTimeout="
                + connectionTimeout + ", timeBetweenEvictionRunsMills="
                + timeBetweenEvictionRunsMills + ", maxEvictableTimeMills="
                + maxEvictableTimeMills + ", maxLifeTime="
                + maxLifeTime + ", keepaliveTime="
                + keepaliveTime + '}';
    }
}
