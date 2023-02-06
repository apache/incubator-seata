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

import java.time.Duration;
import java.util.Map;

import io.seata.common.util.ConvertUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.changelistener.ConfigurationChangeListenerManager;

/**
 * The interface Configuration.
 *
 * @author slievrly
 * @author wang.liang
 */
public interface Configuration extends UpdatableConfiguration
        , ConfigurationChangeListenerManager {

    /**
     * The constant DEFAULT_CONFIG_TIMEOUT.
     */
    long DEFAULT_CONFIG_TIMEOUT = 5 * 1000;

    /**
     * The constant DEFAULT_XXX.
     */
    short DEFAULT_SHORT = (short)0;
    int DEFAULT_INT = 0;
    long DEFAULT_LONG = 0L;
    Duration DEFAULT_DURATION = Duration.ZERO;
    boolean DEFAULT_BOOLEAN = false;

    /**
     * The env map
     */
    Map<String, String> ENV_MAP = System.getenv();


    /**
     * Get latest config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the Latest config
     */
    String getLatestConfig(String dataId, String defaultValue, long timeoutMills);

    /**
     * Get config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @param dataType     the data type
     * @param <T>          the data type
     * @return the Latest config
     */
    default <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }

        String config = this.getLatestConfig(dataId, null, timeoutMills);

        if (StringUtils.isNotBlank(config) || defaultValue == null) {
            // May be null or blank.
            return ConvertUtils.convert(config, dataType);
        }

        return defaultValue;
    }


    /**
     * Gets string config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the config
     */
    default String getString(String dataId, String defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, String.class);
    }

    default String getString(String dataId, String defaultValue) {
        return getString(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default String getString(String dataId) {
        return getString(dataId, null, DEFAULT_CONFIG_TIMEOUT);
    }


    /**
     * Gets short config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the short config
     */
    default short getShort(String dataId, short defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, Short.class);
    }

    default short getShort(String dataId, short defaultValue) {
        return getShort(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default short getShort(String dataId) {
        Short config = getConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT, Short.class);
        return config == null ? DEFAULT_SHORT : config;
    }


    /**
     * Gets int config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the int config
     */
    default int getInt(String dataId, int defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, Integer.class);
    }

    default int getInt(String dataId, int defaultValue) {
        return getInt(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default int getInt(String dataId) {
        Integer config = getConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT, Integer.class);
        return config == null ? DEFAULT_INT : config;
    }


    /**
     * Gets long config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the long config
     */
    long getLong(String dataId, long defaultValue, long timeoutMills);

    long getLong(String dataId, long defaultValue);

    long getLong(String dataId);

    /**
     * Gets duration.
     *
     * @param dataId the data id
     * @return the duration
     */
    Duration getDuration(String dataId);

    /**
     * Gets duration.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the duration
     */
    Duration getDuration(String dataId, Duration defaultValue);

    /**
     * Gets duration.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the duration
     */
    Duration getDuration(String dataId, Duration defaultValue, long timeoutMills);

    /**
     * Gets boolean.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills);

    /**
     * Gets boolean.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the boolean
     */
    boolean getBoolean(String dataId, boolean defaultValue);

    /**
     * Gets boolean.
     *
     * @param dataId the data id
     * @return the boolean
     */
    boolean getBoolean(String dataId);


    /**
     * Gets config from sys pro.
     *
     * @param dataId the data id
     * @return the config from sys pro
     */
    default String getConfigFromSys(String dataId) {
        if (StringUtils.isBlank(dataId)) {
            return null;
        }
        String content = ENV_MAP.get(dataId);
        if (null != content) {
            return content;
        }
        String envDataId = dataId.toUpperCase().replace(".", "_");
        content = ENV_MAP.get(envDataId);
        if (null != content) {
            return content;
        }
        return System.getProperty(dataId);
    }

}
