/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure.properties.config;

import org.apache.seata.spring.boot.autoconfigure.BasePropertiesTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigApolloPropertiesTest extends BasePropertiesTest {

    @Test
    public void testConfigApolloProperties() {
        ConfigApolloProperties configApolloProperties = new ConfigApolloProperties();
        configApolloProperties.setApolloMeta(STR_TEST_AAA);
        Assertions.assertEquals(configApolloProperties.getApolloMeta(), STR_TEST_AAA);
        configApolloProperties.setApolloAccessKeySecret(STR_TEST_BBB);
        Assertions.assertEquals(configApolloProperties.getApolloAccessKeySecret(), STR_TEST_BBB);
        configApolloProperties.setAppId(STR_TEST_CCC);
        Assertions.assertEquals(configApolloProperties.getAppId(), STR_TEST_CCC);
        configApolloProperties.setNamespace(STR_TEST_DDD);
        Assertions.assertEquals(configApolloProperties.getNamespace(), STR_TEST_DDD);
        configApolloProperties.setCluster(STR_TEST_EEE);
        Assertions.assertEquals(configApolloProperties.getCluster(), STR_TEST_EEE);
        configApolloProperties.setApolloConfigService(STR_TEST_FFF);
        Assertions.assertEquals(configApolloProperties.getApolloConfigService(), STR_TEST_FFF);
    }
}
