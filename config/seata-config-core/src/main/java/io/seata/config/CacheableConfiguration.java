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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.executor.Cacheable;
import io.seata.common.executor.Cleanable;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ConvertUtils;
import io.seata.common.util.ObjectUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.changelistener.ConfigChangeListenerUtils;
import io.seata.config.changelistener.ConfigurationChangeType;
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

    private static final String NAME_PREFIX = "cache:";


    /**
     * The cache map.
     */
    @Nonnull
    private final ConcurrentMap<String, ConfigCache> configCacheMap;


    public CacheableConfiguration(String name, @Nonnull ConcurrentMap<String, ConfigCache> configCacheMap) {
        super(NAME_PREFIX + name);

        Objects.requireNonNull(configCacheMap, "The 'configCacheMap' must not be null.");
        this.configCacheMap = configCacheMap;
    }

    public CacheableConfiguration(String name) {
        super(NAME_PREFIX + name);
        this.configCacheMap = new ConcurrentHashMap<>();
    }


    //region # Override AbstractConfiguration

    /**
     * Override to use cache
     */
    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        // Get config from cache
        ConfigCache cache = CollectionUtils.computeIfAbsent(configCacheMap, dataId, key -> {
            // Get config from sources
            ConfigInfo config = this.getConfigFromSources(dataId, timeoutMills);
            // Wrap config, also when config is null or blank
            return ConfigCache.fromConfigInfo(dataId, config, dataType);
        });

        T value = cache.getConfig();
        if (value != null) {
            // If the same config is obtained from cache with different types multiple times,
            // The dataType will be inconsistent with the class of the value.
            if (!dataType.isAssignableFrom(value.getClass())) {
                LOGGER.warn("The dataType '{}' used to get config '{}' is different from type '{}' in the cache." +
                                " Recommended to use the same type multiple times.",
                        dataType.getName(), dataId, value.getClass().getName());

                // Convert to the targetType
                value = ConvertUtils.convert(value, dataType);
            }

            if (ObjectUtils.isNotBlank(value)) {
                return value;
            }
        }

        // Return default value
        if (defaultValue != null) {
            LOGGER.debug("Config '{}' not found, returned defaultValue '{}' of type [{}] from parameter by configuration '{}'.",
                    dataId, defaultValue, defaultValue.getClass().getName(), this.getName());

            return defaultValue;
        }

        // May be null or blank.
        return value;
    }

    //endregion # Override AbstractConfiguration


    //region # Override ConfigSourceManager

    @Override
    public void onAddedSource(ConfigSource newSource, List<ConfigSource> higherSources, List<ConfigSource> lowerSources) {
        super.onAddedSource(newSource, higherSources, lowerSources);

        // Compare the priority of the config in the cache with the value in the newSource
        this.configCacheMap.forEach((dataId, oldCache) -> {
            String oldValue = oldCache.getStringValue();

            if (oldValue != null && oldCache.getSource().getOrder() > newSource.getOrder()) {
                // the source in the cache is higher than newSource
                return;
            }

            String newValue = newSource.getLatestConfig(oldCache.getDataId());
            if (newValue != null && !Objects.equals(oldValue, newValue)) {
                ConfigurationChangeType type = ConfigChangeListenerUtils.getChangeType(oldValue, newValue);
                this.changeCache(oldCache, newSource, newValue, type, "addSource_and_cacheChanged");
            }
        });
    }

    protected void changeCache(ConfigCache oldCache, ConfigSource newSource, String newValue, ConfigurationChangeType type, String namespace) {
        String dataId = oldCache.getDataId();

        ConfigCache newCache = ConfigCache.create(dataId, newValue, oldCache.getType(), newSource);
        this.configCacheMap.put(dataId, newCache);
        this.onCacheChanged(new ConfigCacheChangeEvent(dataId, oldCache, newCache, type, namespace));
    }

    /**
     * After the cache changed, trigger this method
     *
     * @param event the event
     */
    protected void onCacheChanged(ConfigCacheChangeEvent event) {
        // default do nothing
    }

    //endregion # Override ConfigSourceManager


    //region # Override Cacheable, Cleanable


    @Override
    public ConfigCache removeCache(String key) {
        return this.configCacheMap.remove(key);
    }

    @Override
    public void cleanCaches() {
        this.configCacheMap.clear();
    }

    @Override
    public void clean() {
        this.cleanCaches();
    }

    @Nonnull
    protected Map<String, ConfigCache> getCacheMap() {
        return this.configCacheMap;
    }

    @Nullable
    protected ConfigCache getCache(String dataId) {
        return this.configCacheMap.get(dataId);
    }

    @Nullable
    protected Object getCacheValue(String dataId) {
        ConfigCache configCache = this.getCache(dataId);
        return configCache != null ? configCache.getValue() : null;
    }

    //endregion # Override Cacheable, Cleanable
}
