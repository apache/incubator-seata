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
package org.apache.seata.server.transaction.saga;

import io.netty.channel.Channel;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.*;
import org.apache.seata.core.protocol.transaction.*;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SagaCoreUnitTest extends BaseSpringBootTest {
    private RemotingServer remoting;
    private SagaCore core;
    private GlobalSession session;
    private SessionManager manager;
    private MockedStatic<SessionHelper> helper;
    private MockedStatic<SessionHolder> holder;

    @BeforeEach
    void open() {
        remoting = mock(RemotingServer.class);
        core = spy(new SagaCore(remoting));
        session = mock(GlobalSession.class);
        when(session.getXid()).thenReturn("host:8091:42");
        when(session.getApplicationId()).thenReturn("app");
        when(session.getTransactionServiceGroup()).thenReturn("group");
        when(session.getStatus()).thenReturn(GlobalStatus.Begin);
        manager = mock(SessionManager.class);
        holder = mockStatic(SessionHolder.class);
        holder.when(SessionHolder::getRootSessionManager).thenReturn(manager);
        helper = mockStatic(SessionHelper.class);
        helper.when(() -> SessionHelper.newBranch(any(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(new BranchSession());
    }

    @AfterEach
    void close() {
        helper.close();
        holder.close();
    }

    @Test
    void sendUsesSagaResourceChannelAndReturnsRetryableWhenUnavailable() throws Exception {
        BranchCommitRequest commit = new BranchCommitRequest();
        BranchRollbackRequest rollback = new BranchRollbackRequest();
        try (MockedStatic<ChannelManager> channels = mockStatic(ChannelManager.class)) {
            channels.when(ChannelManager::getRmChannels).thenReturn(Collections.emptyMap());
            assertEquals(BranchStatus.PhaseTwo_CommitFailed_Retryable, core.branchCommitSend(commit, session, null));
            assertEquals(
                    BranchStatus.PhaseTwo_RollbackFailed_Retryable, core.branchRollbackSend(rollback, session, null));
            Channel channel = mock(Channel.class);
            channels.when(ChannelManager::getRmChannels).thenReturn(Collections.singletonMap("other", channel));
            assertEquals(BranchStatus.PhaseTwo_CommitFailed_Retryable, core.branchCommitSend(commit, session, null));
            assertEquals(
                    BranchStatus.PhaseTwo_RollbackFailed_Retryable, core.branchRollbackSend(rollback, session, null));
            channels.when(ChannelManager::getRmChannels).thenReturn(Collections.singletonMap("app#group", channel));
            BranchCommitResponse cr = new BranchCommitResponse();
            cr.setBranchStatus(BranchStatus.PhaseTwo_Committed);
            BranchRollbackResponse rr = new BranchRollbackResponse();
            rr.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
            when(remoting.sendSyncRequest(channel, commit)).thenReturn(cr);
            when(remoting.sendSyncRequest(channel, rollback)).thenReturn(rr);
            assertEquals(BranchStatus.PhaseTwo_Committed, core.branchCommitSend(commit, session, null));
            assertEquals(BranchStatus.PhaseTwo_Rollbacked, core.branchRollbackSend(rollback, session, null));
            assertEquals(BranchType.SAGA, core.getHandleBranchType());
            core.globalSessionStatusCheck(session);
            assertThrows(ShouldNeverHappenException.class, () -> core.branchDelete(session, new BranchSession()));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = BranchStatus.class,
            names = {
                "PhaseTwo_Committed",
                "PhaseTwo_Rollbacked",
                "PhaseTwo_RollbackFailed_Retryable",
                "PhaseOne_Failed",
                "PhaseTwo_CommitFailed_Unretryable",
                "PhaseTwo_CommitFailed_Retryable"
            })
    void commitRoutesOutcomeToCleanupOrRecovery(BranchStatus status) throws Exception {
        doReturn(status).when(core).branchCommit(eq(session), any(BranchSession.class));
        assertEquals(status == BranchStatus.PhaseTwo_Committed, core.doGlobalCommit(session, false));
        switch (status) {
            case PhaseTwo_Committed:
                helper.verify(() -> SessionHelper.removeAllBranch(session, true));
                break;
            case PhaseTwo_Rollbacked:
                helper.verify(() -> SessionHelper.endRollbacked(session, false));
                break;
            case PhaseTwo_RollbackFailed_Retryable:
                verify(manager).removeGlobalSession(session);
                verify(session).queueToRetryRollback();
                break;
            case PhaseOne_Failed:
                verify(session).changeGlobalStatus(GlobalStatus.Finished);
                verify(session).end();
                break;
            case PhaseTwo_CommitFailed_Unretryable:
                helper.verify(() -> SessionHelper.endCommitFailed(session, false));
                when(session.canBeCommittedAsync()).thenReturn(true);
                assertTrue(core.doGlobalCommit(session, true));
                break;
            default:
                verify(session).queueToRetryCommit();
                assertFalse(core.doGlobalCommit(session, true));
                verify(session, times(1)).queueToRetryCommit();
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = BranchStatus.class,
            names = {
                "PhaseTwo_Rollbacked",
                "PhaseTwo_RollbackFailed_Unretryable",
                "PhaseTwo_CommitFailed_Retryable",
                "PhaseTwo_RollbackFailed_Retryable"
            })
    void rollbackRoutesOutcomeToCleanupOrRecovery(BranchStatus status) throws Exception {
        doReturn(status).when(core).branchRollback(eq(session), any(BranchSession.class));
        assertEquals(status == BranchStatus.PhaseTwo_Rollbacked, core.doGlobalRollback(session, false));
        switch (status) {
            case PhaseTwo_Rollbacked:
                helper.verify(() -> SessionHelper.removeAllBranch(session, true));
                break;
            case PhaseTwo_RollbackFailed_Unretryable:
                helper.verify(() -> SessionHelper.endRollbackFailed(session, false));
                break;
            case PhaseTwo_CommitFailed_Retryable:
                verify(manager).removeGlobalSession(session);
                verify(session).queueToRetryCommit();
                break;
            default:
                verify(session).queueToRetryRollback();
                assertFalse(core.doGlobalRollback(session, true));
                verify(session, times(1)).queueToRetryRollback();
        }
    }

    @Test
    void transportFailuresQueueRecoveryOnlyOnFirstAttempt() throws Exception {
        doThrow(new TransactionException("offline")).when(core).branchCommit(eq(session), any(BranchSession.class));
        assertThrows(TransactionException.class, () -> core.doGlobalCommit(session, false));
        assertThrows(TransactionException.class, () -> core.doGlobalCommit(session, true));
        verify(session).queueToRetryRollback();
        clearInvocations(session);
        doThrow(new TransactionException("offline")).when(core).branchRollback(eq(session), any(BranchSession.class));
        assertThrows(TransactionException.class, () -> core.doGlobalRollback(session, false));
        assertThrows(TransactionException.class, () -> core.doGlobalRollback(session, true));
        verify(session).queueToRetryRollback();
    }

    @ParameterizedTest
    @EnumSource(GlobalStatus.class)
    void reportRoutesFinalAndRetryStatuses(GlobalStatus status) throws Exception {
        core.doGlobalReport(session, session.getXid(), status);
        if (status == GlobalStatus.Committed) {
            helper.verify(() -> SessionHelper.endCommitted(session, false));
        } else if (status == GlobalStatus.Rollbacked || status == GlobalStatus.Finished) {
            helper.verify(() -> SessionHelper.endRollbacked(session, false));
        } else {
            verify(session).changeGlobalStatus(status);
            if (status == GlobalStatus.RollbackRetrying
                    || status == GlobalStatus.TimeoutRollbackRetrying
                    || status == GlobalStatus.UnKnown) {
                verify(session).queueToRetryRollback();
            } else if (status == GlobalStatus.CommitRetrying) {
                verify(session).queueToRetryCommit();
            } else {
                verify(session, never()).queueToRetryCommit();
                verify(session, never()).queueToRetryRollback();
            }
        }
    }
}
