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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_POLARIS_PREFIX;

/**
 * The Polaris configuration properties.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-17
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_POLARIS_PREFIX)
public class RegistryPolarisProperties {
    private String serverAddr;
    private String namespace = "default";
    private String application = "seata-server";

    /**
     * Remote Polaris Config Server Access Token .
     */
    private String token;

    /**
     * Request Connect Timeout , default value : 6000 (ms) .
     */
    private int connectTimeout = 6000;

    /**
     * Request's response read timeout , default value : 5000 (ms) .
     */
    private int readTimeout = 5000;

    /**
     * Remote Service Instance Refresh Time, default value : 2000 (ms).
     */
    private int refreshTime = 2000;

    public String getServerAddr() {
        return serverAddr;
    }

    public RegistryPolarisProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public RegistryPolarisProperties setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getApplication() {
        return application;
    }

    public RegistryPolarisProperties setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getToken() {
        return token;
    }

    public RegistryPolarisProperties setToken(String token) {
        this.token = token;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public RegistryPolarisProperties setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public RegistryPolarisProperties setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getRefreshTime() {
        return refreshTime;
    }

    public RegistryPolarisProperties setRefreshTime(int refreshTime) {
        this.refreshTime = refreshTime;
        return this;
    }
}
