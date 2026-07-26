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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterIdentityRegistryTest {

    private static ClusterIdentity id(String name) {
        return new ClusterIdentity(name, new byte[32],
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                EnumSet.of(Permission.REGISTER));
    }

    @Test
    void find_returns_registered_identity() {
        ClusterIdentityRegistry reg = new ClusterIdentityRegistry();
        reg.register(id("tenant-a"));
        assertTrue(reg.find("tenant-a").isPresent());
        assertFalse(reg.find("tenant-b").isPresent());
    }

    @Test
    void find_null_returns_empty() {
        ClusterIdentityRegistry reg = new ClusterIdentityRegistry();
        assertFalse(reg.find(null).isPresent());
    }

    @Test
    void reload_replaces_previous_content() {
        ClusterIdentityRegistry reg = new ClusterIdentityRegistry();
        reg.register(id("tenant-a"));
        reg.register(id("tenant-b"));
        reg.reload(Arrays.asList(id("tenant-c")));
        assertEquals(1, reg.size());
        assertFalse(reg.find("tenant-a").isPresent());
        assertTrue(reg.find("tenant-c").isPresent());
    }

    @Test
    void as_map_is_unmodifiable() {
        ClusterIdentityRegistry reg = new ClusterIdentityRegistry();
        reg.register(id("tenant-a"));
        assertThrows(UnsupportedOperationException.class, () -> reg.asMap().clear());
    }
}
