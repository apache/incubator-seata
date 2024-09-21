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

public class ConfigZooKeeperPropertiesTest extends BasePropertiesTest {

    @Test
    public void testConfigZooKeeperProperties() {
        ConfigZooKeeperProperties configZooKeeperProperties = new ConfigZooKeeperProperties();
        configZooKeeperProperties.setServerAddr(STR_TEST_AAA);
        configZooKeeperProperties.setNodePath(STR_TEST_BBB);
        configZooKeeperProperties.setUsername(STR_TEST_CCC);
        configZooKeeperProperties.setPassword(STR_TEST_DDD);
        configZooKeeperProperties.setConnectTimeout(LONG_TEST_ONE);
        configZooKeeperProperties.setSessionTimeout(LONG_TEST_TWO);
        Assertions.assertEquals(STR_TEST_AAA, configZooKeeperProperties.getServerAddr());
        Assertions.assertEquals(STR_TEST_BBB, configZooKeeperProperties.getNodePath());
        Assertions.assertEquals(STR_TEST_CCC, configZooKeeperProperties.getUsername());
        Assertions.assertEquals(STR_TEST_DDD, configZooKeeperProperties.getPassword());
        Assertions.assertEquals(LONG_TEST_ONE, configZooKeeperProperties.getConnectTimeout());
        Assertions.assertEquals(LONG_TEST_TWO, configZooKeeperProperties.getSessionTimeout());
    }
}
