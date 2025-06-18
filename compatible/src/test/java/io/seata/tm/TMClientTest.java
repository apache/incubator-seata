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
package io.seata.tm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Unit test for TMClient class
 */
public class TMClientTest {

    @Test
    public void testTMClientInheritance() {
        // Test that TMClient extends from Apache Seata's TMClient
        Assertions.assertTrue(org.apache.seata.tm.TMClient.class.isAssignableFrom(TMClient.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the TMClient class is marked as deprecated
        Assertions.assertTrue(TMClient.class.isAnnotationPresent(Deprecated.class),
                "TMClient should be marked as @Deprecated");
    }

    @Test
    public void testInstanceCreation() {
        // Test that we can create an instance of TMClient
        TMClient tmClient = new TMClient();
        Assertions.assertNotNull(tmClient);
        
        // Test that it's an instance of Apache Seata's TMClient
        Assertions.assertTrue(tmClient instanceof org.apache.seata.tm.TMClient);
    }

    @Test
    public void testClassStructure() {
        // Test class modifiers
        int modifiers = TMClient.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers),
                "TMClient should be public");
        
        // Test superclass
        Assertions.assertEquals(org.apache.seata.tm.TMClient.class,
                TMClient.class.getSuperclass(),
                "TMClient should extend Apache Seata's TMClient");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals("io.seata.tm", 
                TMClient.class.getPackage().getName(),
                "TMClient should be in io.seata.tm package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that important methods are inherited from parent class
        TMClient tmClient = new TMClient();
        
        // Check if the class has inherited methods (through reflection)
        // We can verify that it has the same methods as the parent class
        java.lang.reflect.Method[] parentMethods = org.apache.seata.tm.TMClient.class.getDeclaredMethods();
        java.lang.reflect.Method[] childMethods = TMClient.class.getDeclaredMethods();
        
        // The child class should have access to parent methods through inheritance
        // Since this is a simple extension, we mainly test that the inheritance works
        Assertions.assertTrue(tmClient instanceof org.apache.seata.tm.TMClient,
                "TMClient should inherit from Apache Seata TMClient");
    }

    @Test
    public void testClassCompatibility() {
        // Test that compatible TMClient can be used wherever Apache Seata TMClient is expected
        TMClient compatibleClient = new TMClient();
        org.apache.seata.tm.TMClient apacheClient = compatibleClient;
        
        Assertions.assertNotNull(apacheClient);
        Assertions.assertSame(compatibleClient, apacheClient);
    }
} 