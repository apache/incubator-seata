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
package org.apache.seata.spring.boot.autoconfigure.properties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_RAFT_PREFIX;

@Component
@ConfigurationProperties(prefix = CONFIG_RAFT_PREFIX)
public class ConfigRaftProperties {
    private String serverAddr;

    private Long metadataMaxAgeMs = 30000L;

    private String username;

    private String password;

    private Long tokenValidityInMilliseconds = 29 * 60 * 1000L;

    public Long getMetadataMaxAgeMs() {
        return metadataMaxAgeMs;
    }

    public ConfigRaftProperties setMetadataMaxAgeMs(Long metadataMaxAgeMs) {
        this.metadataMaxAgeMs = metadataMaxAgeMs;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ConfigRaftProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ConfigRaftProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public Long getTokenValidityInMilliseconds() {
        return tokenValidityInMilliseconds;
    }

    public ConfigRaftProperties setTokenValidityInMilliseconds(Long tokenValidityInMilliseconds) {
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        return this;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public ConfigRaftProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

}
