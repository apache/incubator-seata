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
package io.seata.config.processor.impl;

import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.processor.ConfigurationProcessor;
import io.seata.config.util.ConfigurationUtils;

import static io.seata.config.processor.ConfigProcessorOrdered.CONFIG_FILES_PROCESSOR_ORDER;
import static io.seata.config.source.ConfigSourceOrdered.COMMON_CONFIG_FILES_SOURCE_ORDER;
import static io.seata.config.source.ConfigSourceOrdered.ENV_CONFIG_FILES_SOURCE_ORDER;

/**
 * The type ConfigFilesConfigurationProcessor.
 *
 * @author wang.liang
 */
@LoadLevel(name = "config-files-processor", order = CONFIG_FILES_PROCESSOR_ORDER)
public class ConfigFilesConfigurationProcessor implements ConfigurationProcessor {

    @Override
    public void process(Configuration configuration) {
        // get registry configFileName from configuration
        String configFileName = ConfigurationUtils.getConfigFileName(configuration);

        // load file sources by configFileName
        ConfigurationUtils.loadFileSources(configuration, configFileName,
                COMMON_CONFIG_FILES_SOURCE_ORDER, ENV_CONFIG_FILES_SOURCE_ORDER,
                true, false);
    }
}
