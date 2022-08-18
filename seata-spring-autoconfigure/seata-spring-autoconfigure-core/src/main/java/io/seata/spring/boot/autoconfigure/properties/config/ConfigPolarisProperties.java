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

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_POLARIS_PREFIX;

/**
 * The Polaris configuration properties.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-17
 */
@Component
@ConfigurationProperties(prefix = CONFIG_POLARIS_PREFIX)
public class ConfigPolarisProperties {
    private String serverAddr;
    private String namespace = "default";
    private String group;
    private String file = "seata.properties";

    /**
     * Remote Polaris Config Server Access Token .
     */
    private String token;

    /**
     * Remote config pull interval, default value : 5000 (ms) .
     */
    private int pullIntervalTime = 5000;

    /**
     * Request Connect Timeout , default value : 6000 (ms) .
     */
    private int connectTimeout = 6000;

    /**
     * Request's response read timeout , default value : 5000 (ms) .
     */
    private int readTimeout = 5000;

    public String getServerAddr() {
        return serverAddr;
    }

    public ConfigPolarisProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public ConfigPolarisProperties setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public ConfigPolarisProperties setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getFile() {
        return file;
    }

    public ConfigPolarisProperties setFile(String file) {
        this.file = file;
        return this;
    }

    public String getToken() {
        return token;
    }

    public ConfigPolarisProperties setToken(String token) {
        this.token = token;
        return this;
    }

    public int getPullIntervalTime() {
        return pullIntervalTime;
    }

    public ConfigPolarisProperties setPullIntervalTime(int pullIntervalTime) {
        this.pullIntervalTime = pullIntervalTime;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public ConfigPolarisProperties setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public ConfigPolarisProperties setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }
}
