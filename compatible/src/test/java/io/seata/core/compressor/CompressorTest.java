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
package io.seata.core.compressor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for Compressor interface
 */
public class CompressorTest {

    /**
     * Mock implementation for testing
     */
    private static class MockCompressor implements Compressor {
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes; // Simple mock implementation
        }

        @Override
        public byte[] decompress(byte[] bytes) {
            return bytes; // Simple mock implementation
        }
    }

    @Test
    public void testCompressorInterfaceInheritance() {
        // Test that Compressor extends from Apache Seata's Compressor
        Assertions.assertTrue(org.apache.seata.core.compressor.Compressor.class.isAssignableFrom(Compressor.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the Compressor interface is marked as deprecated
        Assertions.assertTrue(
                Compressor.class.isAnnotationPresent(Deprecated.class), "Compressor should be marked as @Deprecated");
    }

    @Test
    public void testInterfaceStructure() {
        // Test interface modifiers
        int modifiers = Compressor.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isInterface(modifiers), "Compressor should be an interface");
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "Compressor should be public");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals(
                "io.seata.core.compressor",
                Compressor.class.getPackage().getName(),
                "Compressor should be in io.seata.core.compressor package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that the interface has the expected methods from the parent interface
        MockCompressor mockCompressor = new MockCompressor();

        // Test that the mock compressor implements both interfaces
        Assertions.assertTrue(mockCompressor instanceof Compressor);
        Assertions.assertTrue(mockCompressor instanceof org.apache.seata.core.compressor.Compressor);
    }

    @Test
    public void testInterfaceCompatibility() {
        // Test that compatible Compressor can be used wherever Apache Seata Compressor is expected
        MockCompressor compatibleCompressor = new MockCompressor();
        org.apache.seata.core.compressor.Compressor apacheCompressor = compatibleCompressor;

        Assertions.assertNotNull(apacheCompressor);
        Assertions.assertSame(compatibleCompressor, apacheCompressor);
    }

    @Test
    public void testImplementationFunctionality() {
        // Test basic functionality of a mock implementation
        MockCompressor compressor = new MockCompressor();

        byte[] testData = "Hello World".getBytes();

        // Test compress method
        byte[] compressed = compressor.compress(testData);
        Assertions.assertNotNull(compressed);

        // Test decompress method
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertNotNull(decompressed);

        // In our mock implementation, data should be unchanged
        Assertions.assertArrayEquals(testData, decompressed);
    }

    @Test
    public void testInterfaceMethods() throws Exception {
        // Test that the interface has the expected method signatures
        java.lang.reflect.Method compressMethod = Compressor.class.getMethod("compress", byte[].class);
        Assertions.assertNotNull(compressMethod);
        Assertions.assertEquals(byte[].class, compressMethod.getReturnType());

        java.lang.reflect.Method decompressMethod = Compressor.class.getMethod("decompress", byte[].class);
        Assertions.assertNotNull(decompressMethod);
        Assertions.assertEquals(byte[].class, decompressMethod.getReturnType());
    }

    @Test
    public void testPolymorphism() {
        // Test polymorphic behavior
        MockCompressor mockCompressor = new MockCompressor();
        Compressor compressor = mockCompressor;
        Object obj = compressor;

        Assertions.assertTrue(obj instanceof org.apache.seata.core.compressor.Compressor);
        Assertions.assertTrue(obj instanceof Compressor);
    }
}
