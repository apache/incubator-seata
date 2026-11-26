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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void defaultMonotonicClockConstructorWorks() {
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        long token = registry.tryAcquire("xid-mono");
        assertTrue(token > InFlightSessionRegistry.INVALID_TOKEN);
        assertTrue(registry.isInFlight("xid-mono"));
        assertTrue(registry.release("xid-mono", token));
        assertFalse(registry.isInFlight("xid-mono"));
    }

    @Test
    void acquireRejectReleaseAndReacquireWorkDeterministically() {
        AtomicLong clock = new AtomicLong(1000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L, clock::get);

        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquire(null));
        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquire(""));
        assertFalse(registry.isInFlight(null));
        assertFalse(registry.isInFlight("xid-1"));

        long token1 = registry.tryAcquire("xid-1");
        assertTrue(token1 > InFlightSessionRegistry.INVALID_TOKEN);
        assertTrue(registry.isInFlight("xid-1"));
        assertEquals(1, registry.size());

        // Duplicate acquire rejected while unexpired
        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquire("xid-1"));

        // Release with wrong token is ignored
        assertFalse(registry.release("xid-1", token1 + 99L));
        assertTrue(registry.isInFlight("xid-1"));

        // Release with matching token allows immediate re-acquire with a new token
        assertTrue(registry.release("xid-1", token1));
        assertFalse(registry.isInFlight("xid-1"));
        assertEquals(0, registry.size());

        long token2 = registry.tryAcquire("xid-1");
        assertTrue(token2 > InFlightSessionRegistry.INVALID_TOKEN);
        assertNotEquals(token1, token2);
    }

    @Test
    void expiredOwnerCannotReleaseOrRenewLeaseAcquiredByNewOwner() {
        AtomicLong clock = new AtomicLong(10_000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(1_000L, clock::get);

        long tokenA = registry.tryAcquire("xid-contested");
        assertTrue(tokenA > InFlightSessionRegistry.INVALID_TOKEN);

        // Lease expires and worker B acquires a new token
        clock.set(11_000L);
        long tokenB = registry.tryAcquire("xid-contested");
        assertTrue(tokenB > InFlightSessionRegistry.INVALID_TOKEN);
        assertNotEquals(tokenA, tokenB);

        // Stale worker A attempts release and renew -> both MUST fail and keep B's lease active
        assertFalse(registry.release("xid-contested", tokenA));
        assertFalse(registry.renew("xid-contested", tokenA));
        assertTrue(registry.isInFlight("xid-contested"));
        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquire("xid-contested"));

        // Worker B can renew and release its own lease cleanly
        clock.set(11_500L);
        assertTrue(registry.renew("xid-contested", tokenB));
        clock.set(12_499L);
        assertTrue(registry.isInFlight("xid-contested"));
        assertTrue(registry.release("xid-contested", tokenB));
        assertFalse(registry.isInFlight("xid-contested"));
    }

    @Test
    void queuedLeaseDoesNotExpireUnderBacklogUntilActivatedViaRenew() {
        AtomicLong clock = new AtomicLong(1_000L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(500L, clock::get);

        long token = registry.tryAcquireQueued("xid-queued");
        assertTrue(token > InFlightSessionRegistry.INVALID_TOKEN);

        // Advance clock far beyond leaseMs (10x) while still queued
        clock.set(6_000L);
        assertTrue(registry.isInFlight("xid-queued"));
        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquireQueued("xid-queued"));
        assertEquals(InFlightSessionRegistry.INVALID_TOKEN, registry.tryAcquire("xid-queued"));
        assertEquals(0, registry.evictExpired());

        // Worker dequeues and activates execution TTL via renew
        assertTrue(registry.renew("xid-queued", token));
        clock.set(6_499L);
        assertTrue(registry.isInFlight("xid-queued"));

        // Now expires once execution TTL elapses
        clock.set(6_500L);
        assertFalse(registry.isInFlight("xid-queued"));
        assertEquals(0, registry.size());
    }

    @Test
    void evictExpiredAndThrottledAutoEvictReclaimOnlyExpiredActiveEntries() {
        AtomicLong clock = new AtomicLong(0L);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(100L, clock::get);

        assertTrue(registry.tryAcquire("xid-1") > 0L);
        assertTrue(registry.tryAcquire("xid-2") > 0L);
        assertTrue(registry.tryAcquireQueued("xid-queued") > 0L);

        clock.set(60L);
        assertTrue(registry.tryAcquire("xid-3") > 0L);
        assertEquals(4, registry.size());

        // Advance past xid-1 and xid-2 expiry; throttled eviction on next acquire reclaims them
        clock.set(120L);
        assertTrue(registry.tryAcquire("xid-4") > 0L);
        assertFalse(registry.isInFlight("xid-1"));
        assertFalse(registry.isInFlight("xid-2"));
        assertTrue(registry.isInFlight("xid-queued"));
        assertTrue(registry.isInFlight("xid-3"));
        assertTrue(registry.isInFlight("xid-4"));
        assertEquals(3, registry.size());

        // Explicit evictExpired removes xid-3 once it expires too, leaving queued + xid-4
        clock.set(160L);
        assertEquals(1, registry.evictExpired());
        assertEquals(2, registry.size());
        assertTrue(registry.isInFlight("xid-queued"));
        assertTrue(registry.isInFlight("xid-4"));
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
                    if (registry.tryAcquire("shared-xid") != InFlightSessionRegistry.INVALID_TOKEN) {
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
