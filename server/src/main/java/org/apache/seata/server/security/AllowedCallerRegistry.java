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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory table of NamingServer identities that TC trusts. Supports online replacement so
 * operators can hot-swap secrets during rotation without a restart.
 */
public final class AllowedCallerRegistry {

    private final ConcurrentHashMap<String, AllowedCaller> byId = new ConcurrentHashMap<>();

    public void register(AllowedCaller caller) {
        byId.put(caller.getId(), caller);
    }

    /**
     * Swap the whole set. During a rotation the new secret and the old secret can coexist
     * — the caller passes both entries in and this method installs them atomically.
     */
    public void reload(Collection<AllowedCaller> callers) {
        byId.clear();
        for (AllowedCaller c : callers) {
            byId.put(c.getId(), c);
        }
    }

    public Optional<AllowedCaller> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public int size() {
        return byId.size();
    }
}
