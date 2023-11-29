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
package io.seata.spring.boot.autoconfigure.properties.registry;

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_RAFT_PREFIX;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_RAFT_PREFIX)
public class RegistryRaftProperties {
    private String serverAddr;

    private Long metadataMaxAgeMs = 30000L;

    private String username;

    private String password;

    private Long tokenValidityInMilliseconds = 29 * 60 * 1000L;

    public Long getMetadataMaxAgeMs() {
        return metadataMaxAgeMs;
    }

    public void setMetadataMaxAgeMs(Long metadataMaxAgeMs) {
        this.metadataMaxAgeMs = metadataMaxAgeMs;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTokenValidityInMilliseconds() {
        return tokenValidityInMilliseconds;
    }

    public void setTokenValidityInMilliseconds(Long tokenValidityInMilliseconds) {
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public RegistryRaftProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

}
