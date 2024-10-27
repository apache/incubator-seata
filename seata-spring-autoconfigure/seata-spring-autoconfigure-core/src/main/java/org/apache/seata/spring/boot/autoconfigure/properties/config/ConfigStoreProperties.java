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

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_STORE_PREFIX;


@Component
@ConfigurationProperties(prefix = CONFIG_STORE_PREFIX)
public class ConfigStoreProperties {
    /**
     * rocksdb, (leveldb, caffeine)
     */
    private String type = "rocksdb";
    private String dir = "configStore";
    private boolean destroyOnShutdown = false;
    private String namespace = "default";
    private String dataId = "seata.properties";

    public String getType() {
        return type;
    }

    public ConfigStoreProperties setType(String type) {
        this.type = type;
        return this;
    }

    public String getDir() {
        return dir;
    }

    public ConfigStoreProperties setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public boolean isDestroyOnShutdown() {
        return destroyOnShutdown;
    }

    public ConfigStoreProperties setDestroyOnShutdown(boolean destroyOnShutdown) {
        this.destroyOnShutdown = destroyOnShutdown;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public ConfigStoreProperties setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getDataId() {
        return dataId;
    }

    public ConfigStoreProperties setDataId(String dataId) {
        this.dataId = dataId;
        return this;
    }
}
