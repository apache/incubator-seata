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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void flushesImmediatelyWhenBatchSizeIsReachedWithoutWaitingForFlushInterval() throws Exception {
        int batchSize = 100;
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(batchSize)
                .cleanerFlushIntervalMs(5000L)
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

        long startNanos = System.nanoTime();
        try {
            // Enqueue all 100 sessions before the single-writer wakes up, or fast enough to trigger batch-size flush
            for (int i = 0; i < batchSize; i++) {
                String xid = "xid-batch-" + i;
                assertTrue(registry.tryAcquire(xid));
                assertTrue(cleaner.offer(mockSession(xid)));
            }

            assertTrue(allDeleted.await(2, TimeUnit.SECONDS));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            assertTrue(
                    elapsedMs < 2000L,
                    "Expected batch-size trigger well before 5000ms flush interval, took " + elapsedMs + "ms");
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
                assertTrue(registry.tryAcquire(xid));
                assertTrue(cleaner.offer(mockSession(xid)));
            }

            assertTrue(flushed.await(3, TimeUnit.SECONDS));
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
            String[] firstWave = {"ok-1", "poison", "ok-2", "ok-3"};
            for (String xid : firstWave) {
                assertTrue(registry.tryAcquire(xid));
                assertTrue(cleaner.offer(mockSession(xid)));
            }

            // Wait until first wave leases are all released (including the failed poison session)
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (registry.size() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(10L);
            }
            assertEquals(0, registry.size());

            // Submit a second healthy batch to verify the cleaner thread survived
            assertTrue(registry.tryAcquire("after-1"));
            assertTrue(registry.tryAcquire("after-2"));
            assertTrue(cleaner.offer(mockSession("after-1")));
            assertTrue(cleaner.offer(mockSession("after-2")));
            assertTrue(secondBatchDone.await(5, TimeUnit.SECONDS));
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

        try {
            cleaner.start();
            assertTrue(cleaner.offer(mockSession("xid-0")));
            assertTrue(firstInFlight.await(3, TimeUnit.SECONDS));

            // Fill bounded capacity of 2
            assertTrue(cleaner.offer(mockSession("xid-1")));
            assertTrue(cleaner.offer(mockSession("xid-2")));

            // Overflow must return false immediately without blocking
            long startNanos = System.nanoTime();
            assertFalse(cleaner.offer(mockSession("xid-overflow")));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            assertTrue(elapsedMs < 500L);
            assertEquals(1L, metrics.getCleanerDroppedCount());
            assertEquals(2, metrics.getCurrentCleanerBufferDepth());
        } finally {
            releaseDelete.countDown();
            assertTrue(cleaner.shutdown(3000L));
        }
    }

    @Test
    void gracefulShutdownFlushesRemainingBufferedSessions() throws Exception {
        PipelineConfig config = PipelineConfig.builder()
                .cleanerBatchSize(100)
                .cleanerFlushIntervalMs(10_000L)
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
            assertTrue(registry.tryAcquire(xid));
            assertTrue(cleaner.offer(mockSession(xid)));
        }

        // Shutdown immediately without waiting for the 10-second flush interval
        long startNanos = System.nanoTime();
        assertTrue(cleaner.shutdown(3000L));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        assertTrue(elapsedMs < 2000L, "Graceful shutdown took too long: " + elapsedMs + "ms");

        assertEquals(5, flushedXids.size());
        assertEquals(0, registry.size());
        assertFalse(cleaner.isRunning());
        assertFalse(cleaner.offer(mockSession("after-shutdown")));
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
