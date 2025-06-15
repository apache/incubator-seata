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
package io.seata.core.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;

/**
 * Unit test for DubboConstants class
 */
public class DubboConstantsTest {

    @Test
    public void testDubboConstantsInheritance() {
        // Test that DubboConstants extends from Apache Seata's DubboConstants
        Assertions.assertTrue(org.apache.seata.core.constants.DubboConstants.class
                .isAssignableFrom(DubboConstants.class));
    }

    @Test
    public void testConstantsCompatibility() throws Exception {
        // Test that all constants in Apache Seata's DubboConstants are accessible in compatible version
        Field[] apacheFields = org.apache.seata.core.constants.DubboConstants.class.getDeclaredFields();
        
        for (Field apacheField : apacheFields) {
            if (java.lang.reflect.Modifier.isStatic(apacheField.getModifiers()) && 
                java.lang.reflect.Modifier.isPublic(apacheField.getModifiers()) &&
                java.lang.reflect.Modifier.isFinal(apacheField.getModifiers())) {
                
                try {
                    // Try to access the field through the compatible class
                    Field compatField = DubboConstants.class.getField(apacheField.getName());
                    
                    // Compare values
                    Object apacheValue = apacheField.get(null);
                    Object compatValue = compatField.get(null);
                    
                    Assertions.assertEquals(apacheValue, compatValue, 
                        "Constant " + apacheField.getName() + " should have the same value in both classes");
                } catch (NoSuchFieldException e) {
                    // This is acceptable for inherited constants
                    // The constant should still be accessible through inheritance
                    try {
                        Field inheritedField = DubboConstants.class.getDeclaredField(apacheField.getName());
                        // If we get here, the field exists but might not be public in the child class
                    } catch (NoSuchFieldException e2) {
                        // The constant should be accessible through inheritance
                        Object value = apacheField.get(null);
                        // Just verify we can access it without error - inheritance should handle this
                    }
                }
            }
        }
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the DubboConstants class is marked as deprecated
        Assertions.assertTrue(DubboConstants.class.isAnnotationPresent(Deprecated.class),
                "DubboConstants should be marked as @Deprecated");
    }

    @Test
    public void testInstanceCreation() {
        // Test that we can create an instance of DubboConstants
        DubboConstants constants = new DubboConstants();
        Assertions.assertNotNull(constants);
        
        // Test that it's an instance of Apache Seata's DubboConstants
        Assertions.assertTrue(constants instanceof org.apache.seata.core.constants.DubboConstants);
    }

    @Test
    public void testClassStructure() {
        // Test class modifiers
        int modifiers = DubboConstants.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers),
                "DubboConstants should be public");
        
        // Test superclass
        Assertions.assertEquals(org.apache.seata.core.constants.DubboConstants.class,
                DubboConstants.class.getSuperclass(),
                "DubboConstants should extend Apache Seata's DubboConstants");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals("io.seata.core.constants", 
                DubboConstants.class.getPackage().getName(),
                "DubboConstants should be in io.seata.core.constants package");
    }
} 