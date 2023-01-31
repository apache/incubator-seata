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

import io.seata.common.loader.LoadLevel;
import io.seata.config.DefaultValueManager;
import io.seata.config.source.DefaultValueConfigurationSourceProvider;

import static io.seata.config.source.ConfigSourceOrdered.DEFAULT_CONF_SOURCE_ORDER;

/**
 * @author wang.liang
 */
@LoadLevel(name = "default-conf-file", order = DEFAULT_CONF_SOURCE_ORDER)
public class RegistryAndFileDefaultValueConfigurationSourceProvider implements DefaultValueConfigurationSourceProvider {

    private static final String DEFAULT_REGISTRY_CONFIG_FILE_NAME = "default-registry.conf";
    private static final String DEFAULT_FILE_CONFIG_FILE_NAME = "default-file.conf";


    @Override
    public void provide(DefaultValueManager defaultValueManager) {
        defaultValueManager.addSourceLast(new FileConfigurationSource(DEFAULT_FILE_CONFIG_FILE_NAME));
        defaultValueManager.addSourceLast(new FileConfigurationSource(DEFAULT_REGISTRY_CONFIG_FILE_NAME));
    }
}
