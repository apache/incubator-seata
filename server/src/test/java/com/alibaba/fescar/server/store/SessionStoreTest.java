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
package com.alibaba.fescar.server.store;

import java.io.File;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.lock.LockManager;
import com.alibaba.fescar.server.lock.LockManagerFactory;
import com.alibaba.fescar.server.session.BranchSession;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionHelper;
import com.alibaba.fescar.server.session.SessionHolder;

import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The type Session store test.
 */
public class SessionStoreTest {

    /**
     * The constant RESOURCE_ID.
     */
    public static final String RESOURCE_ID = "mysql:xxx";

    /**
     * Clean.
     *
     * @throws Exception the exception
     */
    @BeforeMethod
    public void clean() throws Exception {
        File rootDataFile = new File("./" + SessionHolder.ROOT_SESSION_MANAGER_NAME);
        File rootDataFileHis = new File("./" + SessionHolder.ROOT_SESSION_MANAGER_NAME + ".1");

        if (rootDataFile.exists()) {
            rootDataFile.delete();
            System.currentTimeMillis();
        }
        if (rootDataFileHis.exists()) {
            rootDataFileHis.delete();
        }
        LockManagerFactory.get().cleanAllLocks();
    }

    /**
     * Test restored from file.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFile() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
            "ta:1,2;tb:3", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:4"));
        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "tb:5"));

        lockManager.cleanAllLocks();

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assert.assertNotNull(reloadSession);
        Assert.assertFalse(globalSession == reloadSession);
        Assert.assertEquals(globalSession.getApplicationId(), reloadSession.getApplicationId());

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));
        Assert.assertTrue(lockManager.isLockable(globalSession.getTransactionId(), RESOURCE_ID, "tb:3"));

    }

    /**
     * Test restored from file 2.
     *
     * @throws Exception the exception
     */
    //@Test
    public void testRestoredFromFile2() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");
    }

    /**
     * Test restored from file async committing.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileAsyncCommitting() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
            "ta:1", "xxx");
        Assert.assertTrue(branchSession1.lock());
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalStatus.AsyncCommitting);

        lockManager.cleanAllLocks();

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assert.assertEquals(reloadSession.getStatus(), GlobalStatus.AsyncCommitting);

        GlobalSession sessionInAsyncCommittingQueue = SessionHolder.getAsyncCommittingSessionManager()
            .findGlobalSession(tid);
        Assert.assertTrue(reloadSession == sessionInAsyncCommittingQueue);

        // No locking for session in AsyncCommitting status
        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

    }

    /**
     * Test restored from file commit retry.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileCommitRetry() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
            "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalStatus.Committing);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_CommitFailed_Retryable);
        globalSession.changeStatus(GlobalStatus.CommitRetrying);

        lockManager.cleanAllLocks();

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assert.assertEquals(reloadSession.getStatus(), GlobalStatus.CommitRetrying);

        GlobalSession sessionInRetryCommittingQueue = SessionHolder.getRetryCommittingSessionManager()
            .findGlobalSession(tid);
        Assert.assertTrue(reloadSession == sessionInRetryCommittingQueue);
        BranchSession reloadBranchSession = reloadSession.getBranch(branchSession1.getBranchId());
        Assert.assertEquals(reloadBranchSession.getStatus(), BranchStatus.PhaseTwo_CommitFailed_Retryable);

        // Lock is held by session in CommitRetrying status
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

    }

    /**
     * Test restored from file rollback retry.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileRollbackRetry() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
            "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalStatus.Rollbacking);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_RollbackFailed_Retryable);
        globalSession.changeStatus(GlobalStatus.RollbackRetrying);

        lockManager.cleanAllLocks();

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assert.assertEquals(reloadSession.getStatus(), GlobalStatus.RollbackRetrying);

        GlobalSession sessionInRetryRollbackingQueue = SessionHolder.getRetryRollbackingSessionManager()
            .findGlobalSession(tid);
        Assert.assertTrue(reloadSession == sessionInRetryRollbackingQueue);
        BranchSession reloadBranchSession = reloadSession.getBranch(branchSession1.getBranchId());
        Assert.assertEquals(reloadBranchSession.getStatus(), BranchStatus.PhaseTwo_RollbackFailed_Retryable);

        // Lock is held by session in RollbackRetrying status
        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

    }

    /**
     * Test restored from file rollback failed.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileRollbackFailed() throws Exception {
        SessionHolder.init(".");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
            "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assert.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalStatus.Rollbacking);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_CommitFailed_Unretryable);
        SessionHelper.endRollbackFailed(globalSession);

        // Lock is released.
        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        lockManager.cleanAllLocks();

        Assert.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init(".");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assert.assertNull(reloadSession);

    }
}
