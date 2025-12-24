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
package org.apache.seata.server.controller;

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.result.Result;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class VGroupMappingControllerTest extends BaseSpringBootTest {

    private VGroupMappingController vGroupMappingController;

    @Mock
    private VGroupMappingStoreManager vGroupMappingStoreManager;

    private Instance instance;

    @BeforeEach
    void setUp() throws Exception {
        vGroupMappingController = new VGroupMappingController();
        instance = Instance.getInstance();
        instance.setNamespace("default");
        instance.setClusterName("default");
        instance.setTerm(0);
        setSessionMode("file");
    }

    @Test
    void addVGroupShouldSetTerm_whenSessionModeIsNotRaft() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(vGroupMappingStoreManager.addVGroup(org.mockito.ArgumentMatchers.any(MappingDO.class)))
                    .thenReturn(true);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(vGroupMappingStoreManager);
            long before = instance.getTerm();
            Result<?> result = vGroupMappingController.addVGroup("vgroup", "unit-a");
            assertEquals(Result.SUCCESS_CODE, result.getCode());
            assertEquals(Result.SUCCESS_MSG, result.getMessage());
            assertTrue(instance.getTerm() >= before);
            ArgumentCaptor<MappingDO> mappingCaptor = ArgumentCaptor.forClass(MappingDO.class);
            verify(vGroupMappingStoreManager).addVGroup(mappingCaptor.capture());
            MappingDO mappingDO = mappingCaptor.getValue();
            assertEquals("default", mappingDO.getNamespace());
            assertEquals("default", mappingDO.getCluster());
            assertEquals("unit-a", mappingDO.getUnit());
            assertEquals("vgroup", mappingDO.getVGroup());
        }
    }

    @Test
    void addVGroupShouldReturnErrorWhenStoreFails() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(vGroupMappingStoreManager.addVGroup(org.mockito.ArgumentMatchers.any(MappingDO.class)))
                    .thenReturn(false);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(vGroupMappingStoreManager);
            Result<?> result = vGroupMappingController.addVGroup("vgroup", "unit-a");
            assertEquals("500", result.getCode());
            assertEquals("add vGroup failed!", result.getMessage());
        }
    }

    @Test
    void removeVGroupShouldSetTerm_whenSessionModeIsNotRaft() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(vGroupMappingStoreManager.removeVGroup("vgroup")).thenReturn(true);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(vGroupMappingStoreManager);
            long before = instance.getTerm();
            Result<?> result = vGroupMappingController.removeVGroup("vgroup");
            assertEquals(Result.SUCCESS_CODE, result.getCode());
            assertEquals(Result.SUCCESS_MSG, result.getMessage());
            assertTrue(instance.getTerm() >= before);
            verify(vGroupMappingStoreManager).removeVGroup("vgroup");
        }
    }

    @Test
    void removeVGroupShouldReturnErrorWhenStoreFails() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(vGroupMappingStoreManager.removeVGroup("vgroup")).thenReturn(false);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(vGroupMappingStoreManager);
            Result<?> result = vGroupMappingController.removeVGroup("vgroup");
            assertEquals("500", result.getCode());
            assertEquals("remove vGroup failed!", result.getMessage());
        }
    }

    @Test
    void addVGroupShouldNotModifyTermWhenRaft() throws Exception {
        setSessionMode("raft");
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(vGroupMappingStoreManager.addVGroup(org.mockito.ArgumentMatchers.any(MappingDO.class)))
                    .thenReturn(true);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(vGroupMappingStoreManager);
            long before = instance.getTerm();
            vGroupMappingController.addVGroup("vgroup", "unit-a");
            assertEquals(before, instance.getTerm());
        }
    }

    private void setSessionMode(String mode) throws Exception {
        Field sessionModeField = VGroupMappingController.class.getDeclaredField("sessionMode");
        sessionModeField.setAccessible(true);
        sessionModeField.set(vGroupMappingController, mode);
    }
}
