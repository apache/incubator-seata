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
package com.alibaba.fescar.server.coordinator;

import java.util.Collection;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.session.BranchSession;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionHolder;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author zhimo.xiao@gmail.com
 * @since 2019/1/23
 */

public class DefaultCoreTest {

    private static Core core = new DefaultCore();

    private static final String applicationId = "demo-child-app";

    private static final String txServiceGroup = "my_test_tx_group";

    private static final String txName = "tx-1";

    private static final int timeout = 3000;

    private static final String resourceId = "tb_1";

    private static final String clientId = "c_1";

    private static final String lockKeys_1 = "tb_1:11";

    private static final String lockKeys_2 = "tb_1:12";

    private static final String applicationData = "{\"data\":\"test\"}";

    @BeforeTest
    public void initSessionManager() throws Exception {
        SessionHolder.init(null);
    }

    @Test(dataProvider = "xidProvider")
    public void branchRegisterTest(String xid) throws Exception {
        core.branchRegister(BranchType.AT, resourceId, clientId, xid, lockKeys_1);

        long transactionId = XID.getTransactionId(xid);
        GlobalSession globalSession = SessionHolder.findGlobalSession(transactionId);
        Assert.assertEquals(globalSession.getSortedBranches().size(), 1);
    }

    @Test(dataProvider = "xidAndBranchIdProvider")
    public void branchReportTest(String xid, Long branchId) throws Exception {
        core.branchReport(xid, branchId, BranchStatus.PhaseOne_Done, applicationData);

        long transactionId = XID.getTransactionId(xid);
        GlobalSession globalSession = SessionHolder.findGlobalSession(transactionId);
        BranchSession branchSession = globalSession.getBranch(branchId);
        Assert.assertEquals(branchSession.getStatus(), BranchStatus.PhaseOne_Done);
    }

    @Test
    public void beginTest() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);

        long transactionId = XID.getTransactionId(xid);
        GlobalSession globalSession = SessionHolder.findGlobalSession(transactionId);
        Assert.assertNotNull(globalSession);
    }

    @Test(dataProvider = "xidProvider")
    public void commitTest(String xid) throws Exception {
        GlobalStatus globalStatus = core.commit(xid);
        Assert.assertEquals(globalStatus, GlobalStatus.Begin);
    }

    @Test(dataProvider = "xidProvider")
    public void doGlobalCommitTest(String xid) throws Exception {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        core.doGlobalCommit(globalSession, false);
        Assert.assertEquals(globalSession.getStatus(), GlobalStatus.Committed);
    }

    @Test(dataProvider = "xidProvider")
    public void rollBackTest(String xid) throws Exception {
        GlobalStatus globalStatus = core.rollback(xid);
        Assert.assertEquals(globalStatus, GlobalStatus.Rollbacked);
    }

    @Test(dataProvider = "xidProvider")
    public void doGlobalRollBackTest(String xid) throws Exception {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        core.doGlobalRollback(globalSession, false);
        Assert.assertEquals(globalSession.getStatus(), GlobalStatus.Rollbacked);
    }

    @DataProvider
    public static Object[][] xidProvider() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        return new Object[][] {{xid}};
    }

    @DataProvider
    public static Object[][] xidAndBranchIdProvider() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, lockKeys_2);
        return new Object[][] {{xid, branchId}};
    }

    @AfterTest
    public void releaseSessionManager() throws Exception {
        Collection<GlobalSession> globalSessions = SessionHolder.getRootSessionManager().allSessions();
        Collection<GlobalSession> asyncGlobalSessions = SessionHolder.getAsyncCommittingSessionManager().allSessions();
        for (GlobalSession asyncGlobalSession : asyncGlobalSessions) {
            asyncGlobalSession.closeAndClean();
        }
        for (GlobalSession globalSession : globalSessions) {
            globalSession.closeAndClean();
        }
    }

}
