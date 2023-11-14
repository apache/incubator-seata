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

package io.seata.server.raft.execute;

import io.seata.common.util.NetUtil;
import io.seata.config.ConfigurationCache;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.server.cluster.raft.execute.lock.BranchReleaseLockExecute;
import io.seata.server.cluster.raft.execute.lock.GlobalReleaseLockExecute;
import io.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import io.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import io.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import io.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import io.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;


/**
 * @author ptyin
 */
@SpringBootTest
class LockExecuteTest {

    private static GlobalSession GLOBAL_SESSION;

    private static final String XID = "123:123";

    private static final long BRANCH_ID = 0L;

    @BeforeAll
    public static void setUp(ApplicationContext context) throws TransactionException {
        System.setProperty("server.raft.serverAddr", NetUtil.getLocalIp() + ":9091");
        SessionHolder.init(StoreConfig.SessionMode.RAFT);
        LockerManagerFactory.destroy();
        LockerManagerFactory.init(StoreConfig.LockMode.RAFT);
    }

    @AfterAll
    public static void destroy() throws TransactionException {
        // Clear configuration
        ConfigurationCache.clear();
        System.clearProperty("server.raft.serverAddr");

        // Destroy SessionHolder and LockerManagerFactory
        SessionHolder.destroy();
        SessionHolder.init(null);
        LockerManagerFactory.destroy();
    }

    @BeforeEach
    public void addGlobalSession() throws TransactionException {
        GLOBAL_SESSION = mockGlobalSession();
        SessionHolder.getRootSessionManager().addGlobalSession(GLOBAL_SESSION);
    }

    @AfterEach
    public void removeTestSession() throws TransactionException {
        SessionHolder.getRootSessionManager().removeGlobalSession(GLOBAL_SESSION);
    }

    @Test
    public void testGlobalRelease() throws Throwable {
        BranchSession branchSession1 = mockBranchSession("test:0");
        BranchSession branchSession2 = mockBranchSession("test:1");
        GLOBAL_SESSION.add(branchSession1);
        GLOBAL_SESSION.add(branchSession2);

        LockManager lockerManager = LockerManagerFactory.getLockManager();
        Assertions.assertTrue(lockerManager.acquireLock(branchSession1));
        Assertions.assertTrue(lockerManager.acquireLock(branchSession2));
        Assertions.assertEquals(1, branchSession1.getLockHolder().values().size());
        Assertions.assertEquals(1, branchSession2.getLockHolder().values().size());

        GlobalReleaseLockExecute execute = new GlobalReleaseLockExecute();
        boolean success = execute.execute(convertToGlobalSessionMsg(GLOBAL_SESSION));
        Assertions.assertTrue(success);
        Assertions.assertEquals(0, branchSession1.getLockHolder().values().size());
        Assertions.assertEquals(0, branchSession2.getLockHolder().values().size());
    }

    @Test
    public void testBranchRelease() throws Throwable {
        BranchSession branchSession = mockBranchSession("test:0");
        GLOBAL_SESSION.add(branchSession);

        LockManager lockerManager = LockerManagerFactory.getLockManager();
        Assertions.assertTrue(lockerManager.acquireLock(branchSession));
        Assertions.assertEquals(1, branchSession.getLockHolder().values().size());

        BranchReleaseLockExecute execute = new BranchReleaseLockExecute();
        boolean success = execute.execute(convertToBranchSessionMsg(branchSession));
        Assertions.assertTrue(success);
        Assertions.assertEquals(0, branchSession.getLockHolder().values().size());
    }

    private static GlobalSession mockGlobalSession() {
        GlobalSession session = new GlobalSession("test", "test", "test", 5000);
        session.setXid(XID);
        session.setApplicationData("hello, world");
        session.setTransactionId(123);
        session.setBeginTime(System.currentTimeMillis());
        return session;
    }

    private static BranchSession mockBranchSession(String lockKey) {
        BranchSession session = new BranchSession();
        session.setXid(XID);
        session.setTransactionId(123);
        session.setBranchId(BRANCH_ID);
        session.setClientId("client");
        session.setResourceGroupId(DEFAULT_TX_GROUP);
        session.setResourceId("db");
        session.setLockKey(lockKey);
        session.setBranchType(BranchType.AT);
        session.setApplicationData("hello, world");
        return session;
    }

    private static RaftGlobalSessionSyncMsg convertToGlobalSessionMsg(GlobalSession globalSession) {
        RaftGlobalSessionSyncMsg sessionMsg = new RaftGlobalSessionSyncMsg();
        GlobalTransactionDTO dto = new GlobalTransactionDTO();
        SessionConverter.convertGlobalTransactionDO(dto, globalSession);
        sessionMsg.setGlobalSession(dto);
        return sessionMsg;
    }

    private static RaftBranchSessionSyncMsg convertToBranchSessionMsg(BranchSession branchSession) {
        RaftBranchSessionSyncMsg sessionMsg = new RaftBranchSessionSyncMsg();
        BranchTransactionDTO dto = SessionConverter.convertBranchTransactionDTO(branchSession);
        sessionMsg.setBranchSession(dto);
        return sessionMsg;
    }
}
