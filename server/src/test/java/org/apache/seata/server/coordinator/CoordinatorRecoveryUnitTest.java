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

import io.netty.channel.Channel;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.transaction.*;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoordinatorRecoveryUnitTest extends BaseSpringBootTest {
    private DefaultCoordinator coordinator;
    private DefaultCore core;
    private SessionManager manager;
    private ScheduledExecutorService scheduler;
    private RemotingServer remoting;
    private MockedStatic<SessionHolder> holder;

    @BeforeEach
    void open() {
        coordinator = mock(DefaultCoordinator.class, CALLS_REAL_METHODS);
        core = mock(DefaultCore.class);
        manager = mock(SessionManager.class);
        scheduler = mock(ScheduledThreadPoolExecutor.class);
        remoting = mock(RemotingServer.class);
        ReflectionTestUtils.setField(coordinator, "core", core);
        ReflectionTestUtils.setField(coordinator, "syncProcessing", scheduler);
        coordinator.setRemotingServer(remoting);
        holder = mockStatic(SessionHolder.class);
        holder.when(SessionHolder::getRootSessionManager).thenReturn(manager);
        holder.when(() -> SessionHolder.lockAndExecute(any(), any()))
                .thenAnswer(i -> ((GlobalSession.LockCallable<?>) i.getArgument(1)).call());
    }

    @AfterEach
    void close() {
        holder.close();
        MDC.remove(RootContext.MDC_KEY_XID);
    }

    private GlobalSession session(GlobalStatus status) {
        GlobalSession s = mock(GlobalSession.class);
        when(s.getXid()).thenReturn("host:8091:42");
        when(s.getStatus()).thenReturn(status);
        when(s.getBeginTime()).thenReturn(System.currentTimeMillis());
        when(s.timeToDeadSession()).thenReturn(0L);
        when(manager.findGlobalSessions(any())).thenReturn(new ArrayList<>(Collections.singletonList(s)));
        return s;
    }

    @Test
    void scheduledRecoveryInvokesCoreAndReschedulesAfterFailure() throws Exception {
        GlobalSession s = session(GlobalStatus.Committing);
        coordinator.handleCommittingByScheduled();
        verify(core).doGlobalCommit(s, true);
        verify(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        doThrow(new TransactionException("failed")).when(core).doGlobalCommit(s, true);
        assertDoesNotThrow(coordinator::handleCommittingByScheduled);
        coordinator.handleRollbackingByScheduled();
        verify(core).doGlobalRollback(s, true);
        doThrow(new TransactionException("failed")).when(core).doGlobalRollback(s, true);
        assertDoesNotThrow(coordinator::handleRollbackingByScheduled);
        coordinator.handleEndStatesByScheduled();
        verify(scheduler, times(5)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void retryAndAsyncRecoveryAttemptCoreAndContainTransactionFailures() throws Exception {
        GlobalSession s = session(GlobalStatus.CommitRetrying);
        coordinator.handleRetryCommitting();
        coordinator.handleRetryRollbacking();
        coordinator.handleAsyncCommitting();
        verify(core, times(2)).doGlobalCommit(s, true);
        verify(core).doGlobalRollback(s, true);
        doThrow(new TransactionException("failed")).when(core).doGlobalCommit(s, true);
        doThrow(new TransactionException("failed")).when(core).doGlobalRollback(s, true);
        assertDoesNotThrow(coordinator::handleRetryCommitting);
        assertDoesNotThrow(coordinator::handleRetryRollbacking);
        assertDoesNotThrow(coordinator::handleAsyncCommitting);
    }

    @Test
    void commitAndRollbackResponsesReflectCoreResults() throws Exception {
        GlobalCommitRequest commit = new GlobalCommitRequest();
        commit.setXid("xid");
        GlobalCommitResponse committed = new GlobalCommitResponse();
        when(core.commit("xid")).thenReturn(GlobalStatus.Committed);
        coordinator.doGlobalCommit(commit, committed, null);
        assertEquals(GlobalStatus.Committed, committed.getGlobalStatus());
        GlobalRollbackRequest rollback = new GlobalRollbackRequest();
        rollback.setXid("xid");
        GlobalRollbackResponse rolled = new GlobalRollbackResponse();
        when(core.rollback("xid")).thenReturn(GlobalStatus.Rollbacked);
        coordinator.doGlobalRollback(rollback, rolled, null);
        assertEquals(GlobalStatus.Rollbacked, rolled.getGlobalStatus());
    }

    @Test
    void undoCleanupTargetsEachResourceAndContinuesAfterTransportFailure() throws Exception {
        Channel a = mock(Channel.class), b = mock(Channel.class);
        Map<String, Channel> channels = new LinkedHashMap<>();
        channels.put("db1", a);
        channels.put("db2", b);
        try (MockedStatic<ChannelManager> channelManager = mockStatic(ChannelManager.class)) {
            channelManager.when(ChannelManager::getRmChannels).thenReturn(channels);
            doThrow(new IllegalStateException("offline")).when(remoting).sendAsyncRequest(eq(a), any());
            coordinator.undoLogDelete();
            verify(remoting)
                    .sendAsyncRequest(
                            eq(b),
                            argThat(r -> r instanceof UndoLogDeleteRequest
                                    && "db2".equals(((UndoLogDeleteRequest) r).getResourceId())));
        }
    }
}
