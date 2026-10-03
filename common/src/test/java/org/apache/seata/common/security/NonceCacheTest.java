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
package org.apache.seata.common.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NonceCacheTest {

    @Test
    void first_nonce_admitted_then_replay_rejected() {
        NonceCache cache = new NonceCache.InMemory(60_000L, () -> 1_000L);
        assertTrue(cache.putIfAbsent("tenant-a", "nonce-1"));
        assertFalse(cache.putIfAbsent("tenant-a", "nonce-1"), "second use inside TTL must be rejected");
    }

    @Test
    void same_nonce_across_different_tenants_is_independent() {
        NonceCache cache = new NonceCache.InMemory(60_000L, () -> 1_000L);
        assertTrue(cache.putIfAbsent("tenant-a", "shared-nonce"));
        assertTrue(cache.putIfAbsent("tenant-b", "shared-nonce"), "cluster-id must namespace the nonce");
    }

    @Test
    void expired_nonce_can_be_reused() {
        AtomicLong now = new AtomicLong(1_000L);
        NonceCache cache = new NonceCache.InMemory(500L, now::get);

        assertTrue(cache.putIfAbsent("tenant-a", "nonce"));
        assertFalse(cache.putIfAbsent("tenant-a", "nonce")); // inside TTL

        now.addAndGet(1_000L); // move well past TTL

        assertTrue(cache.putIfAbsent("tenant-a", "nonce"), "after TTL, the same nonce is allowed again");
    }

    @Test
    void evict_expired_clears_stale_entries() {
        AtomicLong now = new AtomicLong(0L);
        NonceCache cache = new NonceCache.InMemory(100L, now::get);

        cache.putIfAbsent("t", "a");
        cache.putIfAbsent("t", "b");
        assertEquals(2, cache.size());

        now.addAndGet(200L);
        cache.evictExpired();
        assertEquals(0, cache.size(), "all entries should have been swept");
    }

    @Test
    void constructor_rejects_zero_or_negative_ttl() {
        assertThrows(IllegalArgumentException.class, () -> new NonceCache.InMemory(0L, () -> 0L));
        assertThrows(IllegalArgumentException.class, () -> new NonceCache.InMemory(-1L, () -> 0L));
    }

    @Test
    void put_rejects_null_arguments() {
        NonceCache cache = new NonceCache.InMemory(1000L, () -> 0L);
        assertThrows(IllegalArgumentException.class, () -> cache.putIfAbsent(null, "n"));
        assertThrows(IllegalArgumentException.class, () -> cache.putIfAbsent("t", null));
    }

    @Test
    void concurrent_inserts_of_same_nonce_admit_exactly_one() throws Exception {
        NonceCache cache = new NonceCache.InMemory(60_000L, System::currentTimeMillis);
        int threads = 16;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        latch.await();
                        if (cache.putIfAbsent("t", "same")) {
                            admitted.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            latch.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, admitted.get(), "even under contention, exactly one thread may register the nonce");
    }
}
