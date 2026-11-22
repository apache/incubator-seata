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
package org.apache.seata.server.coordinator.pipeline;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.seata.common.Constants;
import org.apache.seata.common.XID;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.store.DistributedLocker;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.coordinator.AbstractCore;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.coordinator.DefaultCoordinatorTest;
import org.apache.seata.server.coordinator.DefaultCore;
import org.apache.seata.server.coordinator.RaftCoordinator;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.db.lock.DataBaseLockManager;
import org.apache.seata.server.storage.db.lock.DataBaseLocker;
import org.apache.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import org.apache.seata.server.storage.db.session.DataBaseSessionManager;
import org.apache.seata.server.storage.db.store.DataBaseTransactionStoreManager;
import org.apache.seata.server.storage.db.store.LogStoreDataBaseDAO;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.transaction.at.ATCore;
import org.apache.seata.server.transaction.saga.SagaCore;
import org.apache.seata.server.transaction.tcc.TccCore;
import org.apache.seata.server.transaction.xa.XACore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushModePipelineRegressionTest {

    @TempDir
    static Path databaseDirectory;

    private static BasicDataSource dataSource;
    private static RemotingServer remotingServer;

    private final List<DefaultCoordinator> coordinatorsToCleanup = new ArrayList<>();

    @BeforeAll
    static void initSuite() throws Exception {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
        XID.setIpAddress("127.0.0.1");
        XID.setPort(8091);

        remotingServer = new DefaultCoordinatorTest.MockServerMessageSender();
        ensureSessionHelperInitialized(remotingServer);

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:" + databaseDirectory.resolve("push_mode_regression") + ";MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        prepareSchema(dataSource);
    }

    @AfterAll
    static void closeDataSource() throws SQLException {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @BeforeEach
    void cleanTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement()) {
            st.execute("DELETE FROM lock_table");
            st.execute("DELETE FROM branch_table");
            st.execute("DELETE FROM global_table");
        }
    }

    @AfterEach
    void cleanupCoordinators() {
        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder.when(SessionHolder::destroy).then(invocation -> null);
            for (DefaultCoordinator coordinator : coordinatorsToCleanup) {
                coordinator.destroy();
            }
        } finally {
            coordinatorsToCleanup.clear();
            if (MetricsManager.get().getRegistry() != null) {
                MetricsManager.get().getRegistry().clearUp();
            }
        }
    }

    private static void prepareSchema(BasicDataSource ds) throws SQLException {
        try (Connection conn = ds.getConnection();
                Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS global_table ("
                    + "xid varchar(128) primary key, transaction_id bigint, status int, "
                    + "application_id varchar(32), transaction_service_group varchar(32), "
                    + "transaction_name varchar(128), timeout int, begin_time bigint, "
                    + "application_data varchar(500), gmt_create TIMESTAMP(6), gmt_modified TIMESTAMP(6))");
            s.execute("CREATE TABLE IF NOT EXISTS branch_table ("
                    + "xid varchar(128), transaction_id bigint, branch_id bigint primary key, "
                    + "resource_group_id varchar(32), resource_id varchar(256), lock_key varchar(256), "
                    + "branch_type varchar(32), status int, client_id varchar(128), "
                    + "application_data varchar(500), gmt_create TIMESTAMP(6), gmt_modified TIMESTAMP(6))");
            s.execute("CREATE TABLE IF NOT EXISTS lock_table ("
                    + "row_key varchar(128) primary key, xid varchar(128), transaction_id bigint, "
                    + "branch_id bigint, resource_id varchar(256), table_name varchar(32), "
                    + "pk varchar(36), status int, gmt_create TIMESTAMP(6), gmt_modified TIMESTAMP(6))");
        }
    }

    private static void ensureSessionHelperInitialized(RemotingServer server) {
        try {
            DefaultCoordinator.getInstance();
        } catch (IllegalArgumentException notCreated) {
            try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class, CALLS_REAL_METHODS);
                    MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
                storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
                sessionHolder.when(SessionHolder::destroy).then(invocation -> null);
                DefaultCoordinator bootstrap = DefaultCoordinator.getInstance(server);
                SessionHelper.forEach(Collections.emptyList(), s -> {});
                bootstrap.destroy();
            }
        }
    }

    private DefaultCoordinator track(DefaultCoordinator coordinator) {
        coordinatorsToCleanup.add(coordinator);
        return coordinator;
    }

    private static DefaultCoordinator newProductionCoordinator(RemotingServer server) {
        return new DefaultCoordinator(server) {};
    }

    private static DefaultCoordinator newCoordinator(
            RemotingServer server,
            DefaultCore core,
            SessionMode sessionMode,
            PipelineConfig pipelineConfig,
            int scanPageSize,
            long maxTickDurationMs)
            throws Exception {
        Constructor<DefaultCoordinator> ctor = DefaultCoordinator.class.getDeclaredConstructor(
                RemotingServer.class,
                DefaultCore.class,
                SessionMode.class,
                PipelineConfig.class,
                int.class,
                long.class);
        ctor.setAccessible(true);
        return ctor.newInstance(server, core, sessionMode, pipelineConfig, scanPageSize, maxTickDurationMs);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static void awaitCondition(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10L);
        }
        assertTrue(condition.getAsBoolean(), "Condition not satisfied within " + timeoutMs + "ms");
    }

    private static int countRows(String table) throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private static final class H2Env implements AutoCloseable {
        private final LogStoreDataBaseDAO spyLogStore;
        private final DataBaseSessionManager sessionManager;
        private final DataBaseLockManager lockManager;
        private final Object oldRootSessionManager;
        private final Object oldSessionManagerMap;
        private final Object oldDistributedLocker;
        private final Object oldLockManager;
        private final Map<BranchType, AbstractCore> savedCores = new EnumMap<>(BranchType.class);

        private H2Env() throws Exception {
            LogStoreDataBaseDAO realLogStore = new LogStoreDataBaseDAO(dataSource);
            realLogStore.setDbType("h2");
            realLogStore.setGlobalTable("global_table");
            realLogStore.setBranchTable("branch_table");
            this.spyLogStore = spy(realLogStore);

            DataBaseTransactionStoreManager storeManager =
                    mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
            storeManager.setLogStore(spyLogStore);
            storeManager.setLogQueryLimit(100);

            this.sessionManager = new DataBaseSessionManager();
            this.sessionManager.setTransactionStoreManager(storeManager);

            LockStoreDataBaseDAO lockDao = new LockStoreDataBaseDAO(dataSource);
            lockDao.setDbType("h2");
            lockDao.setLockTable("lock_table");
            DataBaseLocker locker = new DataBaseLocker();
            locker.setLockStore(lockDao);
            this.lockManager = new DataBaseLockManager();
            Field lockerField = DataBaseLockManager.class.getDeclaredField("locker");
            lockerField.setAccessible(true);
            lockerField.set(this.lockManager, locker);

            Field rootField = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
            rootField.setAccessible(true);
            this.oldRootSessionManager = rootField.get(null);
            rootField.set(null, this.sessionManager);

            Field mapField = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
            mapField.setAccessible(true);
            this.oldSessionManagerMap = mapField.get(null);
            mapField.set(null, null);

            Field distLockerField = SessionHolder.class.getDeclaredField("DISTRIBUTED_LOCKER");
            distLockerField.setAccessible(true);
            this.oldDistributedLocker = distLockerField.get(null);
            DistributedLocker noOpDistLocker = mock(DistributedLocker.class);
            when(noOpDistLocker.acquireLock(any())).thenReturn(false);
            distLockerField.set(null, noOpDistLocker);

            Field lockMgrField = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
            lockMgrField.setAccessible(true);
            this.oldLockManager = lockMgrField.get(null);
            lockMgrField.set(null, this.lockManager);
        }

        private DefaultCore createCoreWithSavedCores() {
            DefaultCore core = new DefaultCore(remotingServer);
            for (BranchType type : new BranchType[] {BranchType.AT, BranchType.TCC, BranchType.XA, BranchType.SAGA}) {
                try {
                    savedCores.put(type, core.getCore(type));
                } catch (Exception ignored) {
                    // ignore unregistered branch types
                }
            }
            return core;
        }

        private void registerCoreMock(DefaultCore core, BranchType type, AbstractCore replacement) {
            if (!savedCores.containsKey(type)) {
                try {
                    savedCores.put(type, core.getCore(type));
                } catch (Exception ignored) {
                    // ignore
                }
            }
            core.mockCore(type, replacement);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void close() throws Exception {
            Field coreMapField = DefaultCore.class.getDeclaredField("CORE_MAP");
            coreMapField.setAccessible(true);
            Map<BranchType, AbstractCore> coreMap = (Map<BranchType, AbstractCore>) coreMapField.get(null);
            for (Map.Entry<BranchType, AbstractCore> entry : savedCores.entrySet()) {
                if (entry.getValue() != null) {
                    coreMap.put(entry.getKey(), entry.getValue());
                }
            }

            Field rootField = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
            rootField.setAccessible(true);
            rootField.set(null, oldRootSessionManager);

            Field mapField = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
            mapField.setAccessible(true);
            mapField.set(null, oldSessionManagerMap);

            Field distLockerField = SessionHolder.class.getDeclaredField("DISTRIBUTED_LOCKER");
            distLockerField.setAccessible(true);
            distLockerField.set(null, oldDistributedLocker);

            Field lockMgrField = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
            lockMgrField.setAccessible(true);
            lockMgrField.set(null, oldLockManager);
        }
    }

    @Test
    void configurationMatrixGatesPipelineAndPusherStrictly() throws Exception {
        PipelineConfig offConfig = PipelineConfig.defaults();
        PipelineConfig pullOnlyConfig = PipelineConfig.builder()
                .enabled(true)
                .pushEnabled(false)
                .scanQuietWindowMs(0L)
                .build();
        PipelineConfig fullPushConfig = PipelineConfig.builder()
                .enabled(true)
                .pushEnabled(true)
                .rpcPoolSize(2)
                .queueCapacity(16)
                .cleanerBatchSize(4)
                .cleanerFlushIntervalMs(50L)
                .scanQuietWindowMs(0L)
                .build();

        try (MockedStatic<StoreConfig> storeConfig = mockStatic(StoreConfig.class, CALLS_REAL_METHODS);
                MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            // 1. enable=false, pushEnable=false under DB
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.DB);
            pipelineConfigMock.when(PipelineConfig::fromConfiguration).thenReturn(offConfig);
            DefaultCoordinator offCoord = track(newProductionCoordinator(remotingServer));
            assertNull(readField(offCoord, DefaultCoordinator.class, "pipelineComponents"));
            assertNull(readField(offCoord, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore offCore = readField(offCoord, DefaultCoordinator.class, "core");
            assertNull(offCore.getAsyncCommitPusher());

            // 2. enable=true, pushEnable=false under DB -> cleaner active, pusher NOT registered
            pipelineConfigMock.when(PipelineConfig::fromConfiguration).thenReturn(pullOnlyConfig);
            DefaultCoordinator pullCoord = track(newProductionCoordinator(remotingServer));
            PipelineComponents pullComps = readField(pullCoord, DefaultCoordinator.class, "pipelineComponents");
            assertNotNull(pullComps);
            assertNotNull(pullComps.getCleaner());
            assertNotNull(readField(pullCoord, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore pullCore = readField(pullCoord, DefaultCoordinator.class, "core");
            assertNull(pullCore.getAsyncCommitPusher());

            // 3. enable=true, pushEnable=true under FILE and RAFT -> completely disabled
            pipelineConfigMock.when(PipelineConfig::fromConfiguration).thenReturn(fullPushConfig);
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
            DefaultCoordinator fileCoord = track(newProductionCoordinator(remotingServer));
            assertNull(readField(fileCoord, DefaultCoordinator.class, "pipelineComponents"));
            assertNull(readField(fileCoord, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore fileCore = readField(fileCoord, DefaultCoordinator.class, "core");
            assertNull(fileCore.getAsyncCommitPusher());

            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            DefaultCoordinator raftCoord = track(newProductionCoordinator(remotingServer));
            assertNull(readField(raftCoord, DefaultCoordinator.class, "pipelineComponents"));
            assertNull(readField(raftCoord, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore raftCore = readField(raftCoord, DefaultCoordinator.class, "core");
            assertNull(raftCore.getAsyncCommitPusher());

            RaftCoordinator realRaft = (RaftCoordinator) track(new RaftCoordinator(remotingServer));
            assertNull(readField(realRaft, DefaultCoordinator.class, "pipelineComponents"));
            assertNull(readField(realRaft, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore realRaftCore = readField(realRaft, DefaultCoordinator.class, "core");
            assertNull(realRaftCore.getAsyncCommitPusher());

            // 4. enable=true, pushEnable=true under REDIS -> full push + batch cleaner active
            storeConfig.when(StoreConfig::getSessionMode).thenReturn(SessionMode.REDIS);
            DefaultCoordinator redisCoord = track(newProductionCoordinator(remotingServer));
            PipelineComponents redisComps = readField(redisCoord, DefaultCoordinator.class, "pipelineComponents");
            assertNotNull(redisComps);
            assertNotNull(redisComps.getCleaner());
            assertNotNull(readField(redisCoord, DefaultCoordinator.class, "fallbackRecoveryScanner"));
            DefaultCore redisCore = readField(redisCoord, DefaultCoordinator.class, "core");
            assertNotNull(redisCore.getAsyncCommitPusher());
            try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
                sessionHolder.when(SessionHolder::destroy).then(invocation -> null);
                redisCoord.destroy();
            }
            assertNull(redisCore.getAsyncCommitPusher());
            assertFalse(redisComps.getExecutor().isRunning());
            assertFalse(redisComps.getCleaner().isRunning());
        }

        // Verify pull-only mode (enable=true, pushEnable=false) does not enqueue during foreground commit()
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyAtCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, pullOnlyConfig, 10, 5000L));
            coordinator.init();
            assertNull(core.getAsyncCommitPusher());

            String xid = core.begin("app", "group", "pull-only-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "client-1", xid, "appData", "tb:1");
            assertEquals(GlobalStatus.Committed, core.commit(xid));

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            assertEquals(0L, comps.getMetrics().getEnqueuedCount());
            assertEquals(1, countRows("global_table"));
            assertEquals(1, countRows("branch_table"));

            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");
            FallbackRecoveryScanner.ScanSummary summary = scanner.scanAsyncCommitting();
            assertEquals(1, summary.getDispatched());

            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0 && countRows("branch_table") == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);
            awaitCondition(() -> comps.getInFlightRegistry().size() == 0, 3000L);
        }
    }

    @Test
    void isolatesSagaTccAndXaFromPushModeAndBatchCleaner() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyAtCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            TccCore spyTccCore = spy(new TccCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyTccCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.TCC, spyTccCore);

            XACore spyXaCore = spy(new XACore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyXaCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.XA, spyXaCore);

            SagaCore spySagaCore = spy(new SagaCore(remotingServer));
            doAnswer(inv -> {
                        GlobalSession gs = inv.getArgument(0);
                        SessionHelper.removeAllBranch(gs, false);
                        return true;
                    })
                    .when(spySagaCore)
                    .doGlobalCommit(any(), anyBoolean());
            env.registerCoreMock(core, BranchType.SAGA, spySagaCore);

            List<String> pushedXids = new CopyOnWriteArrayList<>();
            core.setAsyncCommitPusher(session -> pushedXids.add(session.getXid()));

            // 1. Zero-branch Saga ($Saga_ prefix) must NEVER be pushed on commit()
            String emptySagaXid = core.begin("app", "group", Constants.SAGA_TRANS_NAME_PREFIX + "emptyFlow", 60000);
            GlobalStatus emptySagaStatus = core.commit(emptySagaXid);
            assertEquals(GlobalStatus.Committed, emptySagaStatus);
            assertTrue(pushedXids.isEmpty());

            // And doGlobalCommitForPipeline on Saga runs legacy endCommitted and returns false (never sent to cleaner)
            GlobalSession persistedEmptySaga = SessionHolder.findGlobalSession(emptySagaXid);
            assertNotNull(persistedEmptySaga);
            assertFalse(core.doGlobalCommitForPipeline(persistedEmptySaga));
            assertNull(SessionHolder.findGlobalSession(emptySagaXid));

            // 2. Saga with SAGA branch must NEVER be pushed on commit() or sent to cleaner
            String sagaXid = core.begin("app", "group", Constants.SAGA_TRANS_NAME_PREFIX + "orderFlow", 60000);
            GlobalSession sagaSession = SessionHolder.findGlobalSession(sagaXid);
            BranchSession sagaBranch =
                    SessionHelper.newBranchByGlobal(sagaSession, BranchType.SAGA, "saga-rm", null, "c1");
            sagaSession.addBranch(sagaBranch);
            assertEquals(GlobalStatus.Committed, core.commit(sagaXid));
            assertTrue(pushedXids.isEmpty());

            // 3. Pure TCC must NEVER be pushed
            String tccXid = core.begin("app", "group", "tcc-tx", 60000);
            core.branchRegister(BranchType.TCC, "tcc-rm", "c1", tccXid, "{}", null);
            assertEquals(GlobalStatus.Committed, core.commit(tccXid));
            assertTrue(pushedXids.isEmpty());

            // 4. Pure XA must NEVER be pushed
            String xaXid = core.begin("app", "group", "xa-tx", 60000);
            core.branchRegister(BranchType.XA, "xa-rm", "c1", xaXid, "{}", null);
            assertEquals(GlobalStatus.Committed, core.commit(xaXid));
            assertTrue(pushedXids.isEmpty());

            // 5. Mixed TCC + AT: TCC commits synchronously, remaining AT branch enters Path 2 push
            String mixedXid = core.begin("app", "group", "mixed-tcc-at", 60000);
            core.branchRegister(BranchType.TCC, "tcc-rm", "c1", mixedXid, "{}", null);
            core.branchRegister(BranchType.AT, "at-rm", "c1", mixedXid, "{}", "tb:99");
            assertEquals(GlobalStatus.Committed, core.commit(mixedXid));
            assertEquals(Collections.singletonList(mixedXid), pushedXids);
        }
    }

    @Test
    void endToEndH2PushModeBatchesDeletesAndEliminatesCommittedStatusUpdate() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyAtCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(4)
                    .queueCapacity(64)
                    .cleanerBatchSize(12)
                    .cleanerFlushIntervalMs(150L)
                    .cleanerBufferCapacity(64)
                    .dbMaxConcurrency(4)
                    .scanQuietWindowMs(0L)
                    .build();

            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 10, 5000L));
            coordinator.init();
            assertNotNull(core.getAsyncCommitPusher());

            int txCount = 12;
            for (int i = 0; i < txCount; i++) {
                String xid = core.begin("app", "group", "at-push-tx-" + i, 60000);
                core.branchRegister(BranchType.AT, "rm-" + (i % 3), "c-" + i, xid, "{}", "t:" + i);
                assertEquals(GlobalStatus.Committed, core.commit(xid));
            }

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && countRows("branch_table") == 0
                                    && countRows("lock_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);

            verify(spyAtCore, times(txCount)).branchCommit(any(), any());

            // Verify batch delete was used (< txCount invocations)
            ArgumentCaptor<List<GlobalTransactionDO>> batchCaptor = ArgumentCaptor.forClass(List.class);
            verify(env.spyLogStore, atLeastOnce()).deleteGlobalTransactionDOs(batchCaptor.capture());
            List<List<GlobalTransactionDO>> batches = batchCaptor.getAllValues();
            assertTrue(
                    batches.size() < txCount, "Expected fewer batch delete calls than txCount, got " + batches.size());
            int deletedTotal = batches.stream().mapToInt(List::size).sum();
            assertEquals(txCount, deletedTotal);

            // Verify ZERO updates with GlobalStatus.Committed
            ArgumentCaptor<GlobalTransactionDO> updateNoExpectedCaptor =
                    ArgumentCaptor.forClass(GlobalTransactionDO.class);
            verify(env.spyLogStore, times(0)).deleteGlobalTransactionDO(any());
            verify(env.spyLogStore, atLeastOnce()).updateGlobalTransactionDO(any(), any());
            verify(env.spyLogStore, times(0)).updateGlobalTransactionDO(updateNoExpectedCaptor.capture());

            ArgumentCaptor<GlobalTransactionDO> updateWithExpectedCaptor =
                    ArgumentCaptor.forClass(GlobalTransactionDO.class);
            verify(env.spyLogStore, times(txCount))
                    .updateGlobalTransactionDO(updateWithExpectedCaptor.capture(), any());
            for (GlobalTransactionDO updated : updateWithExpectedCaptor.getAllValues()) {
                assertEquals(
                        GlobalStatus.AsyncCommitting.getCode(),
                        updated.getStatus(),
                        "Push mode must not write intermediate Committed status");
            }

            assertEquals(0, comps.getInFlightRegistry().size());
        }
    }

    @Test
    void concurrentPushFallbackScannerAndLegacyNodeRaceIdempotently() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyAtCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(4)
                    .queueCapacity(64)
                    .cleanerBatchSize(4)
                    .cleanerFlushIntervalMs(50L)
                    .cleanerBufferCapacity(64)
                    .dbMaxConcurrency(4)
                    .scanQuietWindowMs(0L)
                    .build();

            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 4, 5000L));
            coordinator.init();

            int txCount = 12;
            List<String> xids = new ArrayList<>(txCount);
            for (int i = 0; i < txCount; i++) {
                String xid = core.begin("app", "group", "race-tx-" + i, 60000);
                core.branchRegister(BranchType.AT, "rm-" + (i % 3), "c-" + i, xid, "{}", "race:" + i);
                xids.add(xid);
            }

            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");
            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            AtomicBoolean stopWorkers = new AtomicBoolean(false);
            ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                Future<?> pushFuture = pool.submit(() -> {
                    for (String xid : xids) {
                        assertEquals(GlobalStatus.Committed, core.commit(xid));
                    }
                    return null;
                });
                Future<?> scannerFuture = pool.submit(() -> {
                    while (!stopWorkers.get()) {
                        scanner.scanAsyncCommitting();
                        Thread.sleep(10L);
                    }
                    return null;
                });
                Future<?> legacyNodeFuture = pool.submit(() -> {
                    while (!stopWorkers.get()) {
                        List<GlobalSession> pending = env.sessionManager.findGlobalSessions(
                                new SessionCondition(GlobalStatus.AsyncCommitting));
                        if (pending != null) {
                            for (GlobalSession s : pending) {
                                try {
                                    core.doGlobalCommit(s, true);
                                } catch (TransactionException | StoreException ignored) {
                                    // Concurrent optimistic update or already deleted by push cleaner
                                }
                            }
                        }
                        Thread.sleep(10L);
                    }
                    return null;
                });

                pushFuture.get(5, TimeUnit.SECONDS);
                awaitCondition(
                        () -> {
                            try {
                                return countRows("global_table") == 0
                                        && countRows("branch_table") == 0
                                        && countRows("lock_table") == 0
                                        && comps.getInFlightRegistry().size() == 0;
                            } catch (SQLException e) {
                                return false;
                            }
                        },
                        5000L);
                stopWorkers.set(true);
                scannerFuture.get(3, TimeUnit.SECONDS);
                legacyNodeFuture.get(3, TimeUnit.SECONDS);
            } finally {
                stopWorkers.set(true);
                pool.shutdownNow();
            }

            assertTrue(comps.getCleaner().isRunning());
            assertEquals(0, comps.getInFlightRegistry().size());
        }
    }

    @Test
    void faultRecoveryWhenPusherThrowsOrQueueFull() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            CountDownLatch workerEntered = new CountDownLatch(1);
            CountDownLatch releaseWorker = new CountDownLatch(1);
            AtomicBoolean blockFirstTx = new AtomicBoolean(false);
            String[] blockedXidHolder = new String[1];

            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doAnswer(inv -> {
                        GlobalSession s = inv.getArgument(0);
                        if (blockFirstTx.get() && s.getXid().equals(blockedXidHolder[0])) {
                            workerEntered.countDown();
                            releaseWorker.await(5, TimeUnit.SECONDS);
                        }
                        return BranchStatus.PhaseTwo_Committed;
                    })
                    .when(spyAtCore)
                    .branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            // Fault 1A: pusher throws RuntimeException -> commit() still returns Committed; scanner cleans later
            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(1)
                    .queueCapacity(1)
                    .cleanerBatchSize(4)
                    .cleanerFlushIntervalMs(50L)
                    .scanQuietWindowMs(0L)
                    .build();
            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 10, 5000L));
            coordinator.init();

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");

            core.setAsyncCommitPusher(s -> {
                throw new IllegalStateException("simulated enqueue failure");
            });
            String throwingXid = core.begin("app", "group", "throw-push-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-1", throwingXid, "{}", "k:1");
            assertEquals(GlobalStatus.Committed, core.commit(throwingXid));
            assertEquals(1, countRows("global_table"));

            // Restore normal non-blocking pusher and verify scanner recovers throwingXid
            core.setAsyncCommitPusher(session -> comps.getExecutor().tryOffer(session));
            assertEquals(1, scanner.scanAsyncCommitting().getDispatched());
            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && countRows("branch_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);

            // Fault 1B: QUEUE_FULL (rpcPoolSize=1, queueCapacity=1) -> commit() still returns Committed
            String tx0 = core.begin("app", "group", "block-worker-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-0", tx0, "{}", "k:0");
            blockedXidHolder[0] = tx0;
            blockFirstTx.set(true);
            assertEquals(GlobalStatus.Committed, core.commit(tx0));
            assertTrue(workerEntered.await(3, TimeUnit.SECONDS));

            String tx1 = core.begin("app", "group", "fill-queue-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-1", tx1, "{}", "k:1");
            assertEquals(GlobalStatus.Committed, core.commit(tx1));

            String tx2 = core.begin("app", "group", "overflow-queue-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-2", tx2, "{}", "k:2");
            assertEquals(GlobalStatus.Committed, core.commit(tx2));
            assertTrue(comps.getMetrics().getDroppedCount() >= 1L);

            releaseWorker.countDown();
            blockFirstTx.set(false);
            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 1
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);

            assertEquals(1, scanner.scanAsyncCommitting().getDispatched());
            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && countRows("branch_table") == 0
                                    && countRows("lock_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);
        }
    }

    @Test
    void faultRecoveryWhenBranchCommitThrowsOnFirstAttempt() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            AtomicInteger attempts = new AtomicInteger(0);
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doAnswer(inv -> {
                        if (attempts.incrementAndGet() == 1) {
                            throw new BranchTransactionException(
                                    TransactionExceptionCode.FailedToSendBranchCommitRequest, "simulated rm failure");
                        }
                        return BranchStatus.PhaseTwo_Committed;
                    })
                    .when(spyAtCore)
                    .branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(2)
                    .queueCapacity(16)
                    .cleanerBatchSize(4)
                    .cleanerFlushIntervalMs(50L)
                    .scanQuietWindowMs(0L)
                    .build();
            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 10, 5000L));
            coordinator.init();

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");

            String xid = core.begin("app", "group", "rm-fail-first-tx", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-1", xid, "{}", "k:10");
            assertEquals(GlobalStatus.Committed, core.commit(xid));

            // Wait for first failed RPC attempt to release its lease while leaving rows in DB
            awaitCondition(
                    () -> attempts.get() >= 1 && comps.getInFlightRegistry().size() == 0, 5000L);
            assertEquals(1, countRows("global_table"));
            assertEquals(1, countRows("branch_table"));

            // Fallback scanner retries and cleaner deletes
            assertEquals(1, scanner.scanAsyncCommitting().getDispatched());
            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && countRows("branch_table") == 0
                                    && countRows("lock_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);
            assertEquals(2, attempts.get());
        }
    }

    @Test
    void faultRecoveryImmediatelyCleansZeroBranchAsyncCommittingWithoutWaitingEndStateWindow() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(2)
                    .queueCapacity(16)
                    .cleanerBatchSize(4)
                    .cleanerFlushIntervalMs(50L)
                    .scanQuietWindowMs(0L)
                    .build();
            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 10, 5000L));
            coordinator.init();

            // Simulate node crash / cleaner drop after all branches were removed, leaving AsyncCommitting with 0
            // branches
            String xid = core.begin("app", "group", "orphan-zero-branch-tx", 60000);
            GlobalSession session = SessionHolder.findGlobalSession(xid);
            assertNotNull(session);
            session.asyncCommit();
            assertEquals(GlobalStatus.AsyncCommitting, session.getStatus());
            assertTrue(session.timeToDeadSession() > 0L);

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");
            FallbackRecoveryScanner.ScanSummary summary = scanner.scanAsyncCommitting();
            assertEquals(1, summary.getDispatched());

            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);
            verify(env.spyLogStore, never()).updateGlobalTransactionDO(any());
        }
    }

    @Test
    void faultRecoveryWhenCleanerBatchDeleteThrowsStoreExceptionOnFirstAttempt() throws Exception {
        try (H2Env env = new H2Env()) {
            DefaultCore core = env.createCoreWithSavedCores();
            ATCore spyAtCore = spy(new ATCore(remotingServer));
            doReturn(BranchStatus.PhaseTwo_Committed).when(spyAtCore).branchCommit(any(), any());
            env.registerCoreMock(core, BranchType.AT, spyAtCore);

            // Fail first two deleteGlobalTransactionDOs calls (the size=2 batch call AND the first item fallback call),
            // then allow subsequent calls (second item fallback + next fallback scan) to succeed.
            AtomicInteger deleteCalls = new AtomicInteger(0);
            doAnswer(inv -> {
                        if (deleteCalls.incrementAndGet() <= 2) {
                            throw new StoreException("simulated batch delete failure");
                        }
                        return inv.callRealMethod();
                    })
                    .when(env.spyLogStore)
                    .deleteGlobalTransactionDOs(anyList());

            PipelineConfig config = PipelineConfig.builder()
                    .enabled(true)
                    .pushEnabled(true)
                    .rpcPoolSize(2)
                    .queueCapacity(16)
                    .cleanerBatchSize(2)
                    .cleanerFlushIntervalMs(400L)
                    .scanQuietWindowMs(0L)
                    .build();
            DefaultCoordinator coordinator =
                    track(newCoordinator(remotingServer, core, SessionMode.DB, config, 10, 5000L));
            coordinator.init();

            String xid1 = core.begin("app", "group", "cleaner-fail-1", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-1", xid1, "{}", "cf:1");
            String xid2 = core.begin("app", "group", "cleaner-fail-2", 60000);
            core.branchRegister(BranchType.AT, "rm-1", "c-2", xid2, "{}", "cf:2");

            assertEquals(GlobalStatus.Committed, core.commit(xid1));
            assertEquals(GlobalStatus.Committed, core.commit(xid2));

            PipelineComponents comps = readField(coordinator, DefaultCoordinator.class, "pipelineComponents");
            FallbackRecoveryScanner scanner =
                    readField(coordinator, DefaultCoordinator.class, "fallbackRecoveryScanner");

            // Wait until first batch failure + per-item fallback finishes and releases all leases
            awaitCondition(
                    () -> comps.getMetrics().getCleanerBatchFailuresCount() >= 1L
                            && comps.getInFlightRegistry().size() == 0,
                    5000L);
            assertTrue(comps.getCleaner().isRunning());
            // One of the two rows remains because its per-item fallback failed too -> fallback scanner recovers it
            if (countRows("global_table") > 0) {
                assertTrue(scanner.scanAsyncCommitting().getDispatched() >= 1);
            }

            awaitCondition(
                    () -> {
                        try {
                            return countRows("global_table") == 0
                                    && countRows("branch_table") == 0
                                    && countRows("lock_table") == 0
                                    && comps.getInFlightRegistry().size() == 0;
                        } catch (SQLException e) {
                            return false;
                        }
                    },
                    5000L);
            assertTrue(comps.getCleaner().isRunning());
        }
    }
}
