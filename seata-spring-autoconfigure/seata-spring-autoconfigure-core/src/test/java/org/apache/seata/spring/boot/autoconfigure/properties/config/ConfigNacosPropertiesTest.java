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

public class ConfigNacosPropertiesTest extends BasePropertiesTest {

    @Test
    public void testConfigNacosProperties() {
        ConfigNacosProperties configNacosProperties = new ConfigNacosProperties();
        configNacosProperties.setServerAddr("addr");
        configNacosProperties.setAccessKey("key");
        configNacosProperties.setSecretKey("key");
        configNacosProperties.setNamespace("namespace");
        configNacosProperties.setUsername("username");
        configNacosProperties.setPassword("password");
        configNacosProperties.setContextPath("path");
        configNacosProperties.setRamRoleName("ram");
        configNacosProperties.setGroup("group");
        configNacosProperties.setDataId("dataId");
        Assertions.assertEquals("addr", configNacosProperties.getServerAddr());
        Assertions.assertEquals("key", configNacosProperties.getAccessKey());
        Assertions.assertEquals("key", configNacosProperties.getSecretKey());
        Assertions.assertEquals("namespace", configNacosProperties.getNamespace());
        Assertions.assertEquals("username", configNacosProperties.getUsername());
        Assertions.assertEquals("password", configNacosProperties.getPassword());
        Assertions.assertEquals("path", configNacosProperties.getContextPath());
        Assertions.assertEquals("ram", configNacosProperties.getRamRoleName());
        Assertions.assertEquals("group", configNacosProperties.getGroup());
        Assertions.assertEquals("dataId", configNacosProperties.getDataId());
    }
}
