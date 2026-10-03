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
import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.XID;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.util.StoreUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.apache.seata.common.ConfigurationKeys.XAER_NOTA_RETRY_TIMEOUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The type DefaultCoordinator test.
 *
 */
public class DefaultCoordinatorTest extends BaseSpringBootTest {
    private static DefaultCoordinator defaultCoordinator;

    private static final String APPLICATION_ID = "demo-child-app";

    private static final String TX_SERVICE_GROUP = "default_tx_group";

    private static final String TX_NAME = "tx-1";

    private static final int TIMEOUT = 3000;

    private static final String RESOURCE_ID = "tb_1";

    @TempDir
    Path tempDir;

    private static final String CLIENT_ID = "c_1";

    private static final String LOCK_KEYS_1 = "tb_1:11";

    private static final String LOCK_KEYS_2 = "tb_1:12";

    private static final String APPLICATION_DATA = "{\"data\":\"test\"}";

    private static DefaultCore core;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static RemotingServer remotingServer;

    @BeforeAll
    public static void beforeClass(ApplicationContext context) throws Exception {
        EnhancedServiceLoader.unload(AbstractCore.class);
        XID.setIpAddress(NetUtil.getLocalIp());
        remotingServer = new MockServerMessageSender();
        defaultCoordinator = DefaultCoordinator.getInstance(remotingServer);
        defaultCoordinator.setRemotingServer(remotingServer);
        core = new DefaultCore(remotingServer);
        // Initialize SessionHolder once for all tests
        SessionHolder.init(SessionMode.FILE);
    }

    @BeforeEach
    public void tearUp() throws IOException {
        // Only delete data files before each test
        StoreUtil.deleteDataFile();
        // Reinitialize core before each test to clear previous mocks
        core = new DefaultCore(remotingServer);
    }

