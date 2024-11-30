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
package org.apache.seata.server.raft.execute;

import java.util.ArrayList;
import java.util.List;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.cluster.raft.execute.branch.AddBranchSessionExecute;
import org.apache.seata.server.cluster.raft.execute.branch.RemoveBranchSessionExecute;
import org.apache.seata.server.cluster.raft.execute.branch.UpdateBranchSessionExecute;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.storage.SessionConverter;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;


/**
 */
@SpringBootTest
class BranchSessionExecuteTest {

    private static GlobalSession GLOBAL_SESSION;

    private static final long BRANCH_ID = 0L;

    @BeforeAll
    public static void setUp(ApplicationContext context) throws TransactionException {
        System.setProperty("server.raft.serverAddr", NetUtil.getLocalIp() + ":9091");
        SessionHolder.init(StoreConfig.SessionMode.RAFT);
        LockerManagerFactory.destroy();
        LockerManagerFactory.init(StoreConfig.LockMode.RAFT);
        GLOBAL_SESSION = mockGlobalSession();
        SessionHolder.getRootSessionManager().addGlobalSession(GLOBAL_SESSION);
    }

    @AfterAll
    public static void destroy() throws Throwable {
        testRemove();
        SessionHolder.getRootSessionManager().removeGlobalSession(GLOBAL_SESSION);
        // Clear configuration
        ConfigurationCache.clear();
        System.clearProperty("server.raft.serverAddr");

        // Destroy SessionHolder and LockerManagerFactory
        SessionHolder.destroy();
        SessionHolder.init(null);
        LockerManagerFactory.destroy();
    }



    @Test
    public void testAdd() throws Throwable {
        BranchSession expected = mockBranchSession(GLOBAL_SESSION.getXid(), GLOBAL_SESSION.getTransactionId());

        AddBranchSessionExecute execute = new AddBranchSessionExecute();
        boolean success = execute.execute(convertToBranchSessionMsg(expected));
        Assertions.assertTrue(success);

        BranchSession branchSession = GLOBAL_SESSION.getBranch(expected.getBranchId());
        assertBranchSessionValid(expected, branchSession);
    }

    public static void testRemove() throws Throwable {
        List<BranchSession> list = new ArrayList<>(GLOBAL_SESSION.getBranchSessions());
        for (BranchSession branchSession : list) {
            Assertions.assertNotNull(branchSession);

            RemoveBranchSessionExecute execute = new RemoveBranchSessionExecute();
            boolean success = execute.execute(convertToBranchSessionMsg(branchSession));
            Assertions.assertTrue(success);

            branchSession = GLOBAL_SESSION.getBranch(branchSession.getBranchId());
            Assertions.assertNull(branchSession);
        }
    }

    @Test
    public void testUpdate() throws Throwable {
        BranchSession branchSession = mockBranchSession(GLOBAL_SESSION.getXid(), GLOBAL_SESSION.getTransactionId());
        GLOBAL_SESSION.add(branchSession);

        branchSession = GLOBAL_SESSION.getBranch(branchSession.getBranchId());
        Assertions.assertNotNull(branchSession);

        branchSession.setStatus(BranchStatus.PhaseTwo_Committed);
        UpdateBranchSessionExecute execute = new UpdateBranchSessionExecute();
        boolean success = execute.execute(convertToBranchSessionMsg(branchSession));
        Assertions.assertTrue(success);

        branchSession = GLOBAL_SESSION.getBranch(branchSession.getBranchId());
        assertBranchSessionValid(branchSession, branchSession);
    }

    private static GlobalSession mockGlobalSession() {
        long txId = UUIDGenerator.generateUUID();
        GlobalSession session = new GlobalSession("test", "test", "test", 5000);
        session.setXid(XID.generateXID(txId));
        session.setApplicationData("hello, world");
        session.setTransactionId(txId);
        session.setBeginTime(System.currentTimeMillis());
        return session;
    }

    private static BranchSession mockBranchSession(String xid,long transactionId) {
        BranchSession session = new BranchSession();
        session.setXid(xid);
        session.setTransactionId(transactionId);
        session.setBranchId(UUIDGenerator.generateUUID());
        session.setClientId("client");
        session.setResourceGroupId(DEFAULT_TX_GROUP);
        session.setResourceId("resource");
        session.setLockKey("test");
        session.setBranchType(BranchType.AT);
        session.setApplicationData("hello, world");
        return session;
    }

    private static void assertBranchSessionValid(BranchSession expected, BranchSession branchSession) {
        Assertions.assertNotNull(branchSession);
        Assertions.assertEquals(expected.getTransactionId(), branchSession.getTransactionId());
        Assertions.assertEquals(expected.getBranchId(), branchSession.getBranchId());
        Assertions.assertEquals(expected.getResourceId(), branchSession.getResourceId());
        Assertions.assertEquals(expected.getLockKey(), branchSession.getLockKey());
        Assertions.assertEquals(expected.getClientId(), branchSession.getClientId());
        Assertions.assertEquals(expected.getApplicationData(), branchSession.getApplicationData());
    }

    private static RaftBranchSessionSyncMsg convertToBranchSessionMsg(BranchSession branchSession) {
        RaftBranchSessionSyncMsg sessionMsg = new RaftBranchSessionSyncMsg();
        BranchTransactionDTO dto = SessionConverter.convertBranchTransactionDTO(branchSession);
        sessionMsg.setBranchSession(dto);
        return sessionMsg;
    }
}
