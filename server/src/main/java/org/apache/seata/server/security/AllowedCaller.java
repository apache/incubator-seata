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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A NamingServer identity that TC is willing to accept inbound calls from.
 *
 * <p>Note this is the mirror of {@code ClusterIdentity} on the NamingServer side, but with a
 * different permission vocabulary. TC doesn't care about namespaces / cluster allow-lists
 * (that's NamingServer's concern) — TC only cares "do you have the right to call me for X?".
 */
public final class AllowedCaller {

    private final String id;

    private final byte[] secret;

    private final Set<CallerPermission> permissions;

    public AllowedCaller(String id, byte[] secret, Set<CallerPermission> permissions) {
        this.id = Objects.requireNonNull(id, "id");
        this.secret = Objects.requireNonNull(secret, "secret").clone();
        this.permissions = permissions == null
                ? EnumSet.noneOf(CallerPermission.class)
                : Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }

    public String getId() {
        return id;
    }

    /** Defensive copy — key material never leaves this object. */
    public byte[] getSecret() {
        return secret.clone();
    }

    public Set<CallerPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(CallerPermission permission) {
        return permissions.contains(permission);
    }
}
