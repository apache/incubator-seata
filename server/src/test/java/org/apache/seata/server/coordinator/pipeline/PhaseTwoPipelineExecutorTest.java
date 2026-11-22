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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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

    @Test
    void endToEndHappyPathExecutesRpcBatchesCleanupAndReleasesLeases() throws Exception {
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
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard dbGuard = new BackgroundDbResourceGuard(4, metrics);

        Set<String> committed = ConcurrentHashMap.newKeySet();
        Set<String> deleted = ConcurrentHashMap.newKeySet();
        CountDownLatch allDeleted = new CountDownLatch(totalSessions);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                batch -> {
                    for (GlobalSession s : batch) {
                        deleted.add(s.getXid());
                        allDeleted.countDown();
                    }
                },
                dbGuard,
                registry,
                metrics);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    committed.add(session.getXid());
                    return true;
                },
                cleaner,
                registry,
                metrics);

        try {
            for (int i = 0; i < totalSessions; i++) {
                assertTrue(executor.offer(mockSession("xid-e2e-" + i, "rm-" + (i % 3))));
            }
            assertTrue(allDeleted.await(5, TimeUnit.SECONDS));

            // Wait for lease release right after deleteBatch returns
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (registry.size() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(5L);
            }
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(totalSessions, committed.size());
        assertEquals(totalSessions, deleted.size());
        assertEquals(0, registry.size());
        assertEquals(totalSessions, metrics.getEnqueuedCount());
        assertEquals(totalSessions, metrics.getRpcSuccessCount());
        assertEquals(0L, metrics.getRpcFailureCount());
        assertEquals(totalSessions, metrics.getCleanerDeletedSessionsCount());
        assertEquals(0L, metrics.getDroppedCount());
    }

    @Test
    void nonBlockingDropOnFullQueueReleasesLeaseAndIncrementsDropMetric() throws Exception {
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

        try {
            assertTrue(executor.offer(mockSession("xid-active", "rm-1")));
            assertTrue(firstStarted.await(3, TimeUnit.SECONDS));

            // Fill bounded queue (capacity 2)
            assertTrue(executor.offer(mockSession("xid-q1", "rm-1")));
            assertTrue(executor.offer(mockSession("xid-q2", "rm-1")));

            // Next offer must drop immediately without blocking and release its in-flight lease
            long startNanos = System.nanoTime();
            assertFalse(executor.offer(mockSession("xid-dropped", "rm-1")));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            assertTrue(elapsedMs < 500L);

            assertFalse(registry.isInFlight("xid-dropped"));
            assertEquals(1L, metrics.getDroppedCount());
            assertEquals(3L, metrics.getEnqueuedCount());
            assertEquals(2, metrics.getCurrentQueueDepth());
        } finally {
            releaseWorker.countDown();
            assertTrue(executor.shutdown(5000L));
        }
    }

    @Test
    void inFlightDedupRejectsDuplicateAndAcceptsAfterCompletionOrExpiry() throws Exception {
        AtomicLong clock = new AtomicLong(1000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L, clock::get);
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(16)
                .rpcPoolSize(2)
                .inFlightLeaseMs(5000L)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger(0);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    int call = executions.incrementAndGet();
                    if (call == 1) {
                        entered.countDown();
                        try {
                            allowFinish.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else if (call == 2) {
                        secondDone.countDown();
                    }
                    return false;
                },
                null,
                registry,
                metrics);

        try {
            assertTrue(executor.offer(mockSession("xid-dup", "rm-1")));
            assertTrue(entered.await(3, TimeUnit.SECONDS));

            // Duplicate offer while in-flight is rejected
            assertFalse(executor.offer(mockSession("xid-dup", "rm-1")));
            assertEquals(1L, metrics.getDedupSkippedCount());

            // Advance clock past lease expiry -> allowed to re-enqueue even while first is finishing
            clock.addAndGet(5000L);
            assertTrue(executor.offer(mockSession("xid-dup", "rm-1")));
            assertTrue(secondDone.await(3, TimeUnit.SECONDS));

            allowFinish.countDown();
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(2, executions.get());
        assertEquals(0, registry.size());
    }

    @Test
    void perRmConcurrencyLimitIsolatesSlowRmWithoutStarvingFastRm() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(32)
                .rpcPoolSize(4)
                .maxConcurrentPerRm(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();

        AtomicInteger activeSlowRm = new AtomicInteger(0);
        AtomicInteger maxSlowRm = new AtomicInteger(0);
        CountDownLatch firstSlowEntered = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        CountDownLatch allFastDone = new CountDownLatch(3);
        CountDownLatch allSlowDone = new CountDownLatch(2);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    String rm = session.getBranchSessions().get(0).getResourceId();
                    if ("rm-slow".equals(rm)) {
                        int active = activeSlowRm.incrementAndGet();
                        maxSlowRm.accumulateAndGet(active, Math::max);
                        try {
                            firstSlowEntered.countDown();
                            assertTrue(releaseSlow.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            activeSlowRm.decrementAndGet();
                            allSlowDone.countDown();
                        }
                    } else {
                        allFastDone.countDown();
                    }
                    return false;
                },
                null,
                null,
                metrics);

        try {
            // Submit 2 tasks for rm-slow (limit=1)
            assertTrue(executor.offer(mockSession("xid-slow-1", "rm-slow")));
            assertTrue(firstSlowEntered.await(3, TimeUnit.SECONDS));
            assertTrue(executor.offer(mockSession("xid-slow-2", "rm-slow")));

            // Submit 3 tasks for rm-fast: must complete concurrently while rm-slow is blocked
            assertTrue(executor.offer(mockSession("xid-fast-1", "rm-fast")));
            assertTrue(executor.offer(mockSession("xid-fast-2", "rm-fast")));
            assertTrue(executor.offer(mockSession("xid-fast-3", "rm-fast")));

            assertTrue(allFastDone.await(3, TimeUnit.SECONDS), "Fast RM was starved by slow RM");
            assertEquals(1, activeSlowRm.get(), "Second slow RM task must not bypass per-RM cap");

            releaseSlow.countDown();
            assertTrue(allSlowDone.await(3, TimeUnit.SECONDS));
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(1, maxSlowRm.get());
        assertEquals(5L, metrics.getRpcSuccessCount());
    }

    @Test
    void multiRmSortedPermitAcquisitionDoesNotDeadlockUnderReversedOrdering() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(128)
                .rpcPoolSize(8)
                .maxConcurrentPerRm(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        int total = 40;
        CountDownLatch allDone = new CountDownLatch(total);

        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                session -> {
                    // Brief work while holding all multi-RM permits
                    Thread.yield();
                    allDone.countDown();
                    return false;
                },
                null,
                null,
                metrics);

        try {
            for (int i = 0; i < total; i++) {
                if (i % 2 == 0) {
                    assertTrue(executor.offer(mockSession("xid-fwd-" + i, "rm-a", "rm-b", "rm-c")));
                } else {
                    assertTrue(executor.offer(mockSession("xid-rev-" + i, "rm-c", "rm-b", "rm-a")));
                }
            }
            assertTrue(allDone.await(5, TimeUnit.SECONDS), "Multi-RM sorted permit acquisition deadlocked");
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(total, metrics.getRpcSuccessCount());
        assertEquals(0L, metrics.getRpcFailureCount());
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
            assertTrue(executor.offer(mockSession("xid-err", "rm-only")));
            assertTrue(firstFailed.await(3, TimeUnit.SECONDS));

            // Wait for lease release after exception
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (registry.isInFlight("xid-err") && System.nanoTime() < deadline) {
                Thread.sleep(5L);
            }
            assertFalse(registry.isInFlight("xid-err"));

            // Re-submit same XID & RM: proves both in-flight lease and per-RM permit were released
            assertTrue(executor.offer(mockSession("xid-err", "rm-only")));
            assertTrue(retrySucceeded.await(3, TimeUnit.SECONDS));
        } finally {
            assertTrue(executor.shutdown(5000L));
        }

        assertEquals(1L, metrics.getRpcFailureCount());
        assertEquals(1L, metrics.getRpcSuccessCount());
    }

    @Test
    void setsMdcAndClusterContextDuringExecutionAndGracefullyDrainsOnShutdown() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(32)
                .rpcPoolSize(2)
                .cleanerBatchSize(100)
                .cleanerFlushIntervalMs(10_000L)
                .cleanerBufferCapacity(32)
                .dbMaxConcurrency(2)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard dbGuard = new BackgroundDbResourceGuard(2, metrics);

        AtomicReference<String> observedMdcXid = new AtomicReference<>();
        AtomicReference<String> observedGroup = new AtomicReference<>();
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
                    return true;
                },
                cleaner,
                registry,
                metrics);

        for (int i = 0; i < 6; i++) {
            assertTrue(executor.offer(mockSession("xid-drain-" + i, "rm-1")));
        }

        // Shutdown drains workers and then flushes cleaner despite 10s flush interval
        assertTrue(executor.shutdown(5000L));
        assertFalse(executor.isRunning());
        assertFalse(cleaner.isRunning());
        assertFalse(executor.offer(mockSession("xid-post-shutdown", "rm-1")));

        assertNotNull(observedMdcXid.get());
        assertTrue(observedMdcXid.get().startsWith("xid-drain-"));
        assertNotNull(observedGroup.get());
        assertEquals(6, flushed.size());
        assertEquals(0, registry.size());
    }
}
