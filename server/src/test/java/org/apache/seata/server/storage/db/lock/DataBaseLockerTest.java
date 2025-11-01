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
package org.apache.seata.server.storage.db.lock;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EnabledIfSystemProperty(named = "dbCaseEnabled", matches = "true")
public class DataBaseLockerTest extends BaseSpringBootTest {

    private DataBaseLocker dataBaseLocker;

    @BeforeEach
    public void setUp() {
        DataSource dataSource =
                EnhancedServiceLoader.load(DataSourceProvider.class, "druid").provide();
        dataBaseLocker = new DataBaseLocker(dataSource);
    }

    @AfterEach
    public void tearDown() {
        // Clean up test locks
        try {
            dataBaseLocker.releaseLock("test-xid-locker-1");
            dataBaseLocker.releaseLock("test-xid-locker-2");
            dataBaseLocker.releaseLock("test-xid-locker-3");
            dataBaseLocker.releaseLock("test-xid-locker-1", 1000L);
            dataBaseLocker.releaseLock("test-xid-locker-1", 1001L);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(dataBaseLocker);
    }

    @Test
    public void testExtendsAbstractLocker() {
        Assertions.assertTrue(dataBaseLocker instanceof AbstractLocker);
    }

    @Test
    public void testAcquireLockWithEmptyList() {
        boolean result = dataBaseLocker.acquireLock(new ArrayList<>());
        Assertions.assertTrue(result);
    }

    @Test
    public void testReleaseLockWithEmptyList() {
        boolean result = dataBaseLocker.releaseLock(new ArrayList<>());
        Assertions.assertTrue(result);
    }

    @Test
    public void testAcquireLockWithData() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "1"));

        boolean result = dataBaseLocker.acquireLock(locks);
        Assertions.assertTrue(result, "Acquire lock with data should succeed");

        // Clean up
        dataBaseLocker.releaseLock(locks);
    }

    @Test
    public void testAcquireBatchLocks() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "10"));
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "11"));
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "12"));

        boolean result = dataBaseLocker.acquireLock(locks);
        Assertions.assertTrue(result, "Batch lock acquisition should succeed");

        // Clean up
        dataBaseLocker.releaseLock(locks);
    }

    @Test
    public void testAcquireLockWithAutoCommitFalse() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "20"));

        boolean result = dataBaseLocker.acquireLock(locks, false, false);
        Assertions.assertTrue(result, "Acquire lock with autoCommit=false should succeed");

        // Clean up
        dataBaseLocker.releaseLock(locks);
    }

    @Test
    public void testAcquireLockWithSkipCheckLock() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "30"));

        boolean result = dataBaseLocker.acquireLock(locks, true, true);
        Assertions.assertTrue(result, "Acquire lock with skipCheckLock should succeed");

        // Clean up
        dataBaseLocker.releaseLock(locks);
    }

    @Test
    public void testReleaseLockWithData() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "40"));

        // Acquire then release
        dataBaseLocker.acquireLock(locks);
        boolean result = dataBaseLocker.releaseLock(locks);
        Assertions.assertTrue(result, "Release lock with data should succeed");
    }

    @Test
    public void testReleaseLockByXid() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-2", 2L, 2L, "test-resource", "test_table", "50"));
        locks.add(createRowLock("test-xid-locker-2", 2L, 3L, "test-resource", "test_table", "51"));

        // Acquire locks
        dataBaseLocker.acquireLock(locks);

        // Release all by xid
        boolean result = dataBaseLocker.releaseLock("test-xid-locker-2");
        Assertions.assertTrue(result, "Release lock by xid should succeed");
    }

    @Test
    public void testReleaseLockByXidAndBranchId() {
        RowLock lock = createRowLock("test-xid-locker-3", 3L, 1000L, "test-resource", "test_table", "60");
        List<RowLock> locks = Arrays.asList(lock);

        // Acquire lock
        dataBaseLocker.acquireLock(locks);

        // Release by xid and branchId
        boolean result = dataBaseLocker.releaseLock("test-xid-locker-3", 1000L);
        Assertions.assertTrue(result, "Release lock by xid and branchId should succeed");
    }

    @Test
    public void testIsLockableWithEmptyList() {
        boolean result = dataBaseLocker.isLockable(new ArrayList<>());
        Assertions.assertTrue(result, "Empty list should be lockable");
    }

    @Test
    public void testIsLockableWithNoExistingLocks() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "70"));

        boolean result = dataBaseLocker.isLockable(locks);
        Assertions.assertTrue(result, "Should be lockable when no existing locks");
    }

    @Test
    public void testIsLockableWithSameXid() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "80"));

        // Acquire lock
        dataBaseLocker.acquireLock(locks);

        // Check if lockable with same xid
        boolean result = dataBaseLocker.isLockable(locks);
        Assertions.assertTrue(result, "Should be lockable by same xid");

        // Clean up
        dataBaseLocker.releaseLock(locks);
    }

    @Test
    public void testIsLockableWithDifferentXid() {
        List<RowLock> locks1 = new ArrayList<>();
        locks1.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "90"));

        // Acquire lock with xid-1
        dataBaseLocker.acquireLock(locks1);

        // Check if lockable with different xid
        List<RowLock> locks2 = new ArrayList<>();
        locks2.add(createRowLock("test-xid-locker-2", 2L, 2L, "test-resource", "test_table", "90"));

        boolean result = dataBaseLocker.isLockable(locks2);
        Assertions.assertFalse(result, "Should not be lockable by different xid");

        // Clean up
        dataBaseLocker.releaseLock(locks1);
    }

    @Test
    public void testUpdateLockStatus() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "100"));

        // Acquire lock
        dataBaseLocker.acquireLock(locks);

        // Update status - should execute without exception
        dataBaseLocker.updateLockStatus("test-xid-locker-1", LockStatus.Rollbacking);

        // Clean up
        dataBaseLocker.releaseLock("test-xid-locker-1");
    }

    @Test
    public void testAcquireLockConflict() {
        List<RowLock> locks1 = new ArrayList<>();
        locks1.add(createRowLock("test-xid-locker-1", 1L, 1L, "test-resource", "test_table", "110"));

        // First acquisition should succeed
        boolean firstResult = dataBaseLocker.acquireLock(locks1);
        Assertions.assertTrue(firstResult, "First lock acquisition should succeed");

        // Second acquisition with different xid should fail
        List<RowLock> locks2 = new ArrayList<>();
        locks2.add(createRowLock("test-xid-locker-2", 2L, 2L, "test-resource", "test_table", "110"));

        boolean secondResult = dataBaseLocker.acquireLock(locks2);
        Assertions.assertFalse(secondResult, "Conflicting lock acquisition should fail");

        // Clean up
        dataBaseLocker.releaseLock(locks1);
    }

    @Test
    public void testReleaseLockByBranchIdMultipleLocks() {
        List<RowLock> locks = new ArrayList<>();
        locks.add(createRowLock("test-xid-locker-1", 1L, 1001L, "test-resource", "test_table", "120"));
        locks.add(createRowLock("test-xid-locker-1", 1L, 1001L, "test-resource", "test_table", "121"));

        // Acquire locks
        dataBaseLocker.acquireLock(locks);

        // Release all by branchId
        boolean result = dataBaseLocker.releaseLock("test-xid-locker-1", 1001L);
        Assertions.assertTrue(result, "Release locks by branchId should succeed");
    }

    /**
     * Helper method to create a RowLock object
     */
    private RowLock createRowLock(
            String xid, Long transactionId, Long branchId, String resourceId, String tableName, String pk) {
        RowLock rowLock = new RowLock();
        rowLock.setXid(xid);
        rowLock.setTransactionId(transactionId);
        rowLock.setBranchId(branchId);
        rowLock.setResourceId(resourceId);
        rowLock.setTableName(tableName);
        rowLock.setPk(pk);
        rowLock.setRowKey(resourceId + "^^^" + tableName + "^^^" + pk);
        return rowLock;
    }
}
