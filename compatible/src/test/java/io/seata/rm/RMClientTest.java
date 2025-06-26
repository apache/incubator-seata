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
package io.seata.rm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for RMClient class
 */
public class RMClientTest {

    @Test
    public void testRMClientInheritance() {
        // Test that RMClient extends from Apache Seata's RMClient
        Assertions.assertTrue(org.apache.seata.rm.RMClient.class.isAssignableFrom(RMClient.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the RMClient class is marked as deprecated
        Assertions.assertTrue(
                RMClient.class.isAnnotationPresent(Deprecated.class), "RMClient should be marked as @Deprecated");
    }

    @Test
    public void testInstanceCreation() {
        // Test that we can create an instance of RMClient
        RMClient rmClient = new RMClient();
        Assertions.assertNotNull(rmClient);

        // Test that it's an instance of Apache Seata's RMClient
        Assertions.assertTrue(rmClient instanceof org.apache.seata.rm.RMClient);
    }

    @Test
    public void testClassStructure() {
        // Test class modifiers
        int modifiers = RMClient.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "RMClient should be public");

        // Test superclass
        Assertions.assertEquals(
                org.apache.seata.rm.RMClient.class,
                RMClient.class.getSuperclass(),
                "RMClient should extend Apache Seata's RMClient");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals(
                "io.seata.rm", RMClient.class.getPackage().getName(), "RMClient should be in io.seata.rm package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that important methods are inherited from parent class
        RMClient rmClient = new RMClient();

        // Check if the class has inherited methods (through reflection)
        // We can verify that it has the same methods as the parent class
        java.lang.reflect.Method[] parentMethods = org.apache.seata.rm.RMClient.class.getDeclaredMethods();
        java.lang.reflect.Method[] childMethods = RMClient.class.getDeclaredMethods();

        // The child class should have access to parent methods through inheritance
        // Since this is a simple extension, we mainly test that the inheritance works
        Assertions.assertTrue(
                rmClient instanceof org.apache.seata.rm.RMClient, "RMClient should inherit from Apache Seata RMClient");
    }

    @Test
    public void testClassCompatibility() {
        // Test that compatible RMClient can be used wherever Apache Seata RMClient is expected
        RMClient compatibleClient = new RMClient();
        org.apache.seata.rm.RMClient apacheClient = compatibleClient;

        Assertions.assertNotNull(apacheClient);
        Assertions.assertSame(compatibleClient, apacheClient);
    }

    @Test
    public void testPolymorphism() {
        // Test polymorphic behavior
        RMClient rmClient = new RMClient();
        Object obj = rmClient;

        Assertions.assertTrue(obj instanceof org.apache.seata.rm.RMClient);
        Assertions.assertTrue(obj instanceof RMClient);
    }
}
