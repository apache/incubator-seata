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

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.core.model.*;
import org.apache.seata.core.store.*;
import org.apache.seata.discovery.registry.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.db.session.DataBaseSessionManager;
import org.apache.seata.server.store.AbstractTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager;
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

    @Test
    void batchRemoveGlobalSessionsDelegatesAndHandlesFailures() throws Exception {
        DataBaseTransactionStoreManager storeManager = mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
        LogStore logStore = mock(LogStore.class);
        storeManager.setLogStore(logStore);

        assertTrue(storeManager.removeGlobalSessions(null));
        assertTrue(storeManager.removeGlobalSessions(Collections.emptyList()));
        assertTrue(storeManager.removeGlobalSessions(Collections.singletonList(null)));
        verify(logStore, never()).deleteGlobalTransactionDOs(anyList());

        GlobalSession s1 = new GlobalSession("app", "group", "tx1", 1000);
        s1.setXid("host:8091:1");
        s1.setTransactionId(1L);
        s1.setStatus(GlobalStatus.Committed);
        GlobalSession s2 = new GlobalSession("app", "group", "tx2", 1000);
        s2.setXid("host:8091:2");
        s2.setTransactionId(2L);
        s2.setStatus(GlobalStatus.Rollbacked);

        when(logStore.deleteGlobalTransactionDOs(anyList())).thenReturn(true);
        assertTrue(storeManager.removeGlobalSessions(Arrays.asList(s1, null, s2)));
        verify(logStore)
                .deleteGlobalTransactionDOs(argThat(list -> list.size() == 2
                        && "host:8091:1".equals(list.get(0).getXid())
                        && "host:8091:2".equals(list.get(1).getXid())));

        DataBaseSessionManager sessionManager = new DataBaseSessionManager();
        TransactionStoreManager mockStore = mock(TransactionStoreManager.class);
        sessionManager.setTransactionStoreManager(mockStore);
        sessionManager.removeGlobalSessions(null);
        sessionManager.removeGlobalSessions(Collections.emptyList());
        verify(mockStore, never()).removeGlobalSessions(anyList());

        List<GlobalSession> sessions = Arrays.asList(s1, s2);
        when(mockStore.removeGlobalSessions(sessions)).thenReturn(true);
        sessionManager.removeGlobalSessions(sessions);
        verify(mockStore).removeGlobalSessions(sessions);

        when(mockStore.removeGlobalSessions(sessions)).thenReturn(false);
        assertThrows(StoreException.class, () -> sessionManager.removeGlobalSessions(sessions));
    }

    @Test
    void defaultBatchRemoveMethodsFallbackToSingleRemoval() throws Exception {
        List<SessionStorable> removedByStore = new ArrayList<>();
        TransactionStoreManager fallbackStore = new AbstractTransactionStoreManager() {
            @Override
            public boolean writeSession(LogOperation logOperation, SessionStorable session) {
                assertEquals(LogOperation.GLOBAL_REMOVE, logOperation);
                removedByStore.add(session);
                return !("host:8091:fail".equals(((GlobalSession) session).getXid()));
            }
        };

        GlobalSession s1 = new GlobalSession("app", "group", "tx1", 1000);
        s1.setXid("host:8091:1");
        GlobalSession sFail = new GlobalSession("app", "group", "tx2", 1000);
        sFail.setXid("host:8091:fail");
        GlobalSession s3 = new GlobalSession("app", "group", "tx3", 1000);
        s3.setXid("host:8091:3");

        assertTrue(fallbackStore.removeGlobalSessions(null));
        assertTrue(fallbackStore.removeGlobalSessions(Collections.emptyList()));
        assertTrue(fallbackStore.removeGlobalSessions(Arrays.asList(s1, null)));
        assertEquals(Collections.singletonList(s1), removedByStore);

        removedByStore.clear();
        assertFalse(fallbackStore.removeGlobalSessions(Arrays.asList(s1, sFail, s3)));
        assertEquals(Arrays.asList(s1, sFail), removedByStore);

        SessionManager fallbackSessionManager = mock(SessionManager.class);
        doCallRealMethod().when(fallbackSessionManager).removeGlobalSessions(any());
        fallbackSessionManager.removeGlobalSessions(null);
        fallbackSessionManager.removeGlobalSessions(Collections.emptyList());
        verify(fallbackSessionManager, never()).removeGlobalSession(any());

        fallbackSessionManager.removeGlobalSessions(Arrays.asList(s1, null, s3));
        verify(fallbackSessionManager).removeGlobalSession(s1);
        verify(fallbackSessionManager).removeGlobalSession(s3);
    }
}
