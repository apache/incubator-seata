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

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.nacos.NacosConfigurationSource;
import io.seata.spring.boot.autoconfigure.BasePropertiesTest;
import io.seata.spring.boot.autoconfigure.provider.SpringApplicationContextProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author slievrly
 */
@org.springframework.context.annotation.Configuration
@Import(SpringApplicationContextProvider.class)
public class NacosPropertiesTest extends BasePropertiesTest {

    @Test
    public void testConfigNacosProperties() {
        Configuration currentConfiguration = ConfigurationFactory.getInstance();

        assertEquals(STR_TEST_AAA, currentConfiguration.getString(NacosConfigurationSource.getNacosAddrFileKey()));
        assertEquals(STR_TEST_BBB, currentConfiguration.getString(NacosConfigurationSource.getNacosDataIdKey()));
        assertEquals(STR_TEST_CCC, currentConfiguration.getString(NacosConfigurationSource.getNacosGroupKey()));
        assertEquals(STR_TEST_DDD, currentConfiguration.getString(NacosConfigurationSource.getNacosNameSpaceFileKey()));
        assertEquals(STR_TEST_EEE, currentConfiguration.getString(NacosConfigurationSource.getNacosUserName()));
        assertEquals(STR_TEST_FFF, currentConfiguration.getString(NacosConfigurationSource.getNacosPassword()));
    }
}
