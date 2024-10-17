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
package org.apache.seata.server.storage.redis.store;

import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.core.store.MappingDO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;


@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
@SpringBootTest
public class RedisVGroupMappingStoreManagerTest {
    private RedisVGroupMappingStoreManager redisVGroupMappingStoreManager;

    @BeforeEach
    public void setUp() {
        redisVGroupMappingStoreManager = new RedisVGroupMappingStoreManager();
    }

    @Test
    public void testLoadVGroups() {
        Instance instance = Instance.getInstance();
        instance.setNamespace("public");
        instance.setClusterName("testCluster");
        instance.setUnit("123");
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup("testVGroup");
        mappingDO.setCluster("testCluster");
        mappingDO.setNamespace("public");
        redisVGroupMappingStoreManager.addVGroup(mappingDO);
        Map<String,Object> map = redisVGroupMappingStoreManager.loadVGroups();
        Assertions.assertTrue(map.containsKey("testVGroup"));
        redisVGroupMappingStoreManager.removeVGroup("testVGroup");
        map = redisVGroupMappingStoreManager.loadVGroups();
        Assertions.assertFalse(map.containsKey("testVGroup"));
    }
}
