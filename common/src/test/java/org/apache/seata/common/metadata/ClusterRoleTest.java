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
package org.apache.seata.common.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClusterRoleTest {

    @Test
    public void testClusterRole() {
        Assertions.assertEquals(0, ClusterRole.LEADER.getRoleCode());
        Assertions.assertEquals(1, ClusterRole.FOLLOWER.getRoleCode());
        Assertions.assertEquals(2, ClusterRole.LEARNER.getRoleCode());
        Assertions.assertEquals(3, ClusterRole.MEMBER.getRoleCode());
        Assertions.assertDoesNotThrow(() -> ClusterRole.MEMBER.setRoleCode(4));
    }

    @Test
    public void testSetRoleCode() {
        ClusterRole role = ClusterRole.LEADER;
        int originalCode = role.getRoleCode();

        // Test setting new role code
        role.setRoleCode(99);
        Assertions.assertEquals(99, role.getRoleCode());

        // Reset to original value
        role.setRoleCode(originalCode);
        Assertions.assertEquals(originalCode, role.getRoleCode());
    }

    @Test
    public void testAllClusterRoles() {
        ClusterRole[] roles = ClusterRole.values();
        Assertions.assertEquals(4, roles.length);

        Assertions.assertTrue(java.util.Arrays.asList(roles).contains(ClusterRole.LEADER));
        Assertions.assertTrue(java.util.Arrays.asList(roles).contains(ClusterRole.FOLLOWER));
        Assertions.assertTrue(java.util.Arrays.asList(roles).contains(ClusterRole.LEARNER));
        Assertions.assertTrue(java.util.Arrays.asList(roles).contains(ClusterRole.MEMBER));
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(ClusterRole.LEADER, ClusterRole.valueOf("LEADER"));
        Assertions.assertEquals(ClusterRole.FOLLOWER, ClusterRole.valueOf("FOLLOWER"));
        Assertions.assertEquals(ClusterRole.LEARNER, ClusterRole.valueOf("LEARNER"));
        Assertions.assertEquals(ClusterRole.MEMBER, ClusterRole.valueOf("MEMBER"));

        // Test invalid name throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ClusterRole.valueOf("INVALID");
        });
    }
}
