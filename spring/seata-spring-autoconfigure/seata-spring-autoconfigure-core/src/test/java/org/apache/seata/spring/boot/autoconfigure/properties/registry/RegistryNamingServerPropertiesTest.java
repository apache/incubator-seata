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
package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegistryNamingServerPropertiesTest {

    @Test
    public void testRegistryNamingServerProperties() {
        RegistryNamingServerProperties namingServerProperties = new RegistryNamingServerProperties();
        namingServerProperties.setCluster("cluster");
        namingServerProperties.setServerAddr("addr");
        namingServerProperties.setNamespace("namespace");
        namingServerProperties.setHeartbeatPeriod(1);
        namingServerProperties.setMetadataMaxAgeMs(1L);
        namingServerProperties.setUsername("username");
        namingServerProperties.setPassword("password");
        namingServerProperties.setTokenValidityInMilliseconds(1L);

        Assertions.assertEquals("cluster", namingServerProperties.getCluster());
        Assertions.assertEquals("addr", namingServerProperties.getServerAddr());
        Assertions.assertEquals("namespace", namingServerProperties.getNamespace());
        Assertions.assertEquals(1, namingServerProperties.getHeartbeatPeriod());
        Assertions.assertEquals(1L, namingServerProperties.getMetadataMaxAgeMs());
        Assertions.assertEquals("username", namingServerProperties.getUsername());
        Assertions.assertEquals("password", namingServerProperties.getPassword());
        Assertions.assertEquals(1L, namingServerProperties.getTokenValidityInMilliseconds());
    }
}
