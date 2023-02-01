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
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.ValueWrapper;
import io.seata.common.util.CollectionUtils;
import io.seata.config.source.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Cacheable configuration.
 *
 * @author wang.liang
 */
public class CacheableConfiguration extends AbstractConfiguration
        implements ConfigurationCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheableConfiguration.class);

    public static final String CACHEABLE_TYPE_NAME = "cacheable-configuration";


    // The cache
    protected final Map<String, ValueWrapper> configCache;


    public CacheableConfiguration(String typeName, Map<String, ValueWrapper> configCache) {
        super(typeName);
        this.configCache = configCache;
    }

    public CacheableConfiguration(String typeName) {
        this(typeName, new ConcurrentHashMap<>());
    }

    public CacheableConfiguration() {
        this(CACHEABLE_TYPE_NAME);
    }


    //region Override Configuration

    /**
     * Override to use cache
     */
    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        ConfigInfo<T> configInfo;

        if (configCache != null) {
            // Get config from cache
            ValueWrapper cache = CollectionUtils.computeIfAbsent(configCache, dataId, key -> {
                // Get config from sources
                ConfigInfo<T> config = this.getConfigFromSources(dataId, timeoutMills, dataType);

                // Wrap config, also when config is null
                return ValueWrapper.create(config);
            });

            configInfo = (ConfigInfo)cache.getValue();
        } else {
            // Get config from sources
            configInfo = this.getConfigFromSources(dataId, timeoutMills, dataType);
        }

        if (configInfo != null) {
            return configInfo.getValue();
        }

        // Return default value
        if (defaultValue != null) {
            LOGGER.debug("Config '{}' not found, returned the defaultValue '{}' of type [{}] from the parameter by the configuration '{}'.",
                    dataId, defaultValue, defaultValue.getClass().getName(), this.getTypeName());
            return defaultValue;
        }

        return null;
    }

    //endregion


    //region Override ConfigurationSourceManager

    @Override
    public void afterAddSource(ConfigurationSource source) {
        super.afterAddSource(source);

        // clean
        this.clean();
    }

    //endregion


    //region Override ConfigurationChangeListener

    /**
     * Override to use cache
     */
    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        String dataId = event.getDataId();

        // Get config from cache
        if (this.configCache != null && this.configCache.containsKey(dataId)) {
            ValueWrapper cache = this.configCache.get(dataId);
            if (cache != null) {
                ConfigInfo<?> configFromCache = (ConfigInfo)cache.getValue();

                if (configFromCache != null && configFromCache.getStringValue().equals(event.getNewValue())) {
                    super.logChangeEvent(event);

                    LOGGER.info("Although the config '{}' has changed (from '{}' to '{}') by source '{}', but it does not affect the final config value," +
                                    " because the new value is equals to the cache value '{}' from source '{}'." +
                                    " (The obtained at {})",
                            dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(),
                            configFromCache.getStringValue(), configFromCache.getFromSourceTypeName(), configFromCache.getStringTime());
                    return;
                }

                // The new value is not equals to the cache,
                this.configCache.remove(dataId);
            }
        }

        super.onChangeEvent(event);
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


    @Override
    public String toString() {
        return '[' +
                "name='" + getTypeName() + '\'' +
                ", mainSource=" + (getMainSource() != null ? getMainSource().getTypeName() : null) +
                ", cache.size=" + (configCache != null ? configCache.size() : null) +
                ", sources.size=" + getSources().size() +
                ']';
    }
}
