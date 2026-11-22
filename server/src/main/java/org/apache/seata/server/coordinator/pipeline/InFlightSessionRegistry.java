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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Lightweight per-node registry tracking global transaction XIDs currently in flight within
 * the two-phase cleanup pipeline.
 *
 * <p>Prevents duplicate concurrent processing of the same XID across push mode and fallback
 * scanning while bounding ownership via a lease TTL so abnormal worker exits cannot permanently
 * block recovery.
 */
public class InFlightSessionRegistry {

    private final ConcurrentHashMap<String, Long> leases = new ConcurrentHashMap<>();
    private final long leaseMs;
    private final LongSupplier clockMs;

    /**
     * Creates a registry backed by {@link System#currentTimeMillis()}.
     *
     * @param leaseMs lease duration in milliseconds (must be {@code >= 1})
     */
    public InFlightSessionRegistry(long leaseMs) {
        this(leaseMs, System::currentTimeMillis);
    }

    /**
     * Creates a registry with an injectable millisecond clock (useful for deterministic testing).
     *
     * @param leaseMs lease duration in milliseconds (must be {@code >= 1})
     * @param clockMs millisecond clock supplier
     */
    public InFlightSessionRegistry(long leaseMs, LongSupplier clockMs) {
        if (leaseMs < 1L) {
            throw new IllegalArgumentException("leaseMs must be >= 1, got " + leaseMs);
        }
        this.leaseMs = leaseMs;
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs must not be null");
    }

    /**
     * Atomically acquires an in-flight lease for {@code xid} if it is absent or its previous
     * lease has expired.
     *
     * @param xid global transaction XID
     * @return {@code true} if this caller acquired the lease; {@code false} if already in flight
     *         or {@code xid} is null/empty
     */
    public boolean tryAcquire(String xid) {
        if (xid == null || xid.isEmpty()) {
            return false;
        }
        long now = clockMs.getAsLong();
        long newExpireAt = (Long.MAX_VALUE - now < leaseMs) ? Long.MAX_VALUE : now + leaseMs;
        boolean[] acquired = new boolean[] {false};
        leases.compute(xid, (key, existingExpireAt) -> {
            if (existingExpireAt == null || existingExpireAt <= now) {
                acquired[0] = true;
                return newExpireAt;
            }
            return existingExpireAt;
        });
        return acquired[0];
    }

    /**
     * Returns {@code true} if {@code xid} currently holds an unexpired lease, lazily evicting
     * an expired entry if encountered.
     *
     * @param xid global transaction XID
     * @return {@code true} if present and unexpired
     */
    public boolean isInFlight(String xid) {
        if (xid == null || xid.isEmpty()) {
            return false;
        }
        Long expireAt = leases.get(xid);
        if (expireAt == null) {
            return false;
        }
        long now = clockMs.getAsLong();
        if (expireAt <= now) {
            leases.remove(xid, expireAt);
            return false;
        }
        return true;
    }

    /**
     * Explicitly releases the lease for {@code xid}.
     *
     * @param xid global transaction XID
     */
    public void release(String xid) {
        if (xid == null || xid.isEmpty()) {
            return;
        }
        leases.remove(xid);
    }

    /**
     * Evicts all currently expired lease entries to prevent unbounded map growth after
     * abnormal worker failures.
     *
     * @return number of expired entries removed
     */
    public int evictExpired() {
        long now = clockMs.getAsLong();
        int evicted = 0;
        for (Map.Entry<String, Long> entry : leases.entrySet()) {
            Long expireAt = entry.getValue();
            if (expireAt != null && expireAt <= now && leases.remove(entry.getKey(), expireAt)) {
                evicted++;
            }
        }
        return evicted;
    }

    /**
     * Returns the current number of entries stored in the registry (including any not yet evicted).
     *
     * @return entry count
     */
    public int size() {
        return leases.size();
    }

    public long getLeaseMs() {
        return leaseMs;
    }
}
