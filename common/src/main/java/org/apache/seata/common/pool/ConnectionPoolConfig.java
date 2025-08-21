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

public class ConnectionPoolConfig {
    private final String serviceName;

    // General field
    private int maxPoolSize;
    private int minIdle;
    private int connectionTimeout;

    // field for Druid
    private long timeBetweenEvictionRunsMills;
    private long maxEvictableTimeMills;

    // field for HikariCP
    private long maxLifeTime;
    private long keepaliveTime;

    public ConnectionPoolConfig() {}

    private ConnectionPoolConfig(Builder builder) {
        this.serviceName = builder.serviceName;
        this.maxPoolSize = builder.maxPoolSize;
        this.minIdle = builder.minIdle;
        this.connectionTimeout = builder.connectionTimeout;
        this.timeBetweenEvictionRunsMills = builder.timeBetweenEvictionRunsMills;
        this.maxEvictableTimeMills = builder.maxEvictableTimeMills;
        this.maxLifeTime = builder.maxLifeTime;
        this.keepaliveTime = builder.keepaliveTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceName;
        private int maxPoolSize;
        private int minIdle;
        private int connectionTimeout;
        private long timeBetweenEvictionRunsMills;
        private long maxEvictableTimeMills;
        private long maxLifeTime;
        private long keepaliveTime;

        public Builder serviceName(String val) {
            this.serviceName = val;
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

        public Builder connectionTimeout(int val) {
            this.connectionTimeout = val;
            return this;
        }

        public Builder timeBetweenEvictionRunsMills(long val) {
            this.timeBetweenEvictionRunsMills = val;
            return this;
        }

        public Builder maxEvictableTimeMills(long val) {
            this.maxEvictableTimeMills = val;
            return this;
        }

        public Builder maxLifeTime(long val) {
            this.maxLifeTime = val;
            return this;
        }

        public Builder keepaliveTime(long val) {
            this.keepaliveTime = val;
            return this;
        }

        public ConnectionPoolConfig build() {
            return new ConnectionPoolConfig(this);
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getTimeBetweenEvictionRunsMills() {
        return timeBetweenEvictionRunsMills;
    }


    public long getMaxEvictableTimeMills() {
        return maxEvictableTimeMills;
    }


    public long getMaxLifeTime() {
        return maxLifeTime;
    }

    public long getKeepaliveTime() {
        return keepaliveTime;
    }
}
