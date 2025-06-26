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
package org.apache.seata.server.storage.raft.store;

import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.DynamicPortTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(DynamicPortTestConfig.class)
public class RaftVGroupMappingStoreManagerTest {

    private RaftVGroupMappingStoreManager raftVGroupMappingStoreManager;

    @BeforeEach
    public void setUp() {
        raftVGroupMappingStoreManager = new RaftVGroupMappingStoreManager();
        raftVGroupMappingStoreManager.clear("unit1");
    }

    @Test
    public void testLocalAddVGroup() {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setUnit("unit1");
        mappingDO.setVGroup("vgroup2");

        boolean result = raftVGroupMappingStoreManager.localAddVGroup(mappingDO);

        assertTrue(result);
        assertEquals(
                mappingDO,
                raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup2"));
    }

    @Test
    public void testLocalAddVGroups() {
        Map<String, MappingDO> vGroups = new HashMap<>();
        MappingDO mappingDO1 = new MappingDO();
        mappingDO1.setUnit("unit1");
        mappingDO1.setVGroup("vgroup1");
        vGroups.put("vgroup1", mappingDO1);

        MappingDO mappingDO2 = new MappingDO();
        mappingDO2.setUnit("unit1");
        mappingDO2.setVGroup("vgroup2");
        vGroups.put("vgroup2", mappingDO2);

        raftVGroupMappingStoreManager.localAddVGroups(vGroups, "unit1");

        assertEquals(
                mappingDO1,
                raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup1"));
        assertEquals(
                mappingDO2,
                raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup2"));
    }

    @Test
    public void testLocalRemoveVGroup() {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setUnit("unit1");
        mappingDO.setVGroup("vgroup1");

        raftVGroupMappingStoreManager.localAddVGroup(mappingDO);
        boolean result = raftVGroupMappingStoreManager.localRemoveVGroup("vgroup1");

        assertTrue(result);
        assertTrue(raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").isEmpty());
    }

    @Test
    public void testLoadVGroupsByUnit() {
        MappingDO mappingDO1 = new MappingDO();
        mappingDO1.setUnit("unit1");
        mappingDO1.setVGroup("vgroup1");

        MappingDO mappingDO2 = new MappingDO();
        mappingDO2.setUnit("unit1");
        mappingDO2.setVGroup("vgroup2");

        raftVGroupMappingStoreManager.localAddVGroup(mappingDO1);
        raftVGroupMappingStoreManager.localAddVGroup(mappingDO2);

        Map<String, MappingDO> result = raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1");

        assertEquals(2, result.size());
        assertEquals(mappingDO1, result.get("vgroup1"));
        assertEquals(mappingDO2, result.get("vgroup2"));
    }
}
