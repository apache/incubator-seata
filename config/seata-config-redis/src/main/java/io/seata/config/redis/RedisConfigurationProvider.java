package io.seata.config.redis;

import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationProvider;

/**
 * @author wangyuewen
 */
@LoadLevel(name = "Redis", order = 1)
public class RedisConfigurationProvider implements ConfigurationProvider {
    /**
     * provide a AbstractConfiguration implementation instance
     *
     * @return Configuration
     */
    @Override
    public Configuration provide() { return RedisConfiguration.getInstance(); }
}
