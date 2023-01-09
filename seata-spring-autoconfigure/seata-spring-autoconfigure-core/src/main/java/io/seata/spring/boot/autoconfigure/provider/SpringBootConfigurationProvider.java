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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.ReflectionUtil;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
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

    private static final Map<String, Object> PROPERTY_BEAN_INSTANCE_MAP = new HashMap<>(64);

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration)Enhancer.create(originalConfiguration.getClass(),
            (MethodInterceptor)(proxy, method, args, methodProxy) -> {
                if (method.getName().startsWith(INTERCEPT_METHOD_PREFIX) && args.length > 0) {
                    Object result;
                    String rawDataId = (String)args[0];
                    result = originalConfiguration.getConfigFromSys(rawDataId);
                    if (null == result) {
                        String dataId = convertDataId(rawDataId);
                        Class<?> dataType = ReflectionUtil.getWrappedClass(method.getReturnType());
                        Object defaultValue = null;

                        // Get defaultValue from the arguments
                        if (args.length > 1) {
                            defaultValue = args[1];

                            // See: Configuration.getConfig(String dataId, long timeoutMills);
                            if (defaultValue != null && !dataType.isAssignableFrom(defaultValue.getClass())) {
                                defaultValue = null;
                            }
                        }

                        result = get(dataId, dataType, defaultValue);
                    }
                    if (result != null) {
                        // If the return type is String,need to convert the object to string
                        if (method.getReturnType().equals(String.class)) {
                            return String.valueOf(result);
                        }
                        return result;
                    }
                }

                return method.invoke(originalConfiguration, args);
            });
    }

    private Object get(String dataId, Class<?> dataType, Object defaultValue) throws IllegalAccessException {
        // If defaultValue is null, get from the property object.
        if (defaultValue == null) {
            String propertyPrefix = getPropertyPrefix(dataId);
            String propertySuffix = getPropertySuffix(dataId);

            // Get the property class
            Class<?> propertyClass = PROPERTY_BEAN_MAP.get(propertyPrefix);
            if (propertyClass == null) {
                throw new ShouldNeverHappenException("PropertyClass for prefix: [" + propertyPrefix + "] should not be null.");
            }

            // Instantiate the property
            Object propertyObj = PROPERTY_BEAN_INSTANCE_MAP.computeIfAbsent(propertyPrefix, k -> {
                try {
                    return propertyClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.warn("PropertyClass for prefix: [" + propertyPrefix + "] should not be null. error :" + e.getMessage(), e);
                }
                return null;
            });
            Objects.requireNonNull(propertyObj, "Instantiate the property fail");

            // Get defaultValue from the property object
            defaultValue = getDefaultValueFromPropertyObject(propertyObj, propertySuffix);
        }

        // Get config
        return getConfig(dataId, dataType, defaultValue);
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
    private Object getDefaultValueFromPropertyObject(Object propertyObj, String fieldName) throws IllegalAccessException {
        Optional<Field> fieldOptional = Stream.of(propertyObj.getClass().getDeclaredFields())
            .filter(f -> f.getName().equalsIgnoreCase(fieldName)).findAny();

        // Get defaultValue from the field
        if (fieldOptional.isPresent()) {
            Field field = fieldOptional.get();
            if (!Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field.get(propertyObj);
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
     * @param dataId       data id
     * @param dataType     data type
     * @param defaultValue default value
     * @return object
     */
    private Object getConfig(String dataId, Class<?> dataType, Object defaultValue) {
        ConfigurableEnvironment environment =
            (ConfigurableEnvironment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Object value = environment.getProperty(dataId, dataType);
        if (value == null) {
            value = environment.getProperty(io.seata.common.util.StringUtils.hump2Line(dataId), dataType);
        }
        return value != null ? value : defaultValue;
    }

}
