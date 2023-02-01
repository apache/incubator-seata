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
package io.seata.config.listener;

import java.util.Set;

/**
 * The interface ConfigListenerManager.
 *
 * @author slievrly
 * @author wang.liang
 */
public interface ConfigListenerManager {

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
     * Gets set of listened config dataId
     *
     * @return the set listened config dataId
     */
    Set<String> getListenedConfigDataIds();

    /**
     * Gets config listeners.
     *
     * @param dataId the data id
     * @return the config listeners
     */
    Set<ConfigurationChangeListener> getConfigListeners(String dataId);

}
