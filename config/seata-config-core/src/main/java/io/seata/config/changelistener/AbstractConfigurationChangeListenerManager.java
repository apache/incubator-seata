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
package io.seata.config.changelistener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;

/**
 * The type AbstractConfigurationChangeListenerManager.
 *
 * @author wang.liang
 */
public class AbstractConfigurationChangeListenerManager implements ConfigurationChangeListenerManager {

    private final Map<String, Set<ConfigurationChangeListener>> listenersMap = new ConcurrentHashMap<>(8);


    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        CollectionUtils.computeIfAbsent(this.listenersMap, dataId, key -> ConcurrentHashMap.newKeySet())
                .add(listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        if (this.listenersMap.containsKey(dataId)) {
            Set<ConfigurationChangeListener> listeners = this.listenersMap.get(dataId);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    this.listenersMap.remove(dataId);
                }
            }
        }
    }

    @Override
    public Set<String> getListenedConfigDataIds() {
        return listenersMap.keySet();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return listenersMap.get(dataId);
    }
}
