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

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

import io.seata.common.ValueWrapper;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.defaultconfig.DefaultConfigManager;
import io.seata.config.defaultconfig.DefaultConfigManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Seata configuration.
 *
 * @author wang.liang
 */
public class SeataConfiguration extends CacheableConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataConfiguration.class);

    public static final String DEFAULT_TYPE_NAME = "seata-default";


    // The default config manager
    @Nonnull
    protected final DefaultConfigManager defaultConfigManager;


    //region Constructor

    public SeataConfiguration(String typeName, Map<String, ValueWrapper> configCache, DefaultConfigManager defaultConfigManager) {
        super(typeName, configCache);
        this.defaultConfigManager = this.requireNonNull(defaultConfigManager);
    }

    public SeataConfiguration(String typeName, DefaultConfigManager defaultConfigManager) {
        super(typeName);
        this.defaultConfigManager = this.requireNonNull(defaultConfigManager);
    }

    public SeataConfiguration(String typeName, Map<String, ValueWrapper> configCache) {
        super(typeName, configCache);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    public SeataConfiguration(String typeName) {
        super(typeName);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    public SeataConfiguration() {
        super(DEFAULT_TYPE_NAME);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }


    private DefaultConfigManager requireNonNull(DefaultConfigManager defaultConfigManager) {
        Objects.requireNonNull(defaultConfigManager, "The defaultConfigManager must be not null");
        return defaultConfigManager;
    }

    //endregion


    //region Override CacheableConfiguration

    /**
     * Override to use defaultConfigManager
     */
    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        T config = super.getConfig(dataId, defaultValue, timeoutMills, dataType);

        if (config == null) {
            return this.getDefaultValueFromDefaultConfigManager(dataId, dataType);
        }

        return config;
    }

    //endregion


    //region The default config manager

    /**
     * build default config manager
     *
     * @return the DefaultValueConfiguration
     */
    protected DefaultConfigManager buildDefaultConfigManager() {
        DefaultConfigManagerBuilder builder = EnhancedServiceLoader.load(DefaultConfigManagerBuilder.class);
        return builder.build();
    }

    /**
     * Get default value from defaultConfigManager
     *
     * @param dataId   the data type
     * @param dataType the data type
     * @param <T>      the data type
     * @return the default value
     */
    protected <T> T getDefaultValueFromDefaultConfigManager(String dataId, Class<T> dataType) {
        T defaultValue = defaultConfigManager.getConfig(dataId, Configuration.DEFAULT_CONFIG_TIMEOUT, dataType);

        if (defaultValue != null) {
            LOGGER.debug("Get config defaultValue ['{}' = '{}'] of type [{}] from defaultConfigManager '{}' by configuration '{}'",
                    dataId, defaultValue, defaultValue.getClass().getName(), defaultConfigManager.getTypeName(), this.getTypeName());
            return defaultValue;
        }

        return null;
    }

    //endregion
}
