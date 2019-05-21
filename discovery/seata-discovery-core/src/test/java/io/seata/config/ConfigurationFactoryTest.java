package io.seata.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
class ConfigurationFactoryTest {

    @Test
    void getInstance() {
        Configuration configuration = ConfigurationFactory.getInstance();
        // check singleton
        Assertions.assertEquals(configuration, ConfigurationFactory.getInstance());
    }
}