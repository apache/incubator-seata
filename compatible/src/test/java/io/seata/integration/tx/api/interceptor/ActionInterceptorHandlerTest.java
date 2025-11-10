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
package io.seata.integration.tx.api.interceptor;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ActionInterceptorHandler compatibility wrapper.
 */
public class ActionInterceptorHandlerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ActionInterceptorHandler.class.isAnnotationPresent(Deprecated.class),
                "ActionInterceptorHandler should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataClass() {
        assertTrue(
                org.apache.seata.integration.tx.api.interceptor.ActionInterceptorHandler.class.isAssignableFrom(
                        ActionInterceptorHandler.class),
                "ActionInterceptorHandler should extend Apache Seata ActionInterceptorHandler");
    }

    @Test
    public void testConstructor() {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();
        assertNotNull(handler);
    }

    @Test
    public void testGetOrCreateActionContextWithNullContext() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method method = ActionInterceptorHandler.class.getDeclaredMethod(
                "getOrCreateActionContextAndResetToArguments", Class[].class, Object[].class);
        method.setAccessible(true);

        Class<?>[] parameterTypes = {String.class, int.class};
        Object[] arguments = {"test", 123};

        BusinessActionContext result = (BusinessActionContext) method.invoke(handler, parameterTypes, arguments);

        assertNotNull(result);
    }

    @Test
    public void testGetOrCreateActionContextWithExistingContext() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method method = ActionInterceptorHandler.class.getDeclaredMethod(
                "getOrCreateActionContextAndResetToArguments", Class[].class, Object[].class);
        method.setAccessible(true);

        BusinessActionContext existingContext = new BusinessActionContext();
        existingContext.setXid("test-xid");

        Class<?>[] parameterTypes = {BusinessActionContext.class, String.class};
        Object[] arguments = {existingContext, "test"};

        BusinessActionContext result = (BusinessActionContext) method.invoke(handler, parameterTypes, arguments);

        assertNotNull(result);
        assertEquals("test-xid", result.getXid());
    }

    @Test
    public void testGetOrCreateActionContextCreatesNewWhenNull() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method method = ActionInterceptorHandler.class.getDeclaredMethod(
                "getOrCreateActionContextAndResetToArguments", Class[].class, Object[].class);
        method.setAccessible(true);

        Class<?>[] parameterTypes = {BusinessActionContext.class, String.class};
        Object[] arguments = {null, "test"};

        BusinessActionContext result = (BusinessActionContext) method.invoke(handler, parameterTypes, arguments);

        assertNotNull(result);
        // Verify the argument was updated
        assertNotNull(arguments[0]);
    }

    @Test
    public void testFetchActionRequestContextWithAnnotatedParam() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method testMethod = TestService.class.getMethod("actionWithAnnotatedParam", String.class, int.class);

        Method fetchMethod = ActionInterceptorHandler.class.getDeclaredMethod(
                "fetchActionRequestContext", Method.class, Object[].class);
        fetchMethod.setAccessible(true);

        Object[] arguments = {"test-value", 100};

        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) fetchMethod.invoke(handler, testMethod, arguments);

        assertNotNull(context);
        assertEquals("test-value", context.get("userId"));
    }

    @Test
    public void testFetchActionRequestContextWithoutAnnotation() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method testMethod = TestService.class.getMethod("actionWithoutAnnotation", String.class);

        Method fetchMethod = ActionInterceptorHandler.class.getDeclaredMethod(
                "fetchActionRequestContext", Method.class, Object[].class);
        fetchMethod.setAccessible(true);

        Object[] arguments = {"test-value"};

        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) fetchMethod.invoke(handler, testMethod, arguments);

        assertNotNull(context);
        assertTrue(context.isEmpty());
    }

    @Test
    public void testFetchActionRequestContextWithNullParam() throws Exception {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();

        Method testMethod = TestService.class.getMethod("actionWithAnnotatedParam", String.class, int.class);

        Method fetchMethod = ActionInterceptorHandler.class.getDeclaredMethod(
                "fetchActionRequestContext", Method.class, Object[].class);
        fetchMethod.setAccessible(true);

        Object[] arguments = {null, 100};

        assertThrows(
                Exception.class,
                () -> fetchMethod.invoke(handler, testMethod, arguments),
                "Should throw exception for null annotated parameter");
    }

    // Test service class
    public static class TestService {

        public void actionWithAnnotatedParam(
                @BusinessActionContextParameter(paramName = "userId") String userId, int amount) {}

        public void actionWithoutAnnotation(String param) {}

        public void actionWithContext(BusinessActionContext context, String param) {}
    }
}
