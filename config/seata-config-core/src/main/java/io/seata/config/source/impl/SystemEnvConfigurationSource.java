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
package io.seata.config.source.impl;

import java.util.Map;

import io.seata.config.source.ConfigurationSource;

/**
 * The type SystemEnvConfigurationSource.
 *
 * @author wang.liang
 */
public class SystemEnvConfigurationSource implements ConfigurationSource {

    private static final Map<String, String> ENV_MAP = System.getenv();


    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        return ENV_MAP.get(dataId);
    }

    @Override
    public String getTypeName() {
        return "system-env";
    }
}
