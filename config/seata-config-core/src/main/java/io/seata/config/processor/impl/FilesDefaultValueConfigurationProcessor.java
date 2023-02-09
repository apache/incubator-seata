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
import io.seata.config.source.impl.FileConfigSource;

import static io.seata.config.processor.ConfigProcessorOrdered.FILES_DEFAULT_VALUE_PROCESSOR_ORDER;
import static io.seata.config.source.ConfigSourceOrdered.FILES_DEFAULT_VALUE_CONFIG_SOURCE_ORDER;

/**
 * The type Files default value ConfigurationProcessor.
 *
 * @author wang.liang
 */
@LoadLevel(name = "files-default-value-processor", order = FILES_DEFAULT_VALUE_PROCESSOR_ORDER)
public class FilesDefaultValueConfigurationProcessor implements ConfigurationProcessor {

    private static final String DEFAULT_CONFIG_COMMON_FILE_NAME = "seata-default-config-common.conf";
    private static final String DEFAULT_CONFIG_CLIENT_FILE_NAME = "seata-default-config-client.conf";


    @Override
    public void process(Configuration configuration) {
        // common
        configuration.addSource(new FileConfigSource(DEFAULT_CONFIG_COMMON_FILE_NAME, FILES_DEFAULT_VALUE_CONFIG_SOURCE_ORDER));
        // client
        configuration.addSource(new FileConfigSource(DEFAULT_CONFIG_CLIENT_FILE_NAME, FILES_DEFAULT_VALUE_CONFIG_SOURCE_ORDER));
    }
}
