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
package org.apache.seata.server.storage.rocksdb.lock;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

class RocksDBLockManagerTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;

    @BeforeEach
    void beforeEach() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
    }

    @AfterEach
    void afterEach() throws Exception {
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testAcquireLockAllowsSameXidAndRejectsConflict() throws Exception {
        try (RocksDBStoreEngine engine = open("conflict")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1,2");
            BranchSession sameXid = branchSession(1001L, 2L, "t_order:1");
            BranchSession conflict = branchSession(1002L, 3L, "t_order:2");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(sameXid));
            Assertions.assertFalse(lockManager.acquireLock(conflict));
        }
    }

    @Test
    void testReleaseBranchLock() throws Exception {
        try (RocksDBStoreEngine engine = open("release-branch")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession next = branchSession(1002L, 2L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertFalse(lockManager.acquireLock(next));
            Assertions.assertTrue(lockManager.releaseLock(first));
            Assertions.assertTrue(lockManager.acquireLock(next));
        }
    }

    @Test
    void testReleaseSameXidDifferentBranchDoesNotReleaseHolder() throws Exception {
        try (RocksDBStoreEngine engine = open("release-same-xid-branch")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession sameXid = branchSession(1001L, 2L, "t_order:1");
            BranchSession conflict = branchSession(1002L, 3L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(sameXid));
            Assertions.assertTrue(lockManager.releaseLock(sameXid));
            Assertions.assertFalse(lockManager.acquireLock(conflict));
            Assertions.assertTrue(lockManager.releaseLock(first));
            Assertions.assertTrue(lockManager.acquireLock(conflict));
        }
    }

    @Test
    void testRangeDeleteReleaseKeepsSameXidOtherBranch() throws Exception {
        try (RocksDBStoreEngine engine = open("range-release-same-xid-branch", true)) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession sameXid = branchSession(1001L, 2L, "t_order:2");
            BranchSession conflict = branchSession(1002L, 3L, "t_order:2");
            GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
            globalSession.setXid(first.getXid());

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(sameXid));

            Assertions.assertTrue(lockManager.releaseLock(first));
            Assertions.assertFalse(lockManager.acquireLock(conflict));

            Assertions.assertTrue(lockManager.releaseGlobalSessionLock(globalSession));
            Assertions.assertTrue(lockManager.acquireLock(conflict));
        }
    }

    @Test
    void testReleaseGlobalSessionLock() throws Exception {
        try (RocksDBStoreEngine engine = open("release-global")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession second = branchSession(1001L, 2L, "t_order:2");
            BranchSession next = branchSession(1002L, 3L, "t_order:1,2");
            GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
            globalSession.setXid(first.getXid());

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(second));
            Assertions.assertFalse(lockManager.acquireLock(next));

            Assertions.assertTrue(lockManager.releaseGlobalSessionLock(globalSession));
            Assertions.assertTrue(lockManager.acquireLock(next));
        }
    }

    @Test
    void testIsLockable() throws Exception {
        try (RocksDBStoreEngine engine = open("lockable")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");

            Assertions.assertTrue(lockManager.isLockable(first.getXid(), first.getResourceId(), first.getLockKey()));
            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.isLockable(first.getXid(), first.getResourceId(), first.getLockKey()));
            Assertions.assertFalse(lockManager.isLockable(xid(1002L), first.getResourceId(), first.getLockKey()));
        }
    }

    @Test
    void testRollbackingConflictFailFast() throws Exception {
        try (RocksDBStoreEngine engine = open("rollbacking")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession conflict = branchSession(1002L, 2L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(first));
            lockManager.updateLockStatus(first.getXid(), LockStatus.Rollbacking);

            StoreException exception = Assertions.assertThrows(
                    StoreException.class, () -> lockManager.acquireLock(conflict, false, false));
            Assertions.assertTrue(exception.getCause() instanceof BranchTransactionException);
        }
    }

    @Test
    void testSkipCheckLockDoesNotOverwriteExistingLock() throws Exception {
        try (RocksDBStoreEngine engine = open("skip-check")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession conflict = branchSession(1002L, 2L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertFalse(lockManager.acquireLock(conflict, true, true));
            Assertions.assertFalse(
                    lockManager.isLockable(conflict.getXid(), conflict.getResourceId(), conflict.getLockKey()));
        }
    }

    @Test
    void testCleanAllLocks() throws Exception {
        try (RocksDBStoreEngine engine = open("clean")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(first));
            lockManager.cleanAllLocks();

            Assertions.assertTrue(
                    engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0]).isEmpty());
            Assertions.assertTrue(engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                    .isEmpty());
        }
    }

    @Test
    void testCleanOrphanLocksRemovesLockWithoutBranchSession() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-orphan")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession orphan = branchSession(1001L, 1L, "t_order:1");
            BranchSession next = branchSession(1002L, 2L, "t_order:1");

            Assertions.assertTrue(lockManager.acquireLock(orphan));
            Assertions.assertFalse(lockManager.acquireLock(next));

            Assertions.assertEquals(1, lockManager.cleanOrphanLocks());
            Assertions.assertTrue(lockManager.acquireLock(next));
        }
    }

    @Test
    void testCleanOrphanLocksKeepsLockWithBranchSession() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-valid")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            BranchSession holder = branchSession(1001L, 1L, "t_order:1");
            BranchSession conflict = branchSession(1002L, 2L, "t_order:1");

            storeManager.writeSession(LogOperation.BRANCH_ADD, holder);
            Assertions.assertTrue(lockManager.acquireLock(holder));

            Assertions.assertEquals(0, lockManager.cleanOrphanLocks());
            Assertions.assertFalse(lockManager.acquireLock(conflict));
        }
    }

    @Test
    void testCleanOrphanLocksDeletesStaleIndexWithoutLockValue() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-stale-index")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession holder = branchSession(1001L, 1L, "t_order:1");
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock(holder.getResourceId(), "t_order", "1");

            engine.put(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndex(holder.getXid(), holder.getBranchId(), lockKey),
                    lockKey);

            Assertions.assertEquals(1, lockManager.cleanOrphanLocks());
            Assertions.assertTrue(engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                    .isEmpty());
        }
    }

    private RocksDBStoreEngine open(String name) {
        return open(name, false);
    }

    private RocksDBStoreEngine open(String name, boolean enableRangeDelete) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true, enableRangeDelete));
    }

    private BranchSession branchSession(long transactionId, long branchId, String lockKey) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid(xid(transactionId));
        branchSession.setTransactionId(transactionId);
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey(lockKey);
        return branchSession;
    }

    private String xid(long transactionId) {
        return "127.0.0.1:8091:" + transactionId;
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }
}
