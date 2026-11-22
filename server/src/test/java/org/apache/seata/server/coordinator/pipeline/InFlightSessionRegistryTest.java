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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InFlightSessionRegistryTest {

    @Test
    void validatesConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new InFlightSessionRegistry(0L));
        assertThrows(IllegalArgumentException.class, () -> new InFlightSessionRegistry(-10L));
        assertThrows(NullPointerException.class, () -> new InFlightSessionRegistry(1000L, null));
    }

    @Test
    void acquireRejectReleaseAndReacquireWorkDeterministically() {
        AtomicLong clock = new AtomicLong(1000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L, clock::get);

        assertFalse(registry.tryAcquire(null));
        assertFalse(registry.tryAcquire(""));
        assertFalse(registry.isInFlight(null));
        assertFalse(registry.isInFlight("xid-1"));

        assertTrue(registry.tryAcquire("xid-1"));
        assertTrue(registry.isInFlight("xid-1"));
        assertEquals(1, registry.size());

        // Duplicate acquire rejected while unexpired
        assertFalse(registry.tryAcquire("xid-1"));

        // Release allows immediate re-acquire
        registry.release("xid-1");
        assertFalse(registry.isInFlight("xid-1"));
        assertEquals(0, registry.size());
        assertTrue(registry.tryAcquire("xid-1"));
    }

    @Test
    void expiredLeaseAllowsReacquireAndLazyCleanup() {
        AtomicLong clock = new AtomicLong(10_000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(1_000L, clock::get);

        assertTrue(registry.tryAcquire("xid-a"));

        // Just before expiration
        clock.set(10_999L);
        assertTrue(registry.isInFlight("xid-a"));
        assertFalse(registry.tryAcquire("xid-a"));

        // Exactly at expiration boundary -> expired
        clock.set(11_000L);
        assertFalse(registry.isInFlight("xid-a"));
        assertEquals(0, registry.size());

        // Re-acquires cleanly with fresh TTL
        assertTrue(registry.tryAcquire("xid-a"));
        clock.set(11_999L);
        assertTrue(registry.isInFlight("xid-a"));

        // Direct tryAcquire after expiry without calling isInFlight first
        clock.set(12_000L);
        assertTrue(registry.tryAcquire("xid-a"));
        assertEquals(1, registry.size());
    }

    @Test
    void evictExpiredRemovesOnlyExpiredEntries() {
        AtomicLong clock = new AtomicLong(0L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(100L, clock::get);

        assertTrue(registry.tryAcquire("xid-1"));
        assertTrue(registry.tryAcquire("xid-2"));

        clock.set(60L);
        assertTrue(registry.tryAcquire("xid-3"));
        assertEquals(3, registry.size());

        clock.set(100L);
        int evicted = registry.evictExpired();
        assertEquals(2, evicted);
        assertEquals(1, registry.size());
        assertFalse(registry.isInFlight("xid-1"));
        assertFalse(registry.isInFlight("xid-2"));
        assertTrue(registry.isInFlight("xid-3"));
    }

    @Test
    void concurrentTryAcquirePermitsExactlyOneWinner() throws Exception {
        AtomicLong clock = new AtomicLong(5000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(30_000L, clock::get);

        int threads = 16;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (registry.tryAcquire("shared-xid")) {
                        winners.incrementAndGet();
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, winners.get());
        assertTrue(registry.isInFlight("shared-xid"));
    }
}
