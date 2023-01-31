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
package io.seata.config.source;

import static io.seata.config.Configuration.DEFAULT_CONFIG_TIMEOUT;

/**
 * The interface UpdatableConfigurationSource.
 *
 * @author wang.liang
 */
public interface UpdatableConfigurationSource {

    /**
     * Put config.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean putConfig(String dataId, String content, long timeoutMills);

    default boolean putConfig(String dataId, String content) {
        return putConfig(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }


    /**
     * Put config if absent.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean putConfigIfAbsent(String dataId, String content, long timeoutMills);

    default boolean putConfigIfAbsent(String dataId, String content) {
        return putConfigIfAbsent(dataId, content, DEFAULT_CONFIG_TIMEOUT);
    }


    /**
     * Remove config.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the boolean
     */
    boolean removeConfig(String dataId, long timeoutMills);

    default boolean removeConfig(String dataId) {
        return removeConfig(dataId, DEFAULT_CONFIG_TIMEOUT);
    }

}
