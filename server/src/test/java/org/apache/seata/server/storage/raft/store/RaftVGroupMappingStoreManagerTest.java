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

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class RaftVGroupMappingStoreManagerTest extends BaseSpringBootTest {

    private RaftVGroupMappingStoreManager raftVGroupMappingStoreManager;

    @BeforeEach
    public void setUp() {
        raftVGroupMappingStoreManager = new RaftVGroupMappingStoreManager();
        raftVGroupMappingStoreManager.clear("unit1");
    }

    @AfterEach
    public void tearDown() {
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

    // ==================== Raft Consensus Methods Tests ====================

    @Test
    public void testAddVGroup_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            MappingDO mappingDO = new MappingDO();
            mappingDO.setUnit("unit1");
            mappingDO.setVGroup("vgroup-raft-test");

            boolean result = raftVGroupMappingStoreManager.addVGroup(mappingDO);

            assertTrue(result);
            assertEquals(
                    mappingDO,
                    raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup-raft-test"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testAddVGroup_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(false);
                        closure.run(status);

                        return future.get();
                    });

            MappingDO mappingDO = new MappingDO();
            mappingDO.setUnit("unit1");
            mappingDO.setVGroup("vgroup-raft-test");

            boolean result = raftVGroupMappingStoreManager.addVGroup(mappingDO);

            assertFalse(result);
            assertNull(raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup-raft-test"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testAddVGroup_Exception() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenThrow(new RuntimeException("Raft error"));

            MappingDO mappingDO = new MappingDO();
            mappingDO.setUnit("unit1");
            mappingDO.setVGroup("vgroup-raft-test");

            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> raftVGroupMappingStoreManager.addVGroup(mappingDO));

            assertTrue(exception.getMessage().contains("Raft error"));
        }
    }

    @Test
    public void testRemoveVGroup_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Add a vGroup first
            MappingDO mappingDO = new MappingDO();
            mappingDO.setUnit("unit1");
            mappingDO.setVGroup("vgroup-to-remove");
            raftVGroupMappingStoreManager.localAddVGroup(mappingDO);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            boolean result = raftVGroupMappingStoreManager.removeVGroup("vgroup-to-remove");

            assertTrue(result);
            assertNull(raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup-to-remove"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveVGroup_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Add a vGroup first
            MappingDO mappingDO = new MappingDO();
            mappingDO.setUnit("unit1");
            mappingDO.setVGroup("vgroup-to-remove");
            raftVGroupMappingStoreManager.localAddVGroup(mappingDO);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(false);
                        closure.run(status);

                        return future.get();
                    });

            boolean result = raftVGroupMappingStoreManager.removeVGroup("vgroup-to-remove");

            assertFalse(result);
            // VGroup should still exist since removal failed
            assertNotNull(
                    raftVGroupMappingStoreManager.loadVGroupsByUnit("unit1").get("vgroup-to-remove"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveVGroup_Exception() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenThrow(new RuntimeException("Raft error"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class, () -> raftVGroupMappingStoreManager.removeVGroup("vgroup-test"));

            assertTrue(exception.getMessage().contains("Raft error"));
        }
    }

    // ==================== notifyMapping Tests ====================

    @Test
    public void testNotifyMapping_Success() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<MultiRegistryFactory> registryFactoryMock =
                        Mockito.mockStatic(MultiRegistryFactory.class)) {

            // Mock RaftServerManager
            Set<String> groups = Collections.singleton("group1");
            raftServerManagerMock.when(RaftServerManager::groups).thenReturn(groups);
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader("group1"))
                    .thenReturn(true);

            // Mock MultiRegistryFactory
            RegistryService<?> mockRegistryService = mock(RegistryService.class);
            List<RegistryService<?>> registryServices = Collections.singletonList(mockRegistryService);
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(registryServices);

            // Add some vGroups
            MappingDO mappingDO1 = new MappingDO();
            mappingDO1.setUnit("unit1");
            mappingDO1.setVGroup("vgroup1");
            raftVGroupMappingStoreManager.localAddVGroup(mappingDO1);

            assertDoesNotThrow(() -> raftVGroupMappingStoreManager.notifyMapping());
        }
    }

    @Test
    public void testNotifyMapping_WithFollowerRole() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<MultiRegistryFactory> registryFactoryMock =
                        Mockito.mockStatic(MultiRegistryFactory.class)) {

            // Mock RaftServerManager - node is follower
            Set<String> groups = Collections.singleton("group1");
            raftServerManagerMock.when(RaftServerManager::groups).thenReturn(groups);
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader("group1"))
                    .thenReturn(false);

            // Mock MultiRegistryFactory
            RegistryService<?> mockRegistryService = mock(RegistryService.class);
            List<RegistryService<?>> registryServices = Collections.singletonList(mockRegistryService);
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(registryServices);

            // Add some vGroups
            MappingDO mappingDO1 = new MappingDO();
            mappingDO1.setUnit("unit1");
            mappingDO1.setVGroup("vgroup1");
            raftVGroupMappingStoreManager.localAddVGroup(mappingDO1);

            assertDoesNotThrow(() -> raftVGroupMappingStoreManager.notifyMapping());
        }
    }

    @Test
    public void testNotifyMapping_WithMultipleGroups() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<MultiRegistryFactory> registryFactoryMock =
                        Mockito.mockStatic(MultiRegistryFactory.class)) {

            // Mock RaftServerManager with multiple groups
            Set<String> groups = new HashSet<>(Arrays.asList("group1", "group2"));
            raftServerManagerMock.when(RaftServerManager::groups).thenReturn(groups);
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader("group1"))
                    .thenReturn(true);
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader("group2"))
                    .thenReturn(false);

            // Mock MultiRegistryFactory
            RegistryService<?> mockRegistryService = mock(RegistryService.class);
            List<RegistryService<?>> registryServices = Collections.singletonList(mockRegistryService);
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(registryServices);

            assertDoesNotThrow(() -> raftVGroupMappingStoreManager.notifyMapping());
        }
    }

    @Test
    public void testNotifyMapping_RegistryException() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<MultiRegistryFactory> registryFactoryMock =
                        Mockito.mockStatic(MultiRegistryFactory.class)) {

            // Mock RaftServerManager
            Set<String> groups = Collections.singleton("group1");
            raftServerManagerMock.when(RaftServerManager::groups).thenReturn(groups);
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader("group1"))
                    .thenReturn(true);

            // Mock MultiRegistryFactory with registry that throws exception
            RegistryService<?> mockRegistryService = mock(RegistryService.class);
            try {
                Mockito.doThrow(new RuntimeException("Registry error"))
                        .when(mockRegistryService)
                        .register(any(ServiceInstance.class));
            } catch (Exception e) {
                // This shouldn't happen as we're just configuring the mock
            }
            List<RegistryService<?>> registryServices = Collections.singletonList(mockRegistryService);
            registryFactoryMock.when(MultiRegistryFactory::getInstances).thenReturn(registryServices);

            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> raftVGroupMappingStoreManager.notifyMapping());

            assertTrue(exception.getMessage().contains("vGroup mapping relationship notified failed"));
        }
    }

    // ==================== Additional Tests ====================

    @Test
    public void testLoadVGroups() {
        MappingDO mappingDO1 = new MappingDO();
        mappingDO1.setUnit("unit1");
        mappingDO1.setVGroup("vgroup1");

        MappingDO mappingDO2 = new MappingDO();
        mappingDO2.setUnit("unit2");
        mappingDO2.setVGroup("vgroup2");

        raftVGroupMappingStoreManager.localAddVGroup(mappingDO1);
        raftVGroupMappingStoreManager.localAddVGroup(mappingDO2);

        Map<String, Object> result = raftVGroupMappingStoreManager.loadVGroups();

        assertEquals(2, result.size());
        assertEquals("unit1", result.get("vgroup1"));
        assertEquals("unit2", result.get("vgroup2"));
    }

    @Test
    public void testReadVGroups() {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setUnit("unit1");
        mappingDO.setVGroup("vgroup1");
        raftVGroupMappingStoreManager.localAddVGroup(mappingDO);

        Map<String, Object> loadResult = raftVGroupMappingStoreManager.loadVGroups();
        Map<String, Object> readResult = raftVGroupMappingStoreManager.readVGroups();

        assertEquals(loadResult, readResult);
    }
}
