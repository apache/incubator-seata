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
package org.apache.seata.server.store.file;

import org.apache.commons.io.FileUtils;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.file.FlushDiskMode;
import org.apache.seata.server.storage.file.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

public class RocksDBTransactionStoreManagerTest extends BaseSpringBootTest {

    @TempDir
    private Path tempDir;

    private RocksDBTransactionStoreManager storeManager;

    @AfterEach
    public void tearDown() throws Exception {
        if (storeManager != null) {
            storeManager.shutdown();
        }
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void testWriteReadAndRemoveSessions() throws Exception {
        storeManager = newStoreManager();
        GlobalSession globalSession = newGlobalSession();
        BranchSession branchSession = newBranchSession(globalSession);

        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, globalSession));
        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession));

        GlobalSession found = storeManager.readSession(globalSession.getXid());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(globalSession.getTransactionId(), found.getTransactionId());
        Assertions.assertEquals(globalSession.getStatus(), found.getStatus());
        Assertions.assertEquals(1, found.getBranchSessions().size());
        Assertions.assertEquals(
                branchSession.getBranchId(), found.getBranchSessions().get(0).getBranchId());

        GlobalSession lazyFound = storeManager.readSession(globalSession.getXid(), false);
        Assertions.assertTrue(lazyFound.isLazyLoadBranch());

        globalSession.setStatus(GlobalStatus.Rollbacking);
        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_UPDATE, globalSession));
        Assertions.assertEquals(
                GlobalStatus.Rollbacking,
                storeManager.readSession(globalSession.getXid()).getStatus());

        branchSession.setStatus(BranchStatus.PhaseTwo_Rollbacked);
        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchSession));
        Assertions.assertEquals(
                BranchStatus.PhaseTwo_Rollbacked,
                storeManager
                        .readSession(globalSession.getXid())
                        .getBranchSessions()
                        .get(0)
                        .getStatus());

        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchSession));
        Assertions.assertTrue(storeManager
                .readSession(globalSession.getXid())
                .getBranchSessions()
                .isEmpty());

        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_REMOVE, globalSession));
        Assertions.assertNull(storeManager.readSession(globalSession.getXid()));
    }

    @Test
    public void testReadSessionByConditions() throws Exception {
        storeManager = newStoreManager();
        GlobalSession beginSession = newGlobalSession();
        GlobalSession committingSession = newGlobalSession();
        committingSession.setStatus(GlobalStatus.Committing);

        Assertions.assertTrue(storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, beginSession));
        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, committingSession));

        SessionCondition xidCondition = new SessionCondition(beginSession.getXid());
        Assertions.assertEquals(1, storeManager.readSession(xidCondition).size());

        SessionCondition transactionIdCondition = new SessionCondition();
        transactionIdCondition.setTransactionId(committingSession.getTransactionId());
        Assertions.assertEquals(
                1, storeManager.readSession(transactionIdCondition).size());

        SessionCondition statusCondition = new SessionCondition(GlobalStatus.Begin);
        List<GlobalSession> beginSessions = storeManager.readSession(statusCondition);
        Assertions.assertEquals(1, beginSessions.size());
        Assertions.assertEquals(beginSession.getXid(), beginSessions.get(0).getXid());

        SessionCondition overtimeCondition = new SessionCondition(GlobalStatus.Begin);
        overtimeCondition.setOverTimeAliveMills(1L);
        Assertions.assertEquals(1, storeManager.readSession(overtimeCondition).size());
    }

    @Test
    public void testDataSurvivesRestart() throws Exception {
        storeManager = newStoreManager();
        GlobalSession globalSession = newGlobalSession();
        BranchSession branchSession = newBranchSession(globalSession);

        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, globalSession));
        Assertions.assertTrue(
                storeManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession));
        storeManager.shutdown();

        storeManager = newStoreManager();
        GlobalSession found = storeManager.readSession(globalSession.getXid());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(globalSession.getTransactionId(), found.getTransactionId());
        Assertions.assertEquals(1, found.getBranchSessions().size());
    }

    private RocksDBTransactionStoreManager newStoreManager() throws Exception {
        return new RocksDBTransactionStoreManager(
                tempDir.resolve("session-rocksdb").toString(), FlushDiskMode.ASYNC_MODEL);
    }

    private GlobalSession newGlobalSession() {
        GlobalSession globalSession = new GlobalSession("test-app", "default_tx_group", "test", 60000);
        globalSession.setBeginTime(System.currentTimeMillis() - 1000);
        return globalSession;
    }

    private BranchSession newBranchSession(GlobalSession globalSession) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid(globalSession.getXid());
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(globalSession.getTransactionId() + 1);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1:3306/seata");
        branchSession.setLockKey("account:1");
        branchSession.setClientId("client");
        branchSession.setApplicationData("applicationData");
        branchSession.setStatus(BranchStatus.Registered);
        return branchSession;
    }
}
