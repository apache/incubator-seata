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
package io.seata.core.serializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Unit test for Serializer interface
 */
public class SerializerTest {

    /**
     * Mock implementation for testing
     */
    private static class MockSerializer implements Serializer {
        @Override
        public <T> byte[] serialize(T t) {
            if (t == null) {
                return new byte[0];
            }
            return t.toString().getBytes(); // Simple mock implementation
        }

        @Override
        public <T> T deserialize(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return (T) new String(bytes); // Simple mock implementation
        }
    }

    @Test
    public void testSerializerInterfaceInheritance() {
        // Test that Serializer extends from Apache Seata's Serializer
        Assertions.assertTrue(org.apache.seata.core.serializer.Serializer.class
                .isAssignableFrom(Serializer.class));
    }

    @Test
    public void testDeprecationAnnotation() {
        // Test that the Serializer interface is marked as deprecated
        Assertions.assertTrue(Serializer.class.isAnnotationPresent(Deprecated.class),
                "Serializer should be marked as @Deprecated");
    }

    @Test
    public void testInterfaceStructure() {
        // Test interface modifiers
        int modifiers = Serializer.class.getModifiers();
        Assertions.assertTrue(java.lang.reflect.Modifier.isInterface(modifiers),
                "Serializer should be an interface");
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(modifiers),
                "Serializer should be public");
    }

    @Test
    public void testPackageName() {
        // Test that the package is the expected compatible package
        Assertions.assertEquals("io.seata.core.serializer", 
                Serializer.class.getPackage().getName(),
                "Serializer should be in io.seata.core.serializer package");
    }

    @Test
    public void testMethodInheritance() throws Exception {
        // Test that the interface has the expected methods from the parent interface
        MockSerializer mockSerializer = new MockSerializer();
        
        // Test that the mock serializer implements both interfaces
        Assertions.assertTrue(mockSerializer instanceof Serializer);
        Assertions.assertTrue(mockSerializer instanceof org.apache.seata.core.serializer.Serializer);
    }

    @Test
    public void testInterfaceCompatibility() {
        // Test that compatible Serializer can be used wherever Apache Seata Serializer is expected
        MockSerializer compatibleSerializer = new MockSerializer();
        org.apache.seata.core.serializer.Serializer apacheSerializer = compatibleSerializer;
        
        Assertions.assertNotNull(apacheSerializer);
        Assertions.assertSame(compatibleSerializer, apacheSerializer);
    }

    @Test
    public void testImplementationFunctionality() {
        // Test basic functionality of a mock implementation
        MockSerializer serializer = new MockSerializer();
        
        String testObject = "Hello World";
        
        // Test serialize method
        byte[] serialized = serializer.serialize(testObject);
        Assertions.assertNotNull(serialized);
        Assertions.assertTrue(serialized.length > 0);
        
        // Test deserialize method
        String deserialized = serializer.deserialize(serialized);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(testObject, deserialized);
    }

    @Test
    public void testInterfaceMethods() throws Exception {
        // Test that the interface has the expected method signatures
        java.lang.reflect.Method serializeMethod = Serializer.class.getMethod("serialize", Object.class);
        Assertions.assertNotNull(serializeMethod);
        Assertions.assertEquals(byte[].class, serializeMethod.getReturnType());
        
        java.lang.reflect.Method deserializeMethod = Serializer.class.getMethod("deserialize", byte[].class);
        Assertions.assertNotNull(deserializeMethod);
        Assertions.assertEquals(Object.class, deserializeMethod.getReturnType());
    }

    @Test
    public void testPolymorphism() {
        // Test polymorphic behavior
        MockSerializer mockSerializer = new MockSerializer();
        Serializer serializer = mockSerializer;
        Object obj = serializer;
        
        Assertions.assertTrue(obj instanceof org.apache.seata.core.serializer.Serializer);
        Assertions.assertTrue(obj instanceof Serializer);
    }

    @Test
    public void testGenericMethods() {
        // Test generic method usage
        MockSerializer serializer = new MockSerializer();
        
        // Test with different types
        Integer intValue = 42;
        byte[] intSerialized = serializer.serialize(intValue);
        String intDeserialized = serializer.deserialize(intSerialized);
        Assertions.assertEquals("42", intDeserialized);
        
        // Test with null
        byte[] nullSerialized = serializer.serialize(null);
        Assertions.assertNotNull(nullSerialized);
        Assertions.assertEquals(0, nullSerialized.length);
        
        String nullDeserialized = serializer.deserialize(null);
        Assertions.assertNull(nullDeserialized);
    }

    @Test
    public void testMethodSignatures() {
        // Test that all required methods are present with correct signatures
        java.lang.reflect.Method[] methods = Serializer.class.getMethods();
        
        boolean hasSerialize = false, hasDeserialize = false;
        
        for (java.lang.reflect.Method method : methods) {
            if (method.getName().equals("serialize") && method.getParameterCount() == 1) {
                hasSerialize = true;
            } else if (method.getName().equals("deserialize") && method.getParameterCount() == 1) {
                hasDeserialize = true;
            }
        }
        
        Assertions.assertTrue(hasSerialize, "Serializer should have serialize method");
        Assertions.assertTrue(hasDeserialize, "Serializer should have deserialize method");
    }
} 