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
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.apache.seata.server.transaction.at.ATCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class RocksDBLockManagerTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;
    private SessionManager originalRootSessionManager;
    private Map<String, SessionManager> originalSessionManagerMap;

    @BeforeEach
    void beforeEach() throws Exception {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
        originalRootSessionManager = rootSessionManagerField().get(null) instanceof SessionManager
                ? (SessionManager) rootSessionManagerField().get(null)
                : null;
        @SuppressWarnings("unchecked")
        Map<String, SessionManager> sessionManagerMap =
                (Map<String, SessionManager>) sessionManagerMapField().get(null);
        originalSessionManagerMap = sessionManagerMap;
    }

    @AfterEach
    void afterEach() throws Exception {
        rootSessionManagerField().set(null, originalRootSessionManager);
        sessionManagerMapField().set(null, originalSessionManagerMap);
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
    void testReleaseBranchLockRemovesStaleIndexWithoutDeletingCurrentOwner() throws Exception {
        try (RocksDBStoreEngine engine = open("release-branch-stale-index")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession staleOwner = branchSession(1001L, 1L, "t_order:1");
            BranchSession currentOwner = branchSession(1002L, 2L, "t_order:1");
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock(currentOwner.getResourceId(), "t_order", "1");
            byte[] staleIndexKey =
                    RocksDBKeyCodec.encodeLockBranchIndex(staleOwner.getXid(), staleOwner.getBranchId(), lockKey);
            byte[] currentIndexKey =
                    RocksDBKeyCodec.encodeLockBranchIndex(currentOwner.getXid(), currentOwner.getBranchId(), lockKey);

            Assertions.assertTrue(lockManager.acquireLock(currentOwner));
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, lockKey);

            Assertions.assertTrue(lockManager.releaseLock(staleOwner));
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertNotNull(engine.get(RocksDBColumnFamily.LOCK, lockKey));
            Assertions.assertNotNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, currentIndexKey));
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
    void testReleaseGlobalSessionLockRemovesStaleIndexWithoutDeletingCurrentOwner() throws Exception {
        try (RocksDBStoreEngine engine = open("release-global-stale-index")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession staleOwner = branchSession(1001L, 1L, "t_order:1");
            BranchSession currentOwner = branchSession(1002L, 2L, "t_order:1");
            GlobalSession staleGlobal = new GlobalSession("app", "group", "tx", 60000);
            staleGlobal.setXid(staleOwner.getXid());
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock(currentOwner.getResourceId(), "t_order", "1");
            byte[] staleIndexKey =
                    RocksDBKeyCodec.encodeLockBranchIndex(staleOwner.getXid(), staleOwner.getBranchId(), lockKey);
            byte[] currentIndexKey =
                    RocksDBKeyCodec.encodeLockBranchIndex(currentOwner.getXid(), currentOwner.getBranchId(), lockKey);

            Assertions.assertTrue(lockManager.acquireLock(currentOwner));
            engine.put(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey, lockKey);

            Assertions.assertTrue(lockManager.releaseGlobalSessionLock(staleGlobal));
            Assertions.assertNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, staleIndexKey));
            Assertions.assertNotNull(engine.get(RocksDBColumnFamily.LOCK, lockKey));
            Assertions.assertNotNull(engine.get(RocksDBColumnFamily.LOCK_BRANCH_INDEX, currentIndexKey));
        }
    }

    @Test
    void testReleaseGlobalSessionLockHandlesMultipleLockIndexBatches() throws Exception {
        try (RocksDBStoreEngine engine = open("release-global-batches")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine, 1);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession second = branchSession(1001L, 2L, "t_order:2");
            BranchSession third = branchSession(1001L, 3L, "t_order:3");
            BranchSession next = branchSession(1002L, 4L, "t_order:1,2,3");
            GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
            globalSession.setXid(first.getXid());

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(second));
            Assertions.assertTrue(lockManager.acquireLock(third));
            Assertions.assertFalse(lockManager.acquireLock(next));

            Assertions.assertTrue(lockManager.releaseGlobalSessionLock(globalSession));
            Assertions.assertTrue(lockManager.acquireLock(next));
        }
    }

    @Test
    void testConcurrentBatchedReleaseAndAcquireKeepsLockSetAtomic() throws Exception {
        try (RocksDBStoreEngine engine = open("release-acquire-race")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine, 1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                for (int round = 0; round < 20; round++) {
                    long oldTransactionId = 2000L + round * 2L;
                    long nextTransactionId = oldTransactionId + 1L;
                    String lockKey = "t_order:" + (round * 3 + 1) + "," + (round * 3 + 2) + "," + (round * 3 + 3);
                    BranchSession oldHolder = branchSession(oldTransactionId, oldTransactionId, lockKey);
                    BranchSession nextHolder = branchSession(nextTransactionId, nextTransactionId, lockKey);
                    BranchSession thirdHolder = branchSession(10000L + round, 10000L + round, lockKey);
                    GlobalSession oldGlobal = new GlobalSession("app", "group", "tx", 60000);
                    oldGlobal.setXid(oldHolder.getXid());
                    Assertions.assertTrue(lockManager.acquireLock(oldHolder));

                    CyclicBarrier start = new CyclicBarrier(2);
                    Future<Boolean> release = executor.submit(() -> {
                        start.await();
                        return lockManager.releaseGlobalSessionLock(oldGlobal);
                    });
                    Future<Boolean> acquire = executor.submit(() -> {
                        start.await();
                        return lockManager.acquireLock(nextHolder);
                    });

                    Assertions.assertTrue(release.get());
                    if (!acquire.get()) {
                        Assertions.assertTrue(lockManager.acquireLock(nextHolder));
                    }
                    Assertions.assertFalse(lockManager.acquireLock(thirdHolder));
                    Assertions.assertTrue(lockManager.releaseLock(nextHolder));
                }
            } finally {
                executor.shutdownNow();
            }
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
    void testUpdateLockStatusHandlesMultipleLockIndexBatches() throws Exception {
        try (RocksDBStoreEngine engine = open("rollbacking-batches")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine, 1);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession second = branchSession(1001L, 2L, "t_order:2");
            BranchSession conflict = branchSession(1002L, 3L, "t_order:2");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(second));
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
    void testCleanOrphanLocksWithLimitLeavesRemainingLocksForNextBatch() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-orphan-limit")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession second = branchSession(1002L, 2L, "t_order:2");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(second));

            RocksDBLockManager.CleanOrphanLocksResult firstBatch = lockManager.cleanOrphanLocks(1);
            Assertions.assertEquals(1, firstBatch.getCleaned());
            Assertions.assertEquals(1, firstBatch.getScanned());
            Assertions.assertTrue(firstBatch.isLimitReached());
            Assertions.assertNotNull(firstBatch.getNextSeekKey());
            Assertions.assertEquals(
                    1, engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0]).size());
            Assertions.assertEquals(
                    1,
                    engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                            .size());

            RocksDBLockManager.CleanOrphanLocksResult secondBatch = lockManager.cleanOrphanLocks(1);
            Assertions.assertEquals(1, secondBatch.getCleaned());
            Assertions.assertEquals(1, secondBatch.getScanned());
            Assertions.assertTrue(secondBatch.isLimitReached());
            Assertions.assertTrue(
                    engine.prefixScan(RocksDBColumnFamily.LOCK, new byte[0]).isEmpty());
            Assertions.assertTrue(engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                    .isEmpty());
        }
    }

    @Test
    void testCleanOrphanLocksBatchesStopsAtMaxBatchesWithCursor() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-orphan-batches")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            BranchSession first = branchSession(1001L, 1L, "t_order:1");
            BranchSession second = branchSession(1002L, 2L, "t_order:2");
            BranchSession third = branchSession(1003L, 3L, "t_order:3");

            Assertions.assertTrue(lockManager.acquireLock(first));
            Assertions.assertTrue(lockManager.acquireLock(second));
            Assertions.assertTrue(lockManager.acquireLock(third));

            RocksDBLockManager.CleanOrphanLocksResult result = lockManager.cleanOrphanLocksBatches(1, 2);

            Assertions.assertEquals(2, result.getCleaned());
            Assertions.assertEquals(2, result.getScanned());
            Assertions.assertEquals(2, result.getBatches());
            Assertions.assertTrue(result.isLimitReached());
            Assertions.assertNotNull(result.getNextSeekKey());
            Assertions.assertEquals(
                    1,
                    engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                            .size());

            RocksDBLockManager.CleanOrphanLocksResult finalResult =
                    lockManager.cleanOrphanLocksBatches(result.getNextSeekKey(), 1, 2);

            Assertions.assertEquals(1, finalResult.getCleaned());
            Assertions.assertEquals(1, finalResult.getScanned());
            Assertions.assertEquals(2, finalResult.getBatches());
            Assertions.assertFalse(finalResult.isLimitReached());
            Assertions.assertNull(finalResult.getNextSeekKey());
            Assertions.assertTrue(engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                    .isEmpty());
        }
    }

    @Test
    void testCleanOrphanLocksWithLimitReportsRemainingWhenScannedLocksAreValid() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-orphan-valid-prefix")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
            BranchSession validFirst = branchSession(1001L, 1L, "t_order:1");
            BranchSession validSecond = branchSession(1002L, 2L, "t_order:2");
            BranchSession orphanAfterValidLocks = branchSession(1003L, 3L, "t_order:3");

            storeManager.writeSession(LogOperation.BRANCH_ADD, validFirst);
            storeManager.writeSession(LogOperation.BRANCH_ADD, validSecond);
            Assertions.assertTrue(lockManager.acquireLock(validFirst));
            Assertions.assertTrue(lockManager.acquireLock(validSecond));
            Assertions.assertTrue(lockManager.acquireLock(orphanAfterValidLocks));

            RocksDBLockManager.CleanOrphanLocksResult result = lockManager.cleanOrphanLocks(2);

            Assertions.assertEquals(0, result.getCleaned());
            Assertions.assertEquals(2, result.getScanned());
            Assertions.assertTrue(result.isLimitReached());
            Assertions.assertNotNull(result.getNextSeekKey());
            Assertions.assertEquals(
                    3,
                    engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                            .size());

            RocksDBLockManager.CleanOrphanLocksResult nextResult =
                    lockManager.cleanOrphanLocks(result.getNextSeekKey(), 2);

            Assertions.assertEquals(1, nextResult.getCleaned());
            Assertions.assertEquals(1, nextResult.getScanned());
            Assertions.assertFalse(nextResult.isLimitReached());
            Assertions.assertNull(nextResult.getNextSeekKey());
            Assertions.assertEquals(
                    2,
                    engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                            .size());
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
    void testCleanOrphanLocksDoesNotDeleteLockDuringBranchRegistration() throws Exception {
        try (RocksDBStoreEngine engine = open("clean-registration-race")) {
            RocksDBLockManager registrationLockManager = new RocksDBLockManager(engine);
            CountDownLatch lockAcquired = new CountDownLatch(1);
            CountDownLatch continueRegistration = new CountDownLatch(1);
            CompletableFuture<Void> cleanupEnteredSessionLock = new CompletableFuture<>();
            AtomicReference<Thread> cleanupThread = new AtomicReference<>();
            RocksDBSessionManager sessionManager = new RocksDBSessionManager("root.data", engine) {
                @Override
                public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
                        throws TransactionException {
                    if (Thread.currentThread() == cleanupThread.get()) {
                        cleanupEnteredSessionLock.complete(null);
                    }
                    return super.lockAndExecute(globalSession, lockCallable);
                }
            };
            setRootSessionManager(sessionManager);
            Field lockManagerField = lockerManagerField();
            Object originalLockManager = lockManagerField.get(null);
            lockManagerField.set(null, registrationLockManager);

            GlobalSession globalSession = new GlobalSession("app", "group", "registration-race", 60_000);
            globalSession.setStatus(GlobalStatus.Begin);
            globalSession.setBeginTime(System.currentTimeMillis());
            sessionManager.addGlobalSession(globalSession);
            String resourceId = "jdbc:mysql://127.0.0.1/db";
            BranchSession branchSession = branchSession(globalSession, 1L, "t_order:1");
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock(resourceId, "t_order", "1");
            class PausingATCore extends ATCore {
                private PausingATCore() {
                    super(Mockito.mock(RemotingServer.class));
                }

                private Long register(GlobalSession registeringGlobal, BranchSession registeringBranch)
                        throws TransactionException {
                    return SessionHolder.lockAndExecute(registeringGlobal, () -> {
                        branchSessionLock(registeringGlobal, registeringBranch);
                        registeringGlobal.addBranch(registeringBranch);
                        return registeringBranch.getBranchId();
                    });
                }

                @Override
                protected void branchSessionLock(GlobalSession registeringGlobal, BranchSession branchSession)
                        throws TransactionException {
                    super.branchSessionLock(registeringGlobal, branchSession);
                    lockAcquired.countDown();
                    try {
                        if (!continueRegistration.await(5, TimeUnit.SECONDS)) {
                            throw new TransactionException("registration test latch timed out");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new TransactionException(e);
                    }
                }
            }
            PausingATCore registrationCore = new PausingATCore();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CompletableFuture<Long> registration = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return registrationCore.register(globalSession, branchSession);
                        } catch (TransactionException e) {
                            throw new CompletionException(e);
                        }
                    },
                    executor);
            CompletableFuture<RocksDBLockManager.CleanOrphanLocksResult> cleanup = null;
            try {
                Assertions.assertTrue(lockAcquired.await(5, TimeUnit.SECONDS));
                cleanup = CompletableFuture.supplyAsync(
                        () -> {
                            cleanupThread.set(Thread.currentThread());
                            return registrationLockManager.cleanOrphanLocks(1);
                        },
                        executor);

                CompletableFuture.anyOf(cleanupEnteredSessionLock, cleanup).get(5, TimeUnit.SECONDS);
                Assertions.assertNotNull(
                        engine.get(RocksDBColumnFamily.LOCK, lockKey),
                        "orphan cleanup must not delete a lock owned by an in-flight registration");

                continueRegistration.countDown();
                Assertions.assertNotNull(registration.get(5, TimeUnit.SECONDS));
                Assertions.assertEquals(0, cleanup.get(5, TimeUnit.SECONDS).getCleaned());
                Assertions.assertEquals(0, registrationLockManager.cleanOrphanLocks());
                Assertions.assertNotNull(engine.get(RocksDBColumnFamily.LOCK, lockKey));
            } finally {
                continueRegistration.countDown();
                awaitCompletion(registration);
                awaitCompletion(cleanup);
                executor.shutdownNow();
                Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                lockManagerField.set(null, originalLockManager);
            }
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

    private BranchSession branchSession(GlobalSession globalSession, long branchId, String lockKey) {
        BranchSession branchSession = branchSession(globalSession.getTransactionId(), branchId, lockKey);
        branchSession.setXid(globalSession.getXid());
        return branchSession;
    }

    private void setRootSessionManager(SessionManager sessionManager) throws Exception {
        rootSessionManagerField().set(null, sessionManager);
        sessionManagerMapField().set(null, null);
    }

    private Field rootSessionManagerField() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
        field.setAccessible(true);
        return field;
    }

    private Field sessionManagerMapField() throws Exception {
        Field field = SessionHolder.class.getDeclaredField("SESSION_MANAGER_MAP");
        field.setAccessible(true);
        return field;
    }

    private Field lockerManagerField() throws Exception {
        Field field = org.apache.seata.server.lock.LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        return field;
    }

    private void awaitCompletion(CompletableFuture<?> future) {
        if (future == null) {
            return;
        }
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // The test assertion reports the primary failure; cleanup only prevents leaked workers.
        }
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
