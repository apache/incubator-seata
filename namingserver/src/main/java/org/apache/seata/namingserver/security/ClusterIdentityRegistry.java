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
package org.apache.seata.namingserver.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory table of {@link ClusterIdentity} keyed by cluster-id. Reads are lock-free; writes
 * only happen at startup or on config reload, so this is heavily biased for the read path.
 *
 * <p>The whole table is held as an immutable {@link Map} behind an {@link AtomicReference}.
 * Register and reload publish a fresh snapshot via {@code compareAndSet} / {@code set}, so
 * concurrent {@link #find(String)} calls always observe either the old table in full or the
 * new table in full — never a transient empty/partial state during rotation.
 *
 * <p>Kept as a small stand-alone class (rather than a full-blown "IdentityService") so it can
 * be unit-tested without Spring context and shared between the inbound filter and the
 * outbound signer.
 */
public final class ClusterIdentityRegistry {

    private final AtomicReference<Map<String, ClusterIdentity>> ref = new AtomicReference<>(Collections.emptyMap());

    /** Register (or replace) an identity. Callers must have already validated the secret length. */
    public void register(ClusterIdentity identity) {
        while (true) {
            Map<String, ClusterIdentity> current = ref.get();
            Map<String, ClusterIdentity> next = new HashMap<>(current);
            next.put(identity.getId(), identity);
            if (ref.compareAndSet(current, Collections.unmodifiableMap(next))) {
                return;
            }
        }
    }

    /** Bulk-load, atomically replacing the whole table via a single reference swap. */
    public void reload(Collection<ClusterIdentity> identities) {
        Map<String, ClusterIdentity> next = new HashMap<>();
        for (ClusterIdentity id : identities) {
            next.put(id.getId(), id);
        }
        ref.set(Collections.unmodifiableMap(next));
    }

    public Optional<ClusterIdentity> find(String clusterId) {
        if (clusterId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ref.get().get(clusterId));
    }

    /** Live, unmodifiable view — mainly for metrics / admin endpoints. */
    public Map<String, ClusterIdentity> asMap() {
        return ref.get();
    }

    public int size() {
        return ref.get().size();
    }
}
