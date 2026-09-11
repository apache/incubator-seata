/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_QUERY_LIMIT;
import static io.seata.common.DefaultValues.DEFAULT_REDIS_MAX_IDLE;
import static io.seata.common.DefaultValues.DEFAULT_REDIS_MIN_IDLE;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SINGLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SENTINEL_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = STORE_REDIS_PREFIX)
public class StoreRedisProperties {
    /**
     * single, sentinel
     */
    private String mode = "single";
    private String password;
    private Integer maxConn = DEFAULT_REDIS_MAX_IDLE;
    private Integer minConn = DEFAULT_REDIS_MIN_IDLE;
    private Integer database = 0;
    private Integer queryLimit = DEFAULT_QUERY_LIMIT;
    private Integer maxTotal = 100;

    public String getMode() {
        return mode;
    }

    public StoreRedisProperties setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public StoreRedisProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public Integer getMaxConn() {
        return maxConn;
    }

    public StoreRedisProperties setMaxConn(Integer maxConn) {
        this.maxConn = maxConn;
        return this;
    }

    public Integer getMinConn() {
        return minConn;
    }

    public StoreRedisProperties setMinConn(Integer minConn) {
        this.minConn = minConn;
        return this;
    }

    public Integer getDatabase() {
        return database;
    }

    public StoreRedisProperties setDatabase(Integer database) {
        this.database = database;
        return this;
    }

    public Integer getQueryLimit() {
        return queryLimit;
    }

    public StoreRedisProperties setQueryLimit(Integer queryLimit) {
        this.queryLimit = queryLimit;
        return this;
    }

    public Integer getMaxTotal() {
        return maxTotal;
    }

    public StoreRedisProperties setMaxTotal(Integer maxTotal) {
        this.maxTotal = maxTotal;
        return this;
    }


    @Component
    @ConfigurationProperties(prefix = STORE_REDIS_SINGLE_PREFIX)
    public static class Single {
        private String host = "127.0.0.1";
        private Integer port = 6379;

        public String getHost() {
            return host;
        }

        public Single setHost(String host) {
            this.host = host;
            return this;
        }

        public Integer getPort() {
            return port;
        }

        public Single setPort(Integer port) {
            this.port = port;
            return this;
        }
    }



    @Component
    @ConfigurationProperties(prefix = STORE_REDIS_SENTINEL_PREFIX)
    public static class Sentinel {
        private String masterName;
        /**
         * such as "10.28.235.65:26379,10.28.235.65:26380,10.28.235.65:26381"
         */
        private String sentinelHosts;

        private String sentinelPassword;

        public String getMasterName() {
            return masterName;
        }

        public Sentinel setMasterName(String masterName) {
            this.masterName = masterName;
            return this;
        }

        public String getSentinelHosts() {
            return sentinelHosts;
        }

        public Sentinel setSentinelHosts(String sentinelHosts) {
            this.sentinelHosts = sentinelHosts;
            return this;
        }

        public String getSentinelPassword() {
            return sentinelPassword;
        }

        public Sentinel setSentinelPassword(String sentinelPassword) {
            this.sentinelPassword = sentinelPassword;
            return this;
        }
    }
}
