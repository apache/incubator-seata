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
package io.seata.config.util;

import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.source.ConfigSourceType;
import io.seata.config.source.ConfigurationSource;
import io.seata.config.source.impl.FileConfigurationSource;

/**
 * The type Configuration utils.
 *
 * @author wang.liang
 */
public final class ConfigurationUtils {

    public static String getConfig(Configuration configuration, String defaultValue, String... keys) {
        String config;
        for (String key : keys) {
            config = configuration.getString(key);
            if (StringUtils.isNotBlank(config)) {
                return config;
            }
        }

        return defaultValue;
    }


    //region get configFileName

    private static final String DEFAULT_REGISTRY_CONFIG_FILE_NAME = "registry";

    private static final String CONFIG_FILE_NAME_SYSTEM_PROPERTY_KEY = "seataConfigName";
    private static final String CONFIG_FILE_NAME_SYSTEM_ENV_KEY = "SEATA_CONFIG_NAME";
    private static final String CONFIG_FILE_NAME_KEY1 = "seata.config.name";
    private static final String CONFIG_FILE_NAME_KEY2 = "config.name";

    public static String getConfigFileName(Configuration configuration) {
        return getConfig(configuration, DEFAULT_REGISTRY_CONFIG_FILE_NAME,
                CONFIG_FILE_NAME_SYSTEM_PROPERTY_KEY, CONFIG_FILE_NAME_SYSTEM_ENV_KEY, CONFIG_FILE_NAME_KEY1, CONFIG_FILE_NAME_KEY2);
    }

    //endregion

    //region get envName

    public static final String ENV_SYSTEM_PROPERTY_KEY = "seataEnv";
    private static final String ENV_SYSTEM_ENV_KEY = "SEATA_ENV";
    private static final String ENV_KEY1 = "seata.env";
    private static final String ENV_KEY2 = "env";

    public static String getEnvName(Configuration configuration) {
        return getConfig(configuration, null,
                ENV_SYSTEM_PROPERTY_KEY, ENV_SYSTEM_ENV_KEY, ENV_KEY1, ENV_KEY2);
    }

    //endregion


    //region get configTypeName and configType

    private static final String DEFAULT_CONFIG_TYPE = "file";

    private static final String CONFIG_TYPE_SYSTEM_PROPERTY_KEY = "seataConfigType";
    private static final String CONFIG_TYPE_SYSTEM_ENV_KEY = "SEATA_CONFIG_TYPE";
    private static final String CONFIG_TYPE_KEY1 = "seata.config.type";
    private static final String CONFIG_TYPE_KEY2 = "config.type";


    private static String getConfigTypeNameInternal(Configuration configuration) {
        return getConfig(configuration, DEFAULT_CONFIG_TYPE,
                CONFIG_TYPE_SYSTEM_PROPERTY_KEY, CONFIG_TYPE_SYSTEM_ENV_KEY, CONFIG_TYPE_KEY2, CONFIG_TYPE_KEY1);
    }

    public static String getConfigTypeName(Configuration configuration) {
        String configTypeName = getConfigTypeNameInternal(configuration);
        try {
            return ConfigSourceType.getType(configTypeName).name();
        } catch (IllegalArgumentException e) {
            return configTypeName;
        }
    }

    public static ConfigSourceType getConfigType(Configuration configuration) {
        String configTypeName = getConfigTypeNameInternal(configuration);
        return ConfigSourceType.getType(configTypeName);
    }

    //endregion


    /**
     * load file sources
     *
     * @param configuration   the configuration
     * @param configFileName  the configFileName
     * @param doSetMainSource if true, do set mainSource
     */
    public static void loadFileSources(Configuration configuration, String configFileName, boolean doSetMainSource) {
        // load commonSource without env
        ConfigurationSource commonSource = new FileConfigurationSource(configFileName);
        configuration.addSourceLast(commonSource);


        // the main file configuration source
        ConfigurationSource mainSource = commonSource;


        // get envName from configuration
        String envName = getEnvName(configuration);

        // load envSource with env
        if (envName != null) {
            String envConfigFileName = configFileName + "-" + envName;
            ConfigurationSource envSource = new FileConfigurationSource(envConfigFileName);

            // add envSource before commonSource
            // The priority of envSource is higher than commonSource
            configuration.addSourceBefore(envSource, commonSource);

            mainSource = envSource;
        }


        // Set main source, if doSetMainSource is true, or the original main source is null
        if (doSetMainSource || configuration.getMainSource() == null) {
            configuration.setMainSource(mainSource);
        }
    }
}
