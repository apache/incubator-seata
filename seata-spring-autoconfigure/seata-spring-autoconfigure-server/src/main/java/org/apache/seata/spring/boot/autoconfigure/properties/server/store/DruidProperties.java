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
package org.apache.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_DB_DRUID_PREFIX;


@Component
@ConfigurationProperties(prefix = STORE_DB_DRUID_PREFIX)
public class DruidProperties {
    private Long timeBetweenEvictionRunsMillis = 120000L;
    private Long minEvictableIdleTimeMillis = 300000L;
    private Boolean testWhileIdle = true;
    private Boolean testOnBorrow = false;
    private Boolean keepAlive = false;


    public Long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public DruidProperties setTimeBetweenEvictionRunsMillis(Long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        return this;
    }

    public Long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public DruidProperties setMinEvictableIdleTimeMillis(Long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        return this;
    }

    public Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public DruidProperties setTestWhileIdle(Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        return this;
    }

    public Boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public DruidProperties setTestOnBorrow(Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        return this;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public DruidProperties setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    @Override
    public String toString() {
        return "DruidProperties{" +
                "timeBetweenEvictionRunsMillis=" + timeBetweenEvictionRunsMillis +
                ", minEvictableIdleTimeMillis=" + minEvictableIdleTimeMillis +
                ", testWhileIdle=" + testWhileIdle +
                ", testOnBorrow=" + testOnBorrow +
                ", keepAlive=" + keepAlive +
                '}';
    }
}
