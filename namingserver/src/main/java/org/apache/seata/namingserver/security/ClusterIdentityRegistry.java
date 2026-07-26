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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory table of {@link ClusterIdentity} keyed by cluster-id. Reads are lock-free; writes
 * only happen at startup or on config reload, so this is heavily biased for the read path.
 *
 * <p>Kept as a small stand-alone class (rather than a full-blown "IdentityService") so it can
 * be unit-tested without Spring context and shared between the inbound filter and the
 * outbound signer.
 */
public final class ClusterIdentityRegistry {

    private final Map<String, ClusterIdentity> byId = new ConcurrentHashMap<>();

    /** Register (or replace) an identity. Callers must have already validated the secret length. */
    public void register(ClusterIdentity identity) {
        byId.put(identity.getId(), identity);
    }

    /** Bulk-load, replacing everything atomically-ish (individual puts, not a swap). */
    public void reload(Collection<ClusterIdentity> identities) {
        byId.clear();
        for (ClusterIdentity id : identities) {
            byId.put(id.getId(), id);
        }
    }

    public Optional<ClusterIdentity> find(String clusterId) {
        if (clusterId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(clusterId));
    }

    /** Live, unmodifiable view — mainly for metrics / admin endpoints. */
    public Map<String, ClusterIdentity> asMap() {
        return Collections.unmodifiableMap(byId);
    }

    public int size() {
        return byId.size();
    }
}
