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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedCallerRegistryTest {

    @Test
    void find_returns_registered_caller() {
        AllowedCallerRegistry reg = new AllowedCallerRegistry();
        reg.register(caller("naming-01"));
        assertTrue(reg.find("naming-01").isPresent());
        assertFalse(reg.find("unknown").isPresent());
        assertFalse(reg.find(null).isPresent());
    }

    @Test
    void reload_supports_key_rotation_with_two_active_secrets() {
        AllowedCallerRegistry reg = new AllowedCallerRegistry();
        AllowedCaller oldSecret = caller("naming-01");
        reg.register(oldSecret);
        // Rotation: install both entries so requests signed with either succeed.
        reg.reload(Arrays.asList(oldSecret, caller("naming-01-new")));
        assertEquals(2, reg.size());
    }

    @Test
    void secret_is_defensively_copied_by_caller() {
        byte[] key = new byte[32];
        AllowedCaller caller = new AllowedCaller("x", key, EnumSet.noneOf(CallerPermission.class));
        assertNotSame(caller.getSecret(), caller.getSecret());
    }

    @Test
    void permissions_default_to_empty_when_null() {
        AllowedCaller c = new AllowedCaller("x", new byte[32], null);
        assertTrue(c.getPermissions().isEmpty());
        assertFalse(c.hasPermission(CallerPermission.VGROUP_WRITE));
    }

    private static AllowedCaller caller(String id) {
        return new AllowedCaller(id, new byte[32], Collections.singleton(CallerPermission.VGROUP_WRITE));
    }
}
