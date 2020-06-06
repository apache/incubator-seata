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

import java.util.HashSet;
import java.util.Set;
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

    private static final Set<String> LISTENER_KEYS = new HashSet<>();

    public static void addConfigListener(String dataId, ConfigurationChangeListener... listeners) {
        synchronized (ConfigurationCache.class) {
            if (StringUtils.isBlank(dataId) || LISTENER_KEYS.contains(dataId)) {
                return;
            }
            ConfigurationFactory.getInstance().addConfigListener(dataId, getInstance());
            LISTENER_KEYS.add(dataId);
        }
        for (int i = 0; i < listeners.length; i++) {
            ConfigurationFactory.getInstance().addConfigListener(dataId, listeners[i]);
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
                    if (method.getReturnType().equals(String.class)) {
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
