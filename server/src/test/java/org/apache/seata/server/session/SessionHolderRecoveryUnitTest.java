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

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionHolderRecoveryUnitTest extends BaseSpringBootTest {
    @ParameterizedTest
    @EnumSource(
            value = GlobalStatus.class,
            names = {"Finished", "UnKnown", "CommitFailed", "RollbackFailed", "TimeoutRollbackFailed"})
    void terminalErrorsAreRemovedAndStoreFailuresContained(GlobalStatus status) throws Exception {
        GlobalSession session = mock(GlobalSession.class);
        when(session.getStatus()).thenReturn(status);
        SessionManager manager = mock(SessionManager.class);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class, CALLS_REAL_METHODS)) {
            holder.when(SessionHolder::getRootSessionManager).thenReturn(manager);
            SessionHolder.reload(Collections.singletonList(session), SessionMode.FILE, false);
            verify(manager).removeGlobalSession(session);
            doThrow(new TransactionException("offline")).when(manager).removeGlobalSession(session);
            assertDoesNotThrow(() -> SessionHolder.reload(Collections.singletonList(session), SessionMode.FILE, false));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = GlobalStatus.class,
            names = {"Committed", "Rollbacked", "TimeoutRollbacked"})
    void completedTransactionsUseFinalizersAndContainFailures(GlobalStatus status) throws Exception {
        GlobalSession session = mock(GlobalSession.class);
        when(session.getStatus()).thenReturn(status);
        try (MockedStatic<SessionHelper> helper = mockStatic(SessionHelper.class)) {
            SessionHolder.reload(Collections.singletonList(session), SessionMode.FILE, false);
            if (status == GlobalStatus.Committed) {
                helper.verify(() -> SessionHelper.endCommitted(session, true));
                helper.when(() -> SessionHelper.endCommitted(session, true))
                        .thenThrow(new TransactionException("failed"));
            } else {
                helper.verify(() -> SessionHelper.endRollbacked(session, true));
                helper.when(() -> SessionHelper.endRollbacked(session, true))
                        .thenThrow(new TransactionException("failed"));
            }
            assertDoesNotThrow(() -> SessionHolder.reload(Collections.singletonList(session), SessionMode.FILE, false));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = GlobalStatus.class,
            names = {"Committing", "CommitRetrying", "AsyncCommitting"})
    void raftCommitRecoveryCleansLocks(GlobalStatus status) throws Exception {
        GlobalSession session = mock(GlobalSession.class);
        when(session.getStatus()).thenReturn(status);
        SessionHolder.reload(Collections.singletonList(session), SessionMode.RAFT, false);
        verify(session).clean();
        doThrow(new TransactionException("failed")).when(session).clean();
        assertThrows(
                RuntimeException.class,
                () -> SessionHolder.reload(Collections.singletonList(session), SessionMode.RAFT, false));
    }

    @Test
    void beginRecoveryDistinguishesNewLeaderTransactionsAndReacquiresBranchLocks() throws Exception {
        GlobalSession session = mock(GlobalSession.class);
        BranchSession branch = mock(BranchSession.class);
        when(session.getStatus()).thenReturn(GlobalStatus.Begin);
        when(session.getSortedBranches()).thenReturn(Collections.singletonList(branch));
        when(session.getBranchSessions()).thenReturn(Collections.singletonList(branch));
        when(session.getBeginTime()).thenReturn(0L);
        SessionHolder.reload(Collections.singletonList(session), SessionMode.RAFT, true);
        verify(branch).lock();
        verify(session).changeGlobalStatus(GlobalStatus.RollbackRetrying);
        when(session.getBeginTime()).thenReturn(Long.MAX_VALUE);
        SessionHolder.reload(Collections.singletonList(session), SessionMode.RAFT, false);
        verify(session).setActive(true);
        when(session.getStatus()).thenReturn(GlobalStatus.Rollbacking);
        SessionHolder.reload(Collections.singletonList(session), SessionMode.RAFT, true);
        verify(branch).setLockStatus(LockStatus.Rollbacking);
        doThrow(new TransactionException("lock failed")).when(branch).lock();
        assertThrows(
                ShouldNeverHappenException.class,
                () -> SessionHolder.reload(Collections.singletonList(session), SessionMode.FILE, true));
    }
}
