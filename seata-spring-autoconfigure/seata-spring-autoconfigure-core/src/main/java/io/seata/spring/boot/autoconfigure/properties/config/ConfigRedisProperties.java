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

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_REDIS_PREFIX;


/**
 * @author wangyuewen
 */
@Component
@ConfigurationProperties(prefix = CONFIG_REDIS_PREFIX)
public class ConfigRedisProperties {
    private String serverAddr = "localhost:6379";
    private int db = 0;
    private String password = "";
    private int timeout = 0;
    private String keyName = "seataConfig";
    private Boolean listenerEnabled = Boolean.FALSE;

    public ConfigRedisProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public ConfigRedisProperties setDb(int db) {
        this.db = db;
        return this;
    }

    public ConfigRedisProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public ConfigRedisProperties setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ConfigRedisProperties setKeyName(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public ConfigRedisProperties setListenerEnabled(Boolean listenerEnabled){
        this.listenerEnabled = listenerEnabled;
        return this;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public int getDb() {
        return db;
    }

    public String getPassword() {
        return password;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getKeyName() {
        return keyName;
    }

    public Boolean getListenerEnabled(){ return listenerEnabled; }
}
