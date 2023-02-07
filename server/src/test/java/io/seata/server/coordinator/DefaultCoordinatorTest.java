/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.coordinator;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.netty.channel.Channel;
import io.seata.common.DefaultValues;
import io.seata.common.XID;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.NetUtil;
import io.seata.common.util.ReflectionUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.server.metrics.MetricsManager;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.store.StoreConfig.SessionMode;
import io.seata.server.util.StoreUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * The type DefaultCoordinator test.
 *
 * @author leizhiyuan
 */
@SpringBootTest
public class DefaultCoordinatorTest {
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

    @BeforeAll
    public static void beforeClass(ApplicationContext context) throws Exception {
        EnhancedServiceLoader.unload(AbstractCore.class);
        XID.setIpAddress(NetUtil.getLocalIp());
        RemotingServer remotingServer = new MockServerMessageSender();
        defaultCoordinator =DefaultCoordinator.getInstance(remotingServer);
        defaultCoordinator.setRemotingServer(remotingServer);
        core = new DefaultCore(remotingServer);
    }

    @BeforeEach
    public void tearUp() throws IOException {
        deleteAndCreateDataFile();
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
    public void test_handleRetryRollbacking() throws TransactionException, InterruptedException {

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
    @DisabledOnJre(JRE.JAVA_17) // `ReflectionUtil.modifyStaticFinalField` does not supported java17
    public void test_handleRetryRollbackingTimeOut() throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", 10L);
        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", false);
        TimeUnit.MILLISECONDS.sleep(100);
        globalSession.queueToRetryRollback();
        defaultCoordinator.handleRetryRollbacking();
        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertTrue(lockSize > 0);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT",
                ConfigurationFactory.getInstance().getLong(ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DefaultValues.DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT));
        }
    }

    @Test
    @DisabledOnJre(JRE.JAVA_17) // `ReflectionUtil.modifyStaticFinalField` does not supported java17
    public void test_handleRetryRollbackingTimeOut_unlock() throws TransactionException, InterruptedException,
        NoSuchFieldException, IllegalAccessException {
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", 10L);
        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", true);
        TimeUnit.MILLISECONDS.sleep(100);

        globalSession.queueToRetryRollback();
        defaultCoordinator.handleRetryRollbacking();

        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertTrue(lockSize == 0);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT",
                ConfigurationFactory.getInstance().getLong(ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DefaultValues.DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT));
        }
    }

    @AfterAll
    public static void afterClass() throws Exception {

        Collection<GlobalSession> globalSessions = SessionHolder.getRootSessionManager().allSessions();
        Collection<GlobalSession> asyncGlobalSessions = SessionHolder.getAsyncCommittingSessionManager().allSessions();
        for (GlobalSession asyncGlobalSession : asyncGlobalSessions) {
            asyncGlobalSession.closeAndClean();
        }
        for (GlobalSession globalSession : globalSessions) {
            globalSession.closeAndClean();
        }
    }

    private static void deleteAndCreateDataFile() throws IOException {
        StoreUtil.deleteDataFile();
        SessionHolder.init(SessionMode.FILE);
    }

    @AfterEach
    public void tearDown() throws IOException {
        MetricsManager.get().getRegistry().clearUp();
        StoreUtil.deleteDataFile();
    }

    static Stream<Arguments> xidAndBranchIdProviderForRollback() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_2);
        return Stream.of(
            Arguments.of(xid, branchId)
        );
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
        public void sendAsyncRequest(Channel channel, Object msg) {

        }

        @Override
        public void sendAsyncResponse(RpcMessage request, Channel channel, Object msg) {

        }

        @Override
        public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {

        }
    }
}