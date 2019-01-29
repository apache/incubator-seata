/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.server.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The type File based session manager test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/22
 */
public class FileBasedSessionManagerTest {

    private SessionManager sessionManager = null;

    /**
     * Sets up.
     *
     * @throws Exception the exception
     */
    @BeforeClass
    public void setUp() throws Exception {
        sessionManager = new FileBasedSessionManager("root.data", ".");
    }

    /**
     * Add global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void addGlobalSessionTest(GlobalSession globalSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * Find global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void findGlobalSessionTest(GlobalSession globalSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        GlobalSession expected = sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNotNull(expected);
        Assert.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
        Assert.assertEquals(expected.getApplicationId(), globalSession.getApplicationId());
        Assert.assertEquals(expected.getTransactionServiceGroup(), globalSession.getTransactionServiceGroup());
        Assert.assertEquals(expected.getTransactionName(), globalSession.getTransactionName());
        Assert.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
        Assert.assertEquals(expected.getStatus(), globalSession.getStatus());
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * Update global session status test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void updateGlobalSessionStatusTest(GlobalSession globalSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        globalSession.setStatus(GlobalStatus.Finished);
        sessionManager.updateGlobalSessionStatus(globalSession, GlobalStatus.Finished);
        GlobalSession expected = sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNotNull(expected);
        Assert.assertEquals(GlobalStatus.Finished, expected.getStatus());
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * Remove global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void removeGlobalSessionTest(GlobalSession globalSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        sessionManager.removeGlobalSession(globalSession);
        GlobalSession expected = sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNull(expected);

    }

    /**
     * Add branch session test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void addBranchSessionTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        sessionManager.addBranchSession(globalSession, branchSession);
        sessionManager.removeBranchSession(globalSession, branchSession);
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * Update branch session status test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void updateBranchSessionStatusTest(GlobalSession globalSession, BranchSession branchSession)
        throws Exception {
        sessionManager.addGlobalSession(globalSession);
        sessionManager.addBranchSession(globalSession, branchSession);
        sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseTwo_Committed);
        sessionManager.removeBranchSession(globalSession, branchSession);
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * Remove branch session test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void removeBranchSessionTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        sessionManager.addGlobalSession(globalSession);
        sessionManager.addBranchSession(globalSession, branchSession);
        sessionManager.removeBranchSession(globalSession, branchSession);
        sessionManager.removeGlobalSession(globalSession);
    }

    /**
     * All sessions test.
     *
     * @param globalSessions the global sessions
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionsProvider")
    public void allSessionsTest(List<GlobalSession> globalSessions) throws Exception {
        for (GlobalSession globalSession : globalSessions) {
            sessionManager.addGlobalSession(globalSession);
        }
        Collection<GlobalSession> expectedGlobalSessions = sessionManager.allSessions();
        Assert.assertNotNull(expectedGlobalSessions);
        Assert.assertEquals(2, expectedGlobalSessions.size());
        for (GlobalSession globalSession : globalSessions) {
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Find global sessions test.
     *
     * @param globalSessions the global sessions
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionsProvider")
    public void findGlobalSessionsTest(List<GlobalSession> globalSessions) throws Exception {
        for (GlobalSession globalSession : globalSessions) {
            sessionManager.addGlobalSession(globalSession);
        }
        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.Begin, 30 * 24 * 3600);
        Collection<GlobalSession> expectedGlobalSessions = sessionManager.findGlobalSessions(sessionCondition);
        Assert.assertNotNull(expectedGlobalSessions);
        Assert.assertEquals(2, expectedGlobalSessions.size());
        for (GlobalSession globalSession : globalSessions) {
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * On begin test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void onBeginTest(GlobalSession globalSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onEnd(globalSession);
    }

    /**
     * On status change test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void onStatusChangeTest(GlobalSession globalSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onStatusChange(globalSession, GlobalStatus.Finished);
        sessionManager.onEnd(globalSession);
    }

    /**
     * On branch status change test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void onBranchStatusChangeTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onAddBranch(globalSession, branchSession);
        sessionManager.onBranchStatusChange(globalSession, branchSession, BranchStatus.PhaseTwo_Committed);
    }

    /**
     * On add branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void onAddBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onAddBranch(globalSession, branchSession);
    }

    /**
     * On remove branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void onRemoveBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onAddBranch(globalSession, branchSession);
        sessionManager.onRemoveBranch(globalSession, branchSession);
    }

    /**
     * On close test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void onCloseTest(GlobalSession globalSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onClose(globalSession);
    }

    /**
     * On end test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void onEndTest(GlobalSession globalSession) throws Exception {
        sessionManager.onBegin(globalSession);
        sessionManager.onEnd(globalSession);
    }

    /**
     * Global session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] globalSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        return new Object[][] {{globalSession}};
    }

    /**
     * Global sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] globalSessionsProvider() {
        GlobalSession globalSession1 = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        GlobalSession globalSession2 = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        return new Object[][] {{Arrays.asList(globalSession1, globalSession2)}};
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] branchSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        return new Object[][] {{globalSession, branchSession}};
    }
}
