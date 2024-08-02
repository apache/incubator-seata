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
package org.apache.seata.server.storage.file.store;

import org.apache.commons.io.FileUtils;
import org.apache.seata.core.store.MappingDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class FileVGroupMappingStoreManagerTest {

    private FileVGroupMappingStoreManager fileVGroupMappingStoreManager;
    private static final String STORE_PATH = "sessionStore/vgroup_mapping.json";
    private static final String VGROUP_NAME = "testVGroup";
    private static final String UNIT = "testUnit";

    @BeforeEach
    public void setUp() {
        fileVGroupMappingStoreManager = new FileVGroupMappingStoreManager("sessionStore");
        File file = new File(STORE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testAddVGroupSuccess() {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(VGROUP_NAME);
        mappingDO.setUnit(UNIT);

        assertTrue(fileVGroupMappingStoreManager.addVGroup(mappingDO));

        HashMap<String, Object> vGroups = fileVGroupMappingStoreManager.loadVGroups();
        assertEquals(UNIT, vGroups.get(VGROUP_NAME));
    }

    @Test
    public void testRemoveVGroupSuccess() {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(VGROUP_NAME);
        mappingDO.setUnit(UNIT);

        fileVGroupMappingStoreManager.addVGroup(mappingDO);
        assertTrue(fileVGroupMappingStoreManager.removeVGroup(VGROUP_NAME));

        HashMap<String, Object> vGroups = fileVGroupMappingStoreManager.loadVGroups();
        assertNull(vGroups.get(VGROUP_NAME));
    }

    @Test
    public void testLoadVGroups() throws IOException {
        HashMap<String, Object> expectedMapping = new HashMap<>();
        expectedMapping.put(VGROUP_NAME, UNIT);
        File file = new File(STORE_PATH);
        FileUtils.writeStringToFile(file, "{\"testVGroup\":\"testUnit\"}", StandardCharsets.UTF_8);

        HashMap<String, Object> actualMapping = fileVGroupMappingStoreManager.loadVGroups();
        assertEquals(expectedMapping, actualMapping);
    }

    @Test
    public void testSave() {
        HashMap<String, Object> vGroupMapping = new HashMap<>();
        vGroupMapping.put(VGROUP_NAME, UNIT);

        assertTrue(fileVGroupMappingStoreManager.save(vGroupMapping));

        File file = new File(STORE_PATH);
        assertTrue(file.exists());

        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            assertEquals("{\"testVGroup\":\"testUnit\"}", content);
        } catch (IOException e) {
            fail("Failed to read the file content");
        }
    }

    @Test
    public void testAddVGroupFailure() {
        FileVGroupMappingStoreManager spyManager = spy(new FileVGroupMappingStoreManager( "src/test/resources"));
        doReturn(false).when(spyManager).save(any(HashMap.class));
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(VGROUP_NAME);
        mappingDO.setUnit(UNIT);

        assertFalse(spyManager.addVGroup(mappingDO));
    }

    @Test
    public void testRemoveVGroupFailure() {
        FileVGroupMappingStoreManager spyManager = spy(new FileVGroupMappingStoreManager("src/test/resources"));
        doReturn(false).when(spyManager).save(any(HashMap.class));
        MappingDO mappingDO = new MappingDO();
        mappingDO.setVGroup(VGROUP_NAME);
        mappingDO.setUnit(UNIT);

        spyManager.addVGroup(mappingDO);
        assertFalse(spyManager.removeVGroup(VGROUP_NAME));
    }
}
