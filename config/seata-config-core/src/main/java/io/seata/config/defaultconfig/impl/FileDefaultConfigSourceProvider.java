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
package io.seata.config.defaultconfig.impl;

import io.seata.common.loader.LoadLevel;
import io.seata.config.defaultconfig.DefaultConfigManager;
import io.seata.config.defaultconfig.DefaultConfigSourceProvider;
import io.seata.config.source.impl.FileConfigSource;

import static io.seata.config.source.ConfigSourceOrdered.FILE_DEFAULT_CONFIG_SOURCE_ORDER;

/**
 * The type File default config source provider.
 *
 * @author wang.liang
 */
@LoadLevel(name = "default-config-file", order = FILE_DEFAULT_CONFIG_SOURCE_ORDER)
public class FileDefaultConfigSourceProvider implements DefaultConfigSourceProvider {

    private static final String DEFAULT_CONFIG_COMMON_FILE_NAME = "default-config-common.conf";
    private static final String DEFAULT_CONFIG_CLIENT_FILE_NAME = "default-config-client.conf";


    @Override
    public void provide(DefaultConfigManager defaultConfigManager) {
        // common
        defaultConfigManager.addSource(new FileConfigSource(DEFAULT_CONFIG_COMMON_FILE_NAME, FILE_DEFAULT_CONFIG_SOURCE_ORDER));
        // client
        defaultConfigManager.addSource(new FileConfigSource(DEFAULT_CONFIG_CLIENT_FILE_NAME, FILE_DEFAULT_CONFIG_SOURCE_ORDER));
    }
}
