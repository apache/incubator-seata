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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterIdentityTest {

    private static final byte[] SECRET = new byte[32];

    @Test
    void empty_allow_lists_default_to_permissive_for_ns_and_cluster() {
        // No allow-list configured → treat as no restriction (matches production defaults).
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
        assertTrue(id.isNamespaceAllowed("anything"));
        assertTrue(id.isClusterAllowed("anything"));
        assertTrue(id.isVgroupAllowed("anything"));
    }

    @Test
    void wildcard_star_in_allow_list_matches_all() {
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                new HashSet<>(Collections.singletonList("*")),
                new HashSet<>(Collections.singletonList("*")),
                Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
        assertTrue(id.isNamespaceAllowed("prod"));
        assertTrue(id.isClusterAllowed("cluster-XYZ"));
    }

    @Test
    void explicit_allow_list_is_strict() {
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                new HashSet<>(Arrays.asList("prod", "staging")),
                new HashSet<>(Arrays.asList("cluster-A")),
                Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
        assertTrue(id.isNamespaceAllowed("prod"));
        assertFalse(id.isNamespaceAllowed("dev"));
        assertTrue(id.isClusterAllowed("cluster-A"));
        assertFalse(id.isClusterAllowed("cluster-B"));
    }

    @Test
    void vgroup_pattern_matches_glob_regex() {
        Pattern p = Pattern.compile("^tenant_a_.*$");
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                Collections.emptySet(), Collections.emptySet(),
                Collections.singletonList(p),
                EnumSet.of(Permission.VGROUP_WRITE));
        assertTrue(id.isVgroupAllowed("tenant_a_group"));
        assertFalse(id.isVgroupAllowed("tenant_b_group"));
        assertFalse(id.isVgroupAllowed(null));
    }

    @Test
    void get_secret_returns_defensive_copy() {
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                EnumSet.noneOf(Permission.class));
        byte[] first = id.getSecret();
        byte[] second = id.getSecret();
        assertNotSame(first, second, "each call must return a fresh copy");
    }

    @Test
    void has_permission_reflects_configured_set() {
        ClusterIdentity id = new ClusterIdentity("t", SECRET,
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                EnumSet.of(Permission.REGISTER, Permission.HEARTBEAT));
        assertTrue(id.hasPermission(Permission.REGISTER));
        assertFalse(id.hasPermission(Permission.CONSOLE_WRITE));
    }
}
