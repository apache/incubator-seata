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
import org.apache.seata.saga.rm.remoting.parser.SagaTransactionalRemotingParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for dual parser system (LocalTCCRemotingParser + SagaTransactionalRemotingParser)
 *
 * This test suite validates that both parsers can work together correctly in the same application:
 *
 * Key scenarios covered:
 * 1. LocalTCCRemotingParser correctly handles @LocalTCC and ignores @SagaTransactional
 * 2. SagaTransactionalRemotingParser correctly handles @SagaTransactional and ignores @LocalTCC
 * 3. Both parsers can work on the same service beans without conflicts
 * 4. Parser precedence and service detection work correctly
 * 5. Mixed annotation scenarios are handled properly by each parser
 * 6. No cross-contamination between parsers
 *
 * This ensures that the new @SagaTransactional annotation system works seamlessly
 * with existing @LocalTCC infrastructure without breaking changes.
 */
public class DualParserIntegrationTest {

    private LocalTCCRemotingParser localTCCParser;
    private SagaTransactionalRemotingParser sagaTransactionalParser;

    // Pure @LocalTCC scenarios
    @LocalTCC
    public static class PureLocalTCCService {
        public boolean doTccOperation() {
            return true;
        }
    }

    @LocalTCC
    public interface PureLocalTCCInterface {
        boolean doTccOperation();
    }

    public static class PureLocalTCCInterfaceImpl implements PureLocalTCCInterface {
        @Override
        public boolean doTccOperation() {
            return true;
        }
    }

    // Pure @SagaTransactional scenarios
    @SagaTransactional
    public static class PureSagaTransactionalService {
        public boolean doSagaOperation() {
            return true;
        }
    }

    @SagaTransactional
    public interface PureSagaTransactionalInterface {
        boolean doSagaOperation();
    }

    public static class PureSagaTransactionalInterfaceImpl implements PureSagaTransactionalInterface {
        @Override
        public boolean doSagaOperation() {
            return true;
        }
    }

    // Mixed scenarios - for comprehensive parser separation testing
    @LocalTCC
    public static class LocalTCCImplWithSagaInterface implements PureSagaTransactionalInterface {
        @Override
        public boolean doSagaOperation() {
            return true;
        }
    }

    @SagaTransactional
    public static class SagaImplWithLocalTCCInterface implements PureLocalTCCInterface {
        @Override
        public boolean doTccOperation() {
            return true;
        }
    }

    // No annotation control groups
    public static class NoAnnotationService {
        public boolean doSomething() {
            return true;
        }
    }

    @BeforeEach
    public void setUp() {
        localTCCParser = new LocalTCCRemotingParser();
        sagaTransactionalParser = new SagaTransactionalRemotingParser();
    }

    // Test LocalTCCRemotingParser behavior
    @Test
    public void testLocalTCCParser_ShouldOnlyRecognizeLocalTCC() {
        // Should recognize @LocalTCC
        PureLocalTCCService localTCCService = new PureLocalTCCService();
        assertTrue(localTCCParser.isService(localTCCService, "localTCCService"));
        assertTrue(localTCCParser.isReference(localTCCService, "localTCCService"));

        RemotingDesc localTCCDesc = localTCCParser.getServiceDesc(localTCCService, "localTCCService");
        assertNotNull(localTCCDesc);
        assertEquals(PureLocalTCCService.class, localTCCDesc.getServiceClass());

        // Should NOT recognize @SagaTransactional
        PureSagaTransactionalService sagaService = new PureSagaTransactionalService();
        assertFalse(localTCCParser.isService(sagaService, "sagaService"));
        assertFalse(localTCCParser.isReference(sagaService, "sagaService"));

        RemotingDesc sagaDesc = localTCCParser.getServiceDesc(sagaService, "sagaService");
        assertNull(sagaDesc);

        // Should NOT recognize no annotation
        NoAnnotationService noAnnotationService = new NoAnnotationService();
        assertFalse(localTCCParser.isService(noAnnotationService, "noAnnotationService"));
        assertNull(localTCCParser.getServiceDesc(noAnnotationService, "noAnnotationService"));
    }

