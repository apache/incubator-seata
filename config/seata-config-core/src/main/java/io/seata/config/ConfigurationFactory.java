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

import java.util.Objects;
import java.util.Optional;

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

    public static volatile Configuration CURRENT_FILE_INSTANCE;

    public static volatile FileConfiguration ORIGIN_FILE_INSTANCE_REGISTRY;

    public static volatile FileConfiguration ORIGIN_FILE_INSTANCE = null;

    static {
        initOriginConfiguraction();
        load();
        maybeNeedOriginFileInstance();
    }

    private static void load() {
        Configuration configuration = ORIGIN_FILE_INSTANCE_REGISTRY;
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load Configuration from :{}",
                    extConfiguration == null ? configuration.getClass().getSimpleName() : "Spring Configuration");
            }
        } catch (EnhancedServiceNotFoundException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.warn("failed to load extConfiguration: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.error("failed to load extConfiguration: {}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = extConfiguration == null ? configuration : extConfiguration;
    }

    private static void initOriginConfiguraction() {
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
        seataConfigName = envValue == null ? seataConfigName : seataConfigName + "-" + envValue;
        // create FileConfiguration for read registry.conf
        ORIGIN_FILE_INSTANCE_REGISTRY = new FileConfiguration(seataConfigName, false);
    }

    public static FileConfiguration getOriginFileInstanceRegistry() {
        return ORIGIN_FILE_INSTANCE_REGISTRY;
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

    private static void maybeNeedOriginFileInstance() {
        if (ConfigType.File == getConfigType()) {
            String pathDataId = String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                    ConfigurationKeys.FILE_ROOT_CONFIG, FILE_TYPE, NAME_KEY);
            String name = CURRENT_FILE_INSTANCE.getConfig(pathDataId);
            // create FileConfiguration for read file.conf
            ORIGIN_FILE_INSTANCE = new FileConfiguration(name);
        }
    }

    private static ConfigType getConfigType() {
        String configTypeName = CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE);
        if (StringUtils.isBlank(configTypeName)) {
            throw new NotSupportYetException("config type can not be null");
        }
        return ConfigType.getType(configTypeName);
    }

    public static Optional<FileConfiguration> getOriginFileInstance() {
        return Optional.ofNullable(ORIGIN_FILE_INSTANCE);
    }

    private static Configuration buildConfiguration() {
        ConfigType configType = getConfigType();
        Configuration extConfiguration = null;
        Configuration configuration = ORIGIN_FILE_INSTANCE;
        if (configuration != null) {
            try {
                extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("load Configuration from :{}",
                        extConfiguration == null ? configuration.getClass().getSimpleName() : "Spring Configuration");
                }
            } catch (EnhancedServiceNotFoundException ignore) {

            } catch (Exception e) {
                LOGGER.error("failed to load extConfiguration:{}", e.getMessage(), e);
            }
        } else {
            configuration = EnhancedServiceLoader
                    .load(ConfigurationProvider.class, Objects.requireNonNull(configType).name()).provide();
        }
        try {
            Configuration configurationCache;
            if (null != extConfiguration) {
                configurationCache = ConfigurationCache.getInstance().proxy(extConfiguration);
            } else {
                configurationCache = ConfigurationCache.getInstance().proxy(configuration);
            }
            if (null != configurationCache) {
                extConfiguration = configurationCache;
            }
        } catch (EnhancedServiceNotFoundException ignore) {

        } catch (Exception e) {
            LOGGER.error("failed to load configurationCacheProvider:{}", e.getMessage(), e);
        }
        return null == extConfiguration ? configuration : extConfiguration;
    }

    protected static void reload() {
        ConfigurationCache.clear();
        initOriginConfiguraction();
        load();
        maybeNeedOriginFileInstance();
        instance = null;
        getInstance();
    }
}
