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
package org.apache.seata.rm.tcc.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for LocalTCCRemotingParser
 * Tests the @LocalTCC annotation support for TCC transaction mode
 *
 * Test scenarios covered:
 * 1. @LocalTCC annotation on implementation class
 * 2. @LocalTCC annotation on interface with implementation
 * 3. No annotations (negative test cases)
 * 4. Multiple interfaces with different annotation patterns
 *
 * The tests verify:
 * - Annotation detection accuracy for @LocalTCC
 * - Service/reference identification
 * - RemotingDesc generation correctness
 * - TCC-specific functionality
 */
public class LocalTCCRemotingParserTest {

    private LocalTCCRemotingParser parser;

    // Test classes with different annotation patterns
    @LocalTCC
    public static class LocalTCCService {
        public String doSomething() {
            return "local-tcc";
        }
    }

    public static class NoAnnotationService {
        public String doSomething() {
            return "none";
        }
    }

    @LocalTCC
    public interface LocalTCCInterface {
        String doSomething();
    }

    public interface NoAnnotationInterface {
        String doSomething();
    }

    public static class LocalTCCInterfaceImpl implements LocalTCCInterface {
        @Override
        public String doSomething() {
            return "impl-local-tcc";
        }
    }

    public static class NoAnnotationInterfaceImpl implements NoAnnotationInterface {
        @Override
        public String doSomething() {
            return "impl-none";
        }
    }

    // Test class with multiple interfaces
    @LocalTCC
    public interface AnotherLocalTCCInterface {
        String doAnotherThing();
    }

    public static class MultipleInterfaceImpl implements LocalTCCInterface, NoAnnotationInterface {
        @Override
        public String doSomething() {
            return "multiple-interfaces";
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new LocalTCCRemotingParser();
    }

    @Test
    public void testIsReference_LocalTCC() {
        LocalTCCService service = new LocalTCCService();
        assertTrue(parser.isReference(service, "localTCCService"));
    }

    @Test
    public void testIsReference_NoAnnotations() {
        NoAnnotationService service = new NoAnnotationService();
        assertFalse(parser.isReference(service, "noAnnotationService"));
    }

    @Test
    public void testIsService_LocalTCC() {
        LocalTCCService service = new LocalTCCService();
        assertTrue(parser.isService(service, "localTCCService"));
    }

    @Test
    public void testIsService_NoAnnotations() {
        NoAnnotationService service = new NoAnnotationService();
        assertFalse(parser.isService(service, "noAnnotationService"));
    }

    @Test
    public void testIsService_Class_LocalTCC() throws FrameworkException {
        assertTrue(parser.isService(LocalTCCService.class));
    }

    @Test
    public void testIsService_Class_NoAnnotations() throws FrameworkException {
        assertFalse(parser.isService(NoAnnotationService.class));
    }

    @Test
    public void testGetServiceDesc_LocalTCCOnImplementation() throws FrameworkException {
        LocalTCCService service = new LocalTCCService();
        RemotingDesc desc = parser.getServiceDesc(service, "localTCCService");

        assertRemotingDescWithServiceClass(desc, service, LocalTCCService.class);
    }

    @Test
    public void testGetServiceDesc_LocalTCCOnInterface() throws FrameworkException {
        LocalTCCInterfaceImpl service = new LocalTCCInterfaceImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "localTCCInterfaceImpl");

        assertRemotingDescWithServiceClass(desc, service, LocalTCCInterface.class);
    }

    @Test
    public void testGetServiceDesc_NoAnnotations_ReturnsNull() {
        NoAnnotationService service = new NoAnnotationService();
        RemotingDesc desc = parser.getServiceDesc(service, "noAnnotationService");

        assertNull(desc);
    }

    @Test
    public void testGetServiceDesc_NoAnnotationsOnInterface_ReturnsNull() {
        NoAnnotationInterfaceImpl service = new NoAnnotationInterfaceImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "noAnnotationInterfaceImpl");

        assertNull(desc);
    }

    @Test
    public void testGetProtocol() {
        assertEquals(Protocols.IN_JVM, parser.getProtocol());
    }

    @Test
    public void testMultipleInterfacesWithLocalTCC() throws FrameworkException {
        MultipleInterfaceImpl service = new MultipleInterfaceImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "multipleInterfaceImpl");

        assertNotNull(desc);
        assertValidRemotingDesc(desc, service);

        // Should detect the @LocalTCC annotation on LocalTCCInterface
        assertTrue(isAnnotatedInterface(desc.getServiceClass()));
        assertEquals(LocalTCCInterface.class, desc.getServiceClass());
    }

    @Test
    public void testIsRemoting_LocalTCC() {
        LocalTCCService service = new LocalTCCService();
        assertTrue(parser.isRemoting(service, "localTCCService"));
    }

    @Test
    public void testIsRemoting_NoAnnotations() {
        NoAnnotationService service = new NoAnnotationService();
        assertFalse(parser.isRemoting(service, "noAnnotationService"));
    }

    private boolean isAnnotatedInterface(Class<?> clazz) {
        return clazz.isAnnotationPresent(LocalTCC.class);
    }

    private void assertValidRemotingDesc(RemotingDesc desc, Object expectedTargetBean) {
        assertNotNull(desc);
        assertTrue(desc.isReference());
        assertTrue(desc.isService());
        assertEquals(Protocols.IN_JVM, desc.getProtocol());
        assertEquals(expectedTargetBean, desc.getTargetBean());
        assertNotNull(desc.getServiceClass());
        assertNotNull(desc.getServiceClassName());
    }

    private void assertRemotingDescWithServiceClass(
            RemotingDesc desc, Object targetBean, Class<?> expectedServiceClass) {
        assertValidRemotingDesc(desc, targetBean);
        assertEquals(expectedServiceClass, desc.getServiceClass());
        assertEquals(expectedServiceClass.getName(), desc.getServiceClassName());
    }
}
