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

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

import io.seata.common.Constants;
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
        } else {
            ORIGIN_FILE_INSTANCE = null;
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

    public static void reload() {
        ConfigurationCache.clear();
        initOriginConfiguraction();
        load();
        maybeNeedOriginFileInstance();
        instance = null;
        getInstance();
    }

    /**
     * Set configuration during the runtime
     * @param conf registry new conf
     * @return old conf
     */
    public static Map<String, String> setConf(Map<String, String> conf) throws Exception {
        Map<String, String> oldConf = new HashMap<>(conf.size());
        for (String key: conf.keySet()) {
            ConfigValidator.ValidateResult validateResult = ConfigValidator.validateConfiguration(key, conf.get(key));
            if (!validateResult.getValid())
                throw new Exception(validateResult.getErrorMessage());
            if (!ConfigValidator.canBeConfiguredDynamically(key))
                throw new Exception("Cannot be configured dynamically");
            oldConf.put(key, instance.getConfig(key));
        }
        for (String key: conf.keySet()) {
            boolean success = ConfigurationFactory.getInstance().putConfig(key, conf.get(key));
            if (!success) {
                throw new Exception("Set config failed");
            }
        }
        return oldConf;
    }

    /**
     * Set registry configs during the runtime
     * @param conf registry new conf
     * @return old conf
     */
    public static Map<String, String> setRegistryConf(Map<String, String> conf) throws Exception {
        // NOTE: notice that the order `env var` > `JVM system property` > `config file(local/conf-center)`,
        //       registry could be set dynamically by injecting new properties into `JVM system property`.
        // TODO: write new conf-center configurations into `file(disk)` instead of `JVM property(memory)`

        String registryTypesPrefix = ConfigurationKeys.FILE_ROOT_REGISTRY
                + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE;
        for (String key: conf.keySet()) {
            if (key.equals(registryTypesPrefix)) {
                String[] types = conf.get(registryTypesPrefix).split(Constants.REGISTRY_TYPE_SPLIT_CHAR);
                for (String type: types) {
                    ConfigValidator.ValidateResult result = ConfigValidator.validateRegistryConf(key, type);
                    if (!result.getValid())
                        throw new Exception(result.getErrorMessage());
                }
            } else {
                ConfigValidator.ValidateResult result = ConfigValidator.validateRegistryConf(key, conf.get(key));
                if (!result.getValid())
                    throw new Exception(result.getErrorMessage());
            }
        }

        Map<String, String> newProperties = new HashMap<>();
        Map<String, String> oldProperties = new HashMap<>();
        // Check if properties are already set in `env var`
        for (String key: conf.keySet()) {
            String value = System.getenv(key);
            if (StringUtils.isBlank(value)) {
                if (!CURRENT_FILE_INSTANCE.getConfig(key).equals(conf.get(key))) {
                    newProperties.put(key, conf.get(key));
                    oldProperties.put(key, CURRENT_FILE_INSTANCE.getConfig(key));
                }
            } else if (!value.equals(conf.get(key))) {
                throw new Exception(String.format("%s is set in env before, value = %s", key, value));
            }
        }

        // Inject properties into `JVM system property`
        for (String key: newProperties.keySet()) {
            System.setProperty(key, newProperties.get(key));
        }

        reload();

        return oldProperties;
    }


    /**
     * Set configuration center configs during the runtime
     * @param conf conf-center new conf
     * @return old conf
     */
    public static Map<String, String> setConfCenterConf(Map<String, String> conf) throws Exception {
        // NOTE: notice that the order `env var` > `JVM system property` > `config file(local/conf-center)`,
        //       conf-center could be set dynamically by injecting new properties into `JVM system property`.
        // TODO: write new conf-center configurations into `file(disk)` instead of `JVM property(memory)`

        for (String key: conf.keySet()) {
            ConfigValidator.ValidateResult validateResult = ConfigValidator.validateCenterConf(key, conf.get(key));
            if (!validateResult.getValid())
                throw new Exception(validateResult.getErrorMessage());
        }

        Map<String, String> newProperties = new HashMap<>();
        Map<String, String> oldProperties = new HashMap<>();
        // Check if properties are already set in `env var`
        for (String key: conf.keySet()) {
            String value = System.getenv(key);
            if (StringUtils.isBlank(value)) {
                if (!CURRENT_FILE_INSTANCE.getConfig(key).equals(conf.get(key))) {
                    newProperties.put(key, conf.get(key));
                    oldProperties.put(key, CURRENT_FILE_INSTANCE.getConfig(key));
                }
            } else if (!value.equals(conf.get(key))){
                throw new Exception(String.format("%s is set in env before, value = %s", key, value));
            }
        }

        // Inject properties into `JVM system property`
        for (String key: newProperties.keySet()) {
            System.setProperty(key, newProperties.get(key));
        }

        try {
            reload();
        } catch (Exception e) {
            // rollback to the old configurations
            for (String key: oldProperties.keySet()) {
                System.setProperty(key, oldProperties.get(key));
            }
            reload();
            throw e;
        }

        return oldProperties;
    }
}
