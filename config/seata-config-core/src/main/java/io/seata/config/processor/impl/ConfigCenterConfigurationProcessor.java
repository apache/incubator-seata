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

import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.processor.ConfigurationProcessor;
import io.seata.config.source.ConfigSourceProvider;
import io.seata.config.util.ConfigurationUtils;

import static io.seata.common.exception.FrameworkErrorCode.ConfigNotFoundError;
import static io.seata.config.processor.ConfigProcessorOrdered.CONFIG_CENTER_PROCESSOR_ORDER;

/**
 * The type ConfigCenterConfigurationProcessor.
 *
 * @author wang.liang
 */
@LoadLevel(name = "config-center", order = CONFIG_CENTER_PROCESSOR_ORDER)
public class ConfigCenterConfigurationProcessor implements ConfigurationProcessor {

    @Override
    public void process(Configuration configuration) {
        // get config type name
        String configTypeName = ConfigurationUtils.getConfigTypeName(configuration);
        if (StringUtils.isBlank(configTypeName)) {
            throw new FrameworkException("Config type name can not be blank", ConfigNotFoundError);
        }

        // load Config source provider by configTypeName
        ConfigSourceProvider sourceProvider = EnhancedServiceLoader.load(ConfigSourceProvider.class, configTypeName);

        // provide one or more config source
        sourceProvider.provide(configuration);
    }
}
