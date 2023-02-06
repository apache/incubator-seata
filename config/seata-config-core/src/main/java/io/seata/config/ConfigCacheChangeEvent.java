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
package io.seata.config;

import io.seata.config.changelistener.ConfigurationChangeType;

/**
 * The type ConfigCacheChangeEvent.
 *
 * @author wang.liang
 */
public class ConfigCacheChangeEvent {

    private final String dataId;

    private final ConfigCache oldCache;
    private final ConfigCache newCache;
    private final ConfigurationChangeType type;
    private final String namespace;


    public ConfigCacheChangeEvent(String dataId, ConfigCache oldCache, ConfigCache newCache, ConfigurationChangeType type, String namespace) {
        this.dataId = dataId;
        this.oldCache = oldCache;
        this.newCache = newCache;
        this.type = type;
        this.namespace = namespace;
    }

    public String getDataId() {
        return dataId;
    }

    public ConfigCache getOldCache() {
        return oldCache;
    }

    public ConfigCache getNewCache() {
        return newCache;
    }

    public ConfigurationChangeType getType() {
        return type;
    }

    public String getNamespace() {
        return namespace;
    }
}
