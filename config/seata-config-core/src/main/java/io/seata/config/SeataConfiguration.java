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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;
import io.seata.config.changelistener.ConfigurationChangeEvent;
import io.seata.config.changelistener.ConfigurationChangeListener;
import io.seata.config.changelistener.ConfigurationChangeListenerManager;
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

    private static final String NAME_PREFIX = "seata:";


    /**
     * The config change listener map.
     */
    protected final Map<String, Set<ConfigurationChangeListener>> listenersMap = new ConcurrentHashMap<>();


    //region # Constructor

    public SeataConfiguration(String name, ConcurrentMap<String, ConfigCache> configCache) {
        super(NAME_PREFIX + name, configCache);
    }

    public SeataConfiguration(String name) {
        super(NAME_PREFIX + name);
    }

    //endregion # Constructor


    //region # Override CacheableConfiguration

    @Override
    protected void onCacheChanged(ConfigCacheChangeEvent event) {
        super.onCacheChanged(event);

        String oldCacheValue = event.getOldCache() != null ? event.getOldCache().getStringValue() : null;

        ConfigurationChangeEvent configChangeEvent = new ConfigurationChangeEvent(event.getDataId(), event.getNamespace(),
                oldCacheValue, event.getNewCache().getStringValue(), event.getType(), event.getNewCache().getSource());

        this.doConfigListenersChangeEvent(configChangeEvent);
    }

    //endregion # Override CacheableConfiguration


    //region # Override UpdatableConfiguration

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource) mainSource).putConfig(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' "
                    + "not support putConfig");
        }
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource) mainSource).putConfigIfAbsent(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' "
                    + "not support atomic operation putConfigIfAbsent");
        }
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigSource) {
            return ((UpdatableConfigSource) mainSource).removeConfig(dataId, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getName() + ")' "
                    + "not support removeConfig");
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

        LOGGER.info("Add config listener: dataId = '{}', listener = '{}'.", dataId, listener);

        listenersMap.computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                .add(listener);

        // add self (self is a ConfigurationChangeListener) to sources
        this.addSelfConfigListenerToSources(dataId);
    }

    protected void addSelfConfigListenerToSource(String dataId, ConfigSource source) {
        if (source instanceof ConfigurationChangeListenerManager) {
            ConfigurationChangeListenerManager manager = (ConfigurationChangeListenerManager) source;
            manager.addConfigListener(dataId, this);
            LOGGER.info("Add config listener to source: dataId = '{}', source = '{}'.", dataId, source.getName());
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
        LOGGER.info("Remove config listener: dataId = '{}', listener = '{}'.", dataId, listener);

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
                ((ConfigurationChangeListenerManager) source).removeConfigListener(dataId, this);
                LOGGER.info("Remove config listener from source: dataId = '{}', source = '{}'.", dataId, source.getName());
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
        String dataId = event.getDataId();

        // Get config cache
        ConfigCache oldCache = this.getCache(dataId);
        if (oldCache != null) {
            String cacheValue = oldCache.getStringValue();

            // Compare the order
            if (oldCache.getSource().getOrder() > event.getChangeEventSource().getOrder()) {
                LOGGER.info("Ignore current change. Although the config '{}' has changed (from '{}' to '{}') by source '{}',"
                                + " but the order of the changeSource is lower than the cache source '{}'. (Cache time '{}')",
                        dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(),
                        oldCache.getSourceName(), oldCache.getTimeStr());
                return;
            }

            // Compare the value
            if (Objects.equals(cacheValue, event.getNewValue())) {
                LOGGER.info("Ignore current change. Although the config '{}' has changed (from '{}' to '{}') by source '{}',"
                                + " but the new value is equals to the cache value '{}' from source '{}'. (Cache time '{}')",
                        dataId, event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName(),
                        cacheValue, oldCache.getSourceName(), oldCache.getTimeStr());
                return;
            }

            this.changeCache(oldCache, event.getChangeEventSource(), event.getNewValue(), event.getChangeType(), event.getNamespace());
        } else {
            // Do nothing: There is no cache, which means that the config of the dataId has not been obtained anywhere.
        }
    }

    private synchronized void doConfigListenersChangeEvent(ConfigurationChangeEvent event) {
        LOGGER.info("The config '{}' has changed by event: {}", event.getDataId(), event);

        Set<ConfigurationChangeListener> configListeners = getConfigListeners(event.getDataId());
        if (configListeners != null) {
            configListeners.forEach(listener -> listener.onChangeEvent(event));
        }
    }

    //endregion ## Override ConfigurationChangeListener


    //region ## Override ConfigSourceManager

    /**
     * Override: Add the config listener to the new source.
     *
     * @param newSource the new source
     */
    @Override
    public void onAddedSource(ConfigSource newSource, List<ConfigSource> higherSources, List<ConfigSource> lowerSources) {
        super.onAddedSource(newSource, higherSources, lowerSources);

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
