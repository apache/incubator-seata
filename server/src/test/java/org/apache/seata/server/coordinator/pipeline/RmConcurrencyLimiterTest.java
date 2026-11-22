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
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RmConcurrencyLimiterTest {

    @BeforeAll
    static void initEnvironment() {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
    }

    @Test
    void extractsDistinctSortedNonBlankResourceIds() {
        BranchSession b1 = mock(BranchSession.class);
        when(b1.getResourceId()).thenReturn("rm-c");
        BranchSession b2 = mock(BranchSession.class);
        when(b2.getResourceId()).thenReturn("rm-a");
        BranchSession b3 = mock(BranchSession.class);
        when(b3.getResourceId()).thenReturn("rm-c");
        BranchSession b4 = mock(BranchSession.class);
        when(b4.getResourceId()).thenReturn("  ");
        BranchSession b5 = mock(BranchSession.class);
        when(b5.getResourceId()).thenReturn(null);

        GlobalSession session = mock(GlobalSession.class);
        when(session.getBranchSessions()).thenReturn(Arrays.asList(b1, b2, b3, b4, b5, null));

        List<String> ids = RmConcurrencyLimiter.extractSortedResourceIds(session);
        assertEquals(Arrays.asList("rm-a", "rm-c"), ids);
        assertTrue(RmConcurrencyLimiter.extractSortedResourceIds(null).isEmpty());
    }

    @Test
    void disabledWhenLimitNonPositive() throws Exception {
        RmConcurrencyLimiter limiter = new RmConcurrencyLimiter(0);
        assertFalse(limiter.isEnabled());
        try (RmConcurrencyLimiter.Permit permit = limiter.acquire(Arrays.asList("rm-1", "rm-2"))) {
            assertEquals(0, limiter.trackedResourceCount());
        }
        Optional<RmConcurrencyLimiter.Permit> nonBlocking = limiter.tryAcquire(Arrays.asList("rm-1", "rm-2"));
        assertTrue(nonBlocking.isPresent());
        nonBlocking.get().close();
        assertEquals(0, limiter.trackedResourceCount());
    }

    @Test
    void nonBlockingTryAcquireRollsBackPartialPermitsAndEvictsIdleSlots() {
        RmConcurrencyLimiter limiter = new RmConcurrencyLimiter(1);

        // Hold rm-b
        Optional<RmConcurrencyLimiter.Permit> rmBHolder = limiter.tryAcquire(Collections.singletonList("rm-b"));
        assertTrue(rmBHolder.isPresent());
        assertEquals(1, limiter.trackedResourceCount());

        // Attempting [rm-b, rm-a] acquires rm-a first (sorted), fails on rm-b, and rolls back rm-a immediately
        assertFalse(limiter.tryAcquire(Arrays.asList("rm-b", "rm-a")).isPresent());
        assertEquals(1, limiter.trackedResourceCount());

        // Prove rm-a was rolled back and is immediately acquirable
        Optional<RmConcurrencyLimiter.Permit> rmAHolder = limiter.tryAcquire(Collections.singletonList("rm-a"));
        assertTrue(rmAHolder.isPresent());
        assertEquals(2, limiter.trackedResourceCount());

        rmAHolder.get().close();
        rmBHolder.get().close();
        // Idle entries are automatically evicted once all permits are released
        assertEquals(0, limiter.trackedResourceCount());
    }

    @Test
    void blockingAcquireDoesNotDeadlockUnderReversedMultiRmOrdering() throws Exception {
        RmConcurrencyLimiter limiter = new RmConcurrencyLimiter(1);
        int totalTasks = 40;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>(totalTasks);
            for (int i = 0; i < totalTasks; i++) {
                final boolean forward = (i % 2 == 0);
                futures.add(pool.submit(() -> {
                    List<String> resources =
                            forward ? Arrays.asList("rm-a", "rm-b", "rm-c") : Arrays.asList("rm-c", "rm-b", "rm-a");
                    try (RmConcurrencyLimiter.Permit ignored = limiter.acquire(resources)) {
                        Thread.yield();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(0, limiter.trackedResourceCount());
    }

    @Test
    void releasesPartiallyAcquiredPermitsWhenInterrupted() throws Exception {
        RmConcurrencyLimiter limiter = new RmConcurrencyLimiter(1);

        RmConcurrencyLimiter.Permit rmBHolder = limiter.acquire(Collections.singletonList("rm-b"));
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);
        Thread waiter = new Thread(() -> {
            started.set(true);
            try {
                limiter.acquire(Arrays.asList("rm-b", "rm-a")).close();
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        });
        try {
            waiter.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while ((!started.get() || limiter.trackedResourceCount() < 2) && System.nanoTime() < deadline) {
                Thread.yield();
            }
            waiter.interrupt();
            assertTrue(interrupted.await(3, TimeUnit.SECONDS));
            waiter.join(2000L);

            // rm-a must have been released on rollback, so acquiring rm-a succeeds immediately
            try (RmConcurrencyLimiter.Permit rmAPermit = limiter.acquire(Collections.singletonList("rm-a"))) {
                rmAPermit.close();
            }
        } finally {
            rmBHolder.close();
        }
        assertEquals(0, limiter.trackedResourceCount());
    }
}
