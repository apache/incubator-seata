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
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class RocksDBOrphanLockCleanupControllerTest {

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
    void testRunCycleCleansAllOrphansInBoundedRounds() throws Exception {
        try (RocksDBStoreEngine engine = open("cycle-bounded")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 25);
            RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 4, 3, 0L, millis -> {});

            controller.runCycle();

            Assertions.assertEquals(0, lockIndexSize(engine));
            Assertions.assertEquals(25, controller.getTotalCleaned());
            Assertions.assertEquals(3, controller.getTotalRounds());
            Assertions.assertEquals(1, controller.getCompletedPasses());
            Assertions.assertEquals(1, controller.getCompletedCycles());
            Assertions.assertNull(controller.loadPersistedCursor());
            controller.close();
        }
    }

    @Test
    void testCursorPersistedBetweenRoundsAndResumedAfterRestart() throws Exception {
        try (RocksDBStoreEngine engine = open("cursor-resume")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 10);

            RocksDBOrphanLockCleanupController first = controller(engine, lockManager, 2, 2, 0L, millis -> {});
            RocksDBLockManager.CleanOrphanLocksResult firstRound = first.runOneRound();

            Assertions.assertEquals(4, firstRound.getCleaned());
            Assertions.assertTrue(firstRound.isLimitReached());
            Assertions.assertNotNull(firstRound.getNextSeekKey());
            Assertions.assertNotNull(
                    first.loadPersistedCursor(), "cursor should be persisted after an incomplete round");
            Assertions.assertEquals(6, lockIndexSize(engine));
            first.close();

            // Simulate a restart: a fresh controller resumes from the persisted cursor.
            RocksDBOrphanLockCleanupController second = controller(engine, lockManager, 2, 2, 0L, millis -> {});
            int cleaned = 0;
            while (true) {
                RocksDBLockManager.CleanOrphanLocksResult result = second.runOneRound();
                cleaned += result.getCleaned();
                if (!result.isLimitReached() || result.getNextSeekKey() == null) {
                    break;
                }
            }

            Assertions.assertEquals(6, cleaned);
            Assertions.assertEquals(0, lockIndexSize(engine));
            Assertions.assertNull(second.loadPersistedCursor(), "cursor should be cleared after a completed pass");
            second.close();
        }
    }

    @Test
    void testRunOneRoundAbortsWhenPersistedCursorLoadFails() {
        RocksDBStoreEngine engine = Mockito.mock(RocksDBStoreEngine.class);
        RocksDBLockManager lockManager = Mockito.mock(RocksDBLockManager.class);
        RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 2, 1, 0L, millis -> {});
        Mockito.when(lockManager.cleanOrphanLocksBatches(Mockito.isNull(), Mockito.eq(2), Mockito.eq(1)))
                .thenReturn(new RocksDBLockManager.CleanOrphanLocksResult(0, 0, false, null));
        Mockito.doThrow(new StoreException("load cursor failed"))
                .when(engine)
                .get(
                        Mockito.eq(RocksDBColumnFamily.METADATA),
                        Mockito.eq(RocksDBOrphanLockCleanupController.ORPHAN_LOCK_CLEAN_CURSOR_KEY));

        try {
            Assertions.assertThrows(StoreException.class, controller::runOneRound);
        } finally {
            controller.close();
        }
    }

    @Test
    void testRunOneRoundAbortsWhenPersistedCursorSaveFails() {
        RocksDBStoreEngine engine = Mockito.mock(RocksDBStoreEngine.class);
        RocksDBLockManager lockManager = Mockito.mock(RocksDBLockManager.class);
        byte[] nextCursor = new byte[] {1, 2, 3};
        Mockito.when(lockManager.cleanOrphanLocksBatches(Mockito.isNull(), Mockito.eq(2), Mockito.eq(1)))
                .thenReturn(new RocksDBLockManager.CleanOrphanLocksResult(0, 2, true, nextCursor));
        Mockito.doThrow(new StoreException("save cursor failed"))
                .when(engine)
                .put(
                        Mockito.eq(RocksDBColumnFamily.METADATA),
                        Mockito.eq(RocksDBOrphanLockCleanupController.ORPHAN_LOCK_CLEAN_CURSOR_KEY),
                        Mockito.eq(nextCursor));
        RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 2, 1, 0L, millis -> {});

        try {
            Assertions.assertThrows(StoreException.class, controller::runOneRound);
        } finally {
            controller.close();
        }
    }

    @Test
    void testRunOneRoundAbortsWhenPersistedCursorClearFails() {
        RocksDBStoreEngine engine = Mockito.mock(RocksDBStoreEngine.class);
        RocksDBLockManager lockManager = Mockito.mock(RocksDBLockManager.class);
        Mockito.when(lockManager.cleanOrphanLocksBatches(Mockito.isNull(), Mockito.eq(2), Mockito.eq(1)))
                .thenReturn(new RocksDBLockManager.CleanOrphanLocksResult(0, 0, false, null));
        Mockito.doThrow(new StoreException("clear cursor failed"))
                .when(engine)
                .delete(
                        Mockito.eq(RocksDBColumnFamily.METADATA),
                        Mockito.eq(RocksDBOrphanLockCleanupController.ORPHAN_LOCK_CLEAN_CURSOR_KEY));
        RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 2, 1, 0L, millis -> {});

        try {
            Assertions.assertThrows(StoreException.class, controller::runOneRound);
        } finally {
            controller.close();
        }
    }

    @Test
    void testScheduledCycleCleansOrphans() throws Exception {
        try (RocksDBStoreEngine engine = open("scheduled")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 12);
            RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 3, 2, 50L, millis -> {});

            controller.start();
            try {
                awaitPasses(controller, 1, 5000L);
                Assertions.assertTrue(controller.getCompletedPasses() >= 1, "scheduled cycle should complete a pass");
                Assertions.assertEquals(0, lockIndexSize(engine));
                Assertions.assertEquals(12, controller.getTotalCleaned());
            } finally {
                controller.close();
            }
        }
    }

    @Test
    void testTriggerNowRunsCycleAsynchronously() throws Exception {
        try (RocksDBStoreEngine engine = open("trigger-now")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 5);
            RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 2, 2, 0L, millis -> {});

            try {
                Assertions.assertTrue(controller.triggerNow());
                awaitPasses(controller, 1, 5000L);
                Assertions.assertEquals(1, controller.getCompletedPasses());
                Assertions.assertEquals(5, controller.getTotalCleaned());
                Assertions.assertEquals(0, lockIndexSize(engine));
            } finally {
                controller.close();
            }
        }
    }

    @Test
    void testCloseWaitsForExecutorTerminationAfterForcedShutdown() throws Exception {
        try (RocksDBStoreEngine engine = open("close-termination-barrier")) {
            CountDownLatch batchStarted = new CountDownLatch(1);
            CountDownLatch releaseBatch = new CountDownLatch(1);
            RocksDBLockManager lockManager = new RocksDBLockManager(engine) {
                @Override
                public CleanOrphanLocksResult cleanOrphanLocksBatches(byte[] seekKey, int batchLimit, int maxBatches) {
                    batchStarted.countDown();
                    boolean interrupted = false;
                    while (true) {
                        try {
                            releaseBatch.await();
                            break;
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return new CleanOrphanLocksResult(0, 1, false, null);
                }
            };
            FirstAwaitTimeoutExecutor cleanupExecutor = new FirstAwaitTimeoutExecutor();
            RocksDBOrphanLockCleanupController controller = new RocksDBOrphanLockCleanupController(
                    lockManager, engine, 50L, 1, 1, 0L, cleanupExecutor, true, millis -> {}, System::nanoTime);
            ExecutorService closeExecutor = Executors.newSingleThreadExecutor();
            CompletableFuture<Void> closeFuture = null;
            try {
                Assertions.assertTrue(controller.triggerNow());
                Assertions.assertTrue(batchStarted.await(5, TimeUnit.SECONDS));

                closeFuture = CompletableFuture.runAsync(controller::close, closeExecutor);
                CompletableFuture.anyOf(cleanupExecutor.secondAwaitEntered, closeFuture)
                        .get(5, TimeUnit.SECONDS);

                Assertions.assertTrue(
                        cleanupExecutor.secondAwaitEntered.isDone(),
                        "close must await termination after forcing executor shutdown");
                Assertions.assertFalse(closeFuture.isDone(), "close must not return while the cleanup batch is active");

                releaseBatch.countDown();
                closeFuture.get(5, TimeUnit.SECONDS);
                Assertions.assertTrue(cleanupExecutor.isTerminated());
            } finally {
                releaseBatch.countDown();
                if (closeFuture != null) {
                    try {
                        closeFuture.get(5, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                        // The assertions above retain the primary failure.
                    }
                }
                controller.close();
                closeExecutor.shutdownNow();
                cleanupExecutor.shutdownNow();
                Assertions.assertTrue(closeExecutor.awaitTermination(5, TimeUnit.SECONDS));
                Assertions.assertTrue(cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void testCloseThrowsAndRemembersExecutorTerminationFailure() throws Exception {
        try (RocksDBStoreEngine engine = open("close-termination-failure")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            NeverTerminatingExecutor executor = new NeverTerminatingExecutor();
            RocksDBOrphanLockCleanupController controller = new RocksDBOrphanLockCleanupController(
                    lockManager, engine, 50L, 1, 1, 0L, executor, true, millis -> {}, System::nanoTime);
            executor.startBlocker();
            try {
                StoreException firstFailure = Assertions.assertThrows(StoreException.class, controller::close);
                StoreException repeatedFailure = Assertions.assertThrows(StoreException.class, controller::close);

                Assertions.assertSame(firstFailure, repeatedFailure);
                Assertions.assertFalse(executor.isTerminated());
            } finally {
                executor.releaseBlocker();
                executor.shutdownNow();
                Assertions.assertTrue(executor.awaitActualTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void testInterruptedClosePreservesInterruptAndFailure() throws Exception {
        try (RocksDBStoreEngine engine = open("close-interrupted")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            InterruptingAwaitExecutor executor = new InterruptingAwaitExecutor();
            RocksDBOrphanLockCleanupController controller = new RocksDBOrphanLockCleanupController(
                    lockManager, engine, 50L, 1, 1, 0L, executor, true, millis -> {}, System::nanoTime);
            try {
                StoreException firstFailure = Assertions.assertThrows(StoreException.class, controller::close);
                Assertions.assertTrue(Thread.currentThread().isInterrupted());
                Thread.interrupted();

                StoreException repeatedFailure = Assertions.assertThrows(StoreException.class, controller::close);
                Assertions.assertSame(firstFailure, repeatedFailure);
            } finally {
                Thread.interrupted();
                executor.shutdownNow();
                Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void testSessionHolderDestroyStopsWhenControllerCloseFails() throws Exception {
        try (RocksDBStoreEngine engine = open("session-holder-close-failure")) {
            NeverTerminatingExecutor executor = new NeverTerminatingExecutor();
            RocksDBOrphanLockCleanupController controller = new RocksDBOrphanLockCleanupController(
                    new RocksDBLockManager(engine),
                    engine,
                    50L,
                    1,
                    1,
                    0L,
                    executor,
                    true,
                    millis -> {},
                    System::nanoTime);
            SessionManager sessionManager = Mockito.mock(SessionManager.class);
            Field controllerField = SessionHolder.class.getDeclaredField("ROCKSDB_ORPHAN_LOCK_CLEANUP_CONTROLLER");
            Field rootSessionManagerField = SessionHolder.class.getDeclaredField("ROOT_SESSION_MANAGER");
            controllerField.setAccessible(true);
            rootSessionManagerField.setAccessible(true);
            Object originalController = controllerField.get(null);
            Object originalRootSessionManager = rootSessionManagerField.get(null);
            controllerField.set(null, controller);
            rootSessionManagerField.set(null, sessionManager);
            executor.startBlocker();
            try {
                Assertions.assertThrows(StoreException.class, SessionHolder::destroy);

                Mockito.verify(sessionManager, Mockito.never()).destroy();
                Assertions.assertSame(controller, controllerField.get(null));
            } finally {
                controllerField.set(null, originalController);
                rootSessionManagerField.set(null, originalRootSessionManager);
                executor.releaseBlocker();
                executor.shutdownNow();
                Assertions.assertTrue(executor.awaitActualTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void testDisabledControllerIsNoOp() throws Exception {
        try (RocksDBStoreEngine engine = open("disabled")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 3);
            RocksDBOrphanLockCleanupController disabled = RocksDBOrphanLockCleanupController.disabled();

            Assertions.assertFalse(disabled.isEnabled());
            disabled.start();
            Assertions.assertFalse(disabled.triggerNow());
            disabled.runCycle();
            Assertions.assertEquals(0, disabled.getCompletedCycles());
            Assertions.assertEquals(3, lockIndexSize(engine), "disabled controller must not clean anything");
            disabled.close();
        }
    }

    @Test
    void testDefaultMaxBatchesFavorsForegroundLatency() throws Exception {
        try (RocksDBStoreEngine engine = open("default-max-batches")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            RocksDBOrphanLockCleanupController controller =
                    RocksDBOrphanLockCleanupController.create(lockManager, engine);
            try {
                Field maxBatches = RocksDBOrphanLockCleanupController.class.getDeclaredField("maxBatches");
                maxBatches.setAccessible(true);
                Assertions.assertEquals(2, maxBatches.getInt(controller));
            } finally {
                controller.close();
            }
        }
    }

    @Test
    void testInterruptedSleepStopsCycleButPersistsCursorForResume() throws Exception {
        try (RocksDBStoreEngine engine = open("interrupted-sleep")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 10);
            RocksDBOrphanLockCleanupController interrupting = controller(engine, lockManager, 2, 2, 10L, millis -> {
                throw new InterruptedException("test interrupt");
            });

            interrupting.runCycle();

            Assertions.assertEquals(4, interrupting.getTotalCleaned());
            Assertions.assertEquals(1, interrupting.getTotalRounds());
            Assertions.assertEquals(0, interrupting.getCompletedPasses());
            Assertions.assertEquals(6, lockIndexSize(engine));
            Assertions.assertNotNull(
                    interrupting.loadPersistedCursor(), "cursor should survive an interrupted cycle for resume");
            interrupting.close();

            RocksDBOrphanLockCleanupController resuming = controller(engine, lockManager, 2, 2, 0L, millis -> {});
            resuming.runCycle();

            Assertions.assertEquals(0, lockIndexSize(engine));
            Assertions.assertEquals(6, resuming.getTotalCleaned());
            Assertions.assertEquals(1, resuming.getCompletedPasses());
            Assertions.assertNull(resuming.loadPersistedCursor());
            resuming.close();
        }
    }

    @Test
    void testSleepInvokedBetweenRoundsButNotAfterFinalRound() throws Exception {
        try (RocksDBStoreEngine engine = open("round-sleep")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            acquireOrphans(lockManager, 10);
            AtomicInteger sleepCount = new AtomicInteger();
            RocksDBOrphanLockCleanupController controller =
                    controller(engine, lockManager, 2, 2, 5L, millis -> sleepCount.incrementAndGet());

            controller.runCycle();

            Assertions.assertEquals(10, controller.getTotalCleaned());
            Assertions.assertEquals(3, controller.getTotalRounds());
            Assertions.assertEquals(
                    controller.getTotalRounds() - 1,
                    sleepCount.get(),
                    "sleep should run between rounds, not after the final round");
            controller.close();
        }
    }

    @Test
    void testCycleOnEmptyLockTableCompletesPass() throws Exception {
        try (RocksDBStoreEngine engine = open("empty")) {
            RocksDBLockManager lockManager = new RocksDBLockManager(engine);
            RocksDBOrphanLockCleanupController controller = controller(engine, lockManager, 4, 3, 0L, millis -> {});

            controller.runCycle();

            Assertions.assertEquals(1, controller.getCompletedPasses());
            Assertions.assertEquals(0, controller.getTotalCleaned());
            Assertions.assertNull(controller.loadPersistedCursor());
            controller.close();
        }
    }

    private static RocksDBOrphanLockCleanupController controller(
            RocksDBStoreEngine engine,
            RocksDBLockManager lockManager,
            int batchLimit,
            int maxBatches,
            long roundSleepMillis,
            RocksDBOrphanLockCleanupController.Sleeper sleeper) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        return new RocksDBOrphanLockCleanupController(
                lockManager,
                engine,
                50L,
                batchLimit,
                maxBatches,
                roundSleepMillis,
                executor,
                true,
                sleeper,
                System::nanoTime);
    }

    private static final class FirstAwaitTimeoutExecutor extends ScheduledThreadPoolExecutor {
        private final AtomicInteger awaitCalls = new AtomicInteger();
        private final CompletableFuture<Void> secondAwaitEntered = new CompletableFuture<>();

        private FirstAwaitTimeoutExecutor() {
            super(1);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (awaitCalls.incrementAndGet() == 1) {
                return false;
            }
            secondAwaitEntered.complete(null);
            return super.awaitTermination(timeout, unit);
        }
    }

    private static final class NeverTerminatingExecutor extends ScheduledThreadPoolExecutor {
        private final CountDownLatch blockerStarted = new CountDownLatch(1);
        private final CountDownLatch releaseBlocker = new CountDownLatch(1);

        private NeverTerminatingExecutor() {
            super(1);
        }

        private void startBlocker() throws InterruptedException {
            execute(() -> {
                blockerStarted.countDown();
                while (true) {
                    try {
                        releaseBlocker.await();
                        return;
                    } catch (InterruptedException ignored) {
                        // Stay active until the test releases the worker explicitly.
                    }
                }
            });
            Assertions.assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
        }

        private void releaseBlocker() {
            releaseBlocker.countDown();
        }

        private boolean awaitActualTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return super.awaitTermination(timeout, unit);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }
    }

    private static final class InterruptingAwaitExecutor extends ScheduledThreadPoolExecutor {
        private final AtomicInteger awaitCalls = new AtomicInteger();

        private InterruptingAwaitExecutor() {
            super(1);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (awaitCalls.incrementAndGet() == 1) {
                throw new InterruptedException("test close interruption");
            }
            return super.awaitTermination(timeout, unit);
        }
    }

    private static void acquireOrphans(RocksDBLockManager lockManager, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            Assertions.assertTrue(
                    lockManager.acquireLock(branchSession(1000L + i, i + 1L, "t_order:" + i)),
                    "orphan lock prepare failed");
        }
    }

    private static int lockIndexSize(RocksDBStoreEngine engine) {
        return engine.prefixScan(RocksDBColumnFamily.LOCK_BRANCH_INDEX, new byte[0])
                .size();
    }

    private static void awaitPasses(RocksDBOrphanLockCleanupController controller, long expected, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (controller.getCompletedPasses() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20L);
        }
    }

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true, false));
    }

    private static BranchSession branchSession(long transactionId, long branchId, String lockKey) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid("127.0.0.1:8091:" + transactionId);
        branchSession.setTransactionId(transactionId);
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey(lockKey);
        return branchSession;
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
