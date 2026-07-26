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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A per-JVM anti-replay cache. It stores nonces recently seen inside the replay window and
 * refuses to re-admit any of them, so an attacker cannot capture a signed request and re-send it.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Backed by {@link ConcurrentHashMap} of {@code cluster-id + '|' + nonce → insertion millis}
 *       — namespacing by cluster-id prevents cross-tenant nonce collisions.</li>
 *   <li>Lazy eviction: entries are removed on access when older than the TTL. A tiny amount of
 *       cross-check work amortises against every {@link #putIfAbsent(String, String)} call,
 *       so we do not need a background thread.</li>
 *   <li>The single-node scope is intentional — this class only defends replay against the
 *       exact same NamingServer node. To defend across nodes when the caller cycles hosts,
 *       bind the target node id into the signature (see {@code CanonicalRequest}). A future
 *       Redis-backed impl of this interface would close the multi-node gap.</li>
 * </ul>
 */
public interface NonceCache {

    /**
     * Attempt to register a nonce for a given cluster identity.
     *
     * @param clusterId caller identity ({@code X-Seata-Cluster-Id} header value)
     * @param nonce     the received nonce ({@code X-Seata-Nonce} header value)
     * @return {@code true} if the nonce was fresh and has been recorded;
     *         {@code false} if the same nonce was already seen inside the TTL
     */
    boolean putIfAbsent(String clusterId, String nonce);

    /** Current cache size — exposed for metrics / tests. */
    int size();

    /** Force TTL eviction now — normally not needed; provided for tests. */
    void evictExpired();

    /**
     * Default in-memory implementation. Thread-safe, lock-free on the fast path.
     */
    final class InMemory implements NonceCache {

        private final ConcurrentHashMap<String, Long> entries = new ConcurrentHashMap<>();
        private final long ttlMillis;
        private final AtomicLong opsSinceLastSweep = new AtomicLong();
        private final long sweepEveryOps;
        private final Clock clock;

        /**
         * @param ttlMillis how long a nonce is remembered
         * @param clock     time source (injectable for deterministic tests)
         */
        public InMemory(long ttlMillis, Clock clock) {
            this(ttlMillis, clock, 1024L);
        }

        /**
         * @param ttlMillis     nonce TTL
         * @param clock         time source
         * @param sweepEveryOps run a lazy TTL sweep once every N operations
         */
        public InMemory(long ttlMillis, Clock clock, long sweepEveryOps) {
            if (ttlMillis <= 0) {
                throw new IllegalArgumentException("ttlMillis must be > 0");
            }
            this.ttlMillis = ttlMillis;
            this.clock = clock == null ? System::currentTimeMillis : clock;
            this.sweepEveryOps = Math.max(1L, sweepEveryOps);
        }

        @Override
        public boolean putIfAbsent(String clusterId, String nonce) {
            if (clusterId == null || nonce == null) {
                throw new IllegalArgumentException("clusterId and nonce must not be null");
            }
            long now = clock.now();
            String key = key(clusterId, nonce);
            Long prev = entries.putIfAbsent(key, now);
            if (prev != null) {
                // If the existing entry has itself expired, treat this as a fresh insert.
                if (now - prev > ttlMillis) {
                    // Replace only if nobody has moved the value in the meantime.
                    if (entries.replace(key, prev, now)) {
                        maybeSweep(now);
                        return true;
                    }
                    // Someone else already registered it after expiry — treat as replay.
                    return false;
                }
                return false;
            }
            maybeSweep(now);
            return true;
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public void evictExpired() {
            long now = clock.now();
            Iterator<Map.Entry<String, Long>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (now - e.getValue() > ttlMillis) {
                    it.remove();
                }
            }
        }

        private void maybeSweep(long now) {
            long ops = opsSinceLastSweep.incrementAndGet();
            if (ops % sweepEveryOps == 0) {
                evictExpired();
            }
        }

        private static String key(String clusterId, String nonce) {
            // Deliberately a plain concatenation with a delimiter that cannot appear inside
            // a UUID nonce or a cluster-id. Avoids the overhead of building a composite key object.
            return clusterId + '|' + nonce;
        }
    }

    /** Trivial injectable clock so we can advance time in tests without sleeping. */
    @FunctionalInterface
    interface Clock {
        long now();
    }
}
