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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.DurationUtil;
import io.seata.common.util.StringUtils;

/**
 * @author funkye
 */
public class ConfigurationCache implements ConfigurationChangeListener {

    private static final String METHOD_PREFIX = "get";

    private static final String METHOD_LATEST_CONFIG = METHOD_PREFIX + "LatestConfig";

    private static final Map<String, ObjectWrapper> CONFIG_CACHE = new ConcurrentHashMap<>();

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

    public static void removeConfigListener(String dataId, ConfigurationChangeListener... listeners) {
        if (StringUtils.isBlank(dataId)) {
            return;
        }
        synchronized (ConfigurationCache.class) {
            final HashSet<ConfigurationChangeListener> listenerSet = getInstance().configListenersMap.get(dataId);
            if (CollectionUtils.isNotEmpty(listenerSet)) {
                for (ConfigurationChangeListener listener : listeners) {
                    if (listenerSet.remove(listener)) {
                        ConfigurationFactory.getInstance().removeConfigListener(dataId, listener);
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
        ObjectWrapper oldWrapper = CONFIG_CACHE.get(event.getDataId());
        // The wrapper.data only exists in the cache when it is not null.
        if (StringUtils.isNotBlank(event.getNewValue())) {
            if (oldWrapper == null) {
                CONFIG_CACHE.put(event.getDataId(), new ObjectWrapper(event.getNewValue(), null));
            } else {
                Object newValue = new ObjectWrapper(event.getNewValue(), null).convertData(oldWrapper.getType());
                if (!Objects.equals(oldWrapper.getData(), newValue)) {
                    CONFIG_CACHE.put(event.getDataId(), new ObjectWrapper(newValue, oldWrapper.getType(),oldWrapper.getLastDefaultValue()));
                }
            }
        } else {
            CONFIG_CACHE.remove(event.getDataId());
        }
    }

    public Configuration proxy(Configuration originalConfiguration) throws Exception {
        return (Configuration)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Configuration.class}
            , (proxy, method, args) -> {
                String methodName = method.getName();
                if (methodName.startsWith(METHOD_PREFIX) && !methodName.equalsIgnoreCase(METHOD_LATEST_CONFIG)) {
                    String rawDataId = (String)args[0];
                    ObjectWrapper wrapper = CONFIG_CACHE.get(rawDataId);
                    ObjectWrapper.ConfigType type =
                        ObjectWrapper.getTypeByName(methodName.substring(METHOD_PREFIX.length()));
                    Object defaultValue = null;
                    if (args.length > 1
                            && method.getParameterTypes()[1].getSimpleName().equalsIgnoreCase(type.name())) {
                        defaultValue = args[1];
                    }
                    if (null == wrapper
                            || (null != defaultValue && !Objects.equals(defaultValue, wrapper.lastDefaultValue))) {
                        Object result = method.invoke(originalConfiguration, args);
                        // The wrapper.data only exists in the cache when it is not null.
                        if (result != null) {
                            wrapper = new ObjectWrapper(result, type, defaultValue);
                            CONFIG_CACHE.put(rawDataId, wrapper);
                        }
                    }
                    return wrapper == null ? null : wrapper.convertData(type);
                }
                return method.invoke(originalConfiguration, args);
            }
        );
    }

    private static class ConfigurationCacheInstance {
        private static final ConfigurationCache INSTANCE = new ConfigurationCache();
    }

    public static void clear() {
        CONFIG_CACHE.clear();
    }

    private static class ObjectWrapper {
        private final Object data;
        private final ConfigType type;
        private final Object lastDefaultValue;

        ObjectWrapper(Object data, ConfigType type) {
            this(data, type, null);
        }

        ObjectWrapper(Object data, ConfigType type, Object lastDefaultValue) {
            this.data = data;
            this.type = type;
            this.lastDefaultValue = lastDefaultValue;
        }

        public Object getData() {
            return data;
        }

        public ConfigType getType() {
            return type;
        }

        public Object getLastDefaultValue() {
            return lastDefaultValue;
        }

        public Object convertData(ConfigType aType) {
            if (data != null && Objects.equals(type, aType)) {
                return data;
            }
            if (data != null) {
                if (ConfigType.INT.equals(aType)) {
                    return Integer.parseInt(data.toString());
                } else if (ConfigType.BOOLEAN.equals(aType)) {
                    return Boolean.parseBoolean(data.toString());
                } else if (ConfigType.DURATION.equals(aType)) {
                    return DurationUtil.parse(data.toString());
                } else if (ConfigType.LONG.equals(aType)) {
                    return Long.parseLong(data.toString());
                } else if (ConfigType.SHORT.equals(aType)) {
                    return Short.parseShort(data.toString());
                }
                return String.valueOf(data);
            }
            return null;
        }

        public static boolean supportType(String type) {
            return getTypeByName(type) != null;
        }

        public static ConfigType getTypeByName(String postfix) {
            return ConfigType.fromCode(postfix);
        }

        /**
         * Config Cache Operation type
         */
        enum ConfigType {

            /**
             * getInt
             */
            INT("Int"),

            /**
             * getBoolean
             */
            BOOLEAN("Boolean"),

            /**
             * getDuration
             */
            DURATION("Duration"),

            /**
             * getLong
             */
            LONG("Long"),

            /**
             * getShort
             */
            SHORT("Short"),

            /**
             * getConfig
             */
            STRING("Config");

            private static final Map<String, ConfigType> CODE_TO_VALUE = new HashMap<>();

            static {
                for (ConfigType configType : ConfigType.values()) {
                    CODE_TO_VALUE.put(configType.code.toUpperCase(), configType);
                }
            }

            private String code;

            ConfigType(String code) {
                this.code = code;
            }

            public String getCode() {
                return code;
            }

            public static ConfigType fromCode(String code) {
                ConfigType configType = CODE_TO_VALUE.get(code.toUpperCase());
                return configType == null ? ConfigType.STRING : configType;
            }

            public static ConfigType fromName(String name) {
                return ConfigType.valueOf(name);
            }
        }
    }
}
