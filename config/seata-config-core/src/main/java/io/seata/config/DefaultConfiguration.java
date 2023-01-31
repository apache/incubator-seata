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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.Cleanable;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.config.source.DefaultValueConfigurationSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default configuration.
 *
 * @author wang.liang
 */
public class DefaultConfiguration extends AbstractConfiguration
        implements ConfigurationCacheManager, Cleanable
        , DefaultConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfiguration.class);

    private static final String DEFAULT_CONFIGURATION_TYPE_NAME = "seata-default-configuration";
    private static final String DEFAULT_CONFIG_MANAGER_TYPE_NAME = "seata-default-config-manager";


    // The cache
    protected final Map<String, ObjectWrapper> configCache;

    // The default config manager
    protected final DefaultConfigManager defaultConfigManager;


    public DefaultConfiguration() {
        this(new ConcurrentHashMap<>());
    }

    public DefaultConfiguration(Map<String, ObjectWrapper> configCache) {
        super(DEFAULT_CONFIGURATION_TYPE_NAME);
        this.configCache = configCache;
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    public DefaultConfiguration(String typeName, DefaultConfigManager defaultConfigManager) {
        this(typeName, new ConcurrentHashMap<>(), defaultConfigManager);
    }

    public DefaultConfiguration(String typeName, Map<String, ObjectWrapper> configCache, DefaultConfigManager defaultConfigManager) {
        super(typeName);
        this.configCache = configCache;
        this.defaultConfigManager = defaultConfigManager;
    }


    //region The default config manager

    /**
     * build default config manager
     *
     * @return the DefaultValueConfiguration
     */
    protected DefaultConfigManager buildDefaultConfigManager() {
        DefaultConfigManager defaultConfigManager = new DefaultConfiguration(DEFAULT_CONFIG_MANAGER_TYPE_NAME, null);

        // load defaultValue source
        List<DefaultValueConfigurationSourceProvider> providers = EnhancedServiceLoader.loadAll(DefaultValueConfigurationSourceProvider.class);
        for (DefaultValueConfigurationSourceProvider provider : providers) {
            provider.provide(defaultConfigManager);
        }

        return defaultConfigManager;
    }

    protected <T> T getDefaultValueFromDefaultConfigManager(String dataId, Class<T> dataType) {
        T defaultValue = null;

        if (defaultConfigManager != null) {
            defaultValue = defaultConfigManager.getConfig(dataId, Configuration.DEFAULT_CONFIG_TIMEOUT, dataType);
            if (defaultValue != null) {
                LOGGER.debug("Get config '{}' defaultValue '{}' from the defaultConfigManager '{}'",
                        dataId, defaultValue, defaultConfigManager.getTypeName());
            }
        }

        return defaultValue;
    }

    //endregion


    //region Override Configuration

    /**
     * Override to use cache and defaultConfigManager
     */
    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        T value;

        if (configCache != null) {
            // Get config from cache
            ObjectWrapper cache = CollectionUtils.computeIfAbsent(configCache, dataId, key -> {
                // Get config from sources
                T config = this.getConfigFromSources(dataId, timeoutMills, dataType);

                // Wrap config, also when config is null
                return new ObjectWrapper(config);
            });

            value = (T)cache.get();
        } else {
            // Get config from sources
            value = this.getConfigFromSources(dataId, timeoutMills, dataType);
        }

        if (value != null) {
            return value;
        }

        // Return default value
        if (defaultValue != null) {
            return defaultValue;
        } else {
            // Get config defaultValue from defaultConfigManager
            return getDefaultValueFromDefaultConfigManager(dataId, dataType);
        }
    }

    //endregion


    //region Override ConfigurationSourceManager

    @Override
    public void afterAddSource() {
        super.afterAddSource();

        // clean
        this.clean();
    }

    //endregion


    //region Override ConfigurationChangeListener

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {

    }

    //endregion


    //region Override Cleanable

    @Override
    public void clean() {
        super.clean();

        if (this.configCache != null) {
            this.configCache.clear();
        }
    }

    //endregion


    /**
     * The type ObjectWrapper
     */
    private static class ObjectWrapper {
        private final Object object;

        ObjectWrapper(Object object) {
            this.object = object;
        }

        Object get() {
            return object;
        }
    }
}
