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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A registered caller identity. Every request the NamingServer receives is bound to at
 * most one of these — the identity is looked up by the {@code X-Seata-Cluster-Id} header.
 *
 * <p>Fields are all immutable after construction; instances are cheap to share across threads.
 */
public final class ClusterIdentity {

    private final String id;

    private final byte[] secret;

    private final Set<String> allowedNamespaces;

    private final Set<String> allowedClusters;

    private final List<Pattern> allowedVgroupPatterns;

    private final Set<Permission> permissions;

    public ClusterIdentity(
            String id,
            byte[] secret,
            Set<String> allowedNamespaces,
            Set<String> allowedClusters,
            List<Pattern> allowedVgroupPatterns,
            Set<Permission> permissions) {
        this.id = Objects.requireNonNull(id, "id");
        this.secret = Objects.requireNonNull(secret, "secret").clone();
        this.allowedNamespaces =
                allowedNamespaces == null ? Collections.emptySet() : Collections.unmodifiableSet(allowedNamespaces);
        this.allowedClusters =
                allowedClusters == null ? Collections.emptySet() : Collections.unmodifiableSet(allowedClusters);
        this.allowedVgroupPatterns = allowedVgroupPatterns == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(allowedVgroupPatterns);
        this.permissions = permissions == null
                ? EnumSet.noneOf(Permission.class)
                : Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }

    public String getId() {
        return id;
    }

    /** Defensive copy: never expose the underlying key material. */
    public byte[] getSecret() {
        return secret.clone();
    }

    public Set<String> getAllowedNamespaces() {
        return allowedNamespaces;
    }

    public Set<String> getAllowedClusters() {
        return allowedClusters;
    }

    public List<Pattern> getAllowedVgroupPatterns() {
        return allowedVgroupPatterns;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    /** Wildcard {@code *} in the allow-list means "no restriction". */
    public boolean isNamespaceAllowed(String namespace) {
        return allowedNamespaces.isEmpty() || allowedNamespaces.contains("*") || allowedNamespaces.contains(namespace);
    }

    public boolean isClusterAllowed(String cluster) {
        return allowedClusters.isEmpty() || allowedClusters.contains("*") || allowedClusters.contains(cluster);
    }

    public boolean isVgroupAllowed(String vGroup) {
        if (allowedVgroupPatterns.isEmpty()) {
            return true;
        }
        if (vGroup == null) {
            return false;
        }
        for (Pattern p : allowedVgroupPatterns) {
            if (p.matcher(vGroup).matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
