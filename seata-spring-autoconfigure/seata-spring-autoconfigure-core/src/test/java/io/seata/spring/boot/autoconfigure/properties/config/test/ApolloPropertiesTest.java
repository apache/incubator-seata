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
import io.seata.config.apollo.ApolloConfiguration;
import io.seata.spring.boot.autoconfigure.BasePropertiesTest;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigApolloProperties;
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
class ApolloPropertiesTest extends BasePropertiesTest {

    @Bean("testConfigApolloProperties")
    public ConfigApolloProperties configApolloProperties() {
        return new ConfigApolloProperties().setApolloMeta(STR_TEST_AAA).setApolloAccessKeySecret(STR_TEST_BBB).setAppId(
            STR_TEST_CCC).setNamespace(STR_TEST_DDD).setCluster(STR_TEST_EEE).setApolloConfigService(STR_TEST_FFF);
    }

    @Test
    public void testConfigApolloProperties() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(
            configuration);

        assertEquals(STR_TEST_AAA, currentConfiguration.getConfig(ApolloConfiguration.getApolloMetaFileKey()));
        assertEquals(STR_TEST_BBB, currentConfiguration.getConfig(ApolloConfiguration.getApolloSecretFileKey()));
        assertEquals(STR_TEST_CCC, currentConfiguration.getConfig(ApolloConfiguration.getApolloAppIdFileKey()));
        assertEquals(STR_TEST_DDD, currentConfiguration.getConfig(ApolloConfiguration.getApolloNamespaceKey()));
        assertEquals(STR_TEST_EEE, currentConfiguration.getConfig(ApolloConfiguration.getApolloCluster()));
        assertEquals(STR_TEST_FFF, currentConfiguration.getConfig(ApolloConfiguration.getApolloConfigService()));
    }
}