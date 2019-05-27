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
package io.seata.config;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;

import java.util.Objects;

/**
 * The type Configuration factory.
 *
 * @author jimin.jm @alibaba-inc.com
 * @author Geng Zhang
 */
public final class ConfigurationFactory {
    private static final String REGISTRY_CONF = "registry.conf";
    private static final String REGISTRY_CONF_PREFIX = "registry";
    private static final String REGISTRY_CONF_SUFFIX = ".conf";
    private static final String ENV_SYSTEM_KEY = "SEATA_CONFIG_ENV";
    private static final String ENV_PROPERTY_KEY = "env";
    private static final String DEFAULT_ENV_VALUE = "default";
    /**
     * The constant FILE_INSTANCE.
     */
    private static String ENV_VALUE;
    static {
        String env = System.getenv(ENV_SYSTEM_KEY);
        if(env != null && System.getProperty(ENV_PROPERTY_KEY) == null){
            //Help users get
            System.setProperty(ENV_PROPERTY_KEY, env);
        }
        ENV_VALUE = System.getProperty(ENV_PROPERTY_KEY);
    }

    private static final Configuration DEFAULT_FILE_INSTANCE = new FileConfiguration(REGISTRY_CONF_PREFIX + REGISTRY_CONF_SUFFIX);
    public static final Configuration CURRENT_FILE_INSTANCE = (ENV_VALUE == null || DEFAULT_ENV_VALUE.equals(ENV_VALUE)) ? DEFAULT_FILE_INSTANCE : new FileConfiguration(REGISTRY_CONF_PREFIX + "-" + ENV_VALUE + REGISTRY_CONF_SUFFIX);
    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";

    private static volatile Configuration CONFIG_INSTANCE = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        if (CONFIG_INSTANCE == null) {
            synchronized (Configuration.class) {
                if (CONFIG_INSTANCE == null) {
                    CONFIG_INSTANCE = buildConfiguration();
                }
            }
        }
        return CONFIG_INSTANCE;
    }

    private static Configuration buildConfiguration() {
        ConfigType configType = null;
        String configTypeName = null;
        try {
            configTypeName = CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + ConfigurationKeys.FILE_ROOT_TYPE);
            configType = ConfigType.getType(configTypeName);
        } catch (Exception e) {
            throw new NotSupportYetException("not support register type: " + configTypeName, e);
        }
        if (ConfigType.File == configType) {
            String pathDataId = ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + FILE_TYPE + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + NAME_KEY;
            String name = CURRENT_FILE_INSTANCE.getConfig(pathDataId);
            return new FileConfiguration(name);
        } else {
            return EnhancedServiceLoader.load(ConfigurationProvider.class, Objects.requireNonNull(configType).name()).provide();
        }
    }
}
