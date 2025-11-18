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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Map;

@EnabledIfSystemProperty(named = "dbCaseEnabled", matches = "true")
public class DataBaseVGroupMappingStoreManagerTest extends BaseSpringBootTest {

    private DataBaseVGroupMappingStoreManager storeManager;

    @BeforeEach
    public void setUp() {
        storeManager = new DataBaseVGroupMappingStoreManager();
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(storeManager);
    }

    @Test
    public void testAddAndRemoveVGroup() {
        Instance instance = Instance.getInstance();
        instance.setNamespace("test-namespace");
        instance.setClusterName("test-cluster");

        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup("test-vgroup");
        mappingDO.setNamespace("test-namespace");
        mappingDO.setCluster("test-cluster");

        boolean added = storeManager.addVGroup(mappingDO);
        Assertions.assertTrue(added);

        Map<String, Object> vGroups = storeManager.loadVGroups();
        Assertions.assertNotNull(vGroups);

        boolean removed = storeManager.removeVGroup("test-vgroup");
        Assertions.assertTrue(removed);
    }

    @Test
    public void testLoadVGroups() {
        Map<String, Object> vGroups = storeManager.loadVGroups();
        Assertions.assertNotNull(vGroups);
    }
}
