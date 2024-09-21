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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ServicePropertiesTest {

    @Test
    public void testServiceProperties() {
        ServiceProperties serviceProperties = new ServiceProperties();

        Map<String, String> vGroupMapping = new HashMap<>();
        vGroupMapping.put("default_tx_group", "default");
        serviceProperties.setVgroupMapping(vGroupMapping);
        Assertions.assertEquals("default", serviceProperties.getVgroupMapping().get("default_tx_group"));

        Map<String, String> groupList = new HashMap<>();
        groupList.put("default", "127.0.0.1:8091");
        serviceProperties.setGrouplist(groupList);
        Assertions.assertEquals("127.0.0.1:8091", serviceProperties.getGrouplist().get("default"));

        serviceProperties.setDisableGlobalTransaction(true);
        Assertions.assertTrue(serviceProperties.isDisableGlobalTransaction());
    }

    @Test
    public void testAfterPropertiesSet() throws Exception {
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.afterPropertiesSet();
        Assertions.assertEquals("default", serviceProperties.getVgroupMapping().get("default_tx_group"));
        Assertions.assertEquals("default", serviceProperties.getVgroupMapping().get("my_test_tx_group"));
        Assertions.assertEquals("127.0.0.1:8091", serviceProperties.getGrouplist().get("default"));

        serviceProperties = new ServiceProperties();

        Map<String, String> vGroupMapping = new HashMap<>();
        vGroupMapping.put("default_tx_group", "default");
        serviceProperties.setVgroupMapping(vGroupMapping);

        Map<String, String> groupList = new HashMap<>();
        groupList.put("default", "127.0.0.1:8091");
        serviceProperties.setGrouplist(groupList);

        serviceProperties.afterPropertiesSet();
        Assertions.assertEquals("default", serviceProperties.getVgroupMapping().get("default_tx_group"));
        Assertions.assertEquals("127.0.0.1:8091", serviceProperties.getGrouplist().get("default"));
    }
}
