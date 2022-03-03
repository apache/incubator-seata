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
import java.util.Optional;
import java.util.stream.Stream;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.holder.ObjectHolder;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;

/**
 * @author xingfudeshi@gmail.com
 * @author funkye
 */
public class SpringBootConfigurationProvider implements ExtConfigurationProvider {
    private static final String INTERCEPT_METHOD_PREFIX = "get";

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration)Enhancer.create(originalConfiguration.getClass(),
            (MethodInterceptor)(proxy, method, args, methodProxy) -> {
                if (method.getName().startsWith(INTERCEPT_METHOD_PREFIX) && args.length > 0) {
                    Object result;
                    String rawDataId = (String)args[0];
                    result = originalConfiguration.getConfigFromSys(rawDataId);
                    if (null == result) {
                        if (args.length == 1) {
                            result = get(convertDataId(rawDataId));
                        } else {
                            result = get(convertDataId(rawDataId), args[1]);
                        }
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

    private Object get(String dataId, Object defaultValue) throws IllegalAccessException, InstantiationException {
        Object result = get(dataId);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    private Object get(String dataId) throws IllegalAccessException, InstantiationException {
        String propertyPrefix = getPropertyPrefix(dataId);
        String propertySuffix = getPropertySuffix(dataId);
        Class<?> propertyClass = PROPERTY_BEAN_MAP.get(propertyPrefix);
        Object valueObject = null;
        if (propertyClass != null) {
            try {
                valueObject = getFieldValue(propertyClass.newInstance(), propertySuffix, dataId);
            } catch (NoSuchBeanDefinitionException ignore) {

            }
        } else {
            throw new ShouldNeverHappenException("PropertyClass for prefix: [" + propertyPrefix + "] should not be null.");
        }

        return valueObject;
    }

    /**
     * get field value
     *
     * @param object
     * @param fieldName
     * @param dataId
     * @return java.lang.Object
     * @author xingfudeshi@gmail.com
     */
    private Object getFieldValue(Object object, String fieldName, String dataId) throws IllegalAccessException {
        ConfigurableEnvironment environment =
            (ConfigurableEnvironment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Optional<Field> fieldOptional = Stream.of(object.getClass().getDeclaredFields())
            .filter(f -> f.getName().equalsIgnoreCase(fieldName)).findAny();
        if (fieldOptional.isPresent()) {
            Field field = fieldOptional.get();
            return environment.getProperty(dataId, field.getType());
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
            String suffix = StringUtils.removeEnd(rawDataId, DOT + SPECIAL_KEY_GROUPLIST);
            //change the format of default.grouplist to grouplist.default
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
}
