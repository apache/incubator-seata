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

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.server.coordinator.pipeline.PhaseTwoPipelineExecutor;
import org.apache.seata.server.coordinator.pipeline.PipelineConfig;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCoordinatorPipelineTest {

    private final List<DefaultCoordinator> coordinatorsToCleanup = new ArrayList<>();

    @BeforeAll
    static void initEnvironment() {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
    }

    @AfterEach
    void cleanup() {
        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder.when(SessionHolder::destroy).then(invocation -> null);
            for (DefaultCoordinator coordinator : coordinatorsToCleanup) {
                coordinator.destroy();
            }
        } finally {
            coordinatorsToCleanup.clear();
            MetricsManager.get().getRegistry().clearUp();
        }
    }

    private DefaultCoordinator track(DefaultCoordinator coordinator) {
        coordinatorsToCleanup.add(coordinator);
        return coordinator;
    }

    private static GlobalSession asyncSession(String xid, long gmtModifiedMs) {
        GlobalSession session = new GlobalSession("app", "group", "tx", 3000, false);
        session.setXid(xid);
        session.setStatus(GlobalStatus.AsyncCommitting);
        session.setGmtModified(new Date(gmtModifiedMs));
        return session;
    }

    private static boolean hasPipelineMetricsRegistered() {
        for (Measurement measurement : MetricsManager.get().getRegistry().measure()) {
            if (measurement.getId().getName().startsWith("seata.recovery.pipeline")) {
                return true;
            }
        }
        return false;
    }

    private static void ensureSessionHelperInitialized(RemotingServer remotingServer) {
        try {
            DefaultCoordinator.getInstance();
        } catch (IllegalArgumentException notCreated) {
            try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class);
                    MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
                storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
                sessionHolder.when(SessionHolder::destroy).then(invocation -> null);
                DefaultCoordinator bootstrap = DefaultCoordinator.getInstance(remotingServer);
                SessionHelper.forEach(Collections.emptyList(), s -> {});
                bootstrap.destroy();
            }
        }
    }

    @Test
    void disabledByDefaultAllocatesNothingAndExecutesLegacySinglePagePath() throws Exception {
        MetricsManager.get().getRegistry().clearUp();
        RemotingServer remotingServer = mock(RemotingServer.class);
        DefaultCore core = mock(DefaultCore.class);
        when(core.doGlobalCommit(any(GlobalSession.class), eq(true))).thenReturn(true);

        DefaultCoordinator productionCoordinator;
        try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class)) {
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.DB);
            productionCoordinator = track(new DefaultCoordinator(remotingServer));
        }
        assertNull(productionCoordinator.getPipelineComponents());
        assertNull(productionCoordinator.getFallbackRecoveryScanner());
        assertFalse(hasPipelineMetricsRegistered());

        DefaultCoordinator legacyCoordinator = track(
                new DefaultCoordinator(remotingServer, core, SessionMode.DB, PipelineConfig.defaults(), 2, 5000L));
        assertNull(legacyCoordinator.getPipelineComponents());
        assertNull(legacyCoordinator.getFallbackRecoveryScanner());
        assertFalse(hasPipelineMetricsRegistered());

        ensureSessionHelperInitialized(remotingServer);

        SessionManager sessionManager = mock(SessionManager.class);
        GlobalSession s1 = asyncSession("10.0.0.1:8091:101", 1000L);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenReturn(Collections.singletonList(s1));

        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            sessionHolder
                    .when(() -> SessionHolder.lockAndExecute(any(), any()))
                    .thenAnswer(inv -> ((GlobalSession.LockCallable<?>) inv.getArgument(1)).call());

            legacyCoordinator.handleAsyncCommitting();

            ArgumentCaptor<SessionCondition> conditionCaptor = ArgumentCaptor.forClass(SessionCondition.class);
            verify(sessionManager, times(1)).findGlobalSessions(conditionCaptor.capture());
            assertFalse(conditionCaptor.getValue().isPagedQuery());
            verify(core, times(1)).doGlobalCommit(s1, true);
        }
    }

    @Test
    void isolatesFileAndRaftModesEvenWhenPipelineEnabled() {
        MetricsManager.get().getRegistry().clearUp();
        RemotingServer remotingServer = mock(RemotingServer.class);
        DefaultCore core = mock(DefaultCore.class);
        PipelineConfig enabledConfig = PipelineConfig.builder().enabled(true).build();

        DefaultCoordinator fileCoordinator =
                track(new DefaultCoordinator(remotingServer, core, SessionMode.FILE, enabledConfig, 10, 5000L));
        assertNull(fileCoordinator.getPipelineComponents());
        assertNull(fileCoordinator.getFallbackRecoveryScanner());
        assertFalse(hasPipelineMetricsRegistered());

        DefaultCoordinator raftCoordinator =
                track(new DefaultCoordinator(remotingServer, core, SessionMode.RAFT, enabledConfig, 10, 5000L));
        assertNull(raftCoordinator.getPipelineComponents());
        assertNull(raftCoordinator.getFallbackRecoveryScanner());
        assertFalse(hasPipelineMetricsRegistered());

        try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class);
                MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            pipelineConfigMock.when(PipelineConfig::fromConfiguration).thenReturn(enabledConfig);
            RaftCoordinator realRaftCoordinator = (RaftCoordinator) track(new RaftCoordinator(remotingServer));
            assertNull(realRaftCoordinator.getPipelineComponents());
            assertNull(realRaftCoordinator.getFallbackRecoveryScanner());
            assertFalse(hasPipelineMetricsRegistered());
        }
    }

    @Test
    void dbModeLifecycleDrainsMultiplePagesWithRetryingTrueAndShutsDownCleanly() throws Exception {
        RemotingServer remotingServer = mock(RemotingServer.class);
        DefaultCore core = mock(DefaultCore.class);
        CountDownLatch commitLatch = new CountDownLatch(5);
        when(core.doGlobalCommit(any(GlobalSession.class), anyBoolean())).thenAnswer(inv -> {
            commitLatch.countDown();
            return true;
        });

        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(2)
                .queueCapacity(16)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();

        DefaultCoordinator coordinator =
                track(new DefaultCoordinator(remotingServer, core, SessionMode.DB, config, 2, 5000L));
        assertNotNull(coordinator.getPipelineComponents());
        assertNotNull(coordinator.getFallbackRecoveryScanner());
        assertNull(coordinator.getPipelineComponents().getCleaner());
        assertTrue(hasPipelineMetricsRegistered());

        PhaseTwoPipelineExecutor executor = coordinator.getPipelineComponents().getExecutor();
        assertFalse(executor.isRunning());

        coordinator.init();
        assertTrue(executor.isRunning());

        List<GlobalSession> rows = Arrays.asList(
                asyncSession("10.0.0.1:8091:201", 1000L),
                asyncSession("10.0.0.1:8091:202", 1000L),
                asyncSession("10.0.0.1:8091:203", 2000L),
                asyncSession("10.0.0.1:8091:204", 3000L),
                asyncSession("10.0.0.1:8091:205", 4000L));
        rows.sort(Comparator.comparing((GlobalSession s) -> s.getGmtModified().getTime())
                .thenComparing(GlobalSession::getXid));

        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition cond = invocation.getArgument(0);
            List<GlobalSession> page = new ArrayList<>();
            int limit = cond.getPageSize() != null ? cond.getPageSize() : 2;
            for (GlobalSession row : rows) {
                if (cond.hasCursor()) {
                    long rowTs = row.getGmtModified().getTime();
                    long curTs = cond.getCursorGmtModified().getTime();
                    boolean after =
                            rowTs > curTs || (rowTs == curTs && row.getXid().compareTo(cond.getCursorXid()) > 0);
                    if (!after) {
                        continue;
                    }
                }
                page.add(row);
                if (page.size() >= limit) {
                    break;
                }
            }
            return page;
        });

        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            sessionHolder.when(SessionHolder::destroy).then(invocation -> null);

            coordinator.handleAsyncCommitting();
            assertTrue(commitLatch.await(3, TimeUnit.SECONDS));

            verify(sessionManager, times(3)).findGlobalSessions(any(SessionCondition.class));
            verify(core, times(5)).doGlobalCommit(any(GlobalSession.class), eq(true));
            verify(core, never()).doGlobalCommit(any(GlobalSession.class), eq(false));

            coordinator.destroy();
            assertFalse(executor.isRunning());
        }
    }

    @Test
    void redisModeContinuouslyPagesPastShortPagesAndInvokesCoreWithRetryingTrue() throws Exception {
        RemotingServer remotingServer = mock(RemotingServer.class);
        DefaultCore core = mock(DefaultCore.class);
        CountDownLatch commitLatch = new CountDownLatch(3);
        when(core.doGlobalCommit(any(GlobalSession.class), anyBoolean())).thenAnswer(inv -> {
            commitLatch.countDown();
            return true;
        });

        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(2)
                .queueCapacity(16)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();

        DefaultCoordinator coordinator =
                track(new DefaultCoordinator(remotingServer, core, SessionMode.REDIS, config, 2, 5000L));
        coordinator.init();

        Map<Integer, List<GlobalSession>> pages = new HashMap<>();
        pages.put(0, Collections.singletonList(asyncSession("10.0.0.1:8091:301", 1000L)));
        pages.put(2, Arrays.asList(asyncSession("10.0.0.1:8091:302", 2000L), asyncSession("10.0.0.1:8091:303", 3000L)));
        pages.put(4, Collections.emptyList());

        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition cond = invocation.getArgument(0);
            return pages.getOrDefault(cond.getOffset(), Collections.emptyList());
        });

        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            sessionHolder.when(SessionHolder::destroy).then(invocation -> null);

            coordinator.handleAsyncCommitting();
            assertTrue(commitLatch.await(3, TimeUnit.SECONDS));

            verify(sessionManager, times(3)).findGlobalSessions(any(SessionCondition.class));
            verify(core, times(3)).doGlobalCommit(any(GlobalSession.class), eq(true));
            verify(core, never()).doGlobalCommit(any(GlobalSession.class), eq(false));

            coordinator.destroy();
            assertFalse(coordinator.getPipelineComponents().getExecutor().isRunning());
        }
    }

    @Test
    void productionConstructorWiresPipelineUnderDbModeWhenEnabled() {
        RemotingServer remotingServer = mock(RemotingServer.class);
        PipelineConfig enabledConfig = PipelineConfig.builder().enabled(true).build();

        try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class);
                MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.DB);
            pipelineConfigMock.when(PipelineConfig::fromConfiguration).thenReturn(enabledConfig);

            DefaultCoordinator coordinator = track(new DefaultCoordinator(remotingServer));
            assertNotNull(coordinator.getPipelineComponents());
            assertNotNull(coordinator.getFallbackRecoveryScanner());
            assertNull(coordinator.getPipelineComponents().getCleaner());
            assertEquals(
                    SessionMode.DB, coordinator.getFallbackRecoveryScanner().getSessionMode());
        }
    }
}