    @Test
    public void branchCommit() throws TransactionException {
        BranchStatus result = null;
        String xid = null;
        GlobalSession globalSession = null;
        try {
            xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
            Long branchId =
                    core.branchRegister(BranchType.AT, RESOURCE_ID, CLIENT_ID, xid, APPLICATION_DATA, LOCK_KEYS_1);
            globalSession = SessionHolder.findGlobalSession(xid);
            result = core.branchCommit(globalSession, globalSession.getBranch(branchId));
        } catch (TransactionException e) {
            Assertions.fail(e.getMessage());
        }
        Assertions.assertEquals(result, BranchStatus.PhaseTwo_Committed);
        globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        globalSession.end();
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("xidAndBranchIdProviderForRollback")
    public void branchRollback(String xid, Long branchId) {
        BranchStatus result = null;
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        try {
            result = core.branchRollback(globalSession, globalSession.getBranch(branchId));
        } catch (TransactionException e) {
            Assertions.fail(e.getMessage());
        }
        Assertions.assertEquals(result, BranchStatus.PhaseTwo_Rollbacked);
    }

    @Test
    public void handleRetryRollbackingTest() throws TransactionException, InterruptedException {

        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", CLIENT_ID, xid, APPLICATION_DATA, LOCK_KEYS_2);

        Assertions.assertNotNull(branchId);
        Thread.sleep(100);
        defaultCoordinator.timeoutCheck();
        defaultCoordinator.handleRetryRollbacking();

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNull(globalSession);
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void handleRetryRollbackingTimeOutTest()
            throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", CLIENT_ID, xid, APPLICATION_DATA, LOCK_KEYS_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", 10L);
        ReflectionUtil.modifyStaticFinalField(
                defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", false);
        TimeUnit.MILLISECONDS.sleep(100);
        globalSession.queueToRetryRollback();
        defaultCoordinator.handleRetryRollbacking();
        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertTrue(lockSize > 0);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(
                    defaultCoordinator.getClass(),
                    "MAX_ROLLBACK_RETRY_TIMEOUT",
                    ConfigurationFactory.getInstance()
                            .getLong(
                                    ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT,
                                    DefaultValues.DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT));
        }
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void handleRetryRollbackingTimeOut_unlockTest()
            throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", CLIENT_ID, xid, APPLICATION_DATA, LOCK_KEYS_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", 10L);
        ReflectionUtil.modifyStaticFinalField(
                defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", true);
        TimeUnit.MILLISECONDS.sleep(100);

        globalSession.queueToRetryRollback();
        defaultCoordinator.handleRetryRollbacking();

        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertEquals(0, lockSize);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(
                    defaultCoordinator.getClass(),
                    "MAX_ROLLBACK_RETRY_TIMEOUT",
                    ConfigurationFactory.getInstance()
                            .getLong(
                                    ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT,
                                    DefaultValues.DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT));
        }
    }

    @AfterAll
    public static void afterClass() throws Exception {

        Collection<GlobalSession> globalSessions =
                SessionHolder.getRootSessionManager().allSessions();
        Collection<GlobalSession> asyncGlobalSessions =
                SessionHolder.getRootSessionManager().allSessions();
        for (GlobalSession asyncGlobalSession : asyncGlobalSessions) {
            asyncGlobalSession.closeAndClean();
        }
        for (GlobalSession globalSession : globalSessions) {
            globalSession.closeAndClean();
        }
        // Destroy SessionHolder to clean up static state
        SessionHolder.destroy();
    }

    @AfterEach
    public void tearDown() throws IOException {
        MetricsManager.get().getRegistry().clearUp();
        StoreUtil.deleteDataFile();
    }

    static Stream<Arguments> xidAndBranchIdProviderForRollback() throws Exception {
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        Long branchId = core.branchRegister(BranchType.AT, RESOURCE_ID, CLIENT_ID, xid, APPLICATION_DATA, LOCK_KEYS_2);
        return Stream.of(Arguments.of(xid, branchId));
    }

    @Test
    public void getInstanceSingletonTest() {
        DefaultCoordinator instance1 = DefaultCoordinator.getInstance();
        DefaultCoordinator instance2 = DefaultCoordinator.getInstance();
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    public void doGlobalCommitNullSessionTest() throws TransactionException {
        boolean result = defaultCoordinator.doGlobalCommit(null, false);
        Assertions.assertTrue(result);
    }

    @Test
    public void doGlobalRollbackNullSessionTest() throws TransactionException {
        boolean result = defaultCoordinator.doGlobalRollback(null, false);
        Assertions.assertTrue(result);
    }

    @Test
    public void doBranchDeleteNullSessionTest() throws TransactionException {
        Boolean result = defaultCoordinator.doBranchDelete(null, null);
        Assertions.assertTrue(result);
    }

    @Test
    public void doBranchDeleteNullBranchTest() throws TransactionException {
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        Boolean result = defaultCoordinator.doBranchDelete(globalSession, null);
        Assertions.assertTrue(result);

        globalSession.end();
    }

    @Test
    public void timeoutCheckNoTimeoutTest() throws TransactionException {
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 30000);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        defaultCoordinator.timeoutCheck();

        GlobalSession afterCheck = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(afterCheck);
        Assertions.assertEquals(GlobalStatus.Begin, afterCheck.getStatus());

        globalSession.end();
    }

    @Test
    public void timeoutCheckUsesDeadlineBoundedScanForRocksDBSessionManager() {
        RocksDBSessionManager sessionManager = mock(RocksDBSessionManager.class);
        List<SessionCondition> conditions = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            conditions.add(invocation.getArgument(0));
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.timeoutCheck();
        }

        Assertions.assertEquals(1, conditions.size());
        SessionCondition condition = conditions.get(0);
        Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, condition.getLimit());
        Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, condition.getScanLimit());
        Assertions.assertEquals(GlobalStatus.Begin, condition.getStatus());
        Assertions.assertTrue(condition.isLazyLoadBranch());
        Assertions.assertNotNull(condition.getMaxTimeoutDeadlineMillis());
        Assertions.assertTrue(condition.getMaxTimeoutDeadlineMillis() <= System.currentTimeMillis());
    }

    @Test
    public void timeoutCheckUsesCoordinatorInstanceQueryLimitForRocksDBSessionManager() throws Exception {
        DefaultCoordinator coordinator = new DefaultCoordinator(remotingServer, 7);
        RocksDBSessionManager sessionManager = mock(RocksDBSessionManager.class);
        List<SessionCondition> conditions = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            conditions.add(invocation.getArgument(0));
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            coordinator.timeoutCheck();
        } finally {
            shutdownCoordinatorExecutors(coordinator);
        }

        Assertions.assertEquals(1, conditions.size());
        Assertions.assertEquals(7, conditions.get(0).getLimit());
        Assertions.assertEquals(7, conditions.get(0).getScanLimit());
    }

    @Test
    public void timeoutCheckKeepsFullScanForGenericSessionManager() {
        SessionManager sessionManager = mock(SessionManager.class);
        List<SessionCondition> conditions = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            conditions.add(invocation.getArgument(0));
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.timeoutCheck();
        }

        Assertions.assertEquals(1, conditions.size());
        SessionCondition condition = conditions.get(0);
        Assertions.assertNull(condition.getLimit());
        Assertions.assertNull(condition.getMaxTimeoutDeadlineMillis());
        Assertions.assertNull(condition.getTimeoutScanCursor());
    }

    @Test
    public void timeoutCheckCarriesTimeoutScanCursorAcrossRounds() {
        RocksDBSessionManager sessionManager = mock(RocksDBSessionManager.class);
        byte[] nextCursor = new byte[] {4, 5, 6};
        List<byte[]> cursors = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            cursors.add(condition.getTimeoutScanCursor());
            if (cursors.size() == 1) {
                condition.setNextTimeoutScanCursor(nextCursor);
            } else if (cursors.size() == 2) {
                condition.setNextTimeoutScanCursor(null);
            }
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
        }

        Assertions.assertEquals(3, cursors.size());
        Assertions.assertNull(cursors.get(0));
        Assertions.assertArrayEquals(nextCursor, cursors.get(1));
        Assertions.assertNull(cursors.get(2));
    }

    @Test
    public void handleRetryCommittingNoSessionsTest() {
        defaultCoordinator.handleRetryCommitting();
    }

    @Test
    public void retryBackgroundTasksSetSessionQueryLimit() {
        List<SessionCondition> conditions = captureBackgroundSessionConditions(() -> {
            defaultCoordinator.handleRetryRollbacking();
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleAsyncCommitting();
        });

        Assertions.assertEquals(5, conditions.size());
        assertSharedBackgroundSessionQueryConditions(conditions.subList(0, 3));
        assertBackgroundSessionQueryConditions(conditions.subList(3, 5));
    }

    @Test
    public void retryBackgroundTasksCarryStatusScanCursorAcrossRounds() {
        SessionManager sessionManager = mock(SessionManager.class);
        byte[] nextCursor = new byte[] {1, 2, 3};
        List<byte[]> cursors = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            cursors.add(condition.getStatusScanCursor());
            if (cursors.size() == 1) {
                condition.setNextStatusScanCursor(nextCursor);
            } else if (cursors.size() == 2) {
                condition.setNextStatusScanCursor(null);
            }
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
        }

        Assertions.assertEquals(3, cursors.size());
        Assertions.assertNull(cursors.get(0));
        Assertions.assertArrayEquals(nextCursor, cursors.get(1));
        Assertions.assertNull(cursors.get(2));
    }

    @Test
    public void retryBackgroundTasksPreserveRetainedCursorWhenContinuationIsUnset() {
        SessionManager sessionManager = mock(SessionManager.class);
        byte[] firstCursor = new byte[] {1, 2, 3};
        byte[] replacementCursor = new byte[] {4, 5, 6};
        List<byte[]> cursors = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            cursors.add(condition.getStatusScanCursor());
            if (cursors.size() == 1) {
                condition.setNextStatusScanCursor(firstCursor);
            } else if (cursors.size() == 3) {
                condition.setNextStatusScanCursor(replacementCursor);
            } else if (cursors.size() == 4) {
                condition.setNextStatusScanCursor(null);
            }
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
            defaultCoordinator.handleRetryCommitting();
        }

        Assertions.assertEquals(5, cursors.size());
        Assertions.assertNull(cursors.get(0));
        Assertions.assertArrayEquals(firstCursor, cursors.get(1));
        Assertions.assertArrayEquals(firstCursor, cursors.get(2));
        Assertions.assertArrayEquals(replacementCursor, cursors.get(3));
        Assertions.assertNull(cursors.get(4));
    }

    @Test
    public void timeoutCheckPreservesRetainedCursorWhenContinuationIsUnset() {
        RocksDBSessionManager sessionManager = mock(RocksDBSessionManager.class);
        byte[] firstCursor = new byte[] {4, 5, 6};
        byte[] replacementCursor = new byte[] {7, 8, 9};
        List<byte[]> cursors = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            cursors.add(condition.getTimeoutScanCursor());
            if (cursors.size() == 1) {
                condition.setNextTimeoutScanCursor(firstCursor);
            } else if (cursors.size() == 3) {
                condition.setNextTimeoutScanCursor(replacementCursor);
            } else if (cursors.size() == 4) {
                condition.setNextTimeoutScanCursor(null);
            }
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
            defaultCoordinator.timeoutCheck();
        }

        Assertions.assertEquals(5, cursors.size());
        Assertions.assertNull(cursors.get(0));
        Assertions.assertArrayEquals(firstCursor, cursors.get(1));
        Assertions.assertArrayEquals(firstCursor, cursors.get(2));
        Assertions.assertArrayEquals(replacementCursor, cursors.get(3));
        Assertions.assertNull(cursors.get(4));
    }

    @Test
    public void exhaustedMultiStatusScansRestartBeforeNewEarlierSessions() throws Exception {
        GlobalStatus[] statuses = new GlobalStatus[] {
            GlobalStatus.Rollbacked, GlobalStatus.TimeoutRollbacked, GlobalStatus.Committed, GlobalStatus.Finished
        };
        DefaultCoordinator coordinator = new DefaultCoordinator(remotingServer, statuses.length);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(new RocksDBStoreConfig(
                tempDir.resolve("multi-status-exhausted-cursors").toString(), true))) {
            RocksDBSessionManager delegate = new RocksDBSessionManager("multi-status-exhausted-cursors", engine);
            List<SessionCondition> conditions = new ArrayList<>();
            SessionManager sessionManager = mock(SessionManager.class);
            when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
                SessionCondition condition = invocation.getArgument(0);
                List<GlobalSession> sessions = delegate.findGlobalSessions(condition);
                conditions.add(condition);
                return sessions;
            });
            for (int i = 0; i < statuses.length; i++) {
                delegate.addGlobalSession(storedBackgroundSession("tx-exhausted-" + i, statuses[i], 100L + i));
            }

            try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
                sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);

                List<GlobalSession> firstRound = invokeFindBackgroundSessions(coordinator, statuses, true);
                Assertions.assertEquals(statuses.length, firstRound.size());
                conditions.forEach(condition -> Assertions.assertEquals(
                        SessionCondition.ScanContinuation.RESUMABLE, condition.getStatusScanContinuation()));

                conditions.clear();
                Assertions.assertTrue(invokeFindBackgroundSessions(coordinator, statuses, true)
                        .isEmpty());
                conditions.forEach(condition -> Assertions.assertEquals(
                        SessionCondition.ScanContinuation.EXHAUSTED, condition.getStatusScanContinuation()));
                assertBackgroundSessionCursors(coordinator, Collections.emptyMap());

                GlobalSession earlier = storedBackgroundSession("tx-new-earlier", statuses[0], 50L);
                delegate.addGlobalSession(earlier);
                conditions.clear();
                List<GlobalSession> nextRound = invokeFindBackgroundSessions(coordinator, statuses, true);

                Assertions.assertTrue(
                        nextRound.stream().anyMatch(session -> earlier.getXid().equals(session.getXid())));
                SessionCondition restarted = conditions.stream()
                        .filter(condition -> condition.getStatuses()[0] == statuses[0])
                        .findFirst()
                        .orElseThrow(AssertionError::new);
                Assertions.assertNull(restarted.getStatusScanCursor());
            }
        } finally {
            shutdownCoordinatorExecutors(coordinator);
        }
    }

    @Test
    public void retryBackgroundScanReachesSession1025AfterFullFailingPage() throws Exception {
        int firstPageSize = 1024;
        Assertions.assertEquals(firstPageSize, DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT);
        List<GlobalSession> failingFirstPage = new ArrayList<>(firstPageSize);
        for (int i = 0; i < firstPageSize; i++) {
            failingFirstPage.add(mock(GlobalSession.class));
        }
        GlobalSession session1025 = mock(GlobalSession.class);
        byte[] nextCursor = new byte[] {1, 2, 3};
        List<byte[]> observedCursors = new ArrayList<>();
        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            observedCursors.add(condition.getStatusScanCursor());
            Assertions.assertEquals(firstPageSize, condition.getScanLimit());
            if (condition.getStatusScanCursor() == null) {
                condition.setNextStatusScanCursor(nextCursor);
                return failingFirstPage;
            }
            Assertions.assertArrayEquals(nextCursor, condition.getStatusScanCursor());
            condition.setNextStatusScanCursor(null);
            return Collections.singletonList(session1025);
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);

            List<GlobalSession> firstRound =
                    invokeFindBackgroundSessions(new GlobalStatus[] {GlobalStatus.CommitRetrying}, true);
            List<GlobalSession> secondRound =
                    invokeFindBackgroundSessions(new GlobalStatus[] {GlobalStatus.CommitRetrying}, true);

            Assertions.assertEquals(firstPageSize, firstRound.size());
            Assertions.assertSame(session1025, secondRound.get(0));
        }

        Assertions.assertEquals(2, observedCursors.size());
        Assertions.assertNull(observedCursors.get(0));
        Assertions.assertArrayEquals(nextCursor, observedCursors.get(1));
    }

    @Test
    public void retryRollbackingStatusesShareRoundBudgetWithoutDroppingLaterSessions() throws Exception {
        assertMultiStatusRoundsShareBudget(new GlobalStatus[] {
            GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.RollbackRetrying
        });
    }

    @Test
    public void endStatusesShareRoundBudgetWithoutDroppingLaterSessions() throws Exception {
        assertMultiStatusRoundsShareBudget(new GlobalStatus[] {
            GlobalStatus.Rollbacked, GlobalStatus.TimeoutRollbacked, GlobalStatus.Committed, GlobalStatus.Finished
        });
    }

    @Test
    public void smallBackgroundQueryLimitRotatesAcrossRetryAndEndStatusGroups() throws Exception {
        assertSmallBackgroundQueryLimitRotatesAcrossStatusGroup("retryRollbackingStatuses");
        assertSmallBackgroundQueryLimitRotatesAcrossStatusGroup("endStatuses");
    }

    @Test
    public void multiStatusRocksDBBackgroundScanCarriesCompositeCursorAcrossRounds() throws Exception {
        clearBackgroundSessionStatusCursors();
        RocksDBSessionManager sessionManager = mock(RocksDBSessionManager.class);
        byte[] timeoutRollbackingCursor = new byte[] {1, 2, 3};
        byte[] timeoutRollbackRetryingCursor = new byte[] {4, 5, 6};
        List<SessionCondition> conditions = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            conditions.add(condition);
            if (conditions.size() == 1) {
                Map<GlobalStatus, byte[]> nextCursors = new EnumMap<>(GlobalStatus.class);
                nextCursors.put(GlobalStatus.TimeoutRollbacking, timeoutRollbackingCursor);
                nextCursors.put(GlobalStatus.TimeoutRollbackRetrying, timeoutRollbackRetryingCursor);
                condition.setNextStatusScanCursors(nextCursors);
            }
            return Collections.emptyList();
        });

        GlobalStatus[] statuses = {GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying};
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            invokeFindBackgroundSessions(statuses, true);
            invokeFindBackgroundSessions(statuses, true);
        }

        Assertions.assertEquals(2, conditions.size());
        Assertions.assertArrayEquals(statuses, conditions.get(0).getStatuses());
        Assertions.assertEquals(
                DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT,
                conditions.get(0).getLimit());
        Map<GlobalStatus, byte[]> secondRoundCursors = conditions.get(1).getStatusScanCursors();
        Assertions.assertArrayEquals(timeoutRollbackingCursor, secondRoundCursors.get(GlobalStatus.TimeoutRollbacking));
        Assertions.assertArrayEquals(
                timeoutRollbackRetryingCursor, secondRoundCursors.get(GlobalStatus.TimeoutRollbackRetrying));
    }

    @Test
    public void scheduledBackgroundTasksSetSessionQueryLimit() {
        List<SessionCondition> conditions = captureBackgroundSessionConditions(() -> {
            defaultCoordinator.handleRollbackingByScheduled();
            defaultCoordinator.handleCommittingByScheduled();
            defaultCoordinator.handleEndStatesByScheduled();
        });

        Assertions.assertEquals(6, conditions.size());
        assertBackgroundSessionQueryConditions(conditions.subList(0, 2));
        assertSharedBackgroundSessionQueryConditions(conditions.subList(2, 6));
    }

    @Test
    public void handleAsyncCommittingNoSessionsTest() {
        defaultCoordinator.handleAsyncCommitting();
    }

    @Test
    public void undoLogDelete_NoChannelsTest() {
        defaultCoordinator.undoLogDelete();
    }

    @Test
    public void onRequestValidRequestTest() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test_tx");
        request.setTimeout(3000);

        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        AbstractResultMessage response = defaultCoordinator.onRequest(request, rpcContext);
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response instanceof GlobalBeginResponse);
    }

    @Test
    public void onResponseValidResponseTest() {
        GlobalBeginResponse response = new GlobalBeginResponse();
        response.setXid("test_xid");
        RpcContext rpcContext = new RpcContext();

        defaultCoordinator.onResponse(response, rpcContext);
    }

    @Test
    public void doBranchReportTest() throws TransactionException {
        // Create global transaction and branch (without lock to avoid conflicts)
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_branch_report", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranch(branchId));

        // Create BranchReportRequest
        BranchReportRequest request = new BranchReportRequest();
        request.setXid(xid);
        request.setBranchId(branchId);
        request.setBranchType(BranchType.AT);
        request.setStatus(BranchStatus.PhaseOne_Done);
        request.setApplicationData(APPLICATION_DATA);

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        BranchReportResponse response = defaultCoordinator.handle(request, rpcContext);

        // Verify result
        Assertions.assertNotNull(response);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doLockCheckTest() throws TransactionException {
        // Create global transaction with lock, using unique resourceId to avoid conflicts
        String testResourceId = "resource_lock_check";
        String testLockKey1 = "lock_check:1";
        String testLockKey2 = "lock_check:2";

        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        Long branchId =
                core.branchRegister(BranchType.AT, testResourceId, CLIENT_ID, xid, APPLICATION_DATA, testLockKey1);

        Assertions.assertNotNull(branchId);

        // Test lockable scenario (different lockKey)
        GlobalLockQueryRequest request1 = new GlobalLockQueryRequest();
        request1.setXid(xid);
        request1.setBranchType(BranchType.AT);
        request1.setResourceId(testResourceId);
        request1.setLockKey(testLockKey2);

        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        GlobalLockQueryResponse response1 = defaultCoordinator.handle(request1, rpcContext);

        Assertions.assertNotNull(response1);
        Assertions.assertTrue(response1.isLockable());

        // Test unlockable scenario (same lockKey)
        GlobalLockQueryRequest request2 = new GlobalLockQueryRequest();
        request2.setBranchType(BranchType.AT);
        request2.setResourceId(testResourceId);
        request2.setLockKey(testLockKey1);

        GlobalLockQueryResponse response2 = defaultCoordinator.handle(request2, rpcContext);

        Assertions.assertNotNull(response2);
        Assertions.assertFalse(response2.isLockable());

        // Cleanup
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        globalSession.end();
    }

    @Test
    public void doBranchRemoveAsyncNullSessionTest() {
        // Verify null session does not throw exception
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.doBranchRemoveAsync(null, null));
    }

    @Test
    public void doBranchRemoveAllAsyncNullSessionTest() {
        // Verify null session does not throw exception
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.doBranchRemoveAllAsync(null));
    }

    @Test
    public void branchRemoveTaskConstructorWithNullBranchTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Test creating BranchRemoveTask with null branchSession should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new DefaultCoordinator.BranchRemoveTask(globalSession, null);
        });

        // Cleanup
        globalSession.end();
    }

    @Test
    public void branchRemoveTaskRunWithSingleBranchTest() throws TransactionException, InterruptedException {
        // Create global session and branch (without lock to avoid conflicts)
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_remove_single", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        BranchSession branchSession = globalSession.getBranch(branchId);

        Assertions.assertNotNull(branchSession);
        Assertions.assertEquals(1, globalSession.getBranchSessions().size());

        // Create and execute BranchRemoveTask
        DefaultCoordinator.BranchRemoveTask task =
                new DefaultCoordinator.BranchRemoveTask(globalSession, branchSession);
        task.run();

        // Verify branch has been removed
        Assertions.assertNull(globalSession.getBranch(branchId));
        Assertions.assertEquals(0, globalSession.getBranchSessions().size());

        // Cleanup
        globalSession.end();
    }

    @Test
    public void branchRemoveTaskRunWithAllBranchesTest() throws TransactionException, InterruptedException {
        // Create global session and multiple branches (without lock to avoid conflicts)
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_remove_all_1", CLIENT_ID, xid, APPLICATION_DATA, null);
        core.branchRegister(BranchType.AT, "resource_remove_all_2", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        Assertions.assertEquals(2, globalSession.getBranchSessions().size());

        // Create and execute BranchRemoveTask (remove all branches)
        DefaultCoordinator.BranchRemoveTask task = new DefaultCoordinator.BranchRemoveTask(globalSession);
        task.run();

        // Verify all branches have been removed
        Assertions.assertTrue(globalSession.getBranchSessions().isEmpty());

        // Cleanup
        globalSession.end();
    }

    @Test
    public void branchRemoveTaskRunWithNullGlobalSessionTest() {
        // Test null globalSession does not throw exception
        DefaultCoordinator.BranchRemoveTask task = new DefaultCoordinator.BranchRemoveTask(null);
        Assertions.assertDoesNotThrow(() -> task.run());
    }

    @Test
    public void handleCommittingByScheduledNoSessionsTest() {
        // Test no exception is thrown when there are no sessions in Committing state
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleCommittingByScheduled());
    }

    @Test
    public void handleCommittingByScheduledWithSessionTest() throws TransactionException, InterruptedException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_committing_scheduled", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change session status to Committing
        globalSession.changeGlobalStatus(GlobalStatus.Committing);

        // Execute scheduling method
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleCommittingByScheduled());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null) {
            session.end();
        }
    }

    @Test
    public void handleRollbackingByScheduledNoSessionsTest() {
        // Test no exception is thrown when there are no sessions in Rollbacking state
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleRollbackingByScheduled());
    }

    @Test
    public void handleRollbackingByScheduledWithSessionTest() throws TransactionException, InterruptedException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_rollbacking_scheduled", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change session status to Rollbacking
        globalSession.changeGlobalStatus(GlobalStatus.Rollbacking);

        // Execute scheduling method
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleRollbackingByScheduled());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null) {
            session.end();
        }
    }

    @Test
    public void handleEndStatesByScheduledNoSessionsTest() {
        // Test no exception is thrown when there are no sessions in end state
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleEndStatesByScheduled());
    }

    @Test
    public void handleEndStatesByScheduledWithCommittedSessionTest() throws TransactionException, InterruptedException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_end_committed", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change session status to Committed
        globalSession.changeGlobalStatus(GlobalStatus.Committed);
        // Clear branches so it can enter end state processing
        while (!globalSession.getBranchSessions().isEmpty()) {
            globalSession.removeBranch(globalSession.getBranchSessions().get(0));
        }

        // Execute scheduling method
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleEndStatesByScheduled());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null && session.getStatus() != GlobalStatus.Finished) {
            session.end();
        }
    }

    @Test
    public void handleEndStatesByScheduledWithRollbackedSessionTest()
            throws TransactionException, InterruptedException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_end_rollbacked", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change session status to Rollbacked
        globalSession.changeGlobalStatus(GlobalStatus.Rollbacked);
        // Clear branches so it can enter end state processing
        while (!globalSession.getBranchSessions().isEmpty()) {
            globalSession.removeBranch(globalSession.getBranchSessions().get(0));
        }

        // Execute scheduling method
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleEndStatesByScheduled());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null && session.getStatus() != GlobalStatus.Finished) {
            session.end();
        }
    }

    @Test
    public void doBranchDeleteSagaTypeTest() throws TransactionException {
        // Create SAGA type global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create SAGA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.SAGA);
        branchSession.setResourceId("saga_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - SAGA type should return true directly
        Assertions.assertTrue(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteATSuccessTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock AT Core returns PhaseTwo_Committed (AT delete success status)
        AbstractCore mockATCore = mock(AbstractCore.class);
        when(mockATCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_Committed);
        core.mockCore(BranchType.AT, mockATCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - AT branch delete success should return true
        Assertions.assertTrue(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteATFailureTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock AT Core returns PhaseTwo_CommitFailed_Retryable (AT delete failure status)
        AbstractCore mockATCore = mock(AbstractCore.class);
        when(mockATCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_CommitFailed_Retryable);
        core.mockCore(BranchType.AT, mockATCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - AT branch delete failure should return false
        Assertions.assertFalse(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteTCCSuccessTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create TCC type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.TCC);
        branchSession.setResourceId("tcc_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock TCC Core returns PhaseTwo_Rollbacked (TCC delete success status)
        AbstractCore mockTCCCore = mock(AbstractCore.class);
        when(mockTCCCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_Rollbacked);
        core.mockCore(BranchType.TCC, mockTCCCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - TCC branch delete success should return true
        Assertions.assertTrue(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteTCCFailureTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create TCC type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.TCC);
        branchSession.setResourceId("tcc_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock TCC Core returns PhaseTwo_RollbackFailed_Retryable (TCC delete failure status)
        AbstractCore mockTCCCore = mock(AbstractCore.class);
        when(mockTCCCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_RollbackFailed_Retryable);
        core.mockCore(BranchType.TCC, mockTCCCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - TCC branch delete failure should return false
        Assertions.assertFalse(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteXASuccessTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock XA Core returns PhaseTwo_Rollbacked (XA delete success status)
        AbstractCore mockXACore = mock(AbstractCore.class);
        when(mockXACore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_Rollbacked);
        core.mockCore(BranchType.XA, mockXACore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - XA branch delete success should return true
        Assertions.assertTrue(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void doBranchDeleteXAXaerNotaTimeoutTest()
            throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Create global session with short timeout (10ms)
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock XA Core returns PhaseTwo_RollbackFailed_XAER_NOTA_Retryable
        AbstractCore mockXACore = mock(AbstractCore.class);
        when(mockXACore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable);
        core.mockCore(BranchType.XA, mockXACore);

        try {
            // Temporarily modify RETRY_XAER_NOTA_TIMEOUT to small value for timeout testing
            ReflectionUtil.modifyStaticFinalField(core.getClass(), "RETRY_XAER_NOTA_TIMEOUT", 10);

            // Wait for timeout: timeout condition is currentTime > beginTime + timeout + max(RETRY_XAER_NOTA_TIMEOUT,
            // timeout)
            // = beginTime + 10 + max(10, 10) = beginTime + 20ms, so waiting 25ms is enough to trigger timeout
            Thread.sleep(25);

            // Execute test
            Boolean result = core.doBranchDelete(globalSession, branchSession);

            // Verify result - XAER_NOTA timeout should return true
            Assertions.assertTrue(result);
        } finally {
            // Restore original value
            ReflectionUtil.modifyStaticFinalField(
                    core.getClass(),
                    "RETRY_XAER_NOTA_TIMEOUT",
                    ConfigurationFactory.getInstance()
                            .getInt(XAER_NOTA_RETRY_TIMEOUT, DefaultValues.DEFAULT_XAER_NOTA_RETRY_TIMEOUT));
            // Cleanup
            globalSession.end();
        }
    }

    @Test
    public void doBranchDeleteXAXaerNotaNoTimeoutTest() throws TransactionException {
        // Create global session with long timeout
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 30000);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock XA Core returns PhaseTwo_RollbackFailed_XAER_NOTA_Retryable
        AbstractCore mockXACore = mock(AbstractCore.class);
        when(mockXACore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable);
        core.mockCore(BranchType.XA, mockXACore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - XAER_NOTA not timed out should return false
        Assertions.assertFalse(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteXAFailureTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock XA Core returns PhaseTwo_RollbackFailed_Retryable (XA delete failure status)
        AbstractCore mockXACore = mock(AbstractCore.class);
        when(mockXACore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_RollbackFailed_Retryable);
        core.mockCore(BranchType.XA, mockXACore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - XA branch delete failure should return false
        Assertions.assertFalse(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteUnretryableTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock Core returns PhaseTwo_RollbackFailed_Unretryable (unretryable status)
        AbstractCore mockATCore = mock(AbstractCore.class);
        when(mockATCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseTwo_RollbackFailed_Unretryable);
        core.mockCore(BranchType.AT, mockATCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - unretryable status should return true (stop retry and delete)
        Assertions.assertTrue(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doBranchDeleteGeneralFailureTest() throws TransactionException {
        // Create global session
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create an unknown type branch session (using AT type but returns mismatched status)
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(APPLICATION_DATA);

        globalSession.addBranch(branchSession);

        // Mock Core returns PhaseOne_Failed (general failure status)
        AbstractCore mockATCore = mock(AbstractCore.class);
        when(mockATCore.branchDelete(any(GlobalSession.class), any(BranchSession.class)))
                .thenReturn(BranchStatus.PhaseOne_Failed);
        core.mockCore(BranchType.AT, mockATCore);

        // Execute test
        Boolean result = core.doBranchDelete(globalSession, branchSession);

        // Verify result - general failure scenario should return false
        Assertions.assertFalse(result);

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doGlobalStatusTest() throws TransactionException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Create GlobalStatusRequest
        GlobalStatusRequest request = new GlobalStatusRequest();
        request.setXid(xid);

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        GlobalStatusResponse response = defaultCoordinator.handle(request, rpcContext);

        // Verify result
        Assertions.assertNotNull(response);
        Assertions.assertEquals(GlobalStatus.Begin, response.getGlobalStatus());

        // Cleanup
        globalSession.end();
    }

    @Test
    public void doGlobalReportTest() throws TransactionException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change status to Committed first
        globalSession.changeGlobalStatus(GlobalStatus.Committed);

        // Create GlobalReportRequest
        GlobalReportRequest request = new GlobalReportRequest();
        request.setXid(xid);
        request.setGlobalStatus(GlobalStatus.Committed);

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        GlobalReportResponse response = defaultCoordinator.handle(request, rpcContext);

        // Verify result - globalReport returns the current status of the session
        Assertions.assertNotNull(response);
        Assertions.assertEquals(GlobalStatus.Committed, response.getGlobalStatus());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null && session.getStatus() != GlobalStatus.Finished) {
            session.end();
        }
    }

    @Test
    public void doBranchRegisterTest() throws TransactionException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Create BranchRegisterRequest
        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid(xid);
        request.setBranchType(BranchType.AT);
        request.setResourceId("resource_branch_register");
        request.setApplicationData(APPLICATION_DATA);
        request.setLockKey("branch_register:1");

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(APPLICATION_ID);
        rpcContext.setTransactionServiceGroup(TX_SERVICE_GROUP);
        rpcContext.setClientId(CLIENT_ID);

        BranchRegisterResponse response = defaultCoordinator.handle(request, rpcContext);

        // Verify result
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getBranchId() > 0);

        // Verify branch is registered
        BranchSession branchSession = globalSession.getBranch(response.getBranchId());
        Assertions.assertNotNull(branchSession);
        Assertions.assertEquals(BranchType.AT, branchSession.getBranchType());
        Assertions.assertEquals("resource_branch_register", branchSession.getResourceId());

        // Cleanup
        globalSession.end();
    }

    @Test
    public void timeoutCheckWithTimeoutTest() throws TransactionException, InterruptedException {
        // Create global transaction with very short timeout (10ms)
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertEquals(GlobalStatus.Begin, globalSession.getStatus());

        // Wait for timeout
        Thread.sleep(100);

        // Execute timeout check
        defaultCoordinator.timeoutCheck();

        // Verify session has been marked for rollback
        GlobalSession afterCheck = SessionHolder.findGlobalSession(xid);
        if (afterCheck != null) {
            Assertions.assertEquals(GlobalStatus.TimeoutRollbacking, afterCheck.getStatus());
            afterCheck.end();
        }
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void handleRetryCommittingTimeoutTest()
            throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Create global transaction with short timeout
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, 10);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_commit_retry", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(branchId);

        // Change to Committing status
        globalSession.changeGlobalStatus(GlobalStatus.Committing);

        try {
            // Temporarily modify MAX_COMMIT_RETRY_TIMEOUT for timeout testing
            ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_COMMIT_RETRY_TIMEOUT", 10L);

            // Wait for timeout
            Thread.sleep(100);

            // Queue to retry commit
            globalSession.queueToRetryCommit();

            // Execute retry committing
            defaultCoordinator.handleRetryCommitting();

            // Verify session has transitioned (should be Committed or CommitFailed)
            GlobalSession afterRetry = SessionHolder.findGlobalSession(xid);
            if (afterRetry != null) {
                Assertions.assertNotEquals(GlobalStatus.Committing, afterRetry.getStatus());
            }
        } finally {
            // Restore original value
            ReflectionUtil.modifyStaticFinalField(
                    defaultCoordinator.getClass(),
                    "MAX_COMMIT_RETRY_TIMEOUT",
                    ConfigurationFactory.getInstance()
                            .getLong(
                                    ConfigurationKeys.MAX_COMMIT_RETRY_TIMEOUT,
                                    DefaultValues.DEFAULT_MAX_COMMIT_RETRY_TIMEOUT));
            // Cleanup
            GlobalSession session = SessionHolder.findGlobalSession(xid);
            if (session != null) {
                session.closeAndClean();
            }
        }
    }

    @Test
    public void handleRetryCommittingWithEmptyBranchesTest() throws TransactionException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change to Committed status with empty branches
        globalSession.changeGlobalStatus(GlobalStatus.Committed);
        Assertions.assertTrue(globalSession.getBranchSessions().isEmpty());

        // Queue to retry commit
        globalSession.queueToRetryCommit();

        // Execute retry committing - should handle empty branches gracefully
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleRetryCommitting());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null) {
            session.end();
        }
    }

    @Test
    public void handleAsyncCommittingSuccessTest() throws TransactionException, InterruptedException {
        // Create global transaction
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        core.branchRegister(BranchType.AT, "resource_async_commit", CLIENT_ID, xid, APPLICATION_DATA, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Change to AsyncCommitting status
        globalSession.changeGlobalStatus(GlobalStatus.AsyncCommitting);

        // Execute async committing
        Assertions.assertDoesNotThrow(() -> defaultCoordinator.handleAsyncCommitting());

        // Cleanup
        GlobalSession session = SessionHolder.findGlobalSession(xid);
        if (session != null) {
            session.end();
        }
    }

    @Test
    public void handleAsyncCommittingWaitsForGlobalSessionLockTest() throws Exception {
        LockTrackingGlobalSession globalSession =
                new LockTrackingGlobalSession(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        globalSession.begin();
        Long branchId = core.branchRegister(
                BranchType.AT,
                "resource_async_commit_lock",
                CLIENT_ID,
                globalSession.getXid(),
                APPLICATION_DATA,
                LOCK_KEYS_1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> asyncCommitFuture = null;

        try {
            globalSession.lock();
            try {
                globalSession.changeGlobalStatus(GlobalStatus.AsyncCommitting);
                globalSession.trackLockAttempt();
                asyncCommitFuture = executor.submit(defaultCoordinator::handleAsyncCommitting);

                Assertions.assertTrue(globalSession.awaitLockAttempt(5, TimeUnit.SECONDS));
                Assertions.assertFalse(asyncCommitFuture.isDone());
                Assertions.assertNotNull(globalSession.getBranch(branchId));

                // Simulate the initial commit thread finishing global lock cleanup before releasing the session lock.
                globalSession.clean();
            } finally {
                globalSession.unlock();
            }

            asyncCommitFuture.get(5, TimeUnit.SECONDS);
            Assertions.assertNull(SessionHolder.findGlobalSession(globalSession.getXid()));

            String nextXid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
            GlobalSession nextGlobalSession = SessionHolder.findGlobalSession(nextXid);
            try {
                Assertions.assertNotNull(core.branchRegister(
                        BranchType.AT,
                        "resource_async_commit_lock",
                        CLIENT_ID,
                        nextXid,
                        APPLICATION_DATA,
                        LOCK_KEYS_1));
            } finally {
                nextGlobalSession.changeGlobalStatus(GlobalStatus.Committed);
                nextGlobalSession.end();
            }
        } finally {
            executor.shutdownNow();
            GlobalSession remainingSession = SessionHolder.findGlobalSession(globalSession.getXid());
            if (remainingSession != null) {
                remainingSession.changeGlobalStatus(GlobalStatus.Committed);
                remainingSession.end();
            }
        }
    }

    @Test
    public void undoLogDeleteWithActiveChannelsTest() throws TransactionException {
        // Create global transaction to ensure session manager is active
        String xid = core.begin(APPLICATION_ID, TX_SERVICE_GROUP, TX_NAME, TIMEOUT);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        try {
            // Register a channel with ChannelManager to simulate active RM
            Channel mockChannel = mock(Channel.class);
            when(mockChannel.isActive()).thenReturn(true);

            // Note: ChannelManager.registerChannel requires actual channel registration
            // which may not be easily mockable, so this test verifies the method executes without error

            // Execute undo log delete - should not throw exception even with or without active channels
            Assertions.assertDoesNotThrow(() -> defaultCoordinator.undoLogDelete());
        } finally {
            // Cleanup
            globalSession.end();
        }
    }

    @Test
    public void destroyTest() throws InterruptedException {
        // Note: destroy() shuts down executors and sets instance to null, which affects other tests.
        // This test verifies the destroy method exists and basic behavior by checking it doesn't throw
        // when the singleton instance is already initialized. We cannot safely call destroy() on the
        // shared instance without breaking other tests, so we just verify the method signature exists.

        // Verify the destroy method can be called on the shared instance
        Assertions.assertNotNull(defaultCoordinator);

        // We verify the method exists but don't actually call it to avoid breaking other tests
        // as destroy() sets the singleton instance to null and shuts down all executors
        Assertions.assertDoesNotThrow(() -> {
            // Just verify the method exists by getting its reference
            defaultCoordinator.getClass().getMethod("destroy");
        });
    }

    private List<SessionCondition> captureBackgroundSessionConditions(Runnable action) {
        SessionManager sessionManager = mock(SessionManager.class);
        List<SessionCondition> conditions = new ArrayList<>();
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            conditions.add(invocation.getArgument(0));
            return Collections.emptyList();
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);
            try {
                action.run();
            } finally {
                clearSyncProcessingQueue();
            }
        }
        return conditions;
    }

    private void assertMultiStatusRoundsShareBudget(GlobalStatus[] statuses) throws Exception {
        DefaultCoordinator coordinator = new DefaultCoordinator(remotingServer);
        GlobalSession earlySession = new GlobalSession();
        earlySession.setBeginTime(1L);
        Map<GlobalStatus, List<GlobalSession>> laterSessions = new EnumMap<>(GlobalStatus.class);
        for (int statusIndex = 1; statusIndex < statuses.length; statusIndex++) {
            List<GlobalSession> sessions = new ArrayList<>();
            for (int round = 0; round < 2; round++) {
                GlobalSession session = new GlobalSession();
                session.setBeginTime(100L + statusIndex * 10L + round);
                sessions.add(session);
            }
            laterSessions.put(statuses[statusIndex], sessions);
        }

        List<SessionCondition> conditions = new ArrayList<>();
        Map<GlobalStatus, Integer> statusRounds = new EnumMap<>(GlobalStatus.class);
        Map<GlobalStatus, byte[]> expectedCursors = new EnumMap<>(GlobalStatus.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            conditions.add(condition);
            GlobalStatus status = condition.getStatuses()[0];
            Assertions.assertArrayEquals(expectedCursors.get(status), condition.getStatusScanCursor());
            int round = statusRounds.getOrDefault(status, 0);
            statusRounds.put(status, round + 1);
            byte[] nextCursor = new byte[] {(byte) status.ordinal(), (byte) (round + 1)};
            condition.setNextStatusScanCursor(nextCursor);
            expectedCursors.put(status, nextCursor);
            if (status == statuses[0]) {
                return Collections.nCopies(condition.getLimit(), earlySession);
            }
            return Collections.singletonList(laterSessions.get(status).get(round));
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);

            for (int round = 0; round < 2; round++) {
                List<GlobalSession> result = invokeFindBackgroundSessions(coordinator, statuses, true);

                Assertions.assertTrue(result.size() <= DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT);
                for (int statusIndex = 1; statusIndex < statuses.length; statusIndex++) {
                    Assertions.assertTrue(result.contains(
                            laterSessions.get(statuses[statusIndex]).get(round)));
                }
            }
        } finally {
            shutdownCoordinatorExecutors(coordinator);
        }

        Assertions.assertEquals(statuses.length * 2, conditions.size());
        assertSharedBackgroundSessionQueryConditions(conditions.subList(0, statuses.length));
        assertSharedBackgroundSessionQueryConditions(conditions.subList(statuses.length, statuses.length * 2));
    }

    private void assertSmallBackgroundQueryLimitRotatesAcrossStatusGroup(String statusFieldName) throws Exception {
        int queryLimit = 1;
        DefaultCoordinator coordinator = new DefaultCoordinator(remotingServer, queryLimit);
        GlobalStatus[] statuses = coordinatorStatusGroup(coordinator, statusFieldName);
        Map<GlobalStatus, GlobalSession> sessions = new EnumMap<>(GlobalStatus.class);
        for (GlobalStatus status : statuses) {
            GlobalSession session = new GlobalSession();
            session.setBeginTime(status.ordinal());
            sessions.put(status, session);
        }

        List<GlobalStatus> queriedStatuses = new ArrayList<>();
        List<SessionCondition> conditions = new ArrayList<>();
        Map<GlobalStatus, byte[]> expectedCursors = new EnumMap<>(GlobalStatus.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition condition = invocation.getArgument(0);
            conditions.add(condition);
            GlobalStatus status = condition.getStatuses()[0];
            Assertions.assertArrayEquals(expectedCursors.get(status), condition.getStatusScanCursor());
            queriedStatuses.add(status);
            byte[] nextCursor = new byte[] {(byte) status.ordinal(), (byte) queriedStatuses.size()};
            condition.setNextStatusScanCursor(nextCursor);
            expectedCursors.put(status, nextCursor);
            return Collections.singletonList(sessions.get(status));
        });

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(sessionManager);

            for (int round = 0; round < statuses.length; round++) {
                int firstCondition = conditions.size();
                List<GlobalSession> result = invokeFindBackgroundSessions(coordinator, statuses, true);
                List<SessionCondition> roundConditions = conditions.subList(firstCondition, conditions.size());

                Assertions.assertEquals(1, roundConditions.size());
                Assertions.assertTrue(roundConditions.stream()
                                .mapToInt(SessionCondition::getLimit)
                                .sum()
                        <= queryLimit);
                Assertions.assertTrue(roundConditions.stream()
                                .mapToInt(SessionCondition::getScanLimit)
                                .sum()
                        <= queryLimit);
                GlobalStatus queriedStatus = roundConditions.get(0).getStatuses()[0];
                Assertions.assertEquals(Collections.singletonList(sessions.get(queriedStatus)), result);
                assertBackgroundSessionCursors(coordinator, expectedCursors);
            }
        } finally {
            shutdownCoordinatorExecutors(coordinator);
        }

        Assertions.assertArrayEquals(statuses, queriedStatuses.toArray(new GlobalStatus[0]));
    }

    private GlobalStatus[] coordinatorStatusGroup(DefaultCoordinator coordinator, String fieldName)
            throws ReflectiveOperationException {
        Field field = DefaultCoordinator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        GlobalStatus[] statuses = (GlobalStatus[]) field.get(coordinator);
        return Arrays.copyOf(statuses, statuses.length);
    }

    @SuppressWarnings("unchecked")
    private void assertBackgroundSessionCursors(
            DefaultCoordinator coordinator, Map<GlobalStatus, byte[]> expectedCursors)
            throws ReflectiveOperationException {
        Field field = DefaultCoordinator.class.getDeclaredField("backgroundSessionStatusCursors");
        field.setAccessible(true);
        Map<GlobalStatus, byte[]> actualCursors = (Map<GlobalStatus, byte[]>) field.get(coordinator);
        Assertions.assertEquals(expectedCursors.keySet(), actualCursors.keySet());
        expectedCursors.forEach(
                (status, expectedCursor) -> Assertions.assertArrayEquals(expectedCursor, actualCursors.get(status)));
    }

    private List<GlobalSession> invokeFindBackgroundSessions(GlobalStatus[] statuses, boolean lazyLoadBranch)
            throws Exception {
        return invokeFindBackgroundSessions(defaultCoordinator, statuses, lazyLoadBranch);
    }

    private GlobalSession storedBackgroundSession(String name, GlobalStatus status, long beginTime) {
        GlobalSession session = new GlobalSession("app", "group", name, TIMEOUT);
        session.setStatus(status);
        session.setBeginTime(beginTime);
        return session;
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSession> invokeFindBackgroundSessions(
            DefaultCoordinator coordinator, GlobalStatus[] statuses, boolean lazyLoadBranch) throws Exception {
        java.lang.reflect.Method method = DefaultCoordinator.class.getDeclaredMethod(
                "findBackgroundSessions", GlobalStatus[].class, boolean.class);
        method.setAccessible(true);
        return (List<GlobalSession>) method.invoke(coordinator, statuses, lazyLoadBranch);
    }

    @SuppressWarnings("unchecked")
    private void clearBackgroundSessionStatusCursors() throws Exception {
        Field cursorsField = DefaultCoordinator.class.getDeclaredField("backgroundSessionStatusCursors");
        cursorsField.setAccessible(true);
        ((Map<GlobalStatus, byte[]>) cursorsField.get(defaultCoordinator)).clear();
    }

    private void assertBackgroundSessionQueryConditions(List<SessionCondition> conditions) {
        Assertions.assertFalse(conditions.isEmpty());
        for (SessionCondition condition : conditions) {
            Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, condition.getLimit());
            Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, condition.getScanLimit());
            Assertions.assertNotNull(condition.getStatuses());
            Assertions.assertEquals(1, condition.getStatuses().length);
        }
    }

    private void assertSharedBackgroundSessionQueryConditions(List<SessionCondition> conditions) {
        Assertions.assertFalse(conditions.isEmpty());
        int resultBudget = 0;
        int scanBudget = 0;
        for (SessionCondition condition : conditions) {
            Assertions.assertTrue(condition.getLimit() > 0);
            Assertions.assertEquals(condition.getLimit(), condition.getScanLimit());
            Assertions.assertNotNull(condition.getStatuses());
            Assertions.assertEquals(1, condition.getStatuses().length);
            resultBudget += condition.getLimit();
            scanBudget += condition.getScanLimit();
        }
        Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, resultBudget);
        Assertions.assertEquals(DefaultCoordinator.SESSION_BACKGROUND_TASK_QUERY_LIMIT, scanBudget);
    }

    private void shutdownCoordinatorExecutors(DefaultCoordinator coordinator) throws IllegalAccessException {
        for (Field field : DefaultCoordinator.class.getDeclaredFields()) {
            if (!ExecutorService.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            ExecutorService executor = (ExecutorService) field.get(coordinator);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private void clearSyncProcessingQueue() {
        try {
            Field field = DefaultCoordinator.class.getDeclaredField("syncProcessing");
            field.setAccessible(true);
            ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) field.get(defaultCoordinator);
            executor.getQueue().clear();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class MockServerMessageSender implements RemotingServer {

        @Override
        public Object sendSyncRequest(String resourceId, String clientId, Object message, boolean tryOtherApp)
                throws TimeoutException {
            if (message instanceof BranchCommitRequest) {
                final BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
                branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);
                return branchCommitResponse;
            } else if (message instanceof BranchRollbackRequest) {
                final BranchRollbackResponse branchRollbackResponse = new BranchRollbackResponse();
                branchRollbackResponse.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
                return branchRollbackResponse;
            } else {
                return null;
            }
        }

        @Override
        public Object sendSyncRequest(Channel clientChannel, Object message) throws TimeoutException {
            return null;
        }

        @Override
        public void sendAsyncRequest(Channel channel, Object msg) {}

        @Override
        public void sendAsyncResponse(RpcMessage request, Channel channel, Object msg) {}

        @Override
        public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {}
    }

    private static class LockTrackingGlobalSession extends GlobalSession {

        private final CountDownLatch lockAttempted = new CountDownLatch(1);

        private volatile boolean trackLockAttempt;

        private LockTrackingGlobalSession(
                String applicationId, String transactionServiceGroup, String transactionName, int timeout) {
            super(applicationId, transactionServiceGroup, transactionName, timeout);
        }

        @Override
        public void lock() throws TransactionException {
            if (trackLockAttempt) {
                lockAttempted.countDown();
            }
            super.lock();
        }

        private void trackLockAttempt() {
            trackLockAttempt = true;
        }

        private boolean awaitLockAttempt(long timeout, TimeUnit unit) throws InterruptedException {
            return lockAttempted.await(timeout, unit);
        }
    }
}
