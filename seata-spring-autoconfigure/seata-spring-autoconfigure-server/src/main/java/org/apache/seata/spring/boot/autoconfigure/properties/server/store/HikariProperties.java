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

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_DB_HIKARI_PREFIX;


@Component
@ConfigurationProperties(prefix = STORE_DB_HIKARI_PREFIX)
public class HikariProperties {
    private Long idleTimeout = 600000L;
    private Long keepaliveTime = 120000L;
    private Long maxLifetime = 1800000L;
    private Long validationTimeout = 5000L;

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public HikariProperties setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public Long getKeepaliveTime() {
        return keepaliveTime;
    }

    public HikariProperties setKeepaliveTime(Long keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public HikariProperties setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
        return this;
    }

    public Long getValidationTimeout() {
        return validationTimeout;
    }

    public HikariProperties setValidationTimeout(Long validationTimeout) {
        this.validationTimeout = validationTimeout;
        return this;
    }

    @Override
    public String toString() {
        return "HikariProperties{" +
                "idleTimeout=" + idleTimeout +
                ", keepaliveTime=" + keepaliveTime +
                ", maxLifetime=" + maxLifetime +
                ", validationTimeout=" + validationTimeout +
                '}';
    }
}
