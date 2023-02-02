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
import java.util.Objects;
import javax.annotation.Nonnull;

import io.seata.common.executor.AbstractInitialize;
import io.seata.common.executor.Initialize;
import io.seata.common.util.ConvertUtils;
import io.seata.common.util.ObjectUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.source.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract configuration
 *
 * @author wang.liang
 */
public abstract class AbstractConfiguration extends AbstractInitialize implements Configuration, Initialize {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);


    /**
     * The name
     */
    private final String name;


    protected AbstractConfiguration(String name) {
        Objects.requireNonNull(name, "The 'name' must not be null.");
        this.name = name;
    }


    //region # Get config from sources

    protected ConfigInfo<?> getConfigFromSources(String dataId, long timeoutMills) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        // get sources
        List<ConfigSource> sources = this.getSources();
        if (sources == null) {
            LOGGER.debug("The sources is null in configuration '{}'.", this.getName());
            return null;
        }

        Object blankValue = null;
        ConfigSource blankValueFromSource = null;

        Object value;
        for (ConfigSource source : sources) {
            value = source.getLatestConfig(dataId, timeoutMills);

            if (value == null) {
                continue;
            }

            if (ObjectUtils.isNullOrBlank(value)) {
                if (blankValue == null) {
                    blankValue = value;
                    blankValueFromSource = source;
                }
                LOGGER.debug("Skip config '{}' blank value '{}' of type [{}] from source '{}' by configuration '{}'.",
                        dataId, value, value.getClass().getName(), source.getTypeName(), this.getName());
                continue;
            }

            if (this.printGetSuccessLog) {
                LOGGER.debug("Get config ['{}' = '{}'] of type [{}] from source '{}' by configuration '{}'.",
                        dataId, value, value.getClass().getName(), source.getTypeName(), this.getName());
            }

            return new ConfigInfo<>(dataId, value, source);
        }

        // May be null or blank.
        return blankValue != null ? new ConfigInfo<>(dataId, blankValue, blankValueFromSource) : null;
    }

    protected <T> ConfigInfo<T> getConfigFromSources(String dataId, long timeoutMills, Class<T> dataType) {
        ConfigInfo<?> configInfo = this.getConfigFromSources(dataId, timeoutMills);
        if (configInfo == null) {
            return null;
        }

        if (dataType.isAssignableFrom(configInfo.getValue().getClass())) {
            return (ConfigInfo<T>)configInfo;
        }

        T value = ConvertUtils.convert(configInfo.getValue(), dataType);
        return new ConfigInfo<>(dataId, value, configInfo.getFromSource());
    }

    protected ConfigInfo<?> getConfigFromSources(String dataId) {
        return this.getConfigFromSources(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    //endregion # Get config from sources


    //region # Override Configuration

    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        ConfigInfo<T> configInfo = this.getConfigFromSources(dataId, timeoutMills, dataType);
        return configInfo == null ? defaultValue : configInfo.getValue();
    }

    @Nonnull
    @Override
    public String getName() {
        return this.name;
    }

    //endregion # Override Configuration


    //region # printGetSuccessLog

    /**
     * Whether to print the get success log. Used to avoid printing the log repeatedly.
     */
    private volatile boolean printGetSuccessLog = true;

    public void enablePrintGetSuccessLog() {
        this.printGetSuccessLog = true;
    }

    public void disablePrintGetSuccessLog() {
        this.printGetSuccessLog = false;
    }

    //endregion # printGetSuccessLog


    @Override
    public String toString() {
        return "[name='" + getName() + "']";
    }
}
