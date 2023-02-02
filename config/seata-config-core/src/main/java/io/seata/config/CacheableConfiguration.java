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
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import io.seata.common.ValueWrapper;
import io.seata.common.executor.Cacheable;
import io.seata.common.executor.Cleanable;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ObjectUtils;
import io.seata.config.source.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Cacheable configuration.
 *
 * @author wang.liang
 */
public class CacheableConfiguration extends SimpleConfiguration
        implements Cacheable, Cleanable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheableConfiguration.class);

    public static final String DEFAULT_NAME = "cacheable-configuration";


    /**
     * The cache map.
     */
    private final Map<String, ValueWrapper> configCache;


    public CacheableConfiguration(String name, Map<String, ValueWrapper> configCache) {
        super(name);

        Objects.requireNonNull(configCache, "The 'configCache' must not be null.");
        this.configCache = configCache;
    }

    public CacheableConfiguration(String name) {
        this(name, new ConcurrentHashMap<>());
    }

    public CacheableConfiguration() {
        this(DEFAULT_NAME);
    }


    //region # Override AbstractConfiguration

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

        if (configInfo != null && !ObjectUtils.isNullOrBlank(configInfo.getValue())) {
            return configInfo.getValue();
        }

        // Return default value
        if (defaultValue != null) {
            LOGGER.debug("Config '{}' not found, returned defaultValue '{}' of type [{}] from parameter by configuration '{}'.",
                    dataId, defaultValue, defaultValue.getClass().getName(), this.getName());
            return defaultValue;
        }

        // Return null or blank value.
        return configInfo != null ? configInfo.getValue() : null;
    }

    //endregion # Override AbstractConfiguration


    //region # Override ConfigSourceManager

    @Override
    public void afterAddingSource(ConfigSource newSource) {
        super.afterAddingSource(newSource);

        // clean cache
        this.cleanCache();
    }

    //endregion # Override ConfigSourceManager


    //region # Override Cacheable, Cleanable

    @Override
    public void cleanCache() {
        if (this.configCache != null) {
            this.configCache.clear();
        }
    }

    @Override
    public void clean() {
        this.cleanCache();
    }

    @Nonnull
    protected Map<String, ValueWrapper> getCacheMap() {
        return this.configCache;
    }

    //endregion # Override Cacheable, Cleanable
}
