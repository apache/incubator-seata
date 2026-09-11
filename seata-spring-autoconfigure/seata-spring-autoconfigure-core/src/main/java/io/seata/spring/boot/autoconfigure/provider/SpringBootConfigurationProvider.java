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
package io.seata.spring.boot.autoconfigure.provider;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_SERVICE;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;

/**
 * @author xingfudeshi@gmail.com
 * @author funkye
 */
public class SpringBootConfigurationProvider implements ExtConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootConfigurationProvider.class);

    private static final String INTERCEPT_METHOD_PREFIX = "get";

    private static final Map<String, Object> PROPERTY_BEAN_INSTANCE_MAP = new ConcurrentHashMap<>(64);

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Configuration.class}
            , (proxy, method, args) -> {
                if (method.getName().startsWith(INTERCEPT_METHOD_PREFIX) && args.length > 0) {
                    Object result;
                    String rawDataId = (String)args[0];
                    Class<?> dataType = ReflectionUtil.getWrappedClass(method.getReturnType());

                    // 1. Get config value from the system property
                    result = originalConfiguration.getConfigFromSys(rawDataId);

                    if (result == null) {
                        String dataId = convertDataId(rawDataId);

                        // 2. Get config value from the springboot environment
                        result = getConfigFromEnvironment(dataId, dataType);
                        if (result != null) {
                            return result;
                        }

                        // 3. Get config defaultValue from the arguments
                        if (args.length > 1) {
                            result = args[1];

                            if (result != null) {
                                // See Configuration#getConfig(String dataId, long timeoutMills)
                                if (dataType.isAssignableFrom(result.getClass())) {
                                    return result;
                                } else {
                                    result = null;
                                }
                            }
                        }

                        // 4. Get config defaultValue from the property object
                        try {
                            result = getDefaultValueFromPropertyObject(dataId);
                        } catch (Throwable t) {
                            LOGGER.error("Get config '{}' default value from the property object failed:", dataId, t);
                        }
                    }

                    if (result != null) {
                        if (dataType.isAssignableFrom(result.getClass())) {
                            return result;
                        }

                        // Convert type
                        return this.convertType(result, dataType);
                    }
                }

                return method.invoke(originalConfiguration, args);
            });
    }

    private Object getDefaultValueFromPropertyObject(String dataId) throws IllegalAccessException, InvocationTargetException {
        String propertyPrefix = getPropertyPrefix(dataId);
        String propertySuffix = getPropertySuffix(dataId);

        // Get the property class
        final Class<?> propertyClass = PROPERTY_BEAN_MAP.get(propertyPrefix);
        if (propertyClass == null) {
            throw new ShouldNeverHappenException("PropertyClass for prefix: [" + propertyPrefix + "] should not be null.");
        }

        // Instantiate the property object
        Object propertyObj = CollectionUtils.computeIfAbsent(PROPERTY_BEAN_INSTANCE_MAP, propertyPrefix, k -> {
            try {
                return propertyClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn("PropertyClass for prefix: [" + propertyPrefix + "] should not be null. error :" + e.getMessage(), e);
            }
            return null;
        });
        Objects.requireNonNull(propertyObj, () -> "Instantiate the property object fail: " + propertyClass.getName());

        // Get defaultValue from the property object
        return getDefaultValueFromPropertyObject(propertyObj, propertySuffix);
    }

    /**
     * Get defaultValue from the property object
     *
     * @param propertyObj the property object
     * @param fieldName   the field name
     * @return defaultValue
     * @author xingfudeshi@gmail.com
     */
    @Nullable
    private Object getDefaultValueFromPropertyObject(Object propertyObj, String fieldName) throws IllegalAccessException, InvocationTargetException {
        try {
            Field field = propertyObj.getClass().getDeclaredField(fieldName);

            if (!Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field.get(propertyObj);
            }
        } catch (NoSuchFieldException e) {
            Method method = null;
            try {
                method = propertyObj.getClass().getMethod("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            } catch (NoSuchMethodException ex) {
                try {
                    method = propertyObj.getClass().getMethod("is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
                } catch (NoSuchMethodException exc) {
                    LOGGER.warn("The get method not found for the field '{}#{}'.", propertyObj.getClass().getSimpleName(), fieldName);
                }
            }
            if (method != null) {
                if (!Map.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return method.invoke(propertyObj);
                }
            }
        }

        return null;
    }

    /**
     * convert data id
     *
     * @param rawDataId
     * @return dataId
     */
    private String convertDataId(String rawDataId) {
        if (rawDataId.endsWith(SPECIAL_KEY_GROUPLIST)) {
            String suffix = StringUtils.removeStart(StringUtils.removeEnd(rawDataId, DOT + SPECIAL_KEY_GROUPLIST),
                SPECIAL_KEY_SERVICE + DOT);
            // change the format of default.grouplist to grouplist.default
            return SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST + DOT + suffix;
        }
        return SEATA_PREFIX + DOT + rawDataId;
    }

    /**
     * Get property prefix
     *
     * @param dataId
     * @return propertyPrefix
     */
    private String getPropertyPrefix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SERVICE_PREFIX;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SERVICE_PREFIX;
        }
        return StringUtils.substringBeforeLast(dataId, String.valueOf(DOT));
    }

    /**
     * Get property suffix
     *
     * @param dataId
     * @return propertySuffix
     */
    private String getPropertySuffix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SPECIAL_KEY_VGROUP_MAPPING;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SPECIAL_KEY_GROUPLIST;
        }
        return StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
    }

    /**
     * get spring config
     *
     * @param dataId   data id
     * @param dataType data type
     * @return object
     */
    @Nullable
    private Object getConfigFromEnvironment(String dataId, Class<?> dataType) {
        ConfigurableEnvironment environment = (ConfigurableEnvironment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Object value = environment.getProperty(dataId, dataType);
        if (value == null) {
            value = environment.getProperty(io.seata.common.util.StringUtils.hump2Line(dataId), dataType);
        }
        if (value == null) {
            String grouplistPrefix = SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST + DOT;
            if (dataId.startsWith(grouplistPrefix)) {
                String vgroup = StringUtils.removeStart(dataId, grouplistPrefix);
                String oldGrouplistDataId = SERVICE_PREFIX + DOT + vgroup + DOT + SPECIAL_KEY_GROUPLIST;
                return getConfigFromEnvironment(oldGrouplistDataId, dataType);
            }
        }
        return value;
    }

    private Object convertType(Object configValue, Class<?> dataType) {
        if (String.class.equals(dataType)) {
            return String.valueOf(configValue);
        }
        if (Long.class.equals(dataType)) {
            return Long.parseLong(String.valueOf(configValue));
        }
        if (Integer.class.equals(dataType)) {
            return Integer.parseInt(String.valueOf(configValue));
        }
        if (Short.class.equals(dataType)) {
            return Short.parseShort(String.valueOf(configValue));
        }
        if (Boolean.class.equals(dataType)) {
            return Boolean.parseBoolean(String.valueOf(configValue));
        }
        if (Duration.class.equals(dataType)) {
            return Duration.parse(String.valueOf(configValue));
        }
        return configValue;
    }

}
