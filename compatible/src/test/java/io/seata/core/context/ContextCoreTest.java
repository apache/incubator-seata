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
package io.seata.core.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for ContextCore interface
 */
public class ContextCoreTest {

    @Test
    public void testContextCoreInterfaceInheritance() {
        // Test that ContextCore extends from Apache Seata's ContextCore
        Assertions.assertTrue(org.apache.seata.core.context.ContextCore.class.isAssignableFrom(ContextCore.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the ContextCore interface is marked as deprecated
        Assertions.assertTrue(
                ContextCore.class.isAnnotationPresent(Deprecated.class), "ContextCore should be marked as @Deprecated");
    }

    @Test
    public void testInterfaceStructure() {
        // Test interface modifiers
        int modifiers = ContextCore.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isInterface(modifiers), "ContextCore should be an interface");
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "ContextCore should be public");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals(
                "io.seata.core.context",
                ContextCore.class.getPackage().getName(),
                "ContextCore should be in io.seata.core.context package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that the interface extends from the parent interface
        Assertions.assertTrue(org.apache.seata.core.context.ContextCore.class.isAssignableFrom(ContextCore.class));
    }

    @Test
    public void testInterfaceCompatibility() {
        // Test that compatible ContextCore is assignable from Apache Seata ContextCore
        Assertions.assertTrue(org.apache.seata.core.context.ContextCore.class.isAssignableFrom(ContextCore.class));
    }

    @Test
    public void testInterfaceMethods() throws Exception {
        // Test that the interface has inherited the expected methods from parent interface
        java.lang.reflect.Method[] parentMethods = org.apache.seata.core.context.ContextCore.class.getMethods();
        java.lang.reflect.Method[] childMethods = ContextCore.class.getMethods();

        // The child interface should have access to parent methods through inheritance
        Assertions.assertTrue(parentMethods.length > 0, "Parent interface should have methods");
        Assertions.assertTrue(
                childMethods.length >= parentMethods.length, "Child interface should inherit parent methods");
    }
}
