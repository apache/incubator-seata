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
package io.seata.spring.boot.autoconfigure.properties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ZK_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_ZK_PREFIX)
public class ConfigZooKeeperProperties {
    private String serverAddr;
    private int sessionTimeout = 6000;
    private int connectTimeout = 2000;
    private String username;
    private String password;
    private String nodePath = "/seata/seata.properties";

    public String getServerAddr() {
        return serverAddr;
    }

    public ConfigZooKeeperProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public ConfigZooKeeperProperties setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public ConfigZooKeeperProperties setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ConfigZooKeeperProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ConfigZooKeeperProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getNodePath() {
        return nodePath;
    }

    public ConfigZooKeeperProperties setNodePath(String nodePath) {
        this.nodePath = nodePath;
        return this;
    }
}
