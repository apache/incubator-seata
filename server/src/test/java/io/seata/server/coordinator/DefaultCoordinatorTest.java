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

import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.netty.channel.Channel;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

/**
 * The type DefaultCoordinator test.
 *
 * @author leizhiyuan
 */
public class DefaultCoordinatorTest {
    private static ServerMessageSender serverMessageSender;
    private static DefaultCoordinator defaultCoordinator;

    private static final String applicationId = "demo-child-app";

    private static final String txServiceGroup = "my_test_tx_group";

    private static final String txName = "tx-1";

    private static final int timeout = 3000;

    private static final String resourceId = "tb_1";

    private static final String clientId = "c_1";

    private static final String lockKeys_1 = "tb_1:11";

    private static final String lockKeys_2 = "tb_1:12";

    private static final String applicationData = "{\"data\":\"test\"}";

    private static Core core = new DefaultCore();

    @BeforeClass
    public static void beforeClass() throws Exception {
        XID.setIpAddress(NetUtil.getLocalIp());
        SessionHolder.init(null);
        serverMessageSender = new MockServerMessageSender();
        defaultCoordinator = new DefaultCoordinator(serverMessageSender);
        defaultCoordinator.init();
    }

    @Test(dataProvider = "xidAndBranchIdProviderForCommit")
    public void branchCommit(String xid, Long branchId) {
        BranchStatus result = null;

        try {
            result = defaultCoordinator.branchCommit(BranchType.AT, xid, branchId, resourceId, applicationData);
        } catch (TransactionException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(result, BranchStatus.PhaseTwo_Committed);

    }

    @Test(dataProvider = "xidAndBranchIdProviderForRollback")
    public void branchRollback(String xid, Long branchId) {
        BranchStatus result = null;
        try {
            result = defaultCoordinator.branchRollback(BranchType.AT, xid, branchId, resourceId, applicationData);
        } catch (TransactionException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(result, BranchStatus.PhaseTwo_Rollbacked);

    }

    @AfterClass
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


    @DataProvider
    public static Object[][] xidAndBranchIdProviderForCommit() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_1);
        return new Object[][]{{xid, branchId}};
    }

    @DataProvider
    public static Object[][] xidAndBranchIdProviderForRollback() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_2);
        return new Object[][]{{xid, branchId}};
    }


    private static class MockServerMessageSender implements ServerMessageSender {

        @Override
        public void sendResponse(long msgId, Channel channel, Object msg) {

        }

        @Override
        public Object sendSyncRequest(String resourceId, String clientId, Object message, long timeout) throws IOException, TimeoutException {
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
        public Object sendSyncRequest(String resourceId, String clientId, Object message) throws IOException, TimeoutException {

            return sendSyncRequest(resourceId, clientId, message, 3000);

        }
    }
}