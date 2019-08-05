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
import java.util.List;

/**
 * The interface Configuration.
 *
 * @param <T> the type parameter
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /12/20
 */
public interface Configuration<T> {

    /**
     * Gets short.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the short
     */
    short getShort(String dataId, int defaultValue, long timeoutMills);

    /**
     * Gets short.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the int
     */
    short getShort(String dataId, short defaultValue);

    /**
     * Gets short.
     *
     * @param dataId the data id
     * @return the int
     */
    short getShort(String dataId);

    /**
     * Gets int.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the int
     */
    int getInt(String dataId, int defaultValue, long timeoutMills);

    /**
     * Gets int.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the int
     */
    int getInt(String dataId, int defaultValue);

    /**
     * Gets int.
     *
     * @param dataId the data id
     * @return the int
     */
    int getInt(String dataId);

    /**
     * Gets long.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the long
     */
    long getLong(String dataId, long defaultValue, long timeoutMills);

    /**
     * Gets long.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the long
     */
    long getLong(String dataId, long defaultValue);

    /**
     * Gets long.
     *
     * @param dataId the data id
     * @return the long
     */
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
     * @return he duration
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
     * Gets config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the config
     */
    String getConfig(String dataId, String defaultValue, long timeoutMills);

    /**
     * Gets config.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @return the config
     */
    String getConfig(String dataId, String defaultValue);

    /**
     * Gets config.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the config
     */
    String getConfig(String dataId, long timeoutMills);

    /**
     * Gets config.
     *
     * @param dataId the data id
     * @return the config
     */
    String getConfig(String dataId);

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
     * Put config boolean.
     *
     * @param dataId  the data id
     * @param content the content
     * @return the boolean
     */
    boolean putConfig(String dataId, String content);

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
     * Put config if absent boolean.
     *
     * @param dataId  the data id
     * @param content the content
     * @return the boolean
     */
    boolean putConfigIfAbsent(String dataId, String content);

    /**
     * Remove config boolean.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean removeConfig(String dataId, long timeoutMills);

    /**
     * Remove config boolean.
     *
     * @param dataId the data id
     * @return the boolean
     */
    boolean removeConfig(String dataId);

    /**
     * Add config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    void addConfigListener(String dataId, T listener);

    /**
     * Remove config listener.
     *
     * @param dataId   the data id
     * @param listener the listener
     */
    void removeConfigListener(String dataId, T listener);

    /**
     * Gets config listeners.
     *
     * @param dataId the data id
     * @return the config listeners
     */
    List<T> getConfigListeners(String dataId);

    /**
     * Gets config from sys pro.
     *
     * @param dataId the data id
     * @return the config from sys pro
     */
    default String getConfigFromSysPro(String dataId) {
        return System.getProperty(dataId);
    }

}
