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
package io.seata.config.defaultconfig;

import java.util.Map;

import io.seata.common.ValueWrapper;
import io.seata.config.CacheableConfiguration;

/**
 * The type Seata default config manager.
 *
 * @author wang.liang
 */
public class SeataDefaultConfigManager extends CacheableConfiguration
        implements DefaultConfigManager {

    public static final String DEFAULT_CONFIG_MANAGER_TYPE_NAME = "seata-default-config-manager";


    public SeataDefaultConfigManager(String typeName, Map<String, ValueWrapper> configCache) {
        super(typeName, configCache);
    }

    public SeataDefaultConfigManager(String typeName) {
        super(typeName);
    }

    public SeataDefaultConfigManager() {
        this(DEFAULT_CONFIG_MANAGER_TYPE_NAME);
    }

}
