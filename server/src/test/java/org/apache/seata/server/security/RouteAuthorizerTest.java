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

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteAuthorizerTest {

    private final RouteAuthorizer authorizer = new RouteAuthorizer();

    @Test
    void vgroup_write_route_requires_vgroup_write() {
        assertEquals(CallerPermission.VGROUP_WRITE, authorizer.requiredPermission("GET", "/vgroup/v1/addVGroup"));
        assertEquals(CallerPermission.VGROUP_WRITE, authorizer.requiredPermission("GET", "/vgroup/v1/removeVGroup"));
    }

    @Test
    void console_paths_require_console_proxy() {
        assertEquals(
                CallerPermission.CONSOLE_PROXY,
                authorizer.requiredPermission("GET", "/api/v1/console/globalSession/query"));
        assertEquals(
                CallerPermission.CONSOLE_PROXY,
                authorizer.requiredPermission("POST", "/api/v1/console/globalSession/forceCommit"));
    }

    @Test
    void unknown_route_returns_null() {
        assertNull(authorizer.requiredPermission("GET", "/random"));
        assertNull(authorizer.requiredPermission(null, "/x"));
        assertNull(authorizer.requiredPermission("GET", null));
    }

    @Test
    void isAuthorized_true_when_caller_holds_permission() {
        AllowedCaller c = new AllowedCaller("n", new byte[32], EnumSet.of(CallerPermission.VGROUP_WRITE));
        assertTrue(authorizer.isAuthorized(c, "GET", "/vgroup/v1/addVGroup"));
    }

    @Test
    void isAuthorized_false_when_permission_missing() {
        AllowedCaller c = new AllowedCaller("n", new byte[32], EnumSet.of(CallerPermission.CONSOLE_PROXY));
        assertFalse(authorizer.isAuthorized(c, "GET", "/vgroup/v1/addVGroup"));
    }

    @Test
    void isAuthorized_false_for_unknown_route_even_if_all_permissions_held() {
        AllowedCaller c = new AllowedCaller("n", new byte[32], EnumSet.allOf(CallerPermission.class));
        assertFalse(authorizer.isAuthorized(c, "GET", "/random"));
    }
}
