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

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.GlobalTransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * The type Abstract core test.
 */
public class AbstractCoreTest extends BaseSpringBootTest {

    private static TestableAbstractCore abstractCore;
    private static RemotingServer remotingServer;

    private static final String applicationId = "demo-app";
    private static final String txServiceGroup = "default_tx_group";
    private static final String txName = "test-tx";
    private static final int timeout = 3000;
    private static final String resourceId = "tb_test";
    private static final String clientId = "test_client";
    private static final String lockKeys = "tb_test:1";
    private static final String applicationData = "{\"data\":\"test\"}";

    @BeforeAll
    public static void initSessionManager(ApplicationContext context) throws Exception {
        SessionHolder.init(SessionMode.FILE);
        remotingServer = new DefaultCoordinatorTest.MockServerMessageSender();
        abstractCore = new TestableAbstractCore(remotingServer);
    }

    @AfterAll
    public static void destroySessionManager() {
        SessionHolder.destroy();
    }

    @AfterEach
    public void cleanSessions() throws Exception {
        Collection<GlobalSession> globalSessions =
                SessionHolder.getRootSessionManager().allSessions();
        for (GlobalSession globalSession : globalSessions) {
            globalSession.closeAndClean();
        }
    }

    @Test
    public void constructorWithValidRemotingServerTest() {
        Assertions.assertNotNull(abstractCore);
        Assertions.assertNotNull(abstractCore.remotingServer);
    }

