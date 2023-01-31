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
public abstract class AbstractConfiguration implements Configuration, UpdatableConfiguration, Cleanable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);


    // The type name
    private final String typeName;

    // The sources
    protected ConfigurationSource mainSource;
    protected List<ConfigurationSource> sources = new CopyOnWriteArrayList<>();

    // The listeners
    protected final Map<String, Set<ConfigurationChangeListener>> listeners = new ConcurrentHashMap<>();


    protected AbstractConfiguration(String typeName) {
        this.typeName = typeName;
    }


    protected Object getConfigFromSources(String dataId, long timeoutMills) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        Object value;
        for (ConfigurationSource source : sources) {
            value = source.getLatestConfig(dataId, timeoutMills);

            if (ObjectUtils.isNullOrBlank(value)) {
                LOGGER.debug("Skip config '{}' blank value from the configuration source '{}' by configuration '{}'",
                        dataId, source.getTypeName(), this.getTypeName());
                continue;
            }

            LOGGER.debug("Get config '{}' value '{}' from the configuration source '{}' by configuration '{}'",
                    dataId, value, source.getTypeName(), this.getTypeName());
            return value;
        }

        return null;
    }

    protected <T> T getConfigFromSources(String dataId, long timeoutMills, Class<T> dataType) {
        Object config = getConfigFromSources(dataId, timeoutMills);
        return ConvertUtils.convert(config, dataType);
    }


    //region Override Configuration

    @Override
    public String getTypeName() {
        return this.typeName;
    }


    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        T value = this.getConfigFromSources(dataId, timeoutMills, dataType);
        return value == null ? defaultValue : value;
    }

    //endregion


    //region Override UpdatableConfiguration

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


    //region Override ConfigurationChangeListenerManager

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> dataIdListeners = listeners.computeIfAbsent(dataId, key -> new HashSet<>());
        dataIdListeners.add(listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> dataIdListeners = listeners.computeIfAbsent(dataId, key -> new HashSet<>());
        dataIdListeners.remove(listener);
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return this.listeners.get(dataId);
    }

    @Override
    public void clean() {
        this.listeners.clear();
    }

    //endregion


    //region Override ConfigurationSourceManager


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
}
