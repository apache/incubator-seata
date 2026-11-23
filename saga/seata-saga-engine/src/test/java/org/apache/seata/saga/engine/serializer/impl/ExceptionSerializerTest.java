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
package org.apache.seata.saga.engine.serializer.impl;

import com.test.MaliciousPayload;
import org.apache.seata.common.exception.SeataRuntimeException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionSerializerTest {

    @Test
    void testSerializeAndDeserializeException() {
        ExceptionSerializer serializer = new ExceptionSerializer();
        RuntimeException original = new RuntimeException("test error");

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);

        Exception deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertEquals("test error", deserialized.getMessage());
        assertTrue(deserialized instanceof RuntimeException);
    }

    @Test
    void testDeserializeNull() {
        ExceptionSerializer serializer = new ExceptionSerializer();
        assertNull(serializer.deserialize(null));
    }

    @Test
    void testSerializeNull() {
        assertNull(ExceptionSerializer.serializeByObjectOutput(null));
    }

    @Test
    void testIsClassAllowed_javaLang() {
        assertTrue(ExceptionSerializer.isClassAllowed("java.lang.RuntimeException"));
    }

    @Test
    void testIsClassAllowed_seataPackage() {
        assertTrue(
                ExceptionSerializer.isClassAllowed("org.apache.seata.saga.engine.exception.EngineExecutionException"));
    }

    @Test
    void testIsClassAllowed_byteArray() {
        assertTrue(ExceptionSerializer.isClassAllowed("[B"));
    }

    @Test
    void testIsClassAllowed_allowsJavaArray() {
        assertTrue(ExceptionSerializer.isClassAllowed("[Ljava.lang.StackTraceElement;"));
    }

    @Test
    void testIsClassAllowed_allowsPrimitiveArray() {
        assertTrue(ExceptionSerializer.isClassAllowed("[B"));
        assertTrue(ExceptionSerializer.isClassAllowed("[I"));
    }

    @Test
    void testIsClassAllowed_blocksUnknownClass() {
        assertFalse(ExceptionSerializer.isClassAllowed("com.evil.MaliciousPayload"));
    }

    @Test
    void testIsClassAllowed_blocksUnknownArray() {
        assertFalse(ExceptionSerializer.isClassAllowed("[Lcom.evil.MaliciousPayload;"));
    }

    @Test
    void testIsClassAllowed_blocksCommonGadgetChain() {
        assertFalse(ExceptionSerializer.isClassAllowed("org.apache.commons.collections.functors.InvokerTransformer"));
    }

    @Test
    void testDeserializeBlocksMaliciousClass() throws Exception {
        MaliciousPayload payload = new MaliciousPayload();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(payload);
        }
        byte[] maliciousBytes = baos.toByteArray();

        assertThrows(
                SeataRuntimeException.class, () -> ExceptionSerializer.deserializeByObjectInputStream(maliciousBytes));
    }

    @Test
    void testDeserializeWithTypeBlocksMaliciousClass() throws Exception {
        MaliciousPayload payload = new MaliciousPayload();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(payload);
        }
        byte[] maliciousBytes = baos.toByteArray();

        assertThrows(
                SeataRuntimeException.class,
                () -> ExceptionSerializer.deserializeByObjectInputStream(maliciousBytes, Exception.class));
    }
}
