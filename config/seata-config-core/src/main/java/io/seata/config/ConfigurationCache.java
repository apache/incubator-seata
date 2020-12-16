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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.util.DurationUtil;
import io.seata.common.util.StringUtils;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * @author funkye
 */
public class ConfigurationCache implements ConfigurationChangeListener {

    private static final String METHOD_PREFIX = "get";

    private static final String METHOD_LATEST_CONFIG = METHOD_PREFIX + "LatestConfig";

    private static final ConcurrentHashMap<String, ObjectWrapper> CONFIG_CACHE = new ConcurrentHashMap<>();

    private Map<String, HashSet<ConfigurationChangeListener>> configListenersMap = new HashMap<>();

    public static void addConfigListener(String dataId, ConfigurationChangeListener... listeners) {
        if (StringUtils.isBlank(dataId)) {
            return;
        }
        synchronized (ConfigurationCache.class) {
            HashSet<ConfigurationChangeListener> listenerHashSet =
                getInstance().configListenersMap.computeIfAbsent(dataId, key -> new HashSet<>());
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
        ObjectWrapper wrapper = CONFIG_CACHE.get(event.getDataId());
        // The wrapper.data only exists in the cache when it is not null.
        if (StringUtils.isNotBlank(event.getNewValue())) {
            if (wrapper == null) {
                CONFIG_CACHE.put(event.getDataId(), new ObjectWrapper(event.getNewValue(), null));
            } else {
                Object newValue = new ObjectWrapper(event.getNewValue(), null).convertData(wrapper.getType());
                if (!Objects.equals(wrapper.getData(), newValue)) {
                    CONFIG_CACHE.put(event.getDataId(), new ObjectWrapper(newValue, wrapper.getType()));
                }
            }
        } else {
            CONFIG_CACHE.remove(event.getDataId());
        }
    }

    public Configuration proxy(Configuration originalConfiguration) {
        return (Configuration)Enhancer.create(Configuration.class,
            (MethodInterceptor)(proxy, method, args, methodProxy) -> {
                if (method.getName().startsWith(METHOD_PREFIX)
                        && !method.getName().equalsIgnoreCase(METHOD_LATEST_CONFIG)) {
                    String rawDataId = (String)args[0];
                    ObjectWrapper wrapper = CONFIG_CACHE.get(rawDataId);
                    String type = method.getName().substring(METHOD_PREFIX.length());
                    if (!ObjectWrapper.supportType(type)) {
                        type = null;
                    }
                    if (null == wrapper) {
                        Object result = method.invoke(originalConfiguration, args);
                        // The wrapper.data only exists in the cache when it is not null.
                        if (result != null) {
                            wrapper = new ObjectWrapper(result, type);
                            CONFIG_CACHE.put(rawDataId, wrapper);
                        }
                    }
                    return wrapper == null ? null : wrapper.convertData(type);
                }
                return method.invoke(originalConfiguration, args);
            });
    }

    private static class ConfigurationCacheInstance {
        private static final ConfigurationCache INSTANCE = new ConfigurationCache();
    }

    public void clear() {
        CONFIG_CACHE.clear();
    }

    private static class ObjectWrapper {

        static final String INT = "Int";
        static final String BOOLEAN = "Boolean";
        static final String DURATION = "Duration";
        static final String LONG = "Long";
        static final String SHORT = "Short";

        private final Object data;
        private final String type;

        ObjectWrapper(Object data, String type) {
            this.data = data;
            this.type = type;
        }

        public Object getData() {
            return data;
        }

        public String getType() {
            return type;
        }

        public Object convertData(String aType) {
            if (data != null && Objects.equals(type, aType)) {
                return data;
            }
            if (data != null) {
                if (INT.equals(aType)) {
                    return Integer.parseInt(data.toString());
                } else if (BOOLEAN.equals(aType)) {
                    return Boolean.parseBoolean(data.toString());
                } else if (DURATION.equals(aType)) {
                    return DurationUtil.parse(data.toString());
                } else if (LONG.equals(aType)) {
                    return Long.parseLong(data.toString());
                } else if (SHORT.equals(aType)) {
                    return Short.parseShort(data.toString());
                }
                return String.valueOf(data);
            }
            return null;
        }

        public static boolean supportType(String type) {
            return INT.equalsIgnoreCase(type)
                    || BOOLEAN.equalsIgnoreCase(type)
                    || DURATION.equalsIgnoreCase(type)
                    || LONG.equalsIgnoreCase(type)
                    || SHORT.equalsIgnoreCase(type);
        }
    }

}
