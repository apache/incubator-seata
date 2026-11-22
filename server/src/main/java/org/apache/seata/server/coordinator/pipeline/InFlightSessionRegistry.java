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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Lightweight per-node registry tracking global transaction XIDs currently in flight within
 * the two-phase cleanup pipeline.
 *
 * <p>Uses a monotonic millisecond clock by default (immune to wall-clock adjustments) and issues
 * unique owner tokens on every acquisition so an expired owner can never accidentally release
 * or renew a lease subsequently acquired by another worker.
 *
 * <p>Supports two ownership states:
 * <ul>
 *   <li><b>Queued</b> ({@link #tryAcquireQueued(String)}): held while waiting inside the bounded
 *       in-memory task queue; does not expire under queue backlog and is explicitly released on
 *       drop/shutdown or promoted to an execution lease via {@link #renew(String, long)} upon
 *       dequeue.</li>
 *   <li><b>Active</b> ({@link #tryAcquire(String)} / {@link #renew(String, long)}): bounded by
 *       {@code leaseMs} TTL so abnormal worker exits cannot permanently block recovery.</li>
 * </ul>
 */
public class InFlightSessionRegistry {

    /**
     * Sentinel token returned when lease acquisition fails.
     */
    public static final long INVALID_TOKEN = 0L;

    private final ConcurrentHashMap<String, LeaseEntry> leases = new ConcurrentHashMap<>();
    private final AtomicLong tokenSequence = new AtomicLong(0L);
    private final long leaseMs;
    private final long evictIntervalMs;
    private final LongSupplier clockMs;
    private final AtomicLong lastEvictMs;

    /**
     * Creates a registry backed by a monotonic millisecond clock derived from
     * {@link System#nanoTime()}.
     *
     * @param leaseMs execution lease duration in milliseconds (must be {@code >= 1})
     */
    public InFlightSessionRegistry(long leaseMs) {
        this(leaseMs, () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }

    /**
     * Creates a registry with an injectable monotonic millisecond clock (useful for deterministic
     * unit testing).
     *
     * @param leaseMs execution lease duration in milliseconds (must be {@code >= 1})
     * @param clockMs monotonic millisecond clock supplier
     */
    public InFlightSessionRegistry(long leaseMs, LongSupplier clockMs) {
        if (leaseMs < 1L) {
            throw new IllegalArgumentException("leaseMs must be >= 1, got " + leaseMs);
        }
        this.leaseMs = leaseMs;
        this.evictIntervalMs = Math.max(1L, leaseMs / 2L);
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs must not be null");
        this.lastEvictMs = new AtomicLong(this.clockMs.getAsLong());
    }

    /**
     * Atomically acquires an execution lease (bounded by {@code leaseMs}) for {@code xid} if
     * absent or expired.
     *
     * @param xid global transaction XID
     * @return positive lease token if acquired; {@link #INVALID_TOKEN} if already in flight
     *         or {@code xid} is null/empty
     */
    public long tryAcquire(String xid) {
        return tryAcquireInternal(xid, false);
    }

    /**
     * Atomically acquires a queued lease for {@code xid} if absent or expired.
     *
     * <p>Queued leases do not expire while waiting in the bounded executor queue; workers must
     * call {@link #renew(String, long)} upon dequeue to start the execution TTL (or
     * {@link #release(String, long)} if dropped or cancelled on shutdown).
     *
     * @param xid global transaction XID
     * @return positive lease token if acquired; {@link #INVALID_TOKEN} if already in flight
     *         or {@code xid} is null/empty
     */
    public long tryAcquireQueued(String xid) {
        return tryAcquireInternal(xid, true);
    }

    private long tryAcquireInternal(String xid, boolean queued) {
        if (xid == null || xid.isEmpty()) {
            return INVALID_TOKEN;
        }
        long now = clockMs.getAsLong();
        maybeEvictExpired(now);
        long[] acquiredToken = new long[] {INVALID_TOKEN};
        leases.compute(xid, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                long token = nextToken();
                acquiredToken[0] = token;
                return new LeaseEntry(token, queued, queued ? 0L : now + leaseMs);
            }
            return existing;
        });
        return acquiredToken[0];
    }

    /**
     * Atomically activates (from queued) or renews an active execution lease for {@code xid}
     * only if the current lease is still owned by {@code token} and has not expired.
     *
     * @param xid   global transaction XID
     * @param token owner lease token returned by {@link #tryAcquire(String)} or
     *              {@link #tryAcquireQueued(String)}
     * @return {@code true} if the lease was renewed; {@code false} if absent, expired, or owned
     *         by a different token
     */
    public boolean renew(String xid, long token) {
        if (xid == null || xid.isEmpty() || token == INVALID_TOKEN) {
            return false;
        }
        long now = clockMs.getAsLong();
        boolean[] renewed = new boolean[] {false};
        leases.computeIfPresent(xid, (key, existing) -> {
            if (existing.token == token && !existing.isExpired(now)) {
                renewed[0] = true;
                return new LeaseEntry(token, false, now + leaseMs);
            }
            if (existing.isExpired(now)) {
                return null;
            }
            return existing;
        });
        return renewed[0];
    }

    /**
     * Atomically releases the lease for {@code xid} only if the currently stored owner token
     * matches {@code token}.
     *
     * <p>If this caller's lease already expired and another worker acquired a new lease token,
     * this method returns {@code false} and leaves the new owner's lease intact.
     *
     * @param xid   global transaction XID
     * @param token owner lease token
     * @return {@code true} if the matching lease was removed; {@code false} otherwise
     */
    public boolean release(String xid, long token) {
        if (xid == null || xid.isEmpty() || token == INVALID_TOKEN) {
            return false;
        }
        boolean[] released = new boolean[] {false};
        leases.computeIfPresent(xid, (key, existing) -> {
            if (existing.token == token) {
                released[0] = true;
                return null;
            }
            return existing;
        });
        return released[0];
    }

    /**
     * Returns {@code true} if {@code xid} currently holds a queued or unexpired execution lease,
     * lazily evicting an expired entry if encountered.
     *
     * @param xid global transaction XID
     * @return {@code true} if present and active
     */
    public boolean isInFlight(String xid) {
        if (xid == null || xid.isEmpty()) {
            return false;
        }
        LeaseEntry entry = leases.get(xid);
        if (entry == null) {
            return false;
        }
        long now = clockMs.getAsLong();
        if (entry.isExpired(now)) {
            leases.remove(xid, entry);
            return false;
        }
        return true;
    }

    /**
     * Performs a throttled expired-lease sweep at most once per {@code max(1, leaseMs / 2)} ms.
     * Safe to call on worker/submit hot paths without relying on cleaner idleness.
     *
     * @return number of expired entries removed during this call (0 if throttled)
     */
    public int maybeEvictExpired() {
        return maybeEvictExpired(clockMs.getAsLong());
    }

    private int maybeEvictExpired(long now) {
        long last = lastEvictMs.get();
        if (now - last < evictIntervalMs) {
            return 0;
        }
        if (lastEvictMs.compareAndSet(last, now)) {
            return evictExpiredAt(now);
        }
        return 0;
    }

    /**
     * Unconditionally evicts all currently expired execution lease entries to prevent unbounded
     * map growth after abnormal worker failures. Queued entries are not evicted by TTL.
     *
     * @return number of expired entries removed
     */
    public int evictExpired() {
        long now = clockMs.getAsLong();
        lastEvictMs.set(now);
        return evictExpiredAt(now);
    }

    private int evictExpiredAt(long now) {
        int evicted = 0;
        for (Map.Entry<String, LeaseEntry> mapEntry : leases.entrySet()) {
            LeaseEntry entry = mapEntry.getValue();
            if (entry != null && entry.isExpired(now) && leases.remove(mapEntry.getKey(), entry)) {
                evicted++;
            }
        }
        return evicted;
    }

    private long nextToken() {
        long token = tokenSequence.incrementAndGet();
        if (token == INVALID_TOKEN) {
            token = tokenSequence.incrementAndGet();
        }
        return token;
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

    private static final class LeaseEntry {
        private final long token;
        private final boolean queued;
        private final long expireAtMs;

        private LeaseEntry(long token, boolean queued, long expireAtMs) {
            this.token = token;
            this.queued = queued;
            this.expireAtMs = expireAtMs;
        }

        private boolean isExpired(long nowMs) {
            return !queued && (nowMs - expireAtMs >= 0L);
        }
    }
}
