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
import io.seata.config.Configuration;
import io.seata.config.source.ConfigSourceProvider;
import io.seata.config.util.ConfigurationUtils;

import static io.seata.common.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_CONFIG;
import static io.seata.config.source.ConfigSourceOrdered.CONFIG_CENTER_SOURCE_ORDER;

/**
 * @author xingfudeshi@gmail.com
 */
@LoadLevel(name = "File", order = CONFIG_CENTER_SOURCE_ORDER)
public class FileConfigSourceProvider implements ConfigSourceProvider {

    private static final String DEFAULT_FILE_NAME = "file.conf";

    private static final String FILE_TYPE = "file";
    private static final String NAME_KEY = "name";

    @Override
    public void provide(Configuration configuration) {
        // the key 'config.file.name'
        String fileNameConfigKey = String.join(FILE_CONFIG_SPLIT_CHAR,
                FILE_ROOT_CONFIG, FILE_TYPE, NAME_KEY);

        // get configFileName from configuration
        String configFileName = configuration.getString(fileNameConfigKey, DEFAULT_FILE_NAME);

        // load file sources by configFileName
        ConfigurationUtils.loadFileSources(configuration, configFileName, CONFIG_CENTER_SOURCE_ORDER, true,true);
    }
}
