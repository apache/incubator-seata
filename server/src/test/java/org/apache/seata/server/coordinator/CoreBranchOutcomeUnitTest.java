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
package org.apache.seata.server.coordinator;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.metrics.MetricsPublisher;
import org.apache.seata.server.session.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreBranchOutcomeUnitTest extends BaseSpringBootTest {
    private DefaultCore core;
    private AbstractCore branchCore;
    private GlobalSession global;
    private BranchSession branch;
    private MockedStatic<MetricsPublisher> metrics;
    private MockedStatic<SessionHelper> helper;

    @BeforeEach
    void open() throws Exception {
        core = mock(DefaultCore.class, CALLS_REAL_METHODS);
        branchCore = mock(AbstractCore.class);
        doReturn(branchCore).when(core).getCore(any());
        global = mock(GlobalSession.class);
        branch = mock(BranchSession.class);
        when(branch.getBranchType()).thenReturn(BranchType.TCC);
        when(branch.getStatus()).thenReturn(BranchStatus.Registered);
        when(branch.getXid()).thenReturn("xid");
        when(global.getXid()).thenReturn("xid");
        when(global.getStatus()).thenReturn(GlobalStatus.Committing);
        when(global.getSortedBranches()).thenReturn(Collections.singletonList(branch));
        when(global.getReverseSortedBranches()).thenReturn(Collections.singletonList(branch));
        metrics = mockStatic(MetricsPublisher.class);
        helper = mockStatic(SessionHelper.class, CALLS_REAL_METHODS);
        helper.when(() -> SessionHelper.removeBranch(any(), any(), anyBoolean()))
                .thenAnswer(i -> null);
        helper.when(() -> SessionHelper.endCommitted(any(), anyBoolean())).thenAnswer(i -> null);
        helper.when(() -> SessionHelper.endRollbacked(any(), anyBoolean())).thenAnswer(i -> null);
        helper.when(() -> SessionHelper.endCommitFailed(any(), anyBoolean())).thenAnswer(i -> null);
        helper.when(() -> SessionHelper.endRollbackFailed(any(), anyBoolean())).thenAnswer(i -> null);
    }

    @AfterEach
    void close() {
        helper.close();
        metrics.close();
    }

    @ParameterizedTest
    @EnumSource(
            value = BranchStatus.class,
            names = {"PhaseTwo_Committed", "PhaseTwo_CommitFailed_Unretryable", "PhaseTwo_CommitFailed_Retryable"})
    void commitOutcomeDrivesCleanupOrRetry(BranchStatus outcome) throws Exception {
        when(branchCore.branchCommit(global, branch)).thenReturn(outcome);
        assertEquals(outcome == BranchStatus.PhaseTwo_Committed, core.doGlobalCommit(global, false));
        if (outcome == BranchStatus.PhaseTwo_Committed) {
            helper.verify(() -> SessionHelper.removeBranch(global, branch, true));
        } else if (outcome == BranchStatus.PhaseTwo_CommitFailed_Unretryable) {
            helper.verify(() -> SessionHelper.endCommitFailed(global, false));
        } else {
            verify(global).queueToRetryCommit();
            assertFalse(core.doGlobalCommit(global, true));
            when(global.canBeCommittedAsync()).thenReturn(true);
            assertTrue(core.doGlobalCommit(global, true));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = BranchStatus.class,
            names = {"PhaseTwo_Rollbacked", "PhaseTwo_RollbackFailed_Unretryable", "PhaseTwo_RollbackFailed_Retryable"})
    void rollbackOutcomeDrivesCleanupOrRetry(BranchStatus outcome) throws Exception {
        when(branchCore.branchRollback(global, branch)).thenReturn(outcome);
        assertEquals(outcome == BranchStatus.PhaseTwo_Rollbacked, core.doGlobalRollback(global, false));
        if (outcome == BranchStatus.PhaseTwo_Rollbacked) {
            helper.verify(() -> SessionHelper.removeBranch(global, branch, true));
        } else if (outcome == BranchStatus.PhaseTwo_RollbackFailed_Unretryable) {
            helper.verify(() -> SessionHelper.endRollbackFailed(global, false));
        } else {
            verify(global).queueToRetryRollback();
            assertFalse(core.doGlobalRollback(global, true));
            verify(global, times(1)).queueToRetryRollback();
        }
    }

    @Test
    void transportExceptionsQueueOnlyFirstAttempt() throws Exception {
        when(branchCore.branchCommit(global, branch)).thenThrow(new TransactionException("offline"));
        assertThrows(TransactionException.class, () -> core.doGlobalCommit(global, false));
        verify(global).queueToRetryCommit();
        assertTrue(core.doGlobalCommit(global, true));
        when(branchCore.branchRollback(global, branch)).thenThrow(new TransactionException("offline"));
        assertThrows(TransactionException.class, () -> core.doGlobalRollback(global, false));
        verify(global).queueToRetryRollback();
        assertThrows(TransactionException.class, () -> core.doGlobalRollback(global, true));
        verify(global, times(1)).queueToRetryRollback();
    }

    @Test
    void deletionUsesBranchSpecificSuccessStatus() throws Exception {
        for (BranchType type : Arrays.asList(BranchType.AT, BranchType.TCC, BranchType.XA)) {
            when(branch.getBranchType()).thenReturn(type);
            when(branchCore.branchDelete(global, branch))
                    .thenReturn(
                            type == BranchType.AT ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_Rollbacked);
            assertTrue(core.doBranchDelete(global, branch));
            when(branchCore.branchDelete(global, branch)).thenReturn(BranchStatus.PhaseOne_Done);
            assertFalse(core.doBranchDelete(global, branch));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = BranchStatus.class,
            names = {"PhaseOne_Failed", "PhaseOne_PrepareFailed", "STOP_RETRY"})
    void failedOrStoppedBranchesDoNotRepeatRemoteWork(BranchStatus status) throws Exception {
        when(branch.getStatus()).thenReturn(status);
        assertTrue(core.doGlobalCommit(global, true));
        assertTrue(core.doGlobalRollback(global, true));
        verifyNoInteractions(branchCore);
        if (status == BranchStatus.STOP_RETRY) {
            helper.verify(() -> SessionHelper.removeBranch(global, branch, false), never());
        } else {
            helper.verify(() -> SessionHelper.removeBranch(global, branch, false), times(2));
        }
    }

    @Test
    void readOnlyXaBranchIsRemovedWithoutCommitRequest() throws Exception {
        when(branch.getBranchType()).thenReturn(BranchType.XA);
        when(branch.getStatus()).thenReturn(BranchStatus.PhaseOne_RDONLY);
        assertTrue(core.doGlobalCommit(global, false));
        verifyNoInteractions(branchCore);
        helper.verify(() -> SessionHelper.removeBranch(global, branch, true));
    }

    @Test
    void commitHandlesMissingTimedOutAndAsynchronousSessions() throws Exception {
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            assertEquals(GlobalStatus.Finished, core.commit("missing"));
            holder.when(() -> SessionHolder.findGlobalSession("xid")).thenReturn(global);
            when(global.isTimeout()).thenReturn(true);
            assertEquals(GlobalStatus.TimeoutRollbacking, core.commit("xid"));
            verify(global, never()).close();
            when(global.isTimeout()).thenReturn(false);
            when(global.getStatus()).thenReturn(GlobalStatus.Begin);
            when(global.canBeCommittedAsync()).thenReturn(true);
            doAnswer(i -> {
                        when(global.getStatus()).thenReturn(GlobalStatus.AsyncCommitting);
                        return null;
                    })
                    .when(global)
                    .asyncCommit();
            holder.when(() -> SessionHolder.lockAndExecute(any(), any()))
                    .thenAnswer(i -> ((GlobalSession.LockCallable<?>) i.getArgument(1)).call());
            assertEquals(GlobalStatus.Committed, core.commit("xid"));
            verify(global).close();
            verify(global).asyncCommit();
            verify(global).clean();
            verifyNoInteractions(branchCore);
        }
    }

    @Test
    void successfulSynchronousCommitQueuesRemainingAsyncBranches() throws Exception {
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            holder.when(() -> SessionHolder.findGlobalSession("xid")).thenReturn(global);
            holder.when(() -> SessionHolder.lockAndExecute(any(), any()))
                    .thenAnswer(i -> ((GlobalSession.LockCallable<?>) i.getArgument(1)).call());
            when(global.getStatus()).thenReturn(GlobalStatus.Begin);
            when(global.canBeCommittedAsync()).thenReturn(false, true);
            when(global.hasBranch()).thenReturn(true);
            doReturn(true).when(core).doGlobalCommit(global, false);
            assertEquals(GlobalStatus.Committed, core.commit("xid"));
            verify(global).close();
            verify(global).changeGlobalStatus(GlobalStatus.Committing);
            verify(global).clean();
            verify(core).doGlobalCommit(global, false);
            verify(global).asyncCommit();
        }
    }

    @Test
    void unretryableBranchDeletionStopsFurtherRetries() throws Exception {
        when(branchCore.branchDelete(global, branch)).thenReturn(BranchStatus.PhaseTwo_RollbackFailed_Unretryable);
        for (BranchType type : Arrays.asList(BranchType.AT, BranchType.TCC, BranchType.XA)) {
            when(branch.getBranchType()).thenReturn(type);
            assertTrue(core.doBranchDelete(global, branch));
        }
    }
}
