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
package org.apache.seata.server.session;

import org.apache.seata.core.exception.*;
import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.lock.*;
import org.apache.seata.server.store.TransactionStoreManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionLifecycleUnitTest extends BaseSpringBootTest {
    @Test
    void listenersReceivePersistedLifecycleAndCanBeRemoved() throws Exception {
        SessionManager manager = mock(SessionManager.class);
        LockManager locks = mock(LockManager.class);
        SessionLifecycleListener listener = mock(SessionLifecycleListener.class);
        GlobalSession global = new GlobalSession("app", "group", "transaction", 1000);
        BranchSession branch = mock(BranchSession.class);
        when(branch.unlock()).thenReturn(true);
        when(locks.releaseGlobalSessionLock(global)).thenReturn(true);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class);
                MockedStatic<LockerManagerFactory> factory = mockStatic(LockerManagerFactory.class);
                MockedStatic<RaftServerManager> raft = mockStatic(RaftServerManager.class)) {
            holder.when(SessionHolder::getRootSessionManager).thenReturn(manager);
            factory.when(LockerManagerFactory::getLockManager).thenReturn(locks);
            global.addSessionLifecycleListener(listener);
            global.begin();
            verify(listener).onBegin(global);
            global.addBranch(branch);
            verify(listener).onAddBranch(global, branch);
            assertTrue(global.getBranchSessions().contains(branch));
            global.changeBranchStatus(branch, BranchStatus.PhaseOne_Done);
            verify(listener).onBranchStatusChange(global, branch, BranchStatus.PhaseOne_Done);
            global.changeGlobalStatus(GlobalStatus.Rollbacking);
            verify(listener).onStatusChange(global, GlobalStatus.Rollbacking);
            global.close();
            verify(listener).onClose(global);
            global.removeAndUnlockBranch(branch);
            verify(listener).onRemoveBranch(global, branch);
            assertTrue(global.getBranchSessions().isEmpty());
            global.setStatus(GlobalStatus.Committed);
            global.end();
            verify(listener).onSuccessEnd(global);
            global.setStatus(GlobalStatus.CommitFailed);
            global.end();
            verify(listener).onFailEnd(global);
            global.removeSessionLifecycleListener(listener);
            clearInvocations(listener);
            global.begin();
            verifyNoInteractions(listener);
            when(locks.releaseGlobalSessionLock(global)).thenReturn(false);
            assertThrows(TransactionException.class, global::clean);
            when(branch.unlock()).thenReturn(false);
            assertThrows(TransactionException.class, () -> global.unlockBranch(branch));
        }
    }

    @Test
    void failedPersistenceIsTranslatedToTransactionExceptions() throws Exception {
        AbstractSessionManager manager = mock(AbstractSessionManager.class, CALLS_REAL_METHODS);
        TransactionStoreManager store = mock(TransactionStoreManager.class);
        manager.setTransactionStoreManager(store);
        GlobalSession global = new GlobalSession("app", "group", "transaction", 1000);
        BranchSession branch = new BranchSession();
        assertThrows(GlobalTransactionException.class, () -> manager.addGlobalSession(global));
        assertThrows(
                GlobalTransactionException.class,
                () -> manager.updateGlobalSessionStatus(global, GlobalStatus.Committing));
        assertThrows(GlobalTransactionException.class, () -> manager.removeGlobalSession(global));
        assertThrows(BranchTransactionException.class, () -> manager.addBranchSession(global, branch));
        assertThrows(
                BranchTransactionException.class,
                () -> manager.updateBranchSessionStatus(branch, BranchStatus.Registered));
        assertThrows(BranchTransactionException.class, () -> manager.removeBranchSession(global, branch));
        manager.destroy();
    }

    @Test
    void lazyBranchesAreLoadedOnceFromRootManager() {
        GlobalSession lazy = new GlobalSession("app", "group", "transaction", 1000, true);
        lazy.setXid("xid");
        GlobalSession stored = new GlobalSession("app", "group", "transaction", 1000);
        stored.add(new BranchSession());
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSession("xid", true)).thenReturn(stored);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            holder.when(SessionHolder::getRootSessionManager).thenReturn(manager);
            lazy.loadBranchs();
            lazy.loadBranchs();
            assertEquals(1, lazy.getBranchSessions().size());
            verify(manager, times(1)).findGlobalSession("xid", true);
        }
    }
}
