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
import javax.annotation.Nullable;

import io.seata.common.executor.AbstractInitialize;
import io.seata.common.util.ConvertUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.source.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract configuration.
 *
 * @author slievrly
 * @author wang.liang
 */
public abstract class AbstractConfiguration extends AbstractInitialize implements Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);


    /**
     * The name
     */
    @Nonnull
    private final String name;


    protected AbstractConfiguration(@Nonnull String name) {
        Objects.requireNonNull(name, "The 'name' must not be null.");
        this.name = name;
    }


    //region # Get config from sources

    @Nullable
    protected ConfigInfo getConfigFromSources(String dataId, long timeoutMills) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        // get sources
        List<ConfigSource> sources = this.getSources();

        String blankValue = null;
        ConfigSource blankValueFromSource = null;

        String value;
        for (ConfigSource source : sources) {
            value = source.getLatestConfig(dataId, timeoutMills);

            if (value == null) {
                continue;
            }

            if (StringUtils.isBlank(value)) {
                if (blankValue == null) {
                    blankValue = value;
                    blankValueFromSource = source;
                }
                LOGGER.debug("Skip config '{}' blank value '{}' of type [{}] from source '{}' by configuration '{}'.",
                        dataId, value, value.getClass().getName(), source.getName(), this.getName());
                continue;
            }

            if (this.printGetSuccessLog) {
                LOGGER.debug("Get config ['{}' = '{}'] of type [{}] from source '{}' by configuration '{}'.",
                        dataId, value, value.getClass().getName(), source.getName(), this.getName());
            }

            // 1. Not blank value.
            return new ConfigInfo(dataId, value, source);
        }

        if (blankValue != null) {
            // 2. Is blank value.
            return new ConfigInfo(dataId, blankValue, blankValueFromSource);
        }

        // 3. null
        return null;
    }

    @Nullable
    protected ConfigInfo getConfigFromSources(String dataId) {
        return this.getConfigFromSources(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    //endregion # Get config from sources


    //region # Override Configuration

    @Override
    public <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        ConfigInfo configInfo = this.getConfigFromSources(dataId, timeoutMills);

        if (configInfo != null
                && (StringUtils.isNotBlank(configInfo.getValue()) || defaultValue == null)) {
            // May be null or blank.
            return ConvertUtils.convert(configInfo.getValue(), dataType);
        }

        return defaultValue;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
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
