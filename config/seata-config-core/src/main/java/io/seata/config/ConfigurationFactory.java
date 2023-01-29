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
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Configuration factory.
 *
 * @author slievrly
 * @author Geng Zhang
 */
public final class ConfigurationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);

    private static final String REGISTRY_CONF_DEFAULT = "registry";
    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    public static final String ENV_PROPERTY_KEY = "seataEnv";

    private static final String SYSTEM_PROPERTY_SEATA_CONFIG_NAME = "seata.config.name";

    private static final String ENV_SEATA_CONFIG_NAME = "SEATA_CONFIG_NAME";

    public static Configuration CURRENT_FILE_INSTANCE;

    static {
        load();
    }

    private static void load() {
        String seataConfigName = System.getProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        if (seataConfigName == null) {
            seataConfigName = System.getenv(ENV_SEATA_CONFIG_NAME);
        }
        if (seataConfigName == null) {
            seataConfigName = REGISTRY_CONF_DEFAULT;
        }
        String envValue = System.getProperty(ENV_PROPERTY_KEY);
        if (envValue == null) {
            envValue = System.getenv(ENV_SYSTEM_KEY);
        }
        Configuration configuration = (envValue == null) ? new FileConfiguration(seataConfigName,
                false) : new FileConfiguration(seataConfigName + "-" + envValue, false);
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load Configuration from :{}", extConfiguration == null ?
                    configuration.getClass().getSimpleName() : "Spring Configuration");
            }
        } catch (EnhancedServiceNotFoundException ignore) {

        } catch (Exception e) {
            LOGGER.error("failed to load extConfiguration:{}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = extConfiguration == null ? configuration : extConfiguration;
    }

    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";

    private static volatile Configuration instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = buildConfiguration();
                }
            }
        }
        return instance;
    }

    private static Configuration buildConfiguration() {
        String configTypeName = getConfigTypeName();

        // build configuration by configType
        Configuration configuration;
        if ("file".equalsIgnoreCase(configTypeName)) {
            configuration = buildFileConfiguration();
        } else {
            ConfigurationProvider configurationProvider = EnhancedServiceLoader.load(ConfigurationProvider.class, configTypeName);
            configuration = configurationProvider.provide();

            // Set localConfiguration, when it's configuration center
            if (configuration instanceof AbstractConfigurationCenter) {
                Configuration fileConfiguration = buildFileConfiguration();
                ((AbstractConfigurationCenter)configuration).setLocalConfiguration(fileConfiguration);
            }
        }

        // proxy by cache
        try {
            Configuration configurationCache = ConfigurationCache.getInstance().proxy(configuration);
            if (null != configurationCache) {
                configuration = configurationCache;
                LOGGER.info("proxy the Configuration by cache.");
            }
        } catch (EnhancedServiceNotFoundException ignore) {
            // do nothing
        } catch (Exception e) {
            LOGGER.error("failed to load configurationCacheProvider:", e);
        }

        return configuration;
    }

    private static Configuration buildFileConfiguration() {
        // load fileName
        String fileNameConfigKey = String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, FILE_TYPE, NAME_KEY);
        String fileName = CURRENT_FILE_INSTANCE.getConfig(fileNameConfigKey);

        // build file configuration
        Configuration fileConfiguration = new FileConfiguration(fileName);

        // build extend configuration
        try {
            Configuration extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(fileConfiguration);
            if (extConfiguration != null) {
                fileConfiguration = extConfiguration;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("load FileConfiguration from the extConfiguration: {}", fileConfiguration.getClass().getSimpleName());
                }
            }
        } catch (EnhancedServiceNotFoundException ignore) {
            // do nothing
        } catch (Exception e) {
            LOGGER.error("failed to load extConfiguration: {}", e.getMessage(), e);
        }

        return fileConfiguration;
    }

    public static String getConfigTypeName() {
        String configTypeName = CURRENT_FILE_INSTANCE.getConfig(
                ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                        + ConfigurationKeys.FILE_ROOT_TYPE);

        if (StringUtils.isBlank(configTypeName)) {
            throw new NotSupportYetException("config type can not be null");
        }

        try {
            ConfigType configType = ConfigType.getType(configTypeName);
            configTypeName = configType.name();
        } catch (IllegalArgumentException ignore) {
            // do nothing
        }

        return configTypeName;
    }

    protected static void reload() {
        ConfigurationCache.clear();
        load();
        instance = null;
        getInstance();
    }
}
