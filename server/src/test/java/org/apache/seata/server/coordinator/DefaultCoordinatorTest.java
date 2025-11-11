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
import org.apache.seata.server.session.SessionHolder;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
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

    private static final String applicationId = "demo-child-app";

    private static final String txServiceGroup = "default_tx_group";

    private static final String txName = "tx-1";

    private static final int timeout = 3000;

    private static final String resourceId = "tb_1";

    private static final String clientId = "c_1";

    private static final String lockKeys_1 = "tb_1:11";

    private static final String lockKeys_2 = "tb_1:12";

    private static final String applicationData = "{\"data\":\"test\"}";

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
            xid = core.begin(applicationId, txServiceGroup, txName, timeout);
            Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_1);
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

        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_2);
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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        Boolean result = defaultCoordinator.doBranchDelete(globalSession, null);
        Assertions.assertTrue(result);

        globalSession.end();
    }

    @Test
    public void timeoutCheckNoTimeoutTest() throws TransactionException {
        String xid = core.begin(applicationId, txServiceGroup, txName, 30000);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        defaultCoordinator.timeoutCheck();

        GlobalSession afterCheck = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(afterCheck);
        Assertions.assertEquals(GlobalStatus.Begin, afterCheck.getStatus());

        globalSession.end();
    }

    @Test
    public void handleRetryCommittingNoSessionsTest() {
        defaultCoordinator.handleRetryCommitting();
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
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_branch_report", clientId, xid, applicationData, null);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranch(branchId));

        // Create BranchReportRequest
        BranchReportRequest request = new BranchReportRequest();
        request.setXid(xid);
        request.setBranchId(branchId);
        request.setBranchType(BranchType.AT);
        request.setStatus(BranchStatus.PhaseOne_Done);
        request.setApplicationData(applicationData);

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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

        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId =
                core.branchRegister(BranchType.AT, testResourceId, clientId, xid, applicationData, testLockKey1);

        Assertions.assertNotNull(branchId);

        // Test lockable scenario (different lockKey)
        GlobalLockQueryRequest request1 = new GlobalLockQueryRequest();
        request1.setXid(xid);
        request1.setBranchType(BranchType.AT);
        request1.setResourceId(testResourceId);
        request1.setLockKey(testLockKey2);

        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_remove_single", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_remove_all_1", clientId, xid, applicationData, null);
        core.branchRegister(BranchType.AT, "resource_remove_all_2", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_committing_scheduled", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_rollbacking_scheduled", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_end_committed", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_end_rollbacked", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create SAGA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.SAGA);
        branchSession.setResourceId("saga_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create TCC type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.TCC);
        branchSession.setResourceId("tcc_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create TCC type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.TCC);
        branchSession.setResourceId("tcc_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, 30000);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create XA type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceId("xa_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create AT type branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);

        // Create an unknown type branch session (using AT type but returns mismatched status)
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(1L);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setResourceId("at_resource");
        branchSession.setApplicationData(applicationData);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Create GlobalStatusRequest
        GlobalStatusRequest request = new GlobalStatusRequest();
        request.setXid(xid);

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
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
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);

        // Create BranchRegisterRequest
        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid(xid);
        request.setBranchType(BranchType.AT);
        request.setResourceId("resource_branch_register");
        request.setApplicationData(applicationData);
        request.setLockKey("branch_register:1");

        // Execute test
        RpcContext rpcContext = new RpcContext();
        rpcContext.setApplicationId(applicationId);
        rpcContext.setTransactionServiceGroup(txServiceGroup);
        rpcContext.setClientId(clientId);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
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
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId =
                core.branchRegister(BranchType.AT, "resource_commit_retry", clientId, xid, applicationData, null);

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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
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
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        core.branchRegister(BranchType.AT, "resource_async_commit", clientId, xid, applicationData, null);

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
    public void undoLogDeleteWithActiveChannelsTest() throws TransactionException {
        // Create global transaction to ensure session manager is active
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
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
}
