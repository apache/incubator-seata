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
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchGlobalSessionCleanerTest {

    @BeforeAll
    static void initEnvironment() {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
    }

    private static GlobalSession mockSession(String xid) {
        GlobalSession session = mock(GlobalSession.class);
        when(session.getXid()).thenReturn(xid);
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
    void rejectsOfferBeforeExplicitStart() {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(10)
                .cleanerFlushIntervalMs(50L)
                .cleanerBufferCapacity(16)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, metrics);
        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(config, sessions -> {}, guard, null, metrics);

        assertFalse(cleaner.isRunning());
        assertFalse(cleaner.offer(mockSession("xid-pre-start")));
        assertTrue(cleaner.shutdown(1000L));
    }

    @Test
    void flushesImmediatelyWhenBatchSizeIsReachedWithoutWaitingForFlushInterval() throws Exception {
        int batchSize = 100;
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(batchSize)
                .cleanerFlushIntervalMs(30_000L)
                .cleanerBufferCapacity(256)
                .dbMaxConcurrency(4)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(4, metrics);

        List<Integer> batchSizes = new CopyOnWriteArrayList<>();
        CountDownLatch allDeleted = new CountDownLatch(batchSize);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    batchSizes.add(sessions.size());
                    for (int i = 0; i < sessions.size(); i++) {
                        allDeleted.countDown();
                    }
                },
                guard,
                registry,
                metrics);

        try {
            cleaner.start();
            for (int i = 0; i < batchSize; i++) {
                String xid = "xid-batch-" + i;
                long token = registry.tryAcquire(xid);
                assertTrue(token > 0L);
                assertTrue(cleaner.offer(mockSession(xid), token));
            }

            // Flush interval is 30s; completing within 5s proves batch-size trigger fired without waiting
            assertTrue(allDeleted.await(5, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(cleaner.shutdown(3000L));
        }

        assertEquals(batchSize, metrics.getCleanerDeletedSessionsCount());
        assertEquals(0, registry.size());
        assertFalse(batchSizes.isEmpty());
        assertEquals(batchSize, batchSizes.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void flushesPartialBatchWithinFlushInterval() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(100)
                .cleanerFlushIntervalMs(50L)
                .cleanerBufferCapacity(128)
                .dbMaxConcurrency(4)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(4, metrics);

        List<Integer> batchSizes = new CopyOnWriteArrayList<>();
        CountDownLatch flushed = new CountDownLatch(1);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    batchSizes.add(sessions.size());
                    flushed.countDown();
                },
                guard,
                registry,
                metrics);

        try {
            cleaner.start();
            for (int i = 0; i < 7; i++) {
                String xid = "xid-partial-" + i;
                long token = registry.tryAcquire(xid);
                assertTrue(token > 0L);
                assertTrue(cleaner.offer(mockSession(xid), token));
            }

            assertTrue(flushed.await(3, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(cleaner.shutdown(3000L));
        }

        assertEquals(Collections.singletonList(7), batchSizes);
        assertEquals(1L, metrics.getCleanerBatchesCount());
        assertEquals(7L, metrics.getCleanerDeletedSessionsCount());
        assertEquals(0, registry.size());
    }

    @Test
    void singleWriterGuaranteesNoOverlappingDeleteBatchInvocations() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(5)
                .cleanerFlushIntervalMs(20L)
                .cleanerBufferCapacity(256)
                .dbMaxConcurrency(8)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(8, metrics);

        AtomicInteger concurrentDeleteCalls = new AtomicInteger(0);
        AtomicInteger maxConcurrentDeleteCalls = new AtomicInteger(0);
        int totalSessions = 50;
        CountDownLatch allDeleted = new CountDownLatch(totalSessions);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    int active = concurrentDeleteCalls.incrementAndGet();
                    maxConcurrentDeleteCalls.accumulateAndGet(active, Math::max);
                    try {
                        for (int i = 0; i < sessions.size(); i++) {
                            allDeleted.countDown();
                        }
                    } finally {
                        concurrentDeleteCalls.decrementAndGet();
                    }
                },
                guard,
                null,
                metrics);

        try {
            cleaner.start();
            for (int i = 0; i < totalSessions; i++) {
                assertTrue(cleaner.offer(mockSession("xid-sw-" + i)));
            }
            assertTrue(allDeleted.await(5, TimeUnit.SECONDS));
        } finally {
            assertTrue(cleaner.shutdown(3000L));
        }

        assertEquals(1, maxConcurrentDeleteCalls.get());
        assertEquals(totalSessions, metrics.getCleanerDeletedSessionsCount());
    }

    @Test
    void batchFailureFallsBackPerItemReleasesLeasesAndKeepsCleanerAlive() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(4)
                .cleanerFlushIntervalMs(30L)
                .cleanerBufferCapacity(64)
                .dbMaxConcurrency(2)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, metrics);

        Set<String> deletedXids = ConcurrentHashMap.newKeySet();
        CountDownLatch secondBatchDone = new CountDownLatch(2);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    if (sessions.size() > 1 && sessions.stream().anyMatch(s -> "poison".equals(s.getXid()))) {
                        throw new TransactionException("simulated batch failure");
                    }
                    for (GlobalSession session : sessions) {
                        if ("poison".equals(session.getXid())) {
                            throw new TransactionException("simulated poison item failure");
                        }
                        deletedXids.add(session.getXid());
                        if (session.getXid().startsWith("after-")) {
                            secondBatchDone.countDown();
                        }
                    }
                },
                guard,
                registry,
                metrics);

        try {
            cleaner.start();
            String[] firstWave = {"ok-1", "poison", "ok-2", "ok-3"};
            for (String xid : firstWave) {
                long token = registry.tryAcquire(xid);
                assertTrue(token > 0L);
                assertTrue(cleaner.offer(mockSession(xid), token));
            }

            awaitCondition(() -> registry.size() == 0, 5000L);

            long t1 = registry.tryAcquire("after-1");
            long t2 = registry.tryAcquire("after-2");
            assertTrue(cleaner.offer(mockSession("after-1"), t1));
            assertTrue(cleaner.offer(mockSession("after-2"), t2));
            assertTrue(secondBatchDone.await(5, TimeUnit.SECONDS));
            awaitCondition(() -> registry.size() == 0, 3000L);
        } finally {
            assertTrue(cleaner.shutdown(3000L));
        }

        assertTrue(metrics.getCleanerBatchFailuresCount() >= 1L);
        assertTrue(deletedXids.contains("ok-1"));
        assertTrue(deletedXids.contains("ok-2"));
        assertTrue(deletedXids.contains("ok-3"));
        assertFalse(deletedXids.contains("poison"));
        assertTrue(deletedXids.contains("after-1"));
        assertTrue(deletedXids.contains("after-2"));
        assertEquals(5L, metrics.getCleanerDeletedSessionsCount());
        assertEquals(0, registry.size());
    }

    @Test
    void survivesStrayInterruptFlagSetByDeleteHandler() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(1)
                .cleanerFlushIntervalMs(20L)
                .cleanerBufferCapacity(16)
                .dbMaxConcurrency(2)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, metrics);

        AtomicBoolean firstInterruptInjected = new AtomicBoolean(false);
        CountDownLatch secondProcessed = new CountDownLatch(1);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    if (firstInterruptInjected.compareAndSet(false, true)) {
                        // Simulate JDBC/Netty handler leaving the interrupt bit set
                        Thread.currentThread().interrupt();
                    } else {
                        secondProcessed.countDown();
                    }
                },
                guard,
                null,
                metrics);

        try {
            cleaner.start();
            assertTrue(cleaner.offer(mockSession("xid-int-1")));
            assertTrue(cleaner.offer(mockSession("xid-int-2")));
            assertTrue(secondProcessed.await(5, TimeUnit.SECONDS));
        } finally {
            assertTrue(cleaner.shutdown(3000L));
        }

        assertEquals(2L, metrics.getCleanerDeletedSessionsCount());
    }

    @Test
    void bufferOverflowDropsWithoutBlockingAndIncrementsMetric() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(1)
                .cleanerFlushIntervalMs(50L)
                .cleanerBufferCapacity(2)
                .dbMaxConcurrency(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, metrics);

        CountDownLatch firstInFlight = new CountDownLatch(1);
        CountDownLatch releaseDelete = new CountDownLatch(1);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    firstInFlight.countDown();
                    try {
                        releaseDelete.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                guard,
                null,
                metrics);

        ExecutorService callerPool = Executors.newSingleThreadExecutor();
        try {
            cleaner.start();
            assertTrue(cleaner.offer(mockSession("xid-0")));
            assertTrue(firstInFlight.await(3, TimeUnit.SECONDS));

            assertTrue(cleaner.offer(mockSession("xid-1")));
            assertTrue(cleaner.offer(mockSession("xid-2")));

            // Verify overflow returns false promptly without blocking the caller
            Future<Boolean> overflowOffer = callerPool.submit(() -> cleaner.offer(mockSession("xid-overflow")));
            assertFalse(overflowOffer.get(2, TimeUnit.SECONDS));
            assertEquals(1L, metrics.getCleanerDroppedCount());
            assertEquals(2, metrics.getCurrentCleanerBufferDepth());
        } finally {
            callerPool.shutdownNow();
            releaseDelete.countDown();
            assertTrue(cleaner.shutdown(3000L));
        }
    }

    @Test
    void gracefulShutdownFlushesRemainingBufferedSessionsWithoutWaitingForFlushInterval() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(100)
                .cleanerFlushIntervalMs(30_000L)
                .cleanerBufferCapacity(64)
                .dbMaxConcurrency(2)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, metrics);

        List<String> flushedXids = new CopyOnWriteArrayList<>();

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    for (GlobalSession s : sessions) {
                        flushedXids.add(s.getXid());
                    }
                },
                guard,
                registry,
                metrics);

        cleaner.start();
        for (int i = 0; i < 5; i++) {
            String xid = "xid-shutdown-" + i;
            long token = registry.tryAcquire(xid);
            assertTrue(token > 0L);
            assertTrue(cleaner.offer(mockSession(xid), token));
        }

        // Shutdown finishes promptly despite 30s flush interval
        assertTrue(cleaner.shutdown(3000L));
        assertEquals(5, flushedXids.size());
        assertEquals(0, registry.size());
        assertEquals(0, cleaner.getBufferSize());
        assertFalse(cleaner.isRunning());
        assertFalse(cleaner.offer(mockSession("after-shutdown")));
    }

    @Test
    void shutdownTimeoutStopsIssuingNewDbCallsAndReleasesLeases() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(1)
                .cleanerFlushIntervalMs(50L)
                .cleanerBufferCapacity(16)
                .dbMaxConcurrency(1)
                .build();
        PipelineMetrics metrics = new PipelineMetrics();
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L);
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, metrics);

        CountDownLatch stuckInFirstFlush = new CountDownLatch(1);
        CountDownLatch releaseStuckFlush = new CountDownLatch(1);
        AtomicInteger deleteCalls = new AtomicInteger(0);

        BatchGlobalSessionCleaner cleaner = new BatchGlobalSessionCleaner(
                config,
                sessions -> {
                    deleteCalls.incrementAndGet();
                    stuckInFirstFlush.countDown();
                    try {
                        releaseStuckFlush.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                guard,
                registry,
                metrics);

        try {
            cleaner.start();
            long t1 = registry.tryAcquire("xid-stuck");
            long t2 = registry.tryAcquire("xid-buffered-1");
            long t3 = registry.tryAcquire("xid-buffered-2");
            assertTrue(cleaner.offer(mockSession("xid-stuck"), t1));
            assertTrue(stuckInFirstFlush.await(3, TimeUnit.SECONDS));
            assertTrue(cleaner.offer(mockSession("xid-buffered-1"), t2));
            assertTrue(cleaner.offer(mockSession("xid-buffered-2"), t3));

            assertEquals(2, cleaner.getBufferSize());

            // Timeout expires while first flush is stuck -> returns false and releases remaining buffered leases
            assertFalse(cleaner.shutdown(30L));
            assertFalse(registry.isInFlight("xid-buffered-1"));
            assertFalse(registry.isInFlight("xid-buffered-2"));
        } finally {
            releaseStuckFlush.countDown();
        }
        awaitCondition(() -> registry.size() == 0, 3000L);
        assertEquals(1, deleteCalls.get());
    }

    @Test
    void fromSessionManagerAdapterDeletesEachSession() throws Exception {
        SessionManager sessionManager = mock(SessionManager.class);
        GlobalSession s1 = mockSession("xid-1");
        GlobalSession s2 = mockSession("xid-2");
        List<GlobalSession> batch = new ArrayList<>();
        batch.add(s1);
        batch.add(s2);

        BatchGlobalSessionCleaner.BatchDeleteHandler handler =
                BatchGlobalSessionCleaner.BatchDeleteHandler.fromSessionManager(sessionManager);
        handler.deleteBatch(batch);

        verify(sessionManager).removeGlobalSession(s1);
        verify(sessionManager).removeGlobalSession(s2);
    }
}
