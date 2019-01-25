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
package com.alibaba.fescar.server.lock;

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.session.BranchSession;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author tianming.xm@gmail.com
 * @since 2019/1/23
 */
public class LockManagerTest {

    @Test(dataProvider = "branchSessionProvider")
    public void acquireLock_success(BranchSession branchSession) throws Exception{
        LockManager lockManager = LockManagerFactory.get();
        Assert.assertTrue(lockManager.acquireLock(branchSession));
    }

    @Test(dataProvider = "branchSessionsProvider")
    public void acquireLock_failed(BranchSession branchSession1,BranchSession branchSession2) throws Exception{
        LockManager lockManager = LockManagerFactory.get();
        Assert.assertTrue(lockManager.acquireLock(branchSession1));
        Assert.assertFalse(lockManager.acquireLock(branchSession2));
    }

    @Test(dataProvider = "branchSessionProvider")
    public void isLockableTest(BranchSession branchSession) throws Exception{
        branchSession.setLockKey("t:4");
        LockManager lockManager = LockManagerFactory.get();
        Assert.assertTrue(lockManager.isLockable(branchSession.getTransactionId(),branchSession.getResourceId(),branchSession.getLockKey()));
        lockManager.acquireLock(branchSession);
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        Assert.assertFalse(lockManager.isLockable(branchSession.getTransactionId(),branchSession.getResourceId(),branchSession.getLockKey()));
    }

    @DataProvider
    public static Object[][] branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(0L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t:0");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return new Object[][] {{ branchSession}};
    }

    @DataProvider
    public static Object[][] branchSessionsProvider() {
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setTransactionId(UUIDGenerator.generateUUID());
        branchSession1.setBranchId(1L);
        branchSession1.setClientId("c1");
        branchSession1.setResourceGroupId("my_test_tx_group");
        branchSession1.setResourceId("tb_1");
        branchSession1.setLockKey("t:1,2");
        branchSession1.setBranchType(BranchType.AT);
        branchSession1.setApplicationId("demo-child-app");
        branchSession1.setTxServiceGroup("my_test_tx_group");
        branchSession1.setApplicationData("{\"data\":\"test\"}");
        branchSession1.setBranchType(BranchType.AT);

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setTransactionId(UUIDGenerator.generateUUID());
        branchSession2.setBranchId(2L);
        branchSession2.setClientId("c1");
        branchSession2.setResourceGroupId("my_test_tx_group");
        branchSession2.setResourceId("tb_1");
        branchSession2.setLockKey("t:1,2");
        branchSession2.setBranchType(BranchType.AT);
        branchSession2.setApplicationId("demo-child-app");
        branchSession2.setTxServiceGroup("my_test_tx_group");
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setBranchType(BranchType.AT);
        return new Object[][] {{ branchSession1,branchSession2}};
    }
}
