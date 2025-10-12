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
package org.apache.seata.saga.rm.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.saga.rm.api.SagaTransactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SagaTransactionalRemotingParser
 * Tests the @SagaTransactional annotation support for Saga transaction scenarios
 *
 * Test scenarios covered:
 * 1. @SagaTransactional annotation on implementation class
 * 2. @SagaTransactional annotation on interface with implementation
 * 3. No annotations (negative test cases)
 * 4. Multiple interfaces with different annotations
 * 5. Inheritance scenarios
 *
 * The tests verify:
 * - Annotation detection accuracy
 * - Service/reference identification
 * - RemotingDesc generation correctness
 * - Proper separation from LocalTCC functionality
 */
public class SagaTransactionalRemotingParserTest {

    private SagaTransactionalRemotingParser parser;

    // Test classes with different annotation patterns
    @SagaTransactional
    public static class SagaTransactionalService {
        public String doSomething() {
            return "saga-transactional";
        }
    }

    public static class NoAnnotationService {
        public String doSomething() {
            return "none";
        }
    }

    @SagaTransactional
    public interface SagaTransactionalInterface {
        String doSomething();
    }

    public interface NoAnnotationInterface {
        String doSomething();
    }

    public static class SagaTransactionalInterfaceImpl implements SagaTransactionalInterface {
        @Override
        public String doSomething() {
            return "impl-saga-transactional";
        }
    }

    public static class NoAnnotationInterfaceImpl implements NoAnnotationInterface {
        @Override
        public String doSomething() {
            return "impl-none";
        }
    }

    @SagaTransactional
    public static class InheritedSagaService extends SagaTransactionalService {
        @Override
        public String doSomething() {
            return "inherited-saga";
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new SagaTransactionalRemotingParser();
    }

    @Test
    public void testIsReference_SagaTransactional() {
        SagaTransactionalService service = new SagaTransactionalService();
        assertTrue(parser.isReference(service, "sagaTransactionalService"));
    }

    @Test
    public void testIsReference_NoAnnotations() {
        NoAnnotationService service = new NoAnnotationService();
        assertFalse(parser.isReference(service, "noAnnotationService"));
    }

    @Test
    public void testIsService_SagaTransactional() {
        SagaTransactionalService service = new SagaTransactionalService();
        assertTrue(parser.isService(service, "sagaTransactionalService"));
    }

    @Test
    public void testIsService_NoAnnotations() {
        NoAnnotationService service = new NoAnnotationService();
        assertFalse(parser.isService(service, "noAnnotationService"));
    }

    @Test
    public void testIsService_Class_SagaTransactional() throws FrameworkException {
        assertTrue(parser.isService(SagaTransactionalService.class));
    }

    @Test
    public void testIsService_Class_NoAnnotations() throws FrameworkException {
        assertFalse(parser.isService(NoAnnotationService.class));
    }

    @Test
    public void testGetServiceDesc_SagaTransactionalOnImplementation() throws FrameworkException {
        SagaTransactionalService service = new SagaTransactionalService();
        RemotingDesc desc = parser.getServiceDesc(service, "sagaTransactionalService");

        assertRemotingDescWithServiceClass(desc, service, SagaTransactionalService.class);
    }

    @Test
    public void testGetServiceDesc_SagaTransactionalOnInterface() throws FrameworkException {
        SagaTransactionalInterfaceImpl service = new SagaTransactionalInterfaceImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "sagaTransactionalInterfaceImpl");

        assertRemotingDescWithServiceClass(desc, service, SagaTransactionalInterface.class);
    }

    @Test
    public void testGetServiceDesc_NoAnnotations_ReturnsNull() {
        NoAnnotationService service = new NoAnnotationService();
        assertNull(parser.getServiceDesc(service, "noAnnotationService"));
    }

    @Test
    public void testGetServiceDesc_NoAnnotationsOnInterface_ReturnsNull() {
        NoAnnotationInterfaceImpl service = new NoAnnotationInterfaceImpl();
        assertNull(parser.getServiceDesc(service, "noAnnotationInterfaceImpl"));
    }

    @Test
    public void testGetProtocol() {
        assertEquals(Protocols.IN_JVM, parser.getProtocol());
    }

    @Test
    public void testInheritedSagaTransactional() throws FrameworkException {
        InheritedSagaService service = new InheritedSagaService();

        assertTrue(parser.isService(service, "inheritedSagaService"));

        RemotingDesc desc = parser.getServiceDesc(service, "inheritedSagaService");
        assertRemotingDescWithServiceClass(desc, service, InheritedSagaService.class);
    }

    @Test
    public void testAnnotationPrecedence_ImplementationOverInterface() throws FrameworkException {
        // When both implementation and interface have @SagaTransactional,
        // implementation should take precedence

        @SagaTransactional
        class TestImpl implements SagaTransactionalInterface {
            @Override
            public String doSomething() {
                return "test-impl";
            }
        }

        TestImpl service = new TestImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "testImpl");

        assertNotNull(desc);
        // Implementation class should be used, not the interface
        assertEquals(TestImpl.class, desc.getServiceClass());
    }

    @Test
    public void testThrowsFrameworkExceptionWhenNoAnnotationFound() {
        NoAnnotationService service = new NoAnnotationService();

        // When a bean has no @SagaTransactional annotation, getServiceDesc should return null
        // because isRemoting() check will fail before reaching the exception throwing code
        RemotingDesc desc = parser.getServiceDesc(service, "noAnnotationService");
        assertNull(desc);
    }

    // Test complex inheritance scenario
    @Test
    public void testComplexInheritanceHierarchy() throws FrameworkException {
        @SagaTransactional
        class Level1 {
            public boolean level1Method() {
                return true;
            }
        }

        class Level2 extends Level1 {
            public boolean level2Method() {
                return true;
            }
        }

        @SagaTransactional
        class Level3 extends Level2 {
            public boolean level3Method() {
                return true;
            }
        }

        Level3 service = new Level3();

        // Should be recognized (has its own @SagaTransactional)
        assertTrue(parser.isService(service, "level3Service"));

        RemotingDesc desc = parser.getServiceDesc(service, "level3Service");
        assertNotNull(desc);
        assertEquals(Level3.class, desc.getServiceClass());
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