    @Test
    public void testSagaTransactionalParser_ShouldOnlyRecognizeSagaTransactional() {
        // Should recognize @SagaTransactional
        PureSagaTransactionalService sagaService = new PureSagaTransactionalService();
        assertTrue(sagaTransactionalParser.isService(sagaService, "sagaService"));
        assertTrue(sagaTransactionalParser.isReference(sagaService, "sagaService"));

        RemotingDesc sagaDesc = sagaTransactionalParser.getServiceDesc(sagaService, "sagaService");
        assertNotNull(sagaDesc);
        assertEquals(PureSagaTransactionalService.class, sagaDesc.getServiceClass());

        // Should NOT recognize @LocalTCC
        PureLocalTCCService localTCCService = new PureLocalTCCService();
        assertFalse(sagaTransactionalParser.isService(localTCCService, "localTCCService"));
        assertFalse(sagaTransactionalParser.isReference(localTCCService, "localTCCService"));

        RemotingDesc localTCCDesc = sagaTransactionalParser.getServiceDesc(localTCCService, "localTCCService");
        assertNull(localTCCDesc);

        // Should NOT recognize no annotation
        NoAnnotationService noAnnotationService = new NoAnnotationService();
        assertFalse(sagaTransactionalParser.isService(noAnnotationService, "noAnnotationService"));
        assertNull(sagaTransactionalParser.getServiceDesc(noAnnotationService, "noAnnotationService"));
    }

    @Test
    public void testInterfaceAnnotationHandling_BothParsers() {
        // LocalTCC interface implementation
        PureLocalTCCInterfaceImpl localTCCImpl = new PureLocalTCCInterfaceImpl();

        // LocalTCCParser should recognize it
        assertTrue(localTCCParser.isService(localTCCImpl, "localTCCImpl"));
        RemotingDesc localTCCDesc = localTCCParser.getServiceDesc(localTCCImpl, "localTCCImpl");
        assertNotNull(localTCCDesc);
        assertEquals(PureLocalTCCInterface.class, localTCCDesc.getServiceClass());

        // SagaTransactionalParser should NOT recognize it
        assertFalse(sagaTransactionalParser.isService(localTCCImpl, "localTCCImpl"));
        assertNull(sagaTransactionalParser.getServiceDesc(localTCCImpl, "localTCCImpl"));

        // SagaTransactional interface implementation
        PureSagaTransactionalInterfaceImpl sagaImpl = new PureSagaTransactionalInterfaceImpl();

        // SagaTransactionalParser should recognize it
        assertTrue(sagaTransactionalParser.isService(sagaImpl, "sagaImpl"));
        RemotingDesc sagaDesc = sagaTransactionalParser.getServiceDesc(sagaImpl, "sagaImpl");
        assertNotNull(sagaDesc);
        assertEquals(PureSagaTransactionalInterface.class, sagaDesc.getServiceClass());

        // LocalTCCParser should NOT recognize it
        assertFalse(localTCCParser.isService(sagaImpl, "sagaImpl"));
        assertNull(localTCCParser.getServiceDesc(sagaImpl, "sagaImpl"));
    }

