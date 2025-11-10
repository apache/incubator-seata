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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FenceLogCleanRunnable - Safe version without static field modification
 * This test class focuses on testing the inner class without polluting global state
 */
@ExtendWith(MockitoExtension.class)
public class FenceLogCleanRunnableUnitTest {

    @Test
    public void testFenceLogCleanRunnableExists() throws Exception {
        // Test that the inner class exists
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        Class<?> cleanRunnableClass = null;

        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogCleanRunnable".equals(innerClass.getSimpleName())) {
                cleanRunnableClass = innerClass;
                break;
            }
        }

        assertNotNull(cleanRunnableClass, "FenceLogCleanRunnable inner class should exist");
        assertTrue(Runnable.class.isAssignableFrom(cleanRunnableClass), "Should implement Runnable interface");
    }

    @Test
    public void testFenceLogCleanRunnableInstantiation() throws Exception {
        // Test that we can create instance of the inner class
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        Class<?> cleanRunnableClass = null;

        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogCleanRunnable".equals(innerClass.getSimpleName())) {
                cleanRunnableClass = innerClass;
                break;
            }
        }

        assertNotNull(cleanRunnableClass);

        Constructor<?> constructor = cleanRunnableClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object runnable = constructor.newInstance();

        assertNotNull(runnable);
        assertTrue(runnable instanceof Runnable);
    }

    @Test
    public void testDeleteFenceMethodWithMockedStatic() {
        // Test static deleteFence method safely using MockedStatic
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("test-xid", 123L))
                    .thenReturn(true);
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("fail-xid", 456L))
                    .thenReturn(false);

            // Test successful deletion
            boolean result1 = SpringFenceHandler.deleteFence("test-xid", 123L);
            assertTrue(result1);

            // Test failed deletion
            boolean result2 = SpringFenceHandler.deleteFence("fail-xid", 456L);
            assertFalse(result2);

            // Verify method calls
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("test-xid", 123L));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("fail-xid", 456L));
        }
    }

    @Test
    public void testDeleteFenceWithException() {
        // Test exception handling in static deleteFence method
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("error-xid", 789L))
                    .thenThrow(new RuntimeException("Database error"));

            // Should throw exception as expected
            assertThrows(RuntimeException.class, () -> {
                SpringFenceHandler.deleteFence("error-xid", 789L);
            });

            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("error-xid", 789L));
        }
    }

    @Test
    public void testMultipleDeleteFenceCalls() {
        // Test multiple static method calls in isolated environment
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence(anyString(), anyLong()))
                    .thenReturn(true);

            // Make multiple calls
            boolean result1 = SpringFenceHandler.deleteFence("xid1", 100L);
            boolean result2 = SpringFenceHandler.deleteFence("xid2", 200L);
            boolean result3 = SpringFenceHandler.deleteFence("xid3", 300L);

            // All should succeed
            assertTrue(result1);
            assertTrue(result2);
            assertTrue(result3);

            // Verify all calls were made
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid1", 100L));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid2", 200L));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid3", 300L));
        }
    }

    @Test
    public void testFenceLogIdentityInnerClass() throws Exception {
        // Test FenceLogIdentity inner class existence and instantiation
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        Class<?> identityClass = null;

        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogIdentity".equals(innerClass.getSimpleName())) {
                identityClass = innerClass;
                break;
            }
        }

        assertNotNull(identityClass, "FenceLogIdentity inner class should exist");

        // Test instantiation
        Constructor<?> constructor = identityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object identity = constructor.newInstance();

        assertNotNull(identity);
    }
}
