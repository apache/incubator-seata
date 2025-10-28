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
package org.apache.seata.rm;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultResourceManager
 */
public class DefaultResourceManagerTest {

    private DefaultResourceManager defaultRm;

    @Mock
    private ResourceManager mockAtRm;

    @Mock
    private ResourceManager mockTccRm;

    @Mock
    private Resource mockAtResource;

    @Mock
    private Resource mockTccResource;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        defaultRm = DefaultResourceManager.get();
        DefaultResourceManager.resourceManagers.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        DefaultResourceManager.resourceManagers.clear();
    }

    @Test
    void testGetInstanceIsSingleton() {
        DefaultResourceManager instance1 = DefaultResourceManager.get();
        DefaultResourceManager instance2 = DefaultResourceManager.get();
        assertEquals(instance1, instance2);
    }

    @Test
    void testInitResourceManagers() {
        try (MockedStatic<EnhancedServiceLoader> mockedLoader = Mockito.mockStatic(EnhancedServiceLoader.class)) {
            List<ResourceManager> rmList = new ArrayList<>();
            rmList.add(mockAtRm);
            rmList.add(mockTccRm);
            when(EnhancedServiceLoader.loadAll(ResourceManager.class)).thenReturn(rmList);
            when(mockAtRm.getBranchType()).thenReturn(BranchType.AT);
            when(mockTccRm.getBranchType()).thenReturn(BranchType.TCC);

            // Re-initialize by creating new instance (using reflection for test)
            DefaultResourceManager.resourceManagers.clear();
            defaultRm.initResourceManagers();

            assertEquals(2, DefaultResourceManager.resourceManagers.size());
            assertEquals(mockAtRm, DefaultResourceManager.resourceManagers.get(BranchType.AT));
            assertEquals(mockTccRm, DefaultResourceManager.resourceManagers.get(BranchType.TCC));
        }
    }

    @Test
    void testMockResourceManager() {
        DefaultResourceManager.mockResourceManager(BranchType.SAGA, mockAtRm);
        assertEquals(mockAtRm, DefaultResourceManager.resourceManagers.get(BranchType.SAGA));
    }

    @Test
    void testGetResourceManagerExistingType() {
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        ResourceManager result = defaultRm.getResourceManager(BranchType.AT);
        assertEquals(mockAtRm, result);
    }

    @Test
    void testGetResourceManagerNonExistingType() {
        FrameworkException exception = assertThrows(FrameworkException.class,
                () -> defaultRm.getResourceManager(BranchType.XA));
        assertTrue(exception.getMessage().contains("No ResourceManager for BranchType:XA"));
    }

    @Test
    void testBranchCommit() throws TransactionException {
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        when(mockAtRm.branchCommit(eq(BranchType.AT), eq("xid1"), eq(123L), eq("res1"), eq("data")))
                .thenReturn(BranchStatus.PhaseTwo_Committed);

        BranchStatus result = defaultRm.branchCommit(BranchType.AT, "xid1", 123L, "res1", "data");
        assertEquals(BranchStatus.PhaseTwo_Committed, result);
        verify(mockAtRm).branchCommit(eq(BranchType.AT), eq("xid1"), eq(123L), eq("res1"), eq("data"));
    }

    @Test
    void testBranchRollback() throws TransactionException {
        DefaultResourceManager.mockResourceManager(BranchType.TCC, mockTccRm);
        when(mockTccRm.branchRollback(eq(BranchType.TCC), eq("xid2"), eq(456L), eq("res2"), eq("data2")))
                .thenReturn(BranchStatus.PhaseTwo_Rollbacked);

        BranchStatus result = defaultRm.branchRollback(BranchType.TCC, "xid2", 456L, "res2", "data2");
        assertEquals(BranchStatus.PhaseTwo_Rollbacked, result);
        verify(mockTccRm).branchRollback(eq(BranchType.TCC), eq("xid2"), eq(456L), eq("res2"), eq("data2"));
    }

    @Test
    void testBranchRegister() throws TransactionException {
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        when(mockAtRm.branchRegister(eq(BranchType.AT), eq("res3"), eq("client1"), eq("xid3"), eq("data3"), eq("locks")))
                .thenReturn(789L);

        Long branchId = defaultRm.branchRegister(BranchType.AT, "res3", "client1", "xid3", "data3", "locks");
        assertEquals(789L, branchId);
        verify(mockAtRm).branchRegister(eq(BranchType.AT), eq("res3"), eq("client1"), eq("xid3"), eq("data3"), eq("locks"));
    }

    @Test
    void testBranchReport() throws TransactionException {
        DefaultResourceManager.mockResourceManager(BranchType.TCC, mockTccRm);

        defaultRm.branchReport(BranchType.TCC, "xid4", 101L, BranchStatus.PhaseOne_Done, "data4");
        verify(mockTccRm).branchReport(eq(BranchType.TCC), eq("xid4"), eq(101L), eq(BranchStatus.PhaseOne_Done), eq("data4"));
    }

    @Test
    void testLockQuery() throws TransactionException {
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        when(mockAtRm.lockQuery(eq(BranchType.AT), eq("res5"), eq("xid5"), eq("locks5")))
                .thenReturn(true);

        boolean result = defaultRm.lockQuery(BranchType.AT, "res5", "xid5", "locks5");
        assertTrue(result);
        verify(mockAtRm).lockQuery(eq(BranchType.AT), eq("res5"), eq("xid5"), eq("locks5"));
    }

    @Test
    void testRegisterResource() {
        when(mockAtResource.getBranchType()).thenReturn(BranchType.AT);
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);

        defaultRm.registerResource(mockAtResource);
        verify(mockAtRm).registerResource(mockAtResource);
    }

    @Test
    void testUnregisterResource() {
        when(mockTccResource.getBranchType()).thenReturn(BranchType.TCC);
        DefaultResourceManager.mockResourceManager(BranchType.TCC, mockTccRm);

        defaultRm.unregisterResource(mockTccResource);
        verify(mockTccRm).unregisterResource(mockTccResource);
    }

    @Test
    void testGetManagedResources() {
        Map<String, Resource> atResources = new HashMap<>();
        atResources.put("resAt1", mockAtResource);
        when(mockAtRm.getManagedResources()).thenReturn(atResources);

        Map<String, Resource> tccResources = new HashMap<>();
        tccResources.put("resTcc1", mockTccResource);
        when(mockTccRm.getManagedResources()).thenReturn(tccResources);

        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        DefaultResourceManager.mockResourceManager(BranchType.TCC, mockTccRm);

        Map<String, Resource> allResources = defaultRm.getManagedResources();
        assertEquals(2, allResources.size());
        assertTrue(allResources.containsKey("resAt1"));
        assertTrue(allResources.containsKey("resTcc1"));
    }

    @Test
    void testGetBranchTypeThrowsException() {
        assertThrows(FrameworkException.class, defaultRm::getBranchType);
    }

    @Test
    void testGetGlobalStatus() {
        DefaultResourceManager.mockResourceManager(BranchType.AT, mockAtRm);
        when(mockAtRm.getGlobalStatus(eq(BranchType.AT), eq("xid6")))
                .thenReturn(GlobalStatus.Committed);

        GlobalStatus status = defaultRm.getGlobalStatus(BranchType.AT, "xid6");
        assertEquals(GlobalStatus.Committed, status);
        verify(mockAtRm).getGlobalStatus(eq(BranchType.AT), eq("xid6"));
    }
}