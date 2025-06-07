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

public class ConfigRaftPropertiesTest extends BasePropertiesTest {

    @Test
    public void testConfigRaftProperties() {
        ConfigRaftProperties configRaftProperties = new ConfigRaftProperties();
        configRaftProperties.setUsername(STR_TEST_AAA);
        configRaftProperties.setPassword(STR_TEST_BBB);
        configRaftProperties.setServerAddr(STR_TEST_CCC);
        configRaftProperties.setMetadataMaxAgeMs((long)LONG_TEST_ONE);
        configRaftProperties.setTokenValidityInMilliseconds((long)LONG_TEST_TWO);

        Assertions.assertEquals(STR_TEST_AAA, configRaftProperties.getUsername());
        Assertions.assertEquals(STR_TEST_BBB, configRaftProperties.getPassword());
        Assertions.assertEquals(STR_TEST_CCC, configRaftProperties.getServerAddr());
        Assertions.assertEquals(LONG_TEST_ONE, configRaftProperties.getMetadataMaxAgeMs());
        Assertions.assertEquals(LONG_TEST_TWO, configRaftProperties.getTokenValidityInMilliseconds());

    }
}
