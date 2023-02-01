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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.seata.common.Cleanable;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.ConvertUtils;
import io.seata.common.util.ObjectUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.listener.ConfigListenerManager;
import io.seata.config.listener.ConfigurationChangeListener;
import io.seata.config.source.ConfigurationSource;
import io.seata.config.source.UpdatableConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract configuration
 *
 * @author wang.liang
 */
public abstract class AbstractConfiguration implements Configuration, UpdatableConfiguration
        , ConfigurationChangeListener, Cleanable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);


    // The type name
    private final String typeName;

    // The sources
    protected volatile ConfigurationSource mainSource;
    protected final List<ConfigurationSource> sources = new CopyOnWriteArrayList<>();

    // The listeners
    protected final Map<String, Set<ConfigurationChangeListener>> listeners = new ConcurrentHashMap<>();

    /**
     * Whether to print the get success log. Used to avoid printing the log repeatedly.
     */
    private volatile boolean printGetSuccessLog = true;


    protected AbstractConfiguration(String typeName) {
        this.typeName = typeName;
    }


    protected ConfigValue<?> getConfigFromSources(String dataId, long timeoutMills) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        Object value;
        for (ConfigurationSource source : this.sources) {
            value = source.getLatestConfig(dataId, timeoutMills);

            if (value == null) {
                continue;
            }

            if (ObjectUtils.isNullOrBlank(value)) {
                LOGGER.debug("Skip config '{}' blank value '{}' of type [{}] from the configuration source '{}' by configuration '{}'",
                        dataId, value, value.getClass().getName(), source.getTypeName(), this.getTypeName());
                continue;
            }

            if (this.printGetSuccessLog) {
                LOGGER.debug("Get config value ['{}' = '{}'] of type [{}] from the configuration source '{}' by configuration '{}'",
                        dataId, value, value.getClass().getName(), source.getTypeName(), this.getTypeName());
            }

            return new ConfigValue(value, source);
        }

        return null;
    }

    protected <T> ConfigValue<T> getConfigFromSources(String dataId, long timeoutMills, Class<T> dataType) {
        ConfigValue<?> configValue = getConfigFromSources(dataId, timeoutMills);
        T value = ConvertUtils.convert(configValue, dataType);
        return new ConfigValue<>(value, configValue.getFromSource());
    }

    protected ConfigValue<?> getConfigFromSources(String dataId) {
        return getConfigFromSources(dataId, DEFAULT_CONFIG_TIMEOUT);
    }


    //region # Override Configuration

    @Override
    public String getTypeName() {
        return this.typeName;
    }


    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        ConfigValue<T> configValue = this.getConfigFromSources(dataId, timeoutMills, dataType);
        return configValue == null ? defaultValue : configValue.getValue();
    }

    //endregion


    //region # Override UpdatableConfiguration

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigurationSource) {
            return ((UpdatableConfigurationSource)mainSource).putConfig(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getTypeName() + ")' " +
                    "not support putConfig");
        }
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigurationSource) {
            return ((UpdatableConfigurationSource)mainSource).putConfigIfAbsent(dataId, content, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getTypeName() + ")' " +
                    "not support atomic operation putConfigIfAbsent");
        }
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        if (mainSource instanceof UpdatableConfigurationSource) {
            return ((UpdatableConfigurationSource)mainSource).removeConfig(dataId, timeoutMills);
        } else {
            throw new NotSupportYetException("Configuration '" + this.getClass().getSimpleName() + "(" + this.getTypeName() + ")' " +
                    "not support removeConfig");
        }
    }

    //endregion


    //region # Override ConfigurationChangeListenerManager

    //region ## add config listener

    @Override
    public synchronized void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> dataIdListeners = listeners.computeIfAbsent(dataId, key -> new HashSet<>());
        dataIdListeners.add(listener);
        this.addConfigListenerToSources(dataId);
    }

    protected synchronized void addConfigListenerToSource(String dataId, ConfigurationSource source) {
        if (source instanceof ConfigListenerManager) {
            ConfigListenerManager manager = (ConfigListenerManager)source;
            manager.addConfigListener(dataId, this);
        }
    }

    protected synchronized void addConfigListenerToSources(String dataId) {
        this.sources.forEach(s -> this.addConfigListenerToSource(dataId, s));
    }

    protected synchronized void addConfigListenerToSource(ConfigurationSource source) {
        this.listeners.keySet().forEach(dataId -> {
            this.addConfigListenerToSource(dataId, source);
        });
    }

    //endregion

    //region ## remove config listener

    @Override
    public synchronized void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> dataIdListeners = this.listeners.computeIfAbsent(dataId, key -> new HashSet<>());
        dataIdListeners.remove(listener);

        if (dataIdListeners.isEmpty()) {
            this.removeConfigListenerFromSources(dataId);
            this.listeners.remove(dataId);
        }
    }

    protected synchronized void removeConfigListenerFromSources(String dataId) {
        this.sources.forEach(s -> {
            if (s instanceof ConfigListenerManager) {
                ((ConfigListenerManager)s).removeConfigListener(dataId, this);
            }
        });
    }

    protected synchronized void removeConfigListenerFromSources() {
        this.listeners.keySet().forEach(this::removeConfigListenerFromSources);
    }

    //endregion

    @Override
    public Set<String> getListenedConfigDataIds() {
        return this.listeners.keySet();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return this.listeners.get(dataId);
    }

    @Override
    public void clean() {
        this.removeConfigListenerFromSources();
        this.listeners.clear();
    }

    //endregion


    //region # Override ConfigurationSourceManager

    @Override
    public void afterAddSource(ConfigurationSource source) {
        this.addConfigListenerToSource(source);
    }

    //endregion

    //region # Override ConfigurationChangeListener

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        this.logChangeEvent(event);

        Set<ConfigurationChangeListener> configListeners = getConfigListeners(event.getDataId());
        configListeners.forEach(listener -> listener.onChangeEvent(event));
    }

    protected void logChangeEvent(ConfigurationChangeEvent event) {
        LOGGER.debug("The config '{}' has changed (value from '{}' to '{}') by the source '{}'.",
                event.getDataId(), event.getOldValue(), event.getNewValue(), event.getChangeEventSourceTypeName());
    }

    //endregion


    //region # Override ConfigurationSourceManager


    @Override
    public ConfigurationSource getMainSource() {
        return this.mainSource;
    }

    @Override
    public void setMainSource(ConfigurationSource mainSource) {
        this.mainSource = mainSource;
    }

    @Override
    public List<ConfigurationSource> getSources() {
        return this.sources;
    }

    //endregion


    public void enablePrintGetSuccessLog() {
        this.printGetSuccessLog = true;
    }

    public void disablePrintGetSuccessLog() {
        this.printGetSuccessLog = false;
    }
}
