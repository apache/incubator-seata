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
package io.seata.spring.boot.autoconfigure.config.defaultvalue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringFormatUtils;
import io.seata.config.defaultconfig.DefaultConfigSource;
import io.seata.spring.boot.autoconfigure.config.source.SpringEnvironmentConfigSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;

/**
 * The type Property object default config source.
 *
 * @author wang.liang
 */
public class PropertyObjectDefaultConfigSource implements DefaultConfigSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyObjectDefaultConfigSource.class);

    private static final String DOT = String.valueOf(StringFormatUtils.DOT);

    private static final Map<String, Object> PROPERTY_BEAN_INSTANCE_MAP = new ConcurrentHashMap<>(64);


    @Override
    public String getLatestConfig(final String rawDataId, long timeoutMills) {
        // Splice the prefix 'seata.'
        String dataId = SpringEnvironmentConfigSource.splicePrefixSeataDot(rawDataId);

        try {
            return getDefaultValueFromPropertyObject(dataId);
        } catch (Throwable t) {
            LOGGER.error("Get config '{}' defaultValue from the property object failed:", dataId, t);
            return null;
        }
    }

    private String getDefaultValueFromPropertyObject(String dataId) throws IllegalAccessException {
        // property name
        String propertyName = getPropertyPrefix(dataId);

        // Get the property class
        final Class<?> propertyClass = PROPERTY_BEAN_MAP.get(propertyName);
        if (propertyClass == null) {
            throw new ShouldNeverHappenException("PropertyClass for prefix: [" + propertyName + "] should not be null.");
        }

        // Instantiate the property object
        Object propertyObj = CollectionUtils.computeIfAbsent(PROPERTY_BEAN_INSTANCE_MAP, propertyName, k -> {
            try {
                return propertyClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn("PropertyClass for prefix: [" + propertyName + "] should not be null. error :" + e.getMessage(), e);
                return null;
            }
        });
        Objects.requireNonNull(propertyObj, () -> "Instantiate the property object fail: " + propertyClass.getName());

        // property field name
        String propertyFieldName = getPropertySuffix(dataId);
        // Get defaultValue from the property object
        return getDefaultValueFromPropertyObject(propertyObj, propertyFieldName);
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
    private String getDefaultValueFromPropertyObject(Object propertyObj, String fieldName) throws IllegalAccessException {
        Optional<Field> fieldOptional = Stream.of(propertyObj.getClass().getDeclaredFields())
                .filter(f -> f.getName().equalsIgnoreCase(fieldName)).findAny();

        // Get defaultValue from the field
        if (fieldOptional.isPresent()) {
            Field field = fieldOptional.get();
            if (!Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object value = field.get(propertyObj);
                return value != null ? String.valueOf(value) : null;
            }
        }

        return null;
    }

    /**
     * Get property prefix
     *
     * @param dataId the dataId
     * @return propertyPrefix
     */
    private String getPropertyPrefix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SERVICE_PREFIX;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SERVICE_PREFIX;
        }
        return StringUtils.substringBeforeLast(dataId, DOT);
    }

    /**
     * Get property suffix
     *
     * @param dataId the dataId
     * @return propertySuffix
     */
    private String getPropertySuffix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SPECIAL_KEY_VGROUP_MAPPING;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SPECIAL_KEY_GROUPLIST;
        }
        return StringUtils.substringAfterLast(dataId, DOT);
    }


    @Nonnull
    @Override
    public String getName() {
        return "default-value-from-property-object";
    }
}
