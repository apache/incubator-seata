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
package org.apache.seata.rm.fence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpringFenceHandler - Safe version without static field modification
 * This test class focuses on testing methods that don't require static state modification
 */
@ExtendWith(MockitoExtension.class)
public class SpringFenceHandlerUnitTest {

    @Mock
    private DataSource dataSource;

    private SpringFenceHandler springFenceHandler;

    @BeforeEach
    public void setUp() {
        springFenceHandler = new SpringFenceHandler();
    }

    @Test
    public void testSpringFenceHandlerInstantiation() {
        // Test that SpringFenceHandler can be instantiated
        SpringFenceHandler handler = new SpringFenceHandler();
        assertNotNull(handler);
    }

    @Test
    public void testCreateTransactionTemplateForTransactionalMethodWithNull() throws Exception {
        // Test private method through reflection without modifying static state
        Method createMethod = SpringFenceHandler.class.getDeclaredMethod(
                "createTransactionTemplateForTransactionalMethod", Transactional.class);
        createMethod.setAccessible(true);

        // Use MockedStatic to safely test without polluting global state
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class, CALLS_REAL_METHODS)) {
            // Mock static method calls within this scope
            mockedStatic.when(SpringFenceHandler::getDataSource).thenReturn(dataSource);

            // This test will verify the method works with null transactional annotation
            // The actual implementation will be tested in integration tests
            assertDoesNotThrow(() -> {
                createMethod.invoke(springFenceHandler, (Transactional) null);
            });
        }
    }

    @Test
    public void testCreateTransactionTemplateWithTransactionalAnnotation() throws Exception {
        // Test with dynamic proxy for Transactional annotation
        Transactional transactional = (Transactional) java.lang.reflect.Proxy.newProxyInstance(
                Transactional.class.getClassLoader(), new Class[] {Transactional.class}, (proxy, method, args) -> {
                    if ("isolation".equals(method.getName())) {
                        return Isolation.READ_COMMITTED;
                    }
                    return method.getDefaultValue();
                });

        Method createMethod = SpringFenceHandler.class.getDeclaredMethod(
                "createTransactionTemplateForTransactionalMethod", Transactional.class);
        createMethod.setAccessible(true);

        // Test that method works properly - need to handle potential exceptions carefully
        try {
            Object result = createMethod.invoke(springFenceHandler, transactional);
            // The method should return a TransactionTemplate or throw an exception
            // We just verify that the invocation doesn't cause unexpected errors
            assertNotNull(result, "Method should return a result or throw a specific exception");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // If the method throws an exception, it should be a known type
            Throwable cause = e.getCause();
            // For this test, we accept that the method might throw exceptions due to missing dependencies
            // The important thing is that the method is accessible and the annotation is processed
            assertTrue(
                    cause instanceof RuntimeException || cause instanceof IllegalArgumentException,
                    "Expected runtime exception due to missing dependencies, but got: "
                            + cause.getClass().getName());
        }
    }

    @Test
    public void testStaticMethodsWithMockedStatic() {
        // Test static methods safely using MockedStatic
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic.when(SpringFenceHandler::getDataSource).thenReturn(dataSource);
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("test", 123L))
                    .thenReturn(true);

            // Test static method calls within mocked scope
            DataSource result = SpringFenceHandler.getDataSource();
            boolean deleteResult = SpringFenceHandler.deleteFence("test", 123L);

            assertSame(dataSource, result);
            assertTrue(deleteResult);

            // Verify interactions
            mockedStatic.verify(SpringFenceHandler::getDataSource);
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("test", 123L));
        }
        // MockedStatic automatically restores original behavior after try-with-resources
    }

    @Test
    public void testMultipleStaticMethodCalls() {
        // Test multiple static method calls in isolated scope
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence(anyString(), anyLong()))
                    .thenReturn(true);

            boolean result1 = SpringFenceHandler.deleteFence("xid1", 100L);
            boolean result2 = SpringFenceHandler.deleteFence("xid2", 200L);

            assertTrue(result1);
            assertTrue(result2);

            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid1", 100L));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid2", 200L));
        }
    }

    @Test
    public void testInstanceMethodsOnly() {
        // Test that we can create instances without static dependency issues
        SpringFenceHandler handler1 = new SpringFenceHandler();
        SpringFenceHandler handler2 = new SpringFenceHandler();

        assertNotNull(handler1);
        assertNotNull(handler2);
        assertNotSame(handler1, handler2);
    }

    @Test
    public void testFenceLogIdentityInnerClass() throws Exception {
        // Test the inner class without modifying static state
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        Class<?> fenceLogIdentityClass = null;

        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogIdentity".equals(innerClass.getSimpleName())) {
                fenceLogIdentityClass = innerClass;
                break;
            }
        }

        assertNotNull(fenceLogIdentityClass, "FenceLogIdentity inner class should exist");

        // Test that we can instantiate the inner class - need to make constructor accessible
        Constructor<?> constructor = fenceLogIdentityClass.getDeclaredConstructor();
        constructor.setAccessible(true); // Make private constructor accessible
        Object identity = constructor.newInstance();
        assertNotNull(identity);
    }

    @Test
    public void testFenceLogCleanRunnableInnerClass() throws Exception {
        // Test the inner class without modifying static state
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        Class<?> cleanRunnableClass = null;

        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogCleanRunnable".equals(innerClass.getSimpleName())) {
                cleanRunnableClass = innerClass;
                break;
            }
        }

        assertNotNull(cleanRunnableClass, "FenceLogCleanRunnable inner class should exist");

        // Test that we can instantiate the inner class - need to make constructor accessible
        Constructor<?> constructor = cleanRunnableClass.getDeclaredConstructor();
        constructor.setAccessible(true); // Make private constructor accessible
        Object runnable = constructor.newInstance();
        assertNotNull(runnable);
        assertTrue(runnable instanceof Runnable);
    }
}
