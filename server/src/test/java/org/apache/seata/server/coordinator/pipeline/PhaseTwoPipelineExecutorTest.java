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
package org.apache.seata.server.coordinator.pipeline;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.coordinator.Core;
import org.apache.seata.server.coordinator.DefaultCore;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseTwoPipelineExecutorTest {

    @BeforeAll
    static void initEnvironment() {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
    }

    private static GlobalSession mockSession(String xid, String... resourceIds) {
        GlobalSession session = mock(GlobalSession.class);
        when(session.getXid()).thenReturn(xid);
        List<BranchSession> branches = new ArrayList<>(resourceIds.length);
        for (String resourceId : resourceIds) {
            BranchSession branch = mock(BranchSession.class);
            when(branch.getResourceId()).thenReturn(resourceId);
            branches.add(branch);
        }
        when(session.getBranchSessions()).thenReturn(branches);
        return session;
    }

    private static void awaitCondition(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5L);
        }
        assertTrue(condition.getAsBoolean(), "Condition not satisfied within " + timeoutMs + "ms");
    }

    @Test
    void enforcesSharedInFlightRegistryWithCleaner() {
        PipelineConfig config = PipelineConfig.builder().rpcPoolSize(2).build();
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard dbGuard = new BackgroundDbResourceGuard(2, metrics);

        // Cleaner with null registry must be rejected
        BatchGlobalSessionCleaner cleanerWithoutRegistry =
                new BatchGlobalSessionCleaner(config, batch -> {}, dbGuard, null, metrics);
        assertThrows(
                IllegalArgumentException.class,
                () -> new PhaseTwoPipelineExecutor(config, s -> false, cleanerWithoutRegistry, null, metrics));

        // Cleaner with a different registry instance must be rejected
        InFlightSessionRegistry regA = new InFlightSessionRegistry(5000L);
        InFlightSessionRegistry regB = new InFlightSessionRegistry(5000L);
        BatchGlobalSessionCleaner cleanerWithRegA =
                new BatchGlobalSessionCleaner(config, batch -> {}, dbGuard, regA, metrics);
        assertThrows(
                IllegalArgumentException.class,
                () -> new PhaseTwoPipelineExecutor(config, s -> false, cleanerWithRegA, regB, metrics));

        // Passing null registry reuses cleaner's registry
        PhaseTwoPipelineExecutor executor =
                new PhaseTwoPipelineExecutor(config, s -> false, cleanerWithRegA, null, metrics);
        assertSame(regA, executor.getInFlightRegistry());
        assertSame(dbGuard, executor.getDbResourceGuard());
        assertTrue(executor.shutdown(1000L));
    }

    @Test
    void requiresExplicitStartAndReturnsExplicitOfferResults() {
        PipelineConfig config =
                PipelineConfig.builder().queueCapacity(4).rpcPoolSize(1).build();
        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(config, s -> false, null, null, null);

        assertEquals(OfferResult.INVALID, executor.tryOffer(null));
        assertEquals(OfferResult.INVALID, executor.tryOffer(mockSession("")));
        assertEquals(OfferResult.NOT_RUNNING, executor.tryOffer(mockSession("xid-1", "rm-1")));
        assertFalse(executor.offer(mockSession("xid-1", "rm-1")));
        assertEquals(0, executor.getInFlightRegistry().size());
        assertTrue(executor.shutdown(1000L));
    }

    @Test
    void endToEndHappyPathViaPipelineComponentsExecutesRpcBatchesCleanupAndReleasesLeases() throws Exception {
        int totalSessions = 20;
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(64)
                .rpcPoolSize(4)
                .maxConcurrentPerRm(4)
                .cleanerBatchSize(10)
                .cleanerFlushIntervalMs(30L)
                .cleanerBufferCapacity(64)
                .dbMaxConcurrency(4)
                .build();

        Set<String> committed = ConcurrentHashMap.newKeySet();
        Set<String> deleted = ConcurrentHashMap.newKeySet();
        CountDownLatch allDeleted = new CountDownLatch(totalSessions);

        PipelineComponents components = PipelineComponents.create(
                config,
                session -> {
                    committed.add(session.getXid());
                    return true;
                },
                batch -> {
                    for (GlobalSession s : batch) {
                        deleted.add(s.getXid());
                        allDeleted.countDown();
                    }
                },
                new PipelineMetrics());

        assertSame(components.getInFlightRegistry(), components.getCleaner().getInFlightRegistry());
        assertSame(components.getInFlightRegistry(), components.getExecutor().getInFlightRegistry());
        assertSame(components.getDbResourceGuard(), components.getExecutor().getDbResourceGuard());

        try {
            components.start();
            for (int i = 0; i < totalSessions; i++) {
                assertEquals(
                        OfferResult.ACCEPTED,
                        components.getExecutor().tryOffer(mockSession("xid-e2e-" + i, "rm-" + (i % 3))));
            }
            assertTrue(allDeleted.await(5, TimeUnit.SECONDS));
            awaitCondition(() -> components.getInFlightRegistry().size() == 0, 3000L);
        } finally {
            assertTrue(components.shutdown(5000L));
        }

        PipelineMetrics metrics = components.getMetrics();
        assertEquals(totalSessions, committed.size());
        assertEquals(totalSessions, deleted.size());
        assertEquals(0, components.getInFlightRegistry().size());
        assertEquals(totalSessions, metrics.getEnqueuedCount());
        assertEquals(totalSessions, metrics.getRpcCompletedCount());
        assertEquals(0L, metrics.getRpcRetryNeededCount());
        assertEquals(0L, metrics.getRpcFailureCount());
        assertEquals(totalSessions, metrics.getCleanerDeletedSessionsCount());
        assertEquals(0L, metrics.getDroppedCount());
    }

    @Test
    void nonBlockingDropOnFullQueueReturnsQueueFullReleasesLeaseAndIncrementsMetric() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(2)
                .rpcPoolSize(1)
                .maxConcurrentPerRm(0)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    firstStarted.countDown();
                    try {
                        releaseWorker.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                },
                null,
                registry,
                metrics);

        ExecutorService callerPool = Executors.newSingleThreadExecutor();
        try {
            executor.start();
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-active", "rm-1")));
            assertTrue(firstStarted.await(3, TimeUnit.SECONDS));

            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-q1", "rm-1")));
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-q2", "rm-1")));
            assertEquals(0, executor.remainingCapacity());

            Future<OfferResult> overflow =
                    callerPool.submit(() -> executor.tryOffer(mockSession("xid-dropped", "rm-1")));
            assertEquals(OfferResult.QUEUE_FULL, overflow.get(2, TimeUnit.SECONDS));

            assertFalse(registry.isInFlight("xid-dropped"));
            assertEquals(1L, metrics.getDroppedCount());
            assertEquals(3L, metrics.getEnqueuedCount());
            assertEquals(2, metrics.getCurrentQueueDepth());
        } finally {
            callerPool.shutdownNow();
            releaseWorker.countDown();
            assertTrue(executor.shutdown(5000L));
        }
    }

    @Test
    void queuedSessionWaitingLongerThanLeaseTtlIsNotDuplicatedAndRenewsOnDequeue() throws Exception {
        AtomicLong clock = new AtomicLong(1_000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(500L, clock::get);
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(8)
                .rpcPoolSize(1)
                .maxConcurrentPerRm(0)
                .inFlightLeaseMs(500L)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();

        CountDownLatch blockerEntered = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        CountDownLatch queuedProcessed = new CountDownLatch(1);
        AtomicInteger queuedExecutions = new AtomicInteger(0);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    if ("xid-blocker".equals(session.getXid())) {
                        blockerEntered.countDown();
                        try {
                            releaseBlocker.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else if ("xid-waiting".equals(session.getXid())) {
                        queuedExecutions.incrementAndGet();
                        // Verify lease was renewed upon dequeue (not expired even after 10x clock jump)
                        assertTrue(registry.isInFlight("xid-waiting"));
                        queuedProcessed.countDown();
                    }
                    return false;
                },
                null,
                registry,
                metrics);

        try {
            executor.start();
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-blocker", "rm-1")));
            assertTrue(blockerEntered.await(3, TimeUnit.SECONDS));

            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-waiting", "rm-1")));

            // Advance clock 10x past inFlightLeaseMs while xid-waiting sits in the queue
            clock.addAndGet(5_000L);

            // Must still be deduplicated and NOT duplicated into the queue
            assertEquals(OfferResult.DUPLICATE, executor.tryOffer(mockSession("xid-waiting", "rm-1")));
            assertEquals(1L, metrics.getDedupSkippedCount());

            releaseBlocker.countDown();
            assertTrue(queuedProcessed.await(3, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(1, queuedExecutions.get());
        assertEquals(0, registry.size());
    }

    @Test
    void expiredWorkerReleaseDoesNotEvictSubsequentOwnerLease() throws Exception {
        AtomicLong clock = new AtomicLong(1_000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(500L, clock::get);
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(16)
                .rpcPoolSize(2)
                .maxConcurrentPerRm(0)
                .inFlightLeaseMs(500L)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();

        CountDownLatch workerAEntered = new CountDownLatch(1);
        CountDownLatch releaseWorkerA = new CountDownLatch(1);
        CountDownLatch workerBEntered = new CountDownLatch(1);
        CountDownLatch releaseWorkerB = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger(0);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    int call = calls.incrementAndGet();
                    if (call == 1) {
                        workerAEntered.countDown();
                        try {
                            releaseWorkerA.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else if (call == 2) {
                        workerBEntered.countDown();
                        try {
                            releaseWorkerB.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return false;
                },
                null,
                registry,
                metrics);

        try {
            executor.start();
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-race", "rm-1")));
            assertTrue(workerAEntered.await(3, TimeUnit.SECONDS));

            // Worker A's execution lease expires -> Worker B acquires a fresh token
            clock.addAndGet(500L);
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-race", "rm-1")));
            assertTrue(workerBEntered.await(3, TimeUnit.SECONDS));

            // Worker A finishes and attempts token-checked release -> must NOT evict Worker B's active lease
            releaseWorkerA.countDown();
            awaitCondition(() -> metrics.getRpcRetryNeededCount() >= 1L, 3000L);

            assertTrue(registry.isInFlight("xid-race"));
            assertEquals(OfferResult.DUPLICATE, executor.tryOffer(mockSession("xid-race", "rm-1")));

            // Once Worker B finishes, its lease is released cleanly
            releaseWorkerB.countDown();
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(2, calls.get());
        assertEquals(0, registry.size());
    }

    @Test
    void slowRmExceedingPoolSizeNeverStarvesFastRmBecauseLimitingIsNonBlocking() throws Exception {
        int poolSize = 4;
        int slowTasks = poolSize + 4;
        int fastTasks = 4;
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(64)
                .rpcPoolSize(poolSize)
                .maxConcurrentPerRm(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);

        AtomicInteger activeSlowRm = new AtomicInteger(0);
        AtomicInteger maxSlowRm = new AtomicInteger(0);
        CountDownLatch slowEntered = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        CountDownLatch allFastDone = new CountDownLatch(fastTasks);
        CountDownLatch slowDone = new CountDownLatch(1);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    String rm = session.getBranchSessions().get(0).getResourceId();
                    if ("rm-slow".equals(rm)) {
                        int active = activeSlowRm.incrementAndGet();
                        maxSlowRm.accumulateAndGet(active, Math::max);
                        try {
                            slowEntered.countDown();
                            assertTrue(releaseSlow.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            activeSlowRm.decrementAndGet();
                            slowDone.countDown();
                        }
                    } else {
                        allFastDone.countDown();
                    }
                    return false;
                },
                null,
                registry,
                metrics);

        try {
            executor.start();
            // Submit first slow task and wait until it holds the sole rm-slow permit
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-slow-0", "rm-slow")));
            assertTrue(slowEntered.await(3, TimeUnit.SECONDS));

            // Submit >= rpcPoolSize additional slow RM tasks followed by fast RM tasks
            for (int i = 1; i < slowTasks; i++) {
                assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-slow-" + i, "rm-slow")));
            }
            for (int i = 0; i < fastTasks; i++) {
                assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-fast-" + i, "rm-fast-" + i)));
            }

            // Fast RM tasks MUST all complete promptly even though slow RM tasks outnumber rpcPoolSize
            assertTrue(allFastDone.await(3, TimeUnit.SECONDS), "Fast RM was starved by slow RM tasks");
            assertEquals(1, activeSlowRm.get());
            assertTrue(metrics.getRmThrottledCount() >= slowTasks - 1L);

            releaseSlow.countDown();
            assertTrue(slowDone.await(3, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(1, maxSlowRm.get());
        assertEquals(0, registry.size());
    }

    @Test
    void workerSurvivesStrayInterruptFlagSetByCommitHandler() throws Exception {
        int poolSize = 2;
        int totalTasks = 10;
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(32)
                .rpcPoolSize(poolSize)
                .maxConcurrentPerRm(0)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        CountDownLatch allDone = new CountDownLatch(totalTasks);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    // Simulate downstream RPC/JDBC code leaving the thread interrupted
                    Thread.currentThread().interrupt();
                    allDone.countDown();
                    return false;
                },
                null,
                null,
                metrics);

        try {
            executor.start();
            for (int i = 0; i < totalTasks; i++) {
                assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-int-" + i, "rm-1")));
            }
            assertTrue(allDone.await(5, TimeUnit.SECONDS), "Workers exited after handler left interrupt flag set");
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(totalTasks, metrics.getRpcRetryNeededCount());
    }

    @Test
    void exceptionInCommitHandlerReleasesPermitsAndLeaseCleanly() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(16)
                .rpcPoolSize(2)
                .maxConcurrentPerRm(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);

        AtomicInteger attempts = new AtomicInteger(0);
        CountDownLatch retrySucceeded = new CountDownLatch(1);
        CountDownLatch firstFailed = new CountDownLatch(1);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    int attempt = attempts.incrementAndGet();
                    if (attempt == 1) {
                        firstFailed.countDown();
                        throw new TransactionException("simulated RM RPC error");
                    }
                    retrySucceeded.countDown();
                    return false;
                },
                null,
                registry,
                metrics);

        try {
            executor.start();
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-err", "rm-only")));
            assertTrue(firstFailed.await(3, TimeUnit.SECONDS));

            awaitCondition(() -> !registry.isInFlight("xid-err"), 3000L);

            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-err", "rm-only")));
            assertTrue(retrySucceeded.await(3, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(1L, metrics.getRpcFailureCount());
        assertEquals(1L, metrics.getRpcRetryNeededCount());
        assertEquals(0, executor.getRmConcurrencyLimiter().trackedResourceCount());
    }

    @Test
    void shutdownReleasesQueuedTasksImmediatelyAndFlushesInFlightCompletionsInCleaner() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(32)
                .rpcPoolSize(1)
                .cleanerBatchSize(100)
                .cleanerFlushIntervalMs(30_000L)
                .cleanerBufferCapacity(32)
                .dbMaxConcurrency(2)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard dbGuard = new BackgroundDbResourceGuard(2, metrics);

        AtomicReference<String> observedMdcXid = new AtomicReference<>();
        AtomicReference<String> observedGroup = new AtomicReference<>();
        CountDownLatch inFlightStarted = new CountDownLatch(1);
        CountDownLatch releaseInFlight = new CountDownLatch(1);
        Set<String> flushed = ConcurrentHashMap.newKeySet();

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                batch -> {
                    for (GlobalSession s : batch) {
                        flushed.add(s.getXid());
                    }
                },
                dbGuard,
                registry,
                metrics);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    observedMdcXid.compareAndSet(null, MDC.get(RootContext.MDC_KEY_XID));
                    observedGroup.compareAndSet(null, SeataClusterContext.getGroup());
                    inFlightStarted.countDown();
                    try {
                        releaseInFlight.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return true;
                },
                cleaner,
                registry,
                metrics);

        executor.start();
        assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-inflight", "rm-1")));
        assertTrue(inFlightStarted.await(3, TimeUnit.SECONDS));

        // Enqueue 4 additional sessions that will sit queued behind the sole active worker
        for (int i = 0; i < 4; i++) {
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(mockSession("xid-queued-" + i, "rm-1")));
        }

        // Release in-flight worker slightly after shutdown begins
        ExecutorService shutdownPool = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> shutdownResult = shutdownPool.submit(() -> executor.shutdown(5000L));
            // Queued tasks must have their leases released immediately on shutdown without waiting for worker
            awaitCondition(() -> !registry.isInFlight("xid-queued-0"), 3000L);
            releaseInFlight.countDown();
            assertTrue(shutdownResult.get(5, TimeUnit.SECONDS));
        } finally {
            shutdownPool.shutdownNow();
        }

        assertFalse(executor.isRunning());
        assertFalse(cleaner.isRunning());
        assertEquals(OfferResult.NOT_RUNNING, executor.tryOffer(mockSession("xid-post-shutdown", "rm-1")));
        assertEquals("xid-inflight", observedMdcXid.get());
        assertNotNull(observedGroup.get());
        // Only the already-in-flight session executed and flushed via cleaner; queued sessions returned to fallback
        assertEquals(1, flushed.size());
        assertTrue(flushed.contains("xid-inflight"));
        assertEquals(0, registry.size());
    }

    @Test
    void fromCoreAdapterAlwaysDelegatesWithRetryingTrueAndReturnsFalse() throws Exception {
        Core core = mock(Core.class);
        GlobalSession session = mockSession("xid-core", "rm-1");
        when(core.doGlobalCommit(session, true)).thenReturn(true);

        PhaseTwoPipelineExecutor.PhaseTwoCommitHandler handler =
                PhaseTwoPipelineExecutor.PhaseTwoCommitHandler.fromCore(core);
        assertFalse(handler.executePhaseTwo(session));
        verify(core).doGlobalCommit(session, true);
    }

    @Test
    void fromCoreForPipelineAdapterDelegatesToDoGlobalCommitForPipeline() throws Exception {
        DefaultCore core = mock(DefaultCore.class);
        GlobalSession completed = mockSession("xid-done", "rm-1");
        GlobalSession incomplete = mockSession("xid-retry", "rm-1");
        when(core.doGlobalCommitForPipeline(completed)).thenReturn(true);
        when(core.doGlobalCommitForPipeline(incomplete)).thenReturn(false);

        assertThrows(
                NullPointerException.class,
                () -> PhaseTwoPipelineExecutor.PhaseTwoCommitHandler.fromCoreForPipeline(null));

        PhaseTwoPipelineExecutor.PhaseTwoCommitHandler handler =
                PhaseTwoPipelineExecutor.PhaseTwoCommitHandler.fromCoreForPipeline(core);
        assertTrue(handler.executePhaseTwo(completed));
        assertFalse(handler.executePhaseTwo(incomplete));
        verify(core).doGlobalCommitForPipeline(completed);
        verify(core).doGlobalCommitForPipeline(incomplete);
    }
}
