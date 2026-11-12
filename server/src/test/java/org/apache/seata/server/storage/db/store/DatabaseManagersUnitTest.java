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
import org.apache.seata.core.model.*;
import org.apache.seata.core.store.*;
import org.apache.seata.discovery.registry.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseManagersUnitTest extends BaseSpringBootTest {
    @Test
    void sessionQueriesPreserveLazyLoadingAndExpectedStatus() {
        DataBaseTransactionStoreManager manager = mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
        LogStore store = mock(LogStore.class);
        manager.setLogStore(store);
        manager.setLogQueryLimit(10);
        GlobalTransactionDO global = new GlobalTransactionDO();
        global.setXid("host:8091:42");
        global.setTransactionId(42L);
        global.setStatus(1);
        global.setTimeout(1000);
        global.setBeginTime(1L);
        when(store.queryGlobalTransactionDO(42L)).thenReturn(global);
        when(store.queryGlobalTransactionDO(global.getXid())).thenReturn(global);
        when(store.queryGlobalTransactionDO(any(int[].class), eq(10))).thenReturn(Collections.singletonList(global));
        BranchTransactionDO branch = new BranchTransactionDO();
        branch.setBranchId(7L);
        branch.setXid(global.getXid());
        branch.setTransactionId(42L);
        branch.setStatus(1);
        branch.setBranchType("AT");
        when(store.queryBranchTransactionDO(global.getXid())).thenReturn(Collections.singletonList(branch));
        when(store.queryBranchTransactionDO(anyList())).thenReturn(Collections.singletonList(branch));
        assertEquals(1, manager.readSession(42L).getBranchSessions().size());
        SessionCondition c = new SessionCondition();
        c.setTransactionId(42L);
        assertEquals(1, manager.readSession(c).size());
        assertTrue(manager.readSession(new GlobalStatus[] {GlobalStatus.Begin}, false)
                .get(0)
                .isLazyLoadBranch());
        assertEquals(
                1,
                manager.readSortByTimeoutBeginSessions(true)
                        .get(0)
                        .getBranchSessions()
                        .size());
        assertNull(manager.readSession(new SessionCondition()));
        assertNull(manager.readSession(new SessionCondition("missing")));
        GlobalSession session = new GlobalSession("app", "group", "order", 1000);
        session.setExpectedStatusFromCurrent();
        session.setStatus(GlobalStatus.Committing);
        when(store.updateGlobalTransactionDO(any(), eq(1))).thenReturn(true);
        assertTrue(manager.writeSession(LogOperation.GLOBAL_UPDATE, session));
        verify(store).updateGlobalTransactionDO(argThat(g -> g.getStatus() == 2), eq(1));
    }

    @Test
    void mappingsFilterClusterAndNotifyRegistryWithCurrentValues() throws Exception {
        DataBaseVGroupMappingStoreManager manager = mock(DataBaseVGroupMappingStoreManager.class, CALLS_REAL_METHODS);
        VGroupMappingDataBaseDAO dao = mock(VGroupMappingDataBaseDAO.class);
        manager.vGroupMappingDataBaseDAO = dao;
        MappingDO matching = new MappingDO();
        matching.setVGroup("payments");
        matching.setCluster("east");
        MappingDO other = new MappingDO();
        other.setVGroup("orders");
        other.setCluster("west");
        when(dao.queryMappingDO()).thenReturn(Arrays.asList(matching, other, new MappingDO()));
        when(dao.insertMappingDO(matching)).thenReturn(true);
        when(dao.deleteMappingDOByVGroup("payments")).thenReturn(true);
        Instance instance = mock(Instance.class);
        when(instance.getClusterName()).thenReturn("east");
        RegistryService registry = mock(RegistryService.class);
        try (MockedStatic<Instance> instances = mockStatic(Instance.class);
                MockedStatic<MultiRegistryFactory> registries = mockStatic(MultiRegistryFactory.class)) {
            instances.when(Instance::getInstance).thenReturn(instance);
            registries.when(MultiRegistryFactory::getInstances).thenReturn(Collections.singletonList(registry));
            assertTrue(manager.addVGroup(matching));
            assertTrue(manager.removeVGroup("payments"));
            assertEquals(
                    Collections.singleton("payments"), manager.loadVGroups().keySet());
            manager.notifyMapping();
            verify(instance).addMetadata(eq("vGroup"), argThat(m -> ((Map<?, ?>) m).containsKey("payments")));
            verify(registry).register(any(java.net.InetSocketAddress.class));
            doThrow(new IllegalStateException("offline"))
                    .when(registry)
                    .register(any(java.net.InetSocketAddress.class));
            assertThrows(RuntimeException.class, manager::notifyMapping);
        }
    }
}
