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
package io.seata.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Unit test for BranchType enum
 */
public class BranchTypeTest {

    @Test
    public void testBranchTypeValues() {
        // Test that all branch types exist
        Assertions.assertNotNull(BranchType.AT);
        Assertions.assertNotNull(BranchType.TCC);
        Assertions.assertNotNull(BranchType.SAGA);
        Assertions.assertNotNull(BranchType.XA);
    }

    @Test
    public void testBranchTypeCompatibility() {
        // Test that compatible BranchType has same values as Apache Seata BranchType
        
        // Test AT branch type
        Assertions.assertEquals(
            org.apache.seata.core.model.BranchType.AT.ordinal(),
            BranchType.AT.ordinal()
        );
        
        // Test TCC branch type
        Assertions.assertEquals(
            org.apache.seata.core.model.BranchType.TCC.ordinal(),
            BranchType.TCC.ordinal()
        );
        
        // Test SAGA branch type
        Assertions.assertEquals(
            org.apache.seata.core.model.BranchType.SAGA.ordinal(),
            BranchType.SAGA.ordinal()
        );
        
        // Test XA branch type
        Assertions.assertEquals(
            org.apache.seata.core.model.BranchType.XA.ordinal(),
            BranchType.XA.ordinal()
        );
    }

    @Test
    public void testGetMethod() {
        // Test the get method for each branch type
        Assertions.assertEquals(BranchType.AT, BranchType.get((byte) 0));
        Assertions.assertEquals(BranchType.TCC, BranchType.get((byte) 1));
        Assertions.assertEquals(BranchType.SAGA, BranchType.get((byte) 2));
        Assertions.assertEquals(BranchType.XA, BranchType.get((byte) 3));
    }

    @Test
    public void testGetMethodWithInvalidValue() {
        // Test get method with invalid byte value
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            BranchType.get((byte) 99);
        });
    }

    @Test
    public void testBranchTypeNames() {
        // Test that branch type names match
        Assertions.assertEquals("AT", BranchType.AT.name());
        Assertions.assertEquals("TCC", BranchType.TCC.name());
        Assertions.assertEquals("SAGA", BranchType.SAGA.name());
        Assertions.assertEquals("XA", BranchType.XA.name());
    }

    @Test
    public void testBranchTypeValuesArray() {
        // Test the values() method
        BranchType[] values = BranchType.values();
        Assertions.assertTrue(values.length >= 4);
        
        // Verify all expected types are present
        boolean hasAT = false, hasTCC = false, hasSAGA = false, hasXA = false;
        for (BranchType type : values) {
            switch (type.name()) {
                case "AT":
                    hasAT = true;
                    break;
                case "TCC":
                    hasTCC = true;
                    break;
                case "SAGA":
                    hasSAGA = true;
                    break;
                case "XA":
                    hasXA = true;
                    break;
            }
        }
        
        Assertions.assertTrue(hasAT, "AT branch type should be present");
        Assertions.assertTrue(hasTCC, "TCC branch type should be present");
        Assertions.assertTrue(hasSAGA, "SAGA branch type should be present");
        Assertions.assertTrue(hasXA, "XA branch type should be present");
    }

    @Test
    public void testValueOf() {
        // Test the valueOf method
        Assertions.assertEquals(BranchType.AT, BranchType.valueOf("AT"));
        Assertions.assertEquals(BranchType.TCC, BranchType.valueOf("TCC"));
        Assertions.assertEquals(BranchType.SAGA, BranchType.valueOf("SAGA"));
        Assertions.assertEquals(BranchType.XA, BranchType.valueOf("XA"));
    }

    @Test
    public void testValueOfWithInvalidName() {
        // Test valueOf with invalid name
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            BranchType.valueOf("INVALID");
        });
    }
} 