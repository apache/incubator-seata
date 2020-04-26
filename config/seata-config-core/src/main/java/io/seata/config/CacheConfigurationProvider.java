package io.seata.config;

/**
 * @author funkye
 */
public interface CacheConfigurationProvider {
    /**
     * provide a AbstractConfiguration implementation instance
     * @param originalConfiguration
     * @return configuration
     */
    Configuration provide(Configuration originalConfiguration);
}
