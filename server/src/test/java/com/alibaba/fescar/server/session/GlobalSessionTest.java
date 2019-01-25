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
 * @author tianming.xm@gmail.com
 * @since 2019/1/23
 */
public class GlobalSessionTest {

    @Test(dataProvider = "branchSessionMTProvider")
    public void canBeCommittedAsyncTest(GlobalSession globalSession) {
        Assert.assertFalse(globalSession.canBeCommittedAsync());
    }

    @Test(dataProvider = "globalSessionProvider")
    public void beginTest(GlobalSession globalSession) throws Exception {
        globalSession.begin();
    }

    @Test(dataProvider = "globalSessionProvider")
    public void changeStatusTest(GlobalSession globalSession) throws Exception {
        globalSession.changeStatus(GlobalStatus.Committed);
    }

    @Test(dataProvider = "branchSessionProvider")
    public void changeBranchStatusTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.changeBranchStatus(branchSession, BranchStatus.PhaseTwo_Committed);
    }

    @Test(dataProvider = "globalSessionProvider")
    public void closeTest(GlobalSession globalSession) throws Exception {
        globalSession.close();
    }

    @Test(dataProvider = "globalSessionProvider")
    public void endTest(GlobalSession globalSession) throws Exception {
        globalSession.end();
    }

    @Test(dataProvider = "branchSessionProvider")
    public void addBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
    }

    @Test(dataProvider = "branchSessionProvider")
    public void removeBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
        globalSession.removeBranch(branchSession);
    }

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

    @DataProvider
    public static Object[][] globalSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        globalSession.setActive(true);
        globalSession.addSessionLifecycleListener(new DefaultSessionManager("default"));
        return new Object[][] {{globalSession}};
    }

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
