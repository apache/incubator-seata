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
package io.seata.config.processor;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;

import java.io.IOException;
import java.util.Properties;

/**
 * The Config Processor.
 *
 * @author zhixing
 */
public class ConfigProcessor {
    private static final String SEPARATOR = ".";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String DEFAULT_DATA_TYPE = "properties";
    /**
     * processing configuration
     *
     * @param config config string
     * @param dataType the data type
     * @return the properties
     * @throws IOException IOException
     */
    public static Properties processConfig(String config, String dataType) throws IOException {
        return EnhancedServiceLoader.load(Processor.class, dataType).processor(config);
    }

    /**
     * resolver config data type
     *
     * @param dataId the configured data id
     * @return data type
     */
    public static String resolverConfigDataType(String dataId) {
        return resolverConfigDataType(FILE_CONFIG.getConfig(getDataTypeKey()),dataId,DEFAULT_DATA_TYPE);
    }

    /**
     * resolver config data type
     *
     * @param dataType the configured data type
     * @param dataId the configured data id
     * @param defaultDataType the default data type
     * @return data type
     */
    public static String resolverConfigDataType(String dataType,String dataId,String defaultDataType) {
        if (StringUtils.isNotBlank(dataType)) {
            return dataType;
        }
        if (!dataId.contains(SEPARATOR)) {
            return defaultDataType;
        }
        String[] splitString = dataId.split("\\" + SEPARATOR);
        try {
            ConfigDataType configDataType = ConfigDataType.getTypeBySuffix(splitString[splitString.length - 1]);
            return configDataType.name();
        } catch (IllegalArgumentException e) {
            return defaultDataType;
        }

    }

    private static String getDataTypeKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, ConfigurationKeys.DATA_TYPE);
    }

}
