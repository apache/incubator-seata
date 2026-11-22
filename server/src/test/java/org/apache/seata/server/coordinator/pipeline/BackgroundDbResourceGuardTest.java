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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class BackgroundDbResourceGuardTest {

    @Test
    void rejectsNonPositiveConcurrency() {
        assertThrows(IllegalArgumentException.class, () -> new BackgroundDbResourceGuard(0, null));
        assertThrows(IllegalArgumentException.class, () -> new BackgroundDbResourceGuard(-2, null));
    }

    @Test
    void capsConcurrentAccessAndRecordsWaitTime() throws Exception {
        int maxConcurrency = 3;
        int totalTasks = 12;
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(maxConcurrency, metrics);

        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxObserved = new AtomicInteger(0);
        CountDownLatch allSaturated = new CountDownLatch(maxConcurrency);
        CountDownLatch releaseFirstWave = new CountDownLatch(1);
        CountDownLatch startGate = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(totalTasks);
        try {
            List<Future<Integer>> futures = new ArrayList<>(totalTasks);
            for (int i = 0; i < totalTasks; i++) {
                final int id = i;
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return guard.execute(() -> {
                        int current = active.incrementAndGet();
                        maxObserved.accumulateAndGet(current, Math::max);
                        try {
                            allSaturated.countDown();
                            assertTrue(releaseFirstWave.await(5, TimeUnit.SECONDS));
                            return id;
                        } finally {
                            active.decrementAndGet();
                        }
                    });
                }));
            }

            startGate.countDown();
            assertTrue(allSaturated.await(5, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (guard.getWaitingCount() == 0 && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(maxConcurrency, guard.getActiveCount());
            assertTrue(guard.getWaitingCount() > 0);

            releaseFirstWave.countDown();

            for (int i = 0; i < totalTasks; i++) {
                assertEquals(i, futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(maxConcurrency, maxObserved.get());
        assertEquals(maxConcurrency, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());
        assertEquals(0, guard.getWaitingCount());
        assertEquals(totalTasks, metrics.getDbGuardWaitCount());
        assertTrue(metrics.getDbGuardWaitTotalNanos() > 0L);
        assertTrue(metrics.getDbGuardWaitMaxNanos() > 0L);
    }

    @Test
    void releasesPermitWhenActionThrows() {
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, metrics);

        assertThrows(
                IllegalStateException.class,
                () -> guard.execute(() -> {
                    assertEquals(0, guard.availablePermits());
                    assertEquals(1, guard.getActiveCount());
                    throw new IllegalStateException("boom");
                }));

        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());
        assertEquals(0, guard.getWaitingCount());
        assertEquals(1L, metrics.getDbGuardWaitCount());

        assertThrows(
                RuntimeException.class,
                () -> guard.run(() -> {
                    throw new RuntimeException("run-boom");
                }));
        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());
    }

    @Test
    void tryAcquireReturnsIdempotentAutoCloseablePermit() throws Exception {
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, metrics);

        Optional<BackgroundDbResourceGuard.Permit> first = guard.tryAcquire(50L);
        assertTrue(first.isPresent());
        assertEquals(0, guard.availablePermits());
        assertEquals(1, guard.getActiveCount());

        assertFalse(guard.tryAcquire(20L).isPresent());
        assertEquals(1, guard.getActiveCount());

        // Closing the same permit multiple times must never inflate permits beyond maxConcurrency
        first.get().close();
        first.get().close();
        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());

        try (BackgroundDbResourceGuard.Permit second =
                guard.tryAcquire(50L, TimeUnit.MILLISECONDS).orElseThrow(AssertionError::new)) {
            assertEquals(0, guard.availablePermits());
            assertEquals(1, guard.getActiveCount());
        }
        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());
        assertEquals(3L, metrics.getDbGuardWaitCount());
    }

    @Test
    void releasesPermitWhenMetricsThrowsDuringAcquire() {
        PipelineMetrics faultyMetrics = spy(new PipelineMetrics());
        doThrow(new RuntimeException("metrics failure")).when(faultyMetrics).recordDbGuardWait(anyLong());
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, faultyMetrics);

        assertThrows(RuntimeException.class, () -> guard.run(() -> {}));
        assertEquals(1, guard.availablePermits());
    }

    @Test
    void interruptedWaitDoesNotLeakPermitOrCounts() throws Exception {
        PipelineMetrics metrics = new PipelineMetrics();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, metrics);
        Optional<BackgroundDbResourceGuard.Permit> held = guard.tryAcquire(50L);
        assertTrue(held.isPresent());

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interruptedObserved = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            entered.countDown();
            try {
                guard.run(() -> {});
            } catch (InterruptedException e) {
                interruptedObserved.countDown();
            }
        });
        try {
            waiter.start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (guard.getWaitingCount() == 0 && System.nanoTime() < deadline) {
                Thread.yield();
            }
            waiter.interrupt();
            assertTrue(interruptedObserved.await(2, TimeUnit.SECONDS));
            waiter.join(2000L);

            assertEquals(1, guard.getActiveCount());
            assertEquals(0, guard.getWaitingCount());
        } finally {
            held.get().close();
        }
        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());
    }
}
