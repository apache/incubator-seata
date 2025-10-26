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
package io.seata.integration.tx.api.interceptor.handler;

import io.seata.common.LockStrategyMode;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.Propagation;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for GlobalTransactionalInterceptorHandler.
 */
public class GlobalTransactionalInterceptorHandlerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                GlobalTransactionalInterceptorHandler.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransactionalInterceptorHandler should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataHandler() {
        assertTrue(
                org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler.class
                        .isAssignableFrom(GlobalTransactionalInterceptorHandler.class),
                "Should extend Apache Seata GlobalTransactionalInterceptorHandler");
    }

    @Test
    public void testGetGlobalLockConfigWithAnnotation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithGlobalLock");
        GlobalLockConfig config = handler.getGlobalLockConfig(method, TestService.class);

        assertNotNull(config);
        assertEquals(100, config.getLockRetryInterval());
        assertEquals(10, config.getLockRetryTimes());
    }

    @Test
    public void testGetGlobalLockConfigWithoutAnnotation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithoutGlobalLock");
        GlobalLockConfig config = handler.getGlobalLockConfig(method, TestService.class);

        assertNull(config);
    }

    @Test
    public void testGetAspectTransactionalWithRequiredPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithRequired");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(30000, aspectTransactional.getTimeoutMills());
        assertEquals("testTx", aspectTransactional.getName());
        assertEquals(org.apache.seata.tm.api.transaction.Propagation.REQUIRED, aspectTransactional.getPropagation());
        assertEquals(org.apache.seata.common.LockStrategyMode.OPTIMISTIC, aspectTransactional.getLockStrategyMode());
    }

    @Test
    public void testGetAspectTransactionalWithRequiresNewPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithRequiresNew");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(
                org.apache.seata.tm.api.transaction.Propagation.REQUIRES_NEW, aspectTransactional.getPropagation());
    }

    @Test
    public void testGetAspectTransactionalWithSupportsPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithSupports");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(org.apache.seata.tm.api.transaction.Propagation.SUPPORTS, aspectTransactional.getPropagation());
    }

    @Test
    public void testGetAspectTransactionalWithNotSupportedPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithNotSupported");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(
                org.apache.seata.tm.api.transaction.Propagation.NOT_SUPPORTED, aspectTransactional.getPropagation());
    }

    @Test
    public void testGetAspectTransactionalWithNeverPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithNever");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(org.apache.seata.tm.api.transaction.Propagation.NEVER, aspectTransactional.getPropagation());
    }

    @Test
    public void testGetAspectTransactionalWithMandatoryPropagation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithMandatory");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(org.apache.seata.tm.api.transaction.Propagation.MANDATORY, aspectTransactional.getPropagation());
    }

    @Test
    public void testGetAspectTransactionalWithPessimisticLock() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithPessimisticLock");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(org.apache.seata.common.LockStrategyMode.PESSIMISTIC, aspectTransactional.getLockStrategyMode());
    }

    @Test
    public void testGetAspectTransactionalWithoutAnnotation() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithoutAnnotation");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNull(aspectTransactional);
    }

    @Test
    public void testGetAspectTransactionalWithRollbackFor() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithRollbackFor");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(1, aspectTransactional.getRollbackFor().length);
        assertEquals(Exception.class, aspectTransactional.getRollbackFor()[0]);
    }

    @Test
    public void testGetAspectTransactionalWithNoRollbackFor() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithNoRollbackFor");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(1, aspectTransactional.getNoRollbackFor().length);
        assertEquals(RuntimeException.class, aspectTransactional.getNoRollbackFor()[0]);
    }

    @Test
    public void testGetAspectTransactionalWithLockRetryConfig() throws Exception {
        GlobalTransactionalInterceptorHandler handler = createHandler();

        Method method = TestService.class.getMethod("methodWithLockRetry");
        AspectTransactional aspectTransactional = handler.getAspectTransactional(method, TestService.class);

        assertNotNull(aspectTransactional);
        assertEquals(500, aspectTransactional.getLockRetryInterval());
        assertEquals(5, aspectTransactional.getLockRetryTimes());
    }

    private GlobalTransactionalInterceptorHandler createHandler() {
        Set<String> methodsToProxy = new HashSet<>();
        return new GlobalTransactionalInterceptorHandler(
                new org.apache.seata.tm.api.DefaultFailureHandlerImpl(), methodsToProxy);
    }

    // Test service class with various annotations
    public static class TestService {

        @GlobalLock(lockRetryInterval = 100, lockRetryTimes = 10)
        public void methodWithGlobalLock() {}

        public void methodWithoutGlobalLock() {}

        @GlobalTransactional(
                timeoutMills = 30000,
                name = "testTx",
                propagation = Propagation.REQUIRED,
                lockStrategyMode = LockStrategyMode.OPTIMISTIC)
        public void methodWithRequired() {}

        @GlobalTransactional(propagation = Propagation.REQUIRES_NEW)
        public void methodWithRequiresNew() {}

        @GlobalTransactional(propagation = Propagation.SUPPORTS)
        public void methodWithSupports() {}

        @GlobalTransactional(propagation = Propagation.NOT_SUPPORTED)
        public void methodWithNotSupported() {}

        @GlobalTransactional(propagation = Propagation.NEVER)
        public void methodWithNever() {}

        @GlobalTransactional(propagation = Propagation.MANDATORY)
        public void methodWithMandatory() {}

        @GlobalTransactional(lockStrategyMode = LockStrategyMode.PESSIMISTIC)
        public void methodWithPessimisticLock() {}

        public void methodWithoutAnnotation() {}

        @GlobalTransactional(rollbackFor = Exception.class)
        public void methodWithRollbackFor() {}

        @GlobalTransactional(noRollbackFor = RuntimeException.class)
        public void methodWithNoRollbackFor() {}

        @GlobalTransactional(lockRetryInterval = 500, lockRetryTimes = 5)
        public void methodWithLockRetry() {}
    }
}
