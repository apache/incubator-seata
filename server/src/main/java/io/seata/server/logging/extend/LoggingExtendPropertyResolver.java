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
package io.seata.server.logging.extend;

import com.google.common.collect.Maps;
import io.seata.common.util.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * @author wlx
 */
public class LoggingExtendPropertyResolver implements PropertyResolver {

    private final ConfigurableEnvironment environment;

    public LoggingExtendPropertyResolver(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public boolean containsProperty(String key) {
        return environment.containsProperty(key);
    }

    @Override
    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return environment.getProperty(key, targetType);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        return environment.getRequiredProperty(key);
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        return environment.getRequiredProperty(key, targetType);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return environment.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return environment.resolveRequiredPlaceholders(text);
    }

    /**
     * get property map by prefix.
     *
     * @param prefix prefix
     */
    @SuppressWarnings("rawtypes")
    public Map<String, Object> getPropertyMapByPrefix(String prefix) {
        Map<String, Object> res = Maps.newHashMap();
        if (StringUtils.isNullOrEmpty(prefix)) {
            return res;
        }
        MutablePropertySources propertySources = environment.getPropertySources();
        StreamSupport.stream(propertySources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream).
                forEach(propName -> {
                    if (propName.startsWith(prefix)) {
                        res.put(propName.substring(prefix.length() + 1), environment.getProperty(propName));
                    }
                });
        return res;
    }

}
