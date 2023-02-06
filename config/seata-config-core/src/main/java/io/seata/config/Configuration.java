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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import io.seata.common.util.ConvertUtils;
import io.seata.config.source.ConfigSourceManager;

/**
 * The interface Configuration.
 *
 * @author slievrly
 * @author wang.liang
 */
public interface Configuration extends ConfigSourceManager {

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
     * Get name
     *
     * @return the name
     */
    @Nonnull
    String getName();

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
    <T> T getConfig(String dataId, T defaultValue, long timeoutMills, Class<T> dataType);

    default <T> T getConfig(String dataId, T defaultValue, Class<T> dataType) {
        return getConfig(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT, dataType);
    }

    default <T> T getConfig(String dataId, long timeoutMills, Class<T> dataType) {
        return getConfig(dataId, null, timeoutMills, dataType);
    }

    default <T> T getConfig(String dataId, Class<T> dataType) {
        return getConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT, dataType);
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
    default long getLong(String dataId, long defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, Long.class);
    }

    default long getLong(String dataId, long defaultValue) {
        return getLong(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default long getLong(String dataId) {
        Long config = getConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT, Long.class);
        return config == null ? DEFAULT_LONG : config;
    }


    /**
     * Gets duration config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the duration config
     */
    default Duration getDuration(String dataId, Duration defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, Duration.class);
    }

    default Duration getDuration(String dataId, Duration defaultValue) {
        return getDuration(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default Duration getDuration(String dataId) {
        return getDuration(dataId, DEFAULT_DURATION, DEFAULT_CONFIG_TIMEOUT);
    }


    /**
     * Gets boolean config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the boolean config
     */
    default boolean getBoolean(String dataId, boolean defaultValue, long timeoutMills) {
        return getConfig(dataId, defaultValue, timeoutMills, Boolean.class);
    }

    default boolean getBoolean(String dataId, boolean defaultValue) {
        return getBoolean(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT);
    }

    default boolean getBoolean(String dataId) {
        Boolean config = getConfig(dataId, null, DEFAULT_CONFIG_TIMEOUT, Boolean.class);
        return config == null ? DEFAULT_BOOLEAN : config;
    }


    /**
     * Gets List config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the duration config
     */
    default <T> List<T> getList(String dataId, List<T> defaultValue, long timeoutMills, Class<T> dataType) {
        List<?> configList = getConfig(dataId, defaultValue, timeoutMills, List.class);

        List<T> resultList = new ArrayList<>();

        if (configList != null) {
            for (Object config : configList) {
                // add the converted value
                resultList.add(ConvertUtils.convert(config, dataType));
            }
        }

        return resultList;
    }

    default <T> List<T> getList(String dataId, List<T> defaultValue, Class<T> dataType) {
        return getList(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT, dataType);
    }

    default <T> List<T> getList(String dataId, Class<T> dataType) {
        return getList(dataId, null, DEFAULT_CONFIG_TIMEOUT, dataType);
    }

    default List<String> getList(String dataId, List<String> defaultValue, long timeoutMills) {
        return getList(dataId, defaultValue, timeoutMills, String.class);
    }

    default List<String> getList(String dataId, List<String> defaultValue) {
        return getList(dataId, defaultValue, DEFAULT_CONFIG_TIMEOUT, String.class);
    }

    default List<String> getList(String dataId) {
        return getList(dataId, null, DEFAULT_CONFIG_TIMEOUT, String.class);
    }

}
