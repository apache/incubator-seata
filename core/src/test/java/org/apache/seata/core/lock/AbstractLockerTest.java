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
package org.apache.seata.core.lock;

import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The AbstractLocker Test
 */
public class AbstractLockerTest {

    @Test
    public void testConvertToLockDO() {
        List<LockDO> lockDOs = getLockDOS();

        // Assert that the converted LockDO objects have the correct values
        Assertions.assertEquals(2, lockDOs.size());

        LockDO lockDO1 = lockDOs.get(0);
        Assertions.assertEquals(1, lockDO1.getBranchId());
        Assertions.assertEquals("123", lockDO1.getPk());
        Assertions.assertEquals("resource1", lockDO1.getResourceId());
        Assertions.assertEquals("xid1", lockDO1.getXid());
        Assertions.assertEquals(1122L, lockDO1.getTransactionId());
        Assertions.assertEquals("table1", lockDO1.getTableName());

        LockDO lockDO2 = lockDOs.get(1);
        Assertions.assertEquals(2, lockDO2.getBranchId());
        Assertions.assertEquals("456", lockDO2.getPk());
        Assertions.assertEquals("resource2", lockDO2.getResourceId());
        Assertions.assertEquals("xid2", lockDO2.getXid());
        Assertions.assertEquals(3344L, lockDO2.getTransactionId());
        Assertions.assertEquals("table2", lockDO2.getTableName());
    }

    private static List<LockDO> getLockDOS() {
        AbstractLocker locker = new AbstractLocker() {
            @Override
            public boolean acquireLock(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public boolean acquireLock(List<RowLock> rowLock, boolean autoCommit, boolean skipCheckLock) {
                return false;
            }

            @Override
            public boolean releaseLock(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public boolean isLockable(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public void updateLockStatus(String xid, LockStatus lockStatus) {

            }
        };
        List<RowLock> locks = getRowLocks();

        // Call the convertToLockDO method
        return locker.convertToLockDO(locks);
    }


    @Test
    public void testGetRowKey() {
        AbstractLocker locker = new AbstractLocker() {
            @Override
            public boolean acquireLock(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public boolean acquireLock(List<RowLock> rowLock, boolean autoCommit, boolean skipCheckLock) {
                return false;
            }

            @Override
            public boolean releaseLock(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public boolean isLockable(List<RowLock> rowLock) {
                return false;
            }

            @Override
            public void updateLockStatus(String xid, LockStatus lockStatus) {

            }
        };

        // Call the getRowKey method
        String rowKey = locker.getRowKey("resource1", "table1", "123");

        // Assert that the row key is constructed correctly
        Assertions.assertEquals("resource1^^^table1^^^123", rowKey);
    }

    private static List<RowLock> getRowLocks() {
        List<RowLock> locks = new ArrayList<>();
        RowLock rowLock1 = new RowLock();
        rowLock1.setBranchId(1L);
        rowLock1.setPk("123");
        rowLock1.setResourceId("resource1");
        rowLock1.setXid("xid1");
        rowLock1.setTransactionId(1122L);
        rowLock1.setTableName("table1");
        locks.add(rowLock1);

        RowLock rowLock2 = new RowLock();
        rowLock2.setBranchId(2L);
        rowLock2.setPk("456");
        rowLock2.setResourceId("resource2");
        rowLock2.setXid("xid2");
        rowLock2.setTransactionId(3344L);
        rowLock2.setTableName("table2");
        locks.add(rowLock2);
        return locks;
    }
}
