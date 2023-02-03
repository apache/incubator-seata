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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.processor.ConfigurationProcessor;
import io.seata.config.source.ConfigSourceProvider;
import io.seata.config.util.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.config.processor.ConfigProcessorOrdered.CONFIG_CENTER_PROCESSOR_ORDER;

/**
 * The type ConfigCenterConfigurationProcessor.
 *
 * @author wang.liang
 */
@LoadLevel(name = "config-center", order = CONFIG_CENTER_PROCESSOR_ORDER)
public class ConfigCenterConfigurationProcessor implements ConfigurationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigCenterConfigurationProcessor.class);


    @Override
    public void process(Configuration configuration) {
        // get config type name
        String configTypeName = ConfigurationUtils.getConfigTypeName(configuration);
        if (StringUtils.isBlank(configTypeName)) {
            LOGGER.warn("Config type name is null or blank: {}, do not load the config center.", configTypeName);
            return;
        }

        // load Config source provider by configTypeName
        ConfigSourceProvider sourceProvider;
        try {
            sourceProvider = EnhancedServiceLoader.load(ConfigSourceProvider.class, configTypeName);
        } catch (EnhancedServiceNotFoundException e) {
            LOGGER.error("The config source provider for config center '{}' is not found, do not load the config center.",
                    configTypeName, e);
            return;
        }

        // provide one or more config source
        sourceProvider.provide(configuration);
    }
}
