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
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionCheckerTest {

    private final PermissionChecker checker = new PermissionChecker();

    @Test
    void register_route_requires_register_permission() {
        assertEquals(Permission.REGISTER,
                checker.requiredPermission("POST", "/naming/v1/register"));
        assertEquals(Permission.REGISTER,
                checker.requiredPermission("POST", "/naming/v1/batchRegister"));
    }

    @Test
    void vgroup_routes_require_vgroup_write() {
        assertEquals(Permission.VGROUP_WRITE, checker.requiredPermission("POST", "/naming/v1/addGroup"));
        assertEquals(Permission.VGROUP_WRITE, checker.requiredPermission("POST", "/naming/v1/changeGroup"));
    }

    @Test
    void discovery_routes_require_console_read() {
        assertEquals(Permission.CONSOLE_READ, checker.requiredPermission("GET", "/naming/v1/discovery"));
        assertEquals(Permission.CONSOLE_READ, checker.requiredPermission("GET", "/naming/v1/namespace"));
        assertEquals(Permission.CONSOLE_READ, checker.requiredPermission("POST", "/naming/v1/watch"));
    }

    @Test
    void console_proxy_uses_method_to_pick_read_or_write() {
        assertEquals(Permission.CONSOLE_READ,
                checker.requiredPermission("GET", "/api/v1/console/globalSession/query"));
        assertEquals(Permission.CONSOLE_WRITE,
                checker.requiredPermission("POST", "/api/v1/console/globalSession/forceCommit"));
    }

    @Test
    void unknown_route_returns_null() {
        assertNull(checker.requiredPermission("GET", "/random/path"));
    }

    @Test
    void check_denies_when_permission_missing() {
        ClusterIdentity id = identity(EnumSet.of(Permission.CONSOLE_READ));
        assertFalse(checker.check(id, "POST", "/naming/v1/register", "prod", "cluster-A", null));
    }

    @Test
    void check_allows_when_all_conditions_met() {
        ClusterIdentity id = identity(EnumSet.of(Permission.REGISTER));
        assertTrue(checker.check(id, "POST", "/naming/v1/register", "prod", "cluster-A", null));
    }

    @Test
    void check_denies_namespace_outside_allow_list() {
        ClusterIdentity id = new ClusterIdentity("t", new byte[32],
                new HashSet<>(Collections.singletonList("staging")),
                Collections.emptySet(), Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
        assertFalse(checker.check(id, "POST", "/naming/v1/register", "prod", "cluster-A", null));
    }

    @Test
    void check_denies_cluster_outside_allow_list() {
        ClusterIdentity id = new ClusterIdentity("t", new byte[32],
                Collections.emptySet(),
                new HashSet<>(Arrays.asList("cluster-A")),
                Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
        assertFalse(checker.check(id, "POST", "/naming/v1/register", "prod", "cluster-B", null));
    }

    @Test
    void check_denies_vgroup_outside_pattern() {
        List<Pattern> patterns = Collections.singletonList(Pattern.compile("^tenant_a_.*$"));
        ClusterIdentity id = new ClusterIdentity("t", new byte[32],
                Collections.emptySet(), Collections.emptySet(), patterns,
                EnumSet.of(Permission.VGROUP_WRITE));
        assertFalse(checker.check(id, "POST", "/naming/v1/addGroup",
                "prod", "cluster-A", "tenant_b_group"));
        assertTrue(checker.check(id, "POST", "/naming/v1/addGroup",
                "prod", "cluster-A", "tenant_a_group"));
    }

    @Test
    void check_denies_unknown_route_by_default() {
        ClusterIdentity id = identity(EnumSet.allOf(Permission.class));
        assertFalse(checker.check(id, "GET", "/random", null, null, null),
                "unknown routes must fail closed");
    }

    private static ClusterIdentity identity(java.util.Set<Permission> perms) {
        return new ClusterIdentity("t", new byte[32],
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                perms);
    }
}
