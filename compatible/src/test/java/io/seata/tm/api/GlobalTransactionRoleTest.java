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
package io.seata.tm.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for GlobalTransactionRole enum compatibility.
 */
public class GlobalTransactionRoleTest {

    @Test
    public void testDeprecatedAnnotation() {
        // Test that GlobalTransactionRole is marked as @Deprecated
        assertTrue(GlobalTransactionRole.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransactionRole should be marked as @Deprecated");
    }

    @Test
    public void testAllEnumValues() {
        // Test all enum values exist
        assertNotNull(GlobalTransactionRole.Launcher);
        assertNotNull(GlobalTransactionRole.Participant);
    }

    @Test
    public void testEnumValueCount() {
        // Test that we have the expected number of enum values
        GlobalTransactionRole[] values = GlobalTransactionRole.values();
        assertEquals(2, values.length, "Should have 2 GlobalTransactionRole enum values");
    }

    @Test
    public void testEnumOrdinals() {
        // Test that ordinals are consistent
        assertEquals(0, GlobalTransactionRole.Launcher.ordinal(), 
                "Launcher should have ordinal 0");
        assertEquals(1, GlobalTransactionRole.Participant.ordinal(), 
                "Participant should have ordinal 1");
    }

    @Test
    public void testEnumNames() {
        // Test enum names
        assertEquals("Launcher", GlobalTransactionRole.Launcher.name());
        assertEquals("Participant", GlobalTransactionRole.Participant.name());
    }

    @Test
    public void testValueOf() {
        // Test valueOf method
        assertEquals(GlobalTransactionRole.Launcher, 
                GlobalTransactionRole.valueOf("Launcher"));
        assertEquals(GlobalTransactionRole.Participant, 
                GlobalTransactionRole.valueOf("Participant"));
    }

    @Test
    public void testValueOfInvalid() {
        // Test valueOf with invalid name
        assertThrows(IllegalArgumentException.class, 
                () -> GlobalTransactionRole.valueOf("InvalidRole"),
                "Should throw IllegalArgumentException for invalid role name");
        
        assertThrows(IllegalArgumentException.class, 
                () -> GlobalTransactionRole.valueOf("launcher"),
                "Should throw IllegalArgumentException for lowercase role name");
        
        assertThrows(IllegalArgumentException.class, 
                () -> GlobalTransactionRole.valueOf("LAUNCHER"),
                "Should throw IllegalArgumentException for uppercase role name");
    }

    @Test
    public void testValues() {
        // Test values method returns correct array
        GlobalTransactionRole[] values = GlobalTransactionRole.values();
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(GlobalTransactionRole.Launcher, values[0]);
        assertEquals(GlobalTransactionRole.Participant, values[1]);
    }

    @Test
    public void testToString() {
        // Test toString method (should return the name)
        assertEquals("Launcher", GlobalTransactionRole.Launcher.toString());
        assertEquals("Participant", GlobalTransactionRole.Participant.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        // Test equals
        assertEquals(GlobalTransactionRole.Launcher, GlobalTransactionRole.Launcher);
        assertEquals(GlobalTransactionRole.Participant, GlobalTransactionRole.Participant);
        assertNotEquals(GlobalTransactionRole.Launcher, GlobalTransactionRole.Participant);
        assertNotEquals(GlobalTransactionRole.Participant, GlobalTransactionRole.Launcher);
        
        // Test hashCode consistency
        assertEquals(GlobalTransactionRole.Launcher.hashCode(), 
                GlobalTransactionRole.Launcher.hashCode());
        assertEquals(GlobalTransactionRole.Participant.hashCode(), 
                GlobalTransactionRole.Participant.hashCode());
    }

    @Test
    public void testCompatibilityWithApacheSeata() {
        // Test that enum names match Apache Seata equivalents
        // This ensures compatibility when converting between packages
        
        for (GlobalTransactionRole role : GlobalTransactionRole.values()) {
            // Test that Apache Seata has equivalent enum value
            assertDoesNotThrow(() -> {
                org.apache.seata.tm.api.GlobalTransactionRole.valueOf(role.name());
            }, "Apache Seata should have equivalent role: " + role.name());
        }
        
        // Test reverse compatibility
        for (org.apache.seata.tm.api.GlobalTransactionRole apacheRole : 
                org.apache.seata.tm.api.GlobalTransactionRole.values()) {
            assertDoesNotThrow(() -> {
                GlobalTransactionRole.valueOf(apacheRole.name());
            }, "Compatible package should have equivalent role: " + apacheRole.name());
        }
    }

    @Test
    public void testEnumCompareTo() {
        // Test enum comparison (based on ordinal)
        assertTrue(GlobalTransactionRole.Launcher.compareTo(GlobalTransactionRole.Participant) < 0,
                "Launcher should come before Participant");
        assertTrue(GlobalTransactionRole.Participant.compareTo(GlobalTransactionRole.Launcher) > 0,
                "Participant should come after Launcher");
        assertEquals(0, GlobalTransactionRole.Launcher.compareTo(GlobalTransactionRole.Launcher),
                "Same enum should compare equal");
    }

    @Test
    public void testSemanticMeaning() {
        // Test the semantic meaning based on documentation comments
        
        // Launcher: The one begins the current global transaction
        assertNotNull(GlobalTransactionRole.Launcher, 
                "Launcher role should exist for transaction initiator");
        
        // Participant: The one just joins into a existing global transaction
        assertNotNull(GlobalTransactionRole.Participant, 
                "Participant role should exist for transaction joiner");
        
        // Ensure they are different
        assertNotEquals(GlobalTransactionRole.Launcher, GlobalTransactionRole.Participant,
                "Launcher and Participant should be different roles");
    }
} 