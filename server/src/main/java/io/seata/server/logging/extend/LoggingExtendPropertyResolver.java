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
 * @date 2022/6/10 8:18 下午
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
