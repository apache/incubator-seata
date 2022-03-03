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
import java.util.Set;

import io.seata.common.util.DurationUtil;
import io.seata.common.util.StringUtils;

/**
 * The interface Configuration.
 *
 * @author slievrly
 * @author funkye
 */
public interface Configuration {
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

    Map<String, String> ENV_MAP = System.getenv();

    /**
     * Put config boolean.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean putConfig(String dataId, String content, long timeoutMills);

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
     * Put config if absent boolean.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean putConfigIfAbsent(String dataId, String content, long timeoutMills);

    /**
     * Remove config boolean.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean removeConfig(String dataId, long timeoutMills);

    /**
     * Add config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    void addConfigListener(String dataId, ConfigurationChangeListener listener);

    /**
     * Remove config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    void removeConfigListener(String dataId, ConfigurationChangeListener listener);

    /**
     * Gets config listeners.
     *
     * @param dataId the data id
     * @return the config listeners
     */
    Set<ConfigurationChangeListener> getConfigListeners(String dataId);

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

    default short getShort(String dataId, short defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Short.parseShort(result);
    }

    default short getShort(String dataId, short defaultValue) {
        return getShort(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default short getShort(String dataId) {
        return getShort(dataId, DEFAULT_SHORT);
    }

    default int getInt(String dataId, int defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Integer.parseInt(result);
    }

    default int getInt(String dataId, int defaultValue) {
        return getInt(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default int getInt(String dataId) {
        return getInt(dataId, DEFAULT_INT);
    }

    default long getLong(String dataId, long defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Long.parseLong(result);
    }

    default long getLong(String dataId, long defaultValue) {
        return getLong(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default long getLong(String dataId) {
        return getLong(dataId, DEFAULT_LONG);
    }

    default Duration getDuration(String dataId) {
        return getDuration(dataId, DEFAULT_DURATION);
    }

    default Duration getDuration(String dataId, Duration defaultValue) {
        return getDuration(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : DurationUtil.parse(result);
    }

    default boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
        String result = getConfig(dataId, timeoutMills);
        return StringUtils.isBlank(result) ? defaultValue : Boolean.parseBoolean(result);
    }

    default boolean getBoolean(String dataId, boolean defaultValue) {
        return getBoolean(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default boolean getBoolean(String dataId) {
        return getBoolean(dataId, DEFAULT_BOOLEAN);
    }

    default String getConfig(String dataId, String defaultValue) {
        return getConfig(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default String getConfig(String dataId, long timeoutMills) {
        return getConfig(dataId, null, timeoutMills);
    }

    default String getConfig(String dataId, String content, long timeoutMills) {
        String value = getConfigFromSys(dataId);
        if (value != null) {
            return value;
        }
        return getLatestConfig(dataId, content, timeoutMills);
    }

    default String getConfig(String dataId) {
        return getConfig(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    default boolean putConfig(String dataId, String content) {
        return putConfig(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }

    default boolean putConfigIfAbsent(String dataId, String content) {
        return putConfigIfAbsent(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }

    default boolean removeConfig(String dataId) {
        return removeConfig(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

    /**
     * Gets type name.
     *
     * @return the type name
     */
    String getTypeName();

}
