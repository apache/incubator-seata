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

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The type Global session test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class GlobalSessionTest {

    /**
     * Can be committed async test.
     *
     * @param globalSession the global session
     */
    @Test(dataProvider = "branchSessionMTProvider")
    public void canBeCommittedAsyncTest(GlobalSession globalSession) {
        Assert.assertFalse(globalSession.canBeCommittedAsync());
    }

    /**
     * Begin test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void beginTest(GlobalSession globalSession) throws Exception {
        globalSession.begin();
    }

    /**
     * Change status test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void changeStatusTest(GlobalSession globalSession) throws Exception {
        globalSession.changeStatus(GlobalStatus.Committed);
    }

    /**
     * Change branch status test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void changeBranchStatusTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.changeBranchStatus(branchSession, BranchStatus.PhaseTwo_Committed);
    }

    /**
     * Close test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void closeTest(GlobalSession globalSession) throws Exception {
        globalSession.close();
    }

    /**
     * End test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @Test(dataProvider = "globalSessionProvider")
    public void endTest(GlobalSession globalSession) throws Exception {
        globalSession.end();
    }

    /**
     * Add branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void addBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
    }

    /**
     * Remove branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @Test(dataProvider = "branchSessionProvider")
    public void removeBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
        globalSession.removeBranch(branchSession);
    }

    /**
     * Codec test.
     *
     * @param globalSession the global session
     */
    @Test(dataProvider = "globalSessionProvider")
    public void codecTest(GlobalSession globalSession) {
        byte[] result = globalSession.encode();
        Assert.assertNotNull(result);
        GlobalSession expected = new GlobalSession();
        expected.decode(result);
        Assert.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
        Assert.assertEquals(expected.getTimeout(), globalSession.getTimeout());
        Assert.assertEquals(expected.getApplicationId(), globalSession.getApplicationId());
        Assert.assertEquals(expected.getTransactionServiceGroup(), globalSession.getTransactionServiceGroup());
        Assert.assertEquals(expected.getTransactionName(), globalSession.getTransactionName());
    }

    /**
     * Global session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] globalSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        globalSession.setActive(true);
        globalSession.addSessionLifecycleListener(new DefaultSessionManager("default"));
        return new Object[][] {{globalSession}};
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
        branchSession.setBranchType(BranchType.AT);
        globalSession.add(branchSession);
        return new Object[][] {{globalSession, branchSession}};
    }

    /**
     * Branch session mt provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] branchSessionMTProvider() {
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
        branchSession.setBranchType(BranchType.MT);
        globalSession.add(branchSession);
        return new Object[][] {{globalSession}};
    }
}