    @Test
    public void testMixedAnnotationScenarios_ParserPriority() {
        // @LocalTCC implementation with @SagaTransactional interface
        LocalTCCImplWithSagaInterface mixedService1 = new LocalTCCImplWithSagaInterface();

        // LocalTCCParser should recognize the @LocalTCC on implementation
        assertTrue(localTCCParser.isService(mixedService1, "mixedService1"));
        RemotingDesc desc1 = localTCCParser.getServiceDesc(mixedService1, "mixedService1");
        assertNotNull(desc1);
        assertEquals(LocalTCCImplWithSagaInterface.class, desc1.getServiceClass());

        // SagaTransactionalParser should recognize the @SagaTransactional on interface
        assertTrue(sagaTransactionalParser.isService(mixedService1, "mixedService1"));
        RemotingDesc desc2 = sagaTransactionalParser.getServiceDesc(mixedService1, "mixedService1");
        assertNotNull(desc2);
        assertEquals(PureSagaTransactionalInterface.class, desc2.getServiceClass());

        // @SagaTransactional implementation with @LocalTCC interface
        SagaImplWithLocalTCCInterface mixedService2 = new SagaImplWithLocalTCCInterface();

        // LocalTCCParser should recognize the @LocalTCC on interface
        assertTrue(localTCCParser.isService(mixedService2, "mixedService2"));
        RemotingDesc desc3 = localTCCParser.getServiceDesc(mixedService2, "mixedService2");
        assertNotNull(desc3);
        assertEquals(PureLocalTCCInterface.class, desc3.getServiceClass());

        // SagaTransactionalParser should recognize the @SagaTransactional on implementation
        assertTrue(sagaTransactionalParser.isService(mixedService2, "mixedService2"));
        RemotingDesc desc4 = sagaTransactionalParser.getServiceDesc(mixedService2, "mixedService2");
        assertNotNull(desc4);
        assertEquals(SagaImplWithLocalTCCInterface.class, desc4.getServiceClass());
    }

    @Test
    public void testBothParsersWorkIndependently() {
        // Create services for both parsers
        PureLocalTCCService localTCCService = new PureLocalTCCService();
        PureSagaTransactionalService sagaService = new PureSagaTransactionalService();
        NoAnnotationService noAnnotationService = new NoAnnotationService();

        // Test all combinations to ensure no cross-contamination

        // LocalTCC service
        assertTrue(localTCCParser.isService(localTCCService, "test"));
        assertFalse(sagaTransactionalParser.isService(localTCCService, "test"));

        // Saga service
        assertFalse(localTCCParser.isService(sagaService, "test"));
        assertTrue(sagaTransactionalParser.isService(sagaService, "test"));

        // No annotation service
        assertFalse(localTCCParser.isService(noAnnotationService, "test"));
        assertFalse(sagaTransactionalParser.isService(noAnnotationService, "test"));
    }

    @Test
    public void testParserProtocolConsistency() {
        // Both parsers should use the same protocol
        assertEquals(localTCCParser.getProtocol(), sagaTransactionalParser.getProtocol());
    }

    @Test
    public void testRemotingDescConsistency_BothParsers() {
        PureLocalTCCService localTCCService = new PureLocalTCCService();
        PureSagaTransactionalService sagaService = new PureSagaTransactionalService();

        RemotingDesc localTCCDesc = localTCCParser.getServiceDesc(localTCCService, "test");
        RemotingDesc sagaDesc = sagaTransactionalParser.getServiceDesc(sagaService, "test");

        // Both should have consistent RemotingDesc structure
        assertNotNull(localTCCDesc);
        assertNotNull(sagaDesc);

        // Both should indicate service and reference
        assertTrue(localTCCDesc.isService());
        assertTrue(localTCCDesc.isReference());
        assertTrue(sagaDesc.isService());
        assertTrue(sagaDesc.isReference());

        // Both should use same protocol
        assertEquals(localTCCDesc.getProtocol(), sagaDesc.getProtocol());

        // Both should have proper target bean references
        assertEquals(localTCCService, localTCCDesc.getTargetBean());
        assertEquals(sagaService, sagaDesc.getTargetBean());
    }

    @Test
    public void testNullBeanHandling_BothParsers() {
        // Both parsers should handle null beans consistently
        try {
            localTCCParser.isService(null, "test");
            assertTrue(false, "Should throw exception for null bean");
        } catch (Exception e) {
            // Expected
        }

        try {
            sagaTransactionalParser.isService(null, "test");
            assertTrue(false, "Should throw exception for null bean");
        } catch (Exception e) {
            // Expected
        }
    }
}
