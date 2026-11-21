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
package org.apache.seata.server.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory table of NamingServer identities that TC trusts. Supports online replacement so
 * operators can hot-swap secrets during rotation without a restart.
 *
 * <p>The whole table is kept as an immutable {@link Map} behind an {@link AtomicReference}.
 * Register and reload publish a fresh snapshot via {@code compareAndSet} / {@code set}, so
 * concurrent {@link #find(String)} calls always observe either the old table in full or the
 * new table in full — never a transient empty/partial state during rotation.
 */
public final class AllowedCallerRegistry {

    private final AtomicReference<Map<String, AllowedCaller>> ref = new AtomicReference<>(Collections.emptyMap());

    public void register(AllowedCaller caller) {
        while (true) {
            Map<String, AllowedCaller> current = ref.get();
            Map<String, AllowedCaller> next = new HashMap<>(current);
            next.put(caller.getId(), caller);
            if (ref.compareAndSet(current, Collections.unmodifiableMap(next))) {
                return;
            }
        }
    }

    /**
     * Swap the whole set. During a rotation the new secret and the old secret can coexist
     * — the caller passes both entries in and this method installs them atomically.
     */
    public void reload(Collection<AllowedCaller> callers) {
        Map<String, AllowedCaller> next = new HashMap<>();
        for (AllowedCaller c : callers) {
            next.put(c.getId(), c);
        }
        ref.set(Collections.unmodifiableMap(next));
    }

    public Optional<AllowedCaller> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(ref.get().get(id));
    }

    public int size() {
        return ref.get().size();
    }
}
