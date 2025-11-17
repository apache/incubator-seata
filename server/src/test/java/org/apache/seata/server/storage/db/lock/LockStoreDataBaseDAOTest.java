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
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
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
public class LockStoreDataBaseDAOTest extends BaseSpringBootTest {

    private LockStoreDataBaseDAO lockStoreDataBaseDAO;

    @BeforeEach
    public void setUp() {
        DataSource dataSource =
                EnhancedServiceLoader.load(DataSourceProvider.class, "druid").provide();
        lockStoreDataBaseDAO = new LockStoreDataBaseDAO(dataSource);
    }

    @AfterEach
    public void tearDown() {
        // Clean up any test locks
        try {
            lockStoreDataBaseDAO.unLock("test-xid-1");
            lockStoreDataBaseDAO.unLock("test-xid-2");
            lockStoreDataBaseDAO.unLock("test-xid-conflict");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(lockStoreDataBaseDAO);
    }

    @Test
    public void testAcquireSingleLock() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "1");

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDO);
        Assertions.assertTrue(result, "Should successfully acquire lock");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDO);
    }

    @Test
    public void testAcquireSingleLockDuplicate() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "2");

        // First acquisition should succeed
        boolean firstResult = lockStoreDataBaseDAO.acquireLock(lockDO);
        Assertions.assertTrue(firstResult, "First lock acquisition should succeed");

        // Second acquisition with same xid should succeed (same transaction)
        boolean secondResult = lockStoreDataBaseDAO.acquireLock(lockDO);
        Assertions.assertTrue(secondResult, "Same XID should be able to reacquire lock");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDO);
    }

    @Test
    public void testAcquireLockConflict() {
        LockDO lockDO1 = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "3");
        LockDO lockDO2 = createLockDO("test-xid-2", 2L, 2L, "test-resource", "test_table", "3");

        // First lock should succeed
        boolean firstResult = lockStoreDataBaseDAO.acquireLock(lockDO1);
        Assertions.assertTrue(firstResult, "First lock should succeed");

        // Second lock with different xid should fail
        boolean secondResult = lockStoreDataBaseDAO.acquireLock(lockDO2);
        Assertions.assertFalse(secondResult, "Conflicting lock should fail");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDO1);
    }

    @Test
    public void testAcquireBatchLocks() {
        List<LockDO> lockDOs = new ArrayList<>();
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "10"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "11"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "12"));

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDOs);
        Assertions.assertTrue(result, "Batch lock acquisition should succeed");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDOs);
    }

    @Test
    public void testAcquireBatchLocksWithDuplicates() {
        List<LockDO> lockDOs = new ArrayList<>();
        // Add same row key twice - should be deduplicated
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "20"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "20"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "21"));

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDOs);
        Assertions.assertTrue(result, "Batch lock with duplicates should succeed after deduplication");

        // Clean up
        lockStoreDataBaseDAO.unLock("test-xid-1");
    }

    @Test
    public void testAcquireBatchLocksConflict() {
        // First acquire a lock
        LockDO existingLock = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "30");
        lockStoreDataBaseDAO.acquireLock(existingLock);

        // Try to acquire batch including conflicting lock
        List<LockDO> lockDOs = new ArrayList<>();
        lockDOs.add(createLockDO("test-xid-2", 2L, 2L, "test-resource", "test_table", "30")); // Conflict
        lockDOs.add(createLockDO("test-xid-2", 2L, 2L, "test-resource", "test_table", "31"));

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDOs);
        Assertions.assertFalse(result, "Batch lock with conflict should fail");

        // Clean up
        lockStoreDataBaseDAO.unLock(existingLock);
    }

    @Test
    public void testAcquireLockWithSkipCheckLock() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "40");
        List<LockDO> lockDOs = Arrays.asList(lockDO);

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDOs, true, true);
        Assertions.assertTrue(result, "Lock acquisition with skipCheckLock should succeed");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDO);
    }

    @Test
    public void testUnLockSingleLock() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "50");

        // Acquire then unlock
        lockStoreDataBaseDAO.acquireLock(lockDO);
        boolean result = lockStoreDataBaseDAO.unLock(lockDO);
        Assertions.assertTrue(result, "Unlock should succeed");
    }

    @Test
    public void testUnLockBatchLocks() {
        List<LockDO> lockDOs = new ArrayList<>();
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "60"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "61"));

        // Acquire then unlock
        lockStoreDataBaseDAO.acquireLock(lockDOs);
        boolean result = lockStoreDataBaseDAO.unLock(lockDOs);
        Assertions.assertTrue(result, "Batch unlock should succeed");
    }

    @Test
    public void testUnLockByXid() {
        List<LockDO> lockDOs = new ArrayList<>();
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "70"));
        lockDOs.add(createLockDO("test-xid-1", 1L, 2L, "test-resource", "test_table", "71"));

        // Acquire locks
        lockStoreDataBaseDAO.acquireLock(lockDOs);

        // Unlock all by xid
        boolean result = lockStoreDataBaseDAO.unLock("test-xid-1");
        Assertions.assertTrue(result, "Unlock by xid should succeed");
    }

    @Test
    public void testUnLockByBranchId() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 100L, "test-resource", "test_table", "80");

        // Acquire lock
        lockStoreDataBaseDAO.acquireLock(lockDO);

        // Unlock by branchId
        boolean result = lockStoreDataBaseDAO.unLock(100L);
        Assertions.assertTrue(result, "Unlock by branchId should succeed");
    }

    @Test
    public void testIsLockable() {
        LockDO lockDO1 = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "90");
        LockDO lockDO2 = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "91");

        // Before acquiring, should be lockable
        List<LockDO> lockDOs = Arrays.asList(lockDO1, lockDO2);
        boolean result = lockStoreDataBaseDAO.isLockable(lockDOs);
        Assertions.assertTrue(result, "Should be lockable when no existing locks");

        // Acquire locks
        lockStoreDataBaseDAO.acquireLock(lockDOs);

        // Same xid should still be lockable
        boolean sameTxResult = lockStoreDataBaseDAO.isLockable(lockDOs);
        Assertions.assertTrue(sameTxResult, "Should be lockable by same xid");

        // Different xid should not be lockable
        LockDO conflictLock = createLockDO("test-xid-2", 2L, 2L, "test-resource", "test_table", "90");
        boolean conflictResult = lockStoreDataBaseDAO.isLockable(Arrays.asList(conflictLock));
        Assertions.assertFalse(conflictResult, "Should not be lockable by different xid");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDOs);
    }

    @Test
    public void testUpdateLockStatus() {
        LockDO lockDO = createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "100");

        // Acquire lock
        lockStoreDataBaseDAO.acquireLock(lockDO);

        // Update status to Rollbacking
        lockStoreDataBaseDAO.updateLockStatus("test-xid-1", LockStatus.Rollbacking);

        // Note: We can't easily verify the status update without querying the DB directly
        // The method should execute without throwing exceptions

        // Clean up
        lockStoreDataBaseDAO.unLock("test-xid-1");
    }

    @Test
    public void testAcquireLockWithAutoCommitFalse() {
        List<LockDO> lockDOs = new ArrayList<>();
        lockDOs.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "110"));

        boolean result = lockStoreDataBaseDAO.acquireLock(lockDOs, false, false);
        Assertions.assertTrue(result, "Lock acquisition with autoCommit=false should succeed");

        // Clean up
        lockStoreDataBaseDAO.unLock(lockDOs);
    }

    @Test
    public void testAcquireLockReacquireAfterPartialExists() {
        // First, acquire some locks
        List<LockDO> firstBatch = new ArrayList<>();
        firstBatch.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "120"));
        firstBatch.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "121"));
        lockStoreDataBaseDAO.acquireLock(firstBatch);

        // Then try to acquire overlapping locks
        List<LockDO> secondBatch = new ArrayList<>();
        secondBatch.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "120")); // Already exists
        secondBatch.add(createLockDO("test-xid-1", 1L, 1L, "test-resource", "test_table", "122")); // New

        boolean result = lockStoreDataBaseDAO.acquireLock(secondBatch);
        Assertions.assertTrue(result, "Should succeed when some locks already exist with same xid");

        // Clean up
        lockStoreDataBaseDAO.unLock("test-xid-1");
    }

    /**
     * Helper method to create a LockDO object
     */
    private LockDO createLockDO(
            String xid, Long transactionId, Long branchId, String resourceId, String tableName, String pk) {
        LockDO lockDO = new LockDO();
        lockDO.setXid(xid);
        lockDO.setTransactionId(transactionId);
        lockDO.setBranchId(branchId);
        lockDO.setResourceId(resourceId);
        lockDO.setTableName(tableName);
        lockDO.setPk(pk);
        lockDO.setRowKey(resourceId + "^^^" + tableName + "^^^" + pk);
        lockDO.setStatus(LockStatus.Locked.getCode());
        return lockDO;
    }
}
