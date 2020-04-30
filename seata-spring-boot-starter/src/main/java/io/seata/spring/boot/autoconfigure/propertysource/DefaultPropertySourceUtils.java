package io.seata.spring.boot.autoconfigure.propertysource;

import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * @author wangliang <841369634@qq.com>
 */
public class DefaultPropertySourceUtils {

    public static final String DEFAULT_PROPERTIES = "defaultProperties";

    /**
     * get the default property source.
     *
     * @param env        environment
     * @param autoCreate auto create if null
     * @return the default property source
     */
    public static MapPropertySource getDefaultPropertySource(ConfigurableEnvironment env, boolean autoCreate) {
        MutablePropertySources propertySources = env.getPropertySources();

        PropertySource propertySource = propertySources.get(DEFAULT_PROPERTIES);
        if (propertySource != null && !(propertySource instanceof MapPropertySource)) {
            propertySource = propertySources.get(DEFAULT_PROPERTIES + "2");
        }

        if (propertySource != null && propertySource instanceof MapPropertySource) {
            return (MapPropertySource) propertySource;
        } else if (!autoCreate) {
            return null;
        } else {
            MapPropertySource newPropertySource;
            if (propertySource != null) {
                newPropertySource = new MapPropertySource(DEFAULT_PROPERTIES + "2", new HashMap<>());
                propertySources.addBefore(DEFAULT_PROPERTIES, newPropertySource);
            } else {
                newPropertySource = new MapPropertySource(DEFAULT_PROPERTIES, new HashMap<>());
                propertySources.addLast(newPropertySource);
            }
            return newPropertySource;
        }
    }

    /**
     * put more default properties.
     *
     * @param env        environment
     * @param properties default
     */
    public static void putDefaultProperties(ConfigurableEnvironment env, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        MapPropertySource propertySource = getDefaultPropertySource(env, true);
        Object source = propertySource.getSource();
        ((Map) source).putAll(properties);
    }
}
