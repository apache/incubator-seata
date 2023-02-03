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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.ObjectUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.changelistener.ConfigurationChangeEvent;
import io.seata.config.changelistener.ConfigurationChangeListener;
import io.seata.config.changelistener.ConfigurationChangeListenerManager;
import io.seata.config.defaultconfig.DefaultConfigManager;
import io.seata.config.defaultconfig.DefaultConfigManagerBuilder;
import io.seata.config.source.ConfigSource;
import io.seata.config.source.UpdatableConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Seata configuration.
 *
 * @author wang.liang
 */
public class SeataConfiguration extends CacheableConfiguration
        implements ConfigurationChangeListener, ConfigurationChangeListenerManager
        , UpdatableConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataConfiguration.class);

    public static final String DEFAULT_NAME = "seata-default";


    /**
     * The default config manager.
     */
    protected final DefaultConfigManager defaultConfigManager;

    /**
     * The config change listener map.
     */
    protected final Map<String, Set<ConfigurationChangeListener>> listenersMap = new ConcurrentHashMap<>();


    //region # Constructor

    public SeataConfiguration(String name, Map<String, ConfigCache> configCache, DefaultConfigManager defaultConfigManager) {
        super(name, configCache);
        this.defaultConfigManager = defaultConfigManager;
    }

    public SeataConfiguration(String name, DefaultConfigManager defaultConfigManager) {
        super(name);
        this.defaultConfigManager = defaultConfigManager;
    }

    public SeataConfiguration(String name, Map<String, ConfigCache> configCache) {
        super(name, configCache);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    public SeataConfiguration(String name) {
        super(name);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    public SeataConfiguration() {
        super(DEFAULT_NAME);
        this.defaultConfigManager = this.buildDefaultConfigManager();
    }

    //endregion # Constructor


    //region # Override CacheableConfiguration

    /**
     * Override to use defaultConfigManager.
     * <p>
     * However, the defaultValue in the parameter still has higher priority, is not null or blank.
     */
    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        T config = super.getConfig(dataId, defaultValue, timeoutMills, dataType);

        if (ObjectUtils.isNullOrBlank(config)) {
            // Get default value from defaultConfigManager.
            defaultValue = this.getDefaultValueFromDefaultConfigManager(dataId, dataType);
            if (defaultValue != null) {
                return defaultValue;
            }
        }

        // May be null or blank.
        return config;
    }


    //region ## The default config manager

    /**
     * build default config manager
     *
     * @return the DefaultValueConfiguration
     */
    @Nullable
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
        if (defaultConfigManager != null) {
            T defaultValue = defaultConfigManager.getConfig(dataId, Configuration.DEFAULT_CONFIG_TIMEOUT, dataType);

            if (defaultValue != null) {
                LOGGER.debug("Get config defaultValue ['{}' = '{}'] of type [{}] from defaultConfigManager '{}' by configuration '{}'",
                        dataId, defaultValue, defaultValue.getClass().getName(), defaultConfigManager.getName(), this.getName());
                return defaultValue;
            }
        }

        return null;
    }

    //endregion ## The default config manager

    //endregion # Override CacheableConfiguration


    //region # Override UpdatableConfiguration

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource)mainSource).putConfig(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' " +
                    "not support putConfig");
        }
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource)mainSource).putConfigIfAbsent(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' " +
                    "not support atomic operation putConfigIfAbsent");
        }
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource)mainSource).removeConfig(dataId, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' " +
                    "not support removeConfig");
        }
    }

    //endregion # Override UpdatableConfiguration


    //region # Override ConfigurationChangeListenerManager

    //region ## add config listener

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        LOGGER.info("Add config listener: dataId = {}, listener = {}", dataId, listener);

        listenersMap.computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                .add(listener);

        // add self (self is a ConfigurationChangeListener) to sources
        this.addSelfConfigListenerToSources(dataId);
    }

    protected void addSelfConfigListenerToSource(String dataId, ConfigSource source) {
        if (source instanceof ConfigurationChangeListenerManager) {
            ConfigurationChangeListenerManager manager = (ConfigurationChangeListenerManager)source;
            manager.addConfigListener(dataId, this);
            LOGGER.info("Add config listener to source: dataId = {}, source = {}", dataId, source.getName());
        }
    }

    protected void addSelfConfigListenerToSources(String dataId) {
        this.getSources().forEach(s -> this.addSelfConfigListenerToSource(dataId, s));
    }

    protected void addSelfConfigListenersToSource(ConfigSource source) {
        this.listenersMap.keySet().forEach(dataId -> {
            this.addSelfConfigListenerToSource(dataId, source);
        });
    }

    //endregion ## add config listener


    //region ## remove config listener

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        LOGGER.info("Remove config listener: dataId = {}, listener = {}", dataId, listener);

        Set<ConfigurationChangeListener> listeners = this.listenersMap.get(dataId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                this.removeSelfConfigListenerFromSources(dataId);
                this.listenersMap.remove(dataId);
            }
        }
    }

    protected void removeSelfConfigListenerFromSources(String dataId) {
        this.getSources().forEach(source -> {
            if (source instanceof ConfigurationChangeListenerManager) {
                ((ConfigurationChangeListenerManager)source).removeConfigListener(dataId, this);
                LOGGER.info("Remove config listener from source: dataId = {}, source = {}", dataId, source.getName());
            }
        });
    }

    protected void removeSelfConfigListenerFromSources() {
        this.listenersMap.keySet().forEach(this::removeSelfConfigListenerFromSources);
    }

    //endregion ## remove config listener


    @Override
    public Set<String> getListenedConfigDataIds() {
        return this.listenersMap.keySet();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return this.listenersMap.get(dataId);
    }


    private void cleanListeners() {
        // First, remove self (self is a ConfigurationChangeListener) from sources.
        this.removeSelfConfigListenerFromSources();

        // Clear the listener map.
        this.listenersMap.clear();
    }


    //region ## Override ConfigurationChangeListener

    /**
     * This method will be called by the {@link ConfigSource}.
     *
     * @see this#addSelfConfigListenerToSource(String dataId, ConfigSource source)
     */
    @Override
    public synchronized void onChangeEvent(ConfigurationChangeEvent event) {
        // If the new value is equals to the cache value, do not trigger the event.
        if (this.newValueEqualsToCacheValue(event)) {
            return;
        }

        LOGGER.info("The config '{}' has changed (from '{}' to '{}') by the source '{}'.",
                event.getDataId(), event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName());

        this.doConfigListenersChangeEvent(event);
    }

    private void doConfigListenersChangeEvent(ConfigurationChangeEvent event) {
        Set<ConfigurationChangeListener> configListeners = getConfigListeners(event.getDataId());
        configListeners.forEach(listener -> listener.onChangeEvent(event));
    }

    private boolean newValueEqualsToCacheValue(ConfigurationChangeEvent event) {
        String dataId = event.getDataId();

        // Get config cache
        ConfigCache configCache = this.getCache(dataId);
        if (configCache != null) {
            String cacheValue = configCache.getStringValue();

            // Compare cache with the new value
            if (cacheValue != null && cacheValue.equals(event.getNewValue())) {
                LOGGER.info("Ignore current change. Although the config '{}' has changed (from '{}' to '{}') by source '{}'," +
                                " but it does not affect the final config value, because the new value is equals to the cache value '{}' from source '{}'. (Cache time '{}')",
                        dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(),
                        cacheValue, configCache.getSourceName(), configCache.getTimeStr());
                return true;
            }

            // Get config from the sources
            ConfigInfo configInfoFromSources = this.getConfigFromSources(dataId);
            String configFromSources = configInfoFromSources != null ? configInfoFromSources.getValue() : null;
            ConfigSource fromSource = configInfoFromSources != null ? configInfoFromSources.getSource() : null;
            if (Objects.equals(configFromSources, cacheValue) && fromSource != configCache.getSource()) {
                String fromSourceName = fromSource != null ? fromSource.getName() : null;
                LOGGER.info("Ignore current change. Although the config '{}' has changed (from '{}' to '{}') by source '{}'," +
                                " but it does not affect the final config value. The config value '{}' from source '{}'.",
                        dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(),
                        configFromSources, fromSourceName
                );
                return true;
            }

            // Remove cache
            LOGGER.info("Remove cache of config '{}', because the config has changed (from '{}' to '{}') by source '{}'," +
                            " and newValue is not equals to cacheValue '{}'. It may affect the final value of the config.",
                    dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(), cacheValue
            );
            this.removeCache(dataId);
        }

        return false;
    }

    //endregion ## Override ConfigurationChangeListener


    //region ## Override ConfigSourceManager

    /**
     * Override: Add the config listener to the new source.
     *
     * @param newSource the new source
     */
    @Override
    public void afterAddingSource(ConfigSource newSource) {
        super.afterAddingSource(newSource);

        // add self (self is a ConfigurationChangeListener) to source
        this.addSelfConfigListenersToSource(newSource);
    }

    //endregion ## Override ConfigSourceManager


    //region ## Override Cleanable

    @Override
    public void clean() {
        super.clean();
        this.cleanListeners();
    }

    //endregion ## Override Cleanable

    //endregion # Override ConfigurationChangeListenerManager
}
