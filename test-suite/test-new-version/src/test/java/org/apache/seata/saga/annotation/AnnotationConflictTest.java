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
package org.apache.seata.saga.annotation;

import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.remoting.parser.LocalTCCRemotingParser;
import org.apache.seata.saga.rm.api.SagaTransactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for LocalTCCRemotingParser behavior with different annotation scenarios
 *
 * This test suite validates that LocalTCCRemotingParser:
 * 1. ONLY recognizes @LocalTCC annotations (its primary responsibility)
 * 2. IGNORES @SagaTransactional annotations (not its responsibility)
 * 3. Handles inheritance correctly for @LocalTCC
 * 4. Handles interface vs implementation scenarios for @LocalTCC
 * 5. Properly handles edge cases and null scenarios
 *
 * Note: @SagaTransactional annotations should be handled by SagaTransactionalRemotingParser,
 * not by LocalTCCRemotingParser. This test verifies proper separation of concerns.
 */
public class AnnotationConflictTest {

    private LocalTCCRemotingParser parser;

    // Valid @LocalTCC scenarios
    @LocalTCC
    public static class LocalTCCService {
        public boolean doSomething() {
            return true;
        }
    }

    @LocalTCC
    public interface LocalTCCInterface {
        boolean doSomething();
    }

    public static class LocalTCCInterfaceImpl implements LocalTCCInterface {
        @Override
        public boolean doSomething() {
            return true;
        }
    }

    // @SagaTransactional scenarios (should be IGNORED by LocalTCCRemotingParser)
    @SagaTransactional
    public static class SagaTransactionalService {
        public boolean doSomething() {
            return true;
        }
    }

    @SagaTransactional
    public interface SagaTransactionalInterface {
        boolean doSomething();
    }

    public static class SagaTransactionalInterfaceImpl implements SagaTransactionalInterface {
        @Override
        public boolean doSomething() {
            return true;
        }
    }

    // Mixed scenarios - implementation vs interface
    @LocalTCC
    public static class LocalTCCImpl implements SagaTransactionalInterface {
        @Override
        public boolean doSomething() {
            return true;
        }
    }

    @SagaTransactional
    public static class SagaImpl implements LocalTCCInterface {
        @Override
        public boolean doSomething() {
            return true;
        }
    }

    // Inheritance scenarios
    @LocalTCC
    public static class BaseLocalTCCService {
        public boolean baseMethod() {
            return true;
        }
    }

    public static class ExtendedLocalTCCService extends BaseLocalTCCService {
        public boolean extendedMethod() {
            return true;
        }
    }

    @SagaTransactional
    public static class BaseSagaService {
        public boolean baseMethod() {
            return true;
        }
    }

    public static class ExtendedSagaService extends BaseSagaService {
        public boolean extendedMethod() {
            return true;
        }
    }

    // No annotation scenarios
    public static class NoAnnotationService {
        public boolean doSomething() {
            return true;
        }
    }

    public interface NoAnnotationInterface {
        boolean doSomething();
    }

    public static class NoAnnotationInterfaceImpl implements NoAnnotationInterface {
        @Override
        public boolean doSomething() {
            return true;
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new LocalTCCRemotingParser();
    }

    // Tests for @LocalTCC recognition (should work)
    @Test
    public void testLocalTCCOnClass_ShouldBeRecognized() {
        LocalTCCService service = new LocalTCCService();

        assertTrue(parser.isService(service, "localTCCService"));
        assertTrue(parser.isReference(service, "localTCCService"));

        RemotingDesc desc = parser.getServiceDesc(service, "localTCCService");
        assertNotNull(desc);
        assertEquals(LocalTCCService.class, desc.getServiceClass());
    }

    @Test
    public void testLocalTCCOnInterface_ShouldBeRecognized() {
        LocalTCCInterfaceImpl service = new LocalTCCInterfaceImpl();

        assertTrue(parser.isService(service, "localTCCInterfaceImpl"));

        RemotingDesc desc = parser.getServiceDesc(service, "localTCCInterfaceImpl");
        assertNotNull(desc);
        assertEquals(LocalTCCInterface.class, desc.getServiceClass());
    }

    @Test
    public void testLocalTCCInheritance_ShouldBeRecognized() {
        ExtendedLocalTCCService service = new ExtendedLocalTCCService();

        // Should inherit @LocalTCC from parent
        assertTrue(parser.isService(service, "extendedLocalTCCService"));

        RemotingDesc desc = parser.getServiceDesc(service, "extendedLocalTCCService");
        assertNotNull(desc);
        assertEquals(ExtendedLocalTCCService.class, desc.getServiceClass());
    }

    // Tests for @SagaTransactional scenarios (should be IGNORED)
    @Test
    public void testSagaTransactionalOnClass_ShouldBeIgnored() {
        SagaTransactionalService service = new SagaTransactionalService();

        // LocalTCCRemotingParser should NOT recognize @SagaTransactional
        assertFalse(parser.isService(service, "sagaTransactionalService"));
        assertFalse(parser.isReference(service, "sagaTransactionalService"));

        RemotingDesc desc = parser.getServiceDesc(service, "sagaTransactionalService");
        assertNull(desc);
    }

