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

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.server.UUIDGenerator;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The type Branch session test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class BranchSessionTest {

    /**
     * Codec test.
     *
     * @param branchSession the branch session
     */
    @Test(dataProvider = "branchSessionProvider")
    public void codecTest(BranchSession branchSession) {
        byte[] result = branchSession.encode();
        Assert.assertNotNull(result);
        BranchSession expected = new BranchSession();
        expected.decode(result);
        Assert.assertEquals(branchSession.getTransactionId(), expected.getTransactionId());
        Assert.assertEquals(branchSession.getBranchId(), expected.getBranchId());
        Assert.assertEquals(branchSession.getResourceId(), expected.getResourceId());
        Assert.assertEquals(branchSession.getLockKey(), expected.getLockKey());
        Assert.assertEquals(branchSession.getApplicationId(), expected.getApplicationId());
        Assert.assertEquals(branchSession.getTxServiceGroup(), expected.getTxServiceGroup());
        Assert.assertEquals(branchSession.getClientId(), expected.getClientId());
        Assert.assertEquals(branchSession.getApplicationData(), expected.getApplicationData());

    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    @DataProvider
    public static Object[][] branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(1L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return new Object[][] {{branchSession}};
    }
}