    @Test
    public void constructorWithNullRemotingServerTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new TestableAbstractCore(null);
        });
    }

    @Test
    public void branchRegisterSuccessTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();

        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);

        Assertions.assertNotNull(branchId);
        Assertions.assertTrue(branchId > 0);

        GlobalSession foundSession = SessionHolder.findGlobalSession(globalSession.getXid());
        Assertions.assertEquals(1, foundSession.getBranchSessions().size());

        globalSession.end();
    }

    @Test
    public void LBranchRegisterWithNullLockKeysTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();

        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, null);

        Assertions.assertNotNull(branchId);
        Assertions.assertTrue(branchId > 0);

        globalSession.end();
    }

    @Test
    public void LBranchRegisterGlobalSessionNotFoundTest() {
        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.branchRegister(BranchType.AT, resourceId, clientId, "invalid_xid", applicationData, lockKeys);
        });
    }

    @Test
    public void LBranchRegisterGlobalSessionNotActiveTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        globalSession.changeGlobalStatus(GlobalStatus.Committed);

        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.branchRegister(
                    BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);
        });

        globalSession.end();
    }

    @Test
    public void LBranchRegisterGlobalSessionStatusInvalidTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        globalSession.changeGlobalStatus(GlobalStatus.Committing);

        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.branchRegister(
                    BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);
        });

        globalSession.end();
    }

    @Test
    public void LBranchReportSuccessTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);

        abstractCore.branchReport(
                BranchType.AT, globalSession.getXid(), branchId, BranchStatus.PhaseOne_Done, applicationData);

        GlobalSession foundSession = SessionHolder.findGlobalSession(globalSession.getXid());
        BranchSession branchSession = foundSession.getBranch(branchId);
        Assertions.assertEquals(BranchStatus.PhaseOne_Done, branchSession.getStatus());

        globalSession.end();
    }

    @Test
    public void LBranchReportGlobalSessionNotFoundTest() {
        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.branchReport(BranchType.AT, "invalid_xid", 1L, BranchStatus.PhaseOne_Done, applicationData);
        });
    }

    @Test
    public void LBranchReportBranchSessionNotFoundTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();

        Assertions.assertThrows(BranchTransactionException.class, () -> {
            abstractCore.branchReport(
                    BranchType.AT, globalSession.getXid(), 999L, BranchStatus.PhaseOne_Done, applicationData);
        });

        globalSession.end();
    }

    @Test
    public void LBranchReportUpdateApplicationDataTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);

        String newApplicationData = "{\"data\":\"updated\"}";
        abstractCore.branchReport(
                BranchType.AT, globalSession.getXid(), branchId, BranchStatus.PhaseOne_Done, newApplicationData);

        GlobalSession foundSession = SessionHolder.findGlobalSession(globalSession.getXid());
        BranchSession branchSession = foundSession.getBranch(branchId);
        Assertions.assertEquals(newApplicationData, branchSession.getApplicationData());

        globalSession.end();
    }

    @Test
    public void LLockQueryDefaultReturnTrueTest() throws Exception {
        boolean result = abstractCore.lockQuery(BranchType.AT, resourceId, "test_xid", lockKeys);
        Assertions.assertTrue(result);
    }

    @Test
    public void LBranchCommitSuccessTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);

        BranchSession branchSession = globalSession.getBranch(branchId);
        BranchStatus status = abstractCore.branchCommit(globalSession, branchSession);

        Assertions.assertEquals(BranchStatus.PhaseTwo_Committed, status);

        globalSession.end();
    }

    @Test
    public void LBranchRollbackSuccessTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);

        BranchSession branchSession = globalSession.getBranch(branchId);
        BranchStatus status = abstractCore.branchRollback(globalSession, branchSession);

        Assertions.assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);

        globalSession.end();
    }

    @Test
    public void LGlobalSessionStatusCheckActiveSessionTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();

        // Should not throw exception
        abstractCore.globalSessionStatusCheck(globalSession);

        globalSession.end();
    }

    @Test
    public void LGlobalSessionStatusCheckInactiveSessionTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        globalSession.changeGlobalStatus(GlobalStatus.Committed);

        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.globalSessionStatusCheck(globalSession);
        });

        globalSession.end();
    }

    @Test
    public void LGlobalSessionStatusCheckInvalidStatusTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        globalSession.changeGlobalStatus(GlobalStatus.Committing);

        Assertions.assertThrows(GlobalTransactionException.class, () -> {
            abstractCore.globalSessionStatusCheck(globalSession);
        });

        globalSession.end();
    }

    @Test
    public void LBeginReturnsNullTest() throws Exception {
        String result = abstractCore.begin(applicationId, txServiceGroup, txName, timeout);
        Assertions.assertNull(result);
    }

    @Test
    public void LCommitReturnsNullTest() throws Exception {
        GlobalStatus result = abstractCore.commit("test_xid");
        Assertions.assertNull(result);
    }

    @Test
    public void LDoGlobalCommitReturnsTrueTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        boolean result = abstractCore.doGlobalCommit(globalSession, false);
        Assertions.assertTrue(result);
        globalSession.end();
    }

    @Test
    public void LGlobalReportReturnsNullTest() throws Exception {
        GlobalStatus result = abstractCore.globalReport("test_xid", GlobalStatus.Committed);
        Assertions.assertNull(result);
    }

    @Test
    public void LRollbackReturnsNullTest() throws Exception {
        GlobalStatus result = abstractCore.rollback("test_xid");
        Assertions.assertNull(result);
    }

    @Test
    public void LDoGlobalRollbackReturnsTrueTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        boolean result = abstractCore.doGlobalRollback(globalSession, false);
        Assertions.assertTrue(result);
        globalSession.end();
    }

    @Test
    public void LGetStatusReturnsNullTest() throws Exception {
        GlobalStatus result = abstractCore.getStatus("test_xid");
        Assertions.assertNull(result);
    }

    @Test
    public void LDoGlobalReportNoExceptionTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        // Should not throw exception
        abstractCore.doGlobalReport(globalSession, globalSession.getXid(), GlobalStatus.Committed);
        globalSession.end();
    }

    @Test
    public void LDoBranchDeleteReturnsTrueTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);
        BranchSession branchSession = globalSession.getBranch(branchId);

        Boolean result = abstractCore.doBranchDelete(globalSession, branchSession);
        Assertions.assertTrue(result);

        globalSession.end();
    }

    @Test
    public void LBranchDeleteReturnsNullTest() throws Exception {
        GlobalSession globalSession = createGlobalSession();
        Long branchId = abstractCore.branchRegister(
                BranchType.AT, resourceId, clientId, globalSession.getXid(), applicationData, lockKeys);
        BranchSession branchSession = globalSession.getBranch(branchId);

        BranchStatus result = abstractCore.branchDelete(globalSession, branchSession);
        Assertions.assertNull(result);

        globalSession.end();
    }

    private GlobalSession createGlobalSession() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession(applicationId, txServiceGroup, txName, timeout);
        globalSession.begin();
        return globalSession;
    }

    /**
     * Testable implementation of AbstractCore for testing purposes
     */
    private static class TestableAbstractCore extends AbstractCore {

        public TestableAbstractCore(RemotingServer remotingServer) {
            super(remotingServer);
        }

        @Override
        public BranchType getHandleBranchType() {
            return BranchType.AT;
        }

        // Expose protected method for testing
        public void globalSessionStatusCheck(GlobalSession globalSession) throws GlobalTransactionException {
            super.globalSessionStatusCheck(globalSession);
        }
    }
}