    @Test
    public void testSagaTransactionalOnInterface_ShouldBeIgnored() {
        SagaTransactionalInterfaceImpl service = new SagaTransactionalInterfaceImpl();

        // LocalTCCRemotingParser should NOT recognize @SagaTransactional
        assertFalse(parser.isService(service, "sagaTransactionalInterfaceImpl"));

        RemotingDesc desc = parser.getServiceDesc(service, "sagaTransactionalInterfaceImpl");
        assertNull(desc);
    }

    @Test
    public void testSagaTransactionalInheritance_ShouldBeIgnored() {
        ExtendedSagaService service = new ExtendedSagaService();

        // LocalTCCRemotingParser should NOT recognize inherited @SagaTransactional
        assertFalse(parser.isService(service, "extendedSagaService"));

        RemotingDesc desc = parser.getServiceDesc(service, "extendedSagaService");
        assertNull(desc);
    }

    // Tests for mixed scenarios
    @Test
    public void testLocalTCCImplWithSagaTransactionalInterface_ShouldRecognizeLocalTCC() {
        LocalTCCImpl service = new LocalTCCImpl();

        // Should recognize @LocalTCC on implementation, ignore @SagaTransactional on interface
        assertTrue(parser.isService(service, "localTCCImpl"));

        RemotingDesc desc = parser.getServiceDesc(service, "localTCCImpl");
        assertNotNull(desc);
        // Implementation class should be used (has @LocalTCC)
        assertEquals(LocalTCCImpl.class, desc.getServiceClass());
    }

    @Test
    public void testSagaImplWithLocalTCCInterface_ShouldRecognizeInterfaceLocalTCC() {
        SagaImpl service = new SagaImpl();

        // Should recognize @LocalTCC on interface, ignore @SagaTransactional on implementation
        assertTrue(parser.isService(service, "sagaImpl"));

        RemotingDesc desc = parser.getServiceDesc(service, "sagaImpl");
        assertNotNull(desc);
        // Interface should be used (has @LocalTCC)
        assertEquals(LocalTCCInterface.class, desc.getServiceClass());
    }

    // Tests for no annotation scenarios
    @Test
    public void testNoAnnotations_ShouldNotBeRecognized() {
        NoAnnotationService service = new NoAnnotationService();

        assertFalse(parser.isService(service, "noAnnotationService"));
        assertFalse(parser.isReference(service, "noAnnotationService"));

        RemotingDesc desc = parser.getServiceDesc(service, "noAnnotationService");
        assertNull(desc);
    }

    @Test
    public void testNoAnnotationInterface_ShouldNotBeRecognized() {
        NoAnnotationInterfaceImpl service = new NoAnnotationInterfaceImpl();

        assertFalse(parser.isService(service, "noAnnotationInterfaceImpl"));

        RemotingDesc desc = parser.getServiceDesc(service, "noAnnotationInterfaceImpl");
        assertNull(desc);
    }

    // Edge case tests
    @Test
    public void testNullService_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> {
            parser.isService(null, "nullService");
        });

        assertThrows(RuntimeException.class, () -> {
            parser.getServiceDesc(null, "nullService");
        });
    }

    @Test
    public void testNullBeanName_ShouldNotThrowException() {
        LocalTCCService service = new LocalTCCService();

        assertDoesNotThrow(() -> {
            boolean result = parser.isService(service, null);
            assertTrue(result);
        });

        assertDoesNotThrow(() -> {
            RemotingDesc desc = parser.getServiceDesc(service, null);
            assertNotNull(desc);
        });
    }

    @Test
    public void testEmptyBeanName_ShouldNotThrowException() {
        LocalTCCService service = new LocalTCCService();

        assertDoesNotThrow(() -> {
            boolean result = parser.isService(service, "");
            assertTrue(result);
        });

        assertDoesNotThrow(() -> {
            RemotingDesc desc = parser.getServiceDesc(service, "");
            assertNotNull(desc);
        });
    }

    @Test
    public void testAnnotationPrecedence_ImplementationOverInterface() {
        // When implementation has @LocalTCC and interface has @SagaTransactional,
        // implementation should take precedence

        LocalTCCImpl service = new LocalTCCImpl();
        RemotingDesc desc = parser.getServiceDesc(service, "testImpl");

        assertNotNull(desc);
        // Implementation class should be used (has @LocalTCC)
        assertEquals(LocalTCCImpl.class, desc.getServiceClass());
    }

    @Test
    public void testClassHierarchyWithLocalTCC() {
        // Test inheritance chain with @LocalTCC
        @LocalTCC
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

        class Level3 extends Level2 {
            public boolean level3Method() {
                return true;
            }
        }

        Level3 service = new Level3();

        // Should be recognized (inherits @LocalTCC)
        assertTrue(parser.isService(service, "level3Service"));

        RemotingDesc desc = parser.getServiceDesc(service, "level3Service");
        assertNotNull(desc);
        assertEquals(Level3.class, desc.getServiceClass());
    }
}
