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
package org.apache.seata.core.store;

import org.apache.seata.core.model.BranchStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class BranchTransactionDOTest {

    @Test
    public void testConstructorWithXidAndBranchId() {
        String xid = "192.168.1.1:8091:123456";
        long branchId = 1001L;
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO(xid, branchId);

        Assertions.assertEquals(xid, branchTransactionDO.getXid());
        Assertions.assertEquals(branchId, branchTransactionDO.getBranchId());
    }

    @Test
    public void testDefaultConstructor() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        Assertions.assertNotNull(branchTransactionDO);
    }

    @Test
    public void testSetAndGetXid() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String xid = "192.168.1.1:8091:123456";
        branchTransactionDO.setXid(xid);
        Assertions.assertEquals(xid, branchTransactionDO.getXid());
    }

    @Test
    public void testSetAndGetTransactionId() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        long transactionId = 100L;
        branchTransactionDO.setTransactionId(transactionId);
        Assertions.assertEquals(transactionId, branchTransactionDO.getTransactionId());
    }

    @Test
    public void testSetAndGetBranchId() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        long branchId = 200L;
        branchTransactionDO.setBranchId(branchId);
        Assertions.assertEquals(branchId, branchTransactionDO.getBranchId());
    }

    @Test
    public void testSetAndGetResourceGroupId() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String resourceGroupId = "test-resource-group";
        branchTransactionDO.setResourceGroupId(resourceGroupId);
        Assertions.assertEquals(resourceGroupId, branchTransactionDO.getResourceGroupId());
    }

    @Test
    public void testSetAndGetResourceId() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String resourceId = "jdbc:mysql://localhost:3306/test";
        branchTransactionDO.setResourceId(resourceId);
        Assertions.assertEquals(resourceId, branchTransactionDO.getResourceId());
    }

    @Test
    public void testSetAndGetBranchType() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String branchType = "AT";
        branchTransactionDO.setBranchType(branchType);
        Assertions.assertEquals(branchType, branchTransactionDO.getBranchType());
    }

    @Test
    public void testDefaultStatus() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        Assertions.assertEquals(BranchStatus.Unknown.getCode(), branchTransactionDO.getStatus());
    }

    @Test
    public void testSetAndGetStatus() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        int status = BranchStatus.PhaseOne_Done.getCode();
        branchTransactionDO.setStatus(status);
        Assertions.assertEquals(status, branchTransactionDO.getStatus());
    }

    @Test
    public void testSetAndGetClientId() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String clientId = "192.168.1.100:8080";
        branchTransactionDO.setClientId(clientId);
        Assertions.assertEquals(clientId, branchTransactionDO.getClientId());
    }

    @Test
    public void testSetAndGetApplicationData() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        String applicationData = "{\"key\":\"value\"}";
        branchTransactionDO.setApplicationData(applicationData);
        Assertions.assertEquals(applicationData, branchTransactionDO.getApplicationData());
    }

    @Test
    public void testSetAndGetGmtCreate() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        Date gmtCreate = new Date();
        branchTransactionDO.setGmtCreate(gmtCreate);
        Assertions.assertEquals(gmtCreate, branchTransactionDO.getGmtCreate());
    }

    @Test
    public void testSetAndGetGmtModified() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        Date gmtModified = new Date();
        branchTransactionDO.setGmtModified(gmtModified);
        Assertions.assertEquals(gmtModified, branchTransactionDO.getGmtModified());
    }

    @Test
    public void testToString() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO("test-xid", 100L);
        String result = branchTransactionDO.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("test-xid"));
    }

    @Test
    public void testCompareTo() throws InterruptedException {
        BranchTransactionDO branch1 = new BranchTransactionDO();
        Date date1 = new Date();
        branch1.setGmtCreate(date1);

        Thread.sleep(10);

        BranchTransactionDO branch2 = new BranchTransactionDO();
        Date date2 = new Date();
        branch2.setGmtCreate(date2);

        Assertions.assertTrue(branch1.compareTo(branch2) < 0);
        Assertions.assertTrue(branch2.compareTo(branch1) > 0);
        Assertions.assertEquals(0, branch1.compareTo(branch1));
    }
}
