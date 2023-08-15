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
package io.seata.spring.boot.autoconfigure.properties.config.test;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import io.seata.config.FileConfiguration;
import io.seata.spring.boot.autoconfigure.BasePropertiesTest;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.provider.SpringApplicationContextProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author slievrly
 */
@org.springframework.context.annotation.Configuration
@Import(SpringApplicationContextProvider.class)
public class ZooKeeperPropertiesTest extends BasePropertiesTest {
    @Bean("testConfigZooKeeperProperties")
    public ConfigZooKeeperProperties configZooKeeperProperties() {
        return new ConfigZooKeeperProperties().setNodePath(STR_TEST_AAA).setServerAddr(STR_TEST_BBB).setUsername(STR_TEST_CCC).setPassword(STR_TEST_DDD).setConnectTimeout(LONG_TEST_ONE).setSessionTimeout(LONG_TEST_TWO);
    }

    @Test
    public void testConfigZooKeeperProperties() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);

        assertEquals(STR_TEST_AAA, currentConfiguration.getConfig("config.zk.nodePath"));
        assertEquals(STR_TEST_BBB, currentConfiguration.getConfig("config.zk.serverAddr"));
        assertEquals(STR_TEST_CCC, currentConfiguration.getConfig("config.zk.username"));
        assertEquals(STR_TEST_DDD, currentConfiguration.getConfig("config.zk.password"));
        assertEquals(LONG_TEST_ONE, currentConfiguration.getInt("config.zk.connectTimeout"));
        assertEquals(LONG_TEST_TWO, currentConfiguration.getInt("config.zk.sessionTimeout"));
    }

}
