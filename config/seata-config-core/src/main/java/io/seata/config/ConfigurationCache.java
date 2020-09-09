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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.seata.common.util.StringUtils;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * @author funkye
 */
public class ConfigurationCache implements ConfigurationChangeListener {

    private static final String METHOD_PREFIX = "get";

    private static final String METHOD_LATEST_CONFIG = METHOD_PREFIX + "LatestConfig";

    private static final ConcurrentHashMap<String, Object> CONFIG_CACHE = new ConcurrentHashMap<>();

    private Map<String, HashSet<ConfigurationChangeListener>> configListenersMap =
        new HashMap<>();

    public static void addConfigListener(String dataId, ConfigurationChangeListener... listeners) {
        if (StringUtils.isBlank(dataId)) {
            return;
        }
        synchronized (ConfigurationCache.class) {
            HashSet<ConfigurationChangeListener> listenerHashSet =
                getInstance().configListenersMap.computeIfAbsent(dataId, k -> new HashSet<>());
            if (!listenerHashSet.contains(getInstance())) {
                ConfigurationFactory.getInstance().addConfigListener(dataId, getInstance());
                listenerHashSet.add(getInstance());
            }
            if (null != listeners && listeners.length > 0) {
                for (ConfigurationChangeListener listener : listeners) {
                    if (!listenerHashSet.contains(listener)) {
                        listenerHashSet.add(listener);
                        ConfigurationFactory.getInstance().addConfigListener(dataId, listener);
                    }
                }
            }
        }
    }

    public static ConfigurationCache getInstance() {
        return ConfigurationCacheInstance.INSTANCE;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        Object oldValue = CONFIG_CACHE.get(event.getDataId());
        if (null == oldValue || !oldValue.equals(event.getNewValue())) {
            if (StringUtils.isNotBlank(event.getNewValue())) {
                CONFIG_CACHE.put(event.getDataId(), event.getNewValue());
            } else {
                CONFIG_CACHE.remove(event.getDataId());
            }
        }
    }

    public Configuration proxy(Configuration originalConfiguration) {
        return (Configuration)Enhancer.create(Configuration.class,
            (MethodInterceptor)(proxy, method, args, methodProxy) -> {
                if (method.getName().startsWith(METHOD_PREFIX)
                    && !method.getName().equalsIgnoreCase(METHOD_LATEST_CONFIG)) {
                    String rawDataId = (String)args[0];
                    Object result = CONFIG_CACHE.get(rawDataId);
                    if (null == result) {
                        result = method.invoke(originalConfiguration, args);
                        if (result != null) {
                            CONFIG_CACHE.put(rawDataId, result);
                        }
                    }
                    if (null != result && method.getReturnType().equals(String.class)) {
                        return String.valueOf(result);
                    }
                    return result;
                }
                return method.invoke(originalConfiguration, args);
            });
    }

    private static class ConfigurationCacheInstance {
        private static final ConfigurationCache INSTANCE = new ConfigurationCache();
    }

}
