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

import org.apache.seata.core.model.LockStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LockDOTest {

    @Test
    public void testDefaultConstructor() {
        LockDO lockDO = new LockDO();
        Assertions.assertNotNull(lockDO);
        Assertions.assertEquals(LockStatus.Locked.getCode(), lockDO.getStatus());
    }

    @Test
    public void testSetAndGetXid() {
        LockDO lockDO = new LockDO();
        String xid = "192.168.1.1:8091:123456";
        lockDO.setXid(xid);
        Assertions.assertEquals(xid, lockDO.getXid());
    }

    @Test
    public void testSetAndGetTransactionId() {
        LockDO lockDO = new LockDO();
        Long transactionId = 100L;
        lockDO.setTransactionId(transactionId);
        Assertions.assertEquals(transactionId, lockDO.getTransactionId());
    }

    @Test
    public void testSetAndGetBranchId() {
        LockDO lockDO = new LockDO();
        Long branchId = 200L;
        lockDO.setBranchId(branchId);
        Assertions.assertEquals(branchId, lockDO.getBranchId());
    }

    @Test
    public void testSetAndGetResourceId() {
        LockDO lockDO = new LockDO();
        String resourceId = "jdbc:mysql://localhost:3306/test";
        lockDO.setResourceId(resourceId);
        Assertions.assertEquals(resourceId, lockDO.getResourceId());
    }

    @Test
    public void testSetAndGetTableName() {
        LockDO lockDO = new LockDO();
        String tableName = "user_table";
        lockDO.setTableName(tableName);
        Assertions.assertEquals(tableName, lockDO.getTableName());
    }

    @Test
    public void testSetAndGetPk() {
        LockDO lockDO = new LockDO();
        String pk = "1";
        lockDO.setPk(pk);
        Assertions.assertEquals(pk, lockDO.getPk());
    }

    @Test
    public void testDefaultStatus() {
        LockDO lockDO = new LockDO();
        Assertions.assertEquals(LockStatus.Locked.getCode(), lockDO.getStatus());
    }

    @Test
    public void testSetAndGetStatus() {
        LockDO lockDO = new LockDO();
        Integer status = LockStatus.Rollbacking.getCode();
        lockDO.setStatus(status);
        Assertions.assertEquals(status, lockDO.getStatus());
    }

    @Test
    public void testSetAndGetRowKey() {
        LockDO lockDO = new LockDO();
        String rowKey = "user_table:1";
        lockDO.setRowKey(rowKey);
        Assertions.assertEquals(rowKey, lockDO.getRowKey());
    }

    @Test
    public void testToString() {
        LockDO lockDO = new LockDO();
        lockDO.setXid("test-xid");
        lockDO.setTransactionId(100L);
        lockDO.setBranchId(200L);
        lockDO.setResourceId("jdbc:mysql://localhost:3306/test");
        lockDO.setTableName("user_table");
        lockDO.setPk("1");

        String result = lockDO.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("test-xid"));
    }

    @Test
    public void testLockDOWithAllFields() {
        LockDO lockDO = new LockDO();
        lockDO.setXid("192.168.1.1:8091:123456");
        lockDO.setTransactionId(1000L);
        lockDO.setBranchId(2000L);
        lockDO.setResourceId("jdbc:mysql://localhost:3306/test_db");
        lockDO.setTableName("test_table");
        lockDO.setPk("123");
        lockDO.setStatus(LockStatus.Locked.getCode());
        lockDO.setRowKey("test_table:123");

        Assertions.assertEquals("192.168.1.1:8091:123456", lockDO.getXid());
        Assertions.assertEquals(1000L, lockDO.getTransactionId());
        Assertions.assertEquals(2000L, lockDO.getBranchId());
        Assertions.assertEquals("jdbc:mysql://localhost:3306/test_db", lockDO.getResourceId());
        Assertions.assertEquals("test_table", lockDO.getTableName());
        Assertions.assertEquals("123", lockDO.getPk());
        Assertions.assertEquals(LockStatus.Locked.getCode(), lockDO.getStatus());
        Assertions.assertEquals("test_table:123", lockDO.getRowKey());
    }
}
