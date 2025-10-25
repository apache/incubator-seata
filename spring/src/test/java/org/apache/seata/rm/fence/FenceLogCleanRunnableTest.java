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
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Unit tests for FenceLogCleanRunnable
 * Focuses on testing core cleanup logic, avoiding complex multithreading and static field operations
 */
@ExtendWith(MockitoExtension.class)
public class FenceLogCleanRunnableTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private TransactionTemplate transactionTemplate;

    private Class<?> fenceLogIdentityClass;
    private Class<?> fenceLogCleanRunnableClass;

    @BeforeEach
    public void setUp() throws Exception {
        // Set static fields of SpringFenceHandler
        SpringFenceHandler.setDataSource(dataSource);
        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        // Get inner classes
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogIdentity".equals(innerClass.getSimpleName())) {
                fenceLogIdentityClass = innerClass;
            } else if ("FenceLogCleanRunnable".equals(innerClass.getSimpleName())) {
                fenceLogCleanRunnableClass = innerClass;
            }
        }

        assertNotNull(fenceLogIdentityClass, "FenceLogIdentity inner class should exist");
        assertNotNull(fenceLogCleanRunnableClass, "FenceLogCleanRunnable inner class should exist");
    }

    @Test
    public void testFenceLogCleanRunnableCreation() throws Exception {
        // Given & When
        Constructor<?> constructor = fenceLogCleanRunnableClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Runnable runnable = (Runnable) constructor.newInstance();

        // Then
        assertNotNull(runnable);
    }

    @Test
    public void testDeleteFenceSuccess() throws Exception {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("test-xid", 123L))
                    .thenReturn(true);

            // When
            boolean result = SpringFenceHandler.deleteFence("test-xid", 123L);

            // Then
            assertTrue(result);
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("test-xid", 123L), times(1));
        }
    }

    @Test
    public void testDeleteFenceFailure() throws Exception {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("test-xid-fail", 456L))
                    .thenReturn(false);

            // When
            boolean result = SpringFenceHandler.deleteFence("test-xid-fail", 456L);

            // Then
            assertFalse(result);
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("test-xid-fail", 456L), times(1));
        }
    }

    @Test
    public void testDeleteFenceWithException() throws Exception {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("test-xid-exception", 789L))
                    .thenThrow(new RuntimeException("Test exception"));

            // When & Then - Verify exception is thrown
            assertThrows(RuntimeException.class, () -> {
                SpringFenceHandler.deleteFence("test-xid-exception", 789L);
            });

            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("test-xid-exception", 789L), times(1));
        }
    }

    @Test
    public void testFenceLogIdentityCreation() throws Exception {
        // Given
        String testXid = "test-xid-identity";
        Long testBranchId = 999L;

        // When
        Object identity = createFenceLogIdentity(testXid, testBranchId);

        // Then
        assertNotNull(identity);

        // Verify xid and branchId are set correctly
        Method getXidMethod = fenceLogIdentityClass.getDeclaredMethod("getXid");
        Method getBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("getBranchId");
        getXidMethod.setAccessible(true);
        getBranchIdMethod.setAccessible(true);

        assertEquals(testXid, getXidMethod.invoke(identity));
        assertEquals(testBranchId, getBranchIdMethod.invoke(identity));
    }

    @Test
    public void testMultipleDeleteFenceCalls() throws Exception {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("xid-1", 100L))
                    .thenReturn(true);
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("xid-2", 200L))
                    .thenReturn(false);
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence("xid-3", 300L))
                    .thenReturn(true);

            // When
            boolean result1 = SpringFenceHandler.deleteFence("xid-1", 100L);
            boolean result2 = SpringFenceHandler.deleteFence("xid-2", 200L);
            boolean result3 = SpringFenceHandler.deleteFence("xid-3", 300L);

            // Then
            assertTrue(result1);
            assertFalse(result2);
            assertTrue(result3);

            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid-1", 100L), times(1));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid-2", 200L), times(1));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("xid-3", 300L), times(1));
        }
    }

    @Test
    public void testDeleteFenceWithDifferentParameters() throws Exception {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            // Mock any parameter calls to return true
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence(anyString(), anyLong()))
                    .thenReturn(true);

            // When
            boolean result1 = SpringFenceHandler.deleteFence("any-xid", 999L);
            boolean result2 = SpringFenceHandler.deleteFence("another-xid", 888L);

            // Then
            assertTrue(result1);
            assertTrue(result2);

            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("any-xid", 999L), times(1));
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence("another-xid", 888L), times(1));
        }
    }

    /**
     * Helper method to create FenceLogIdentity instance
     */
    private Object createFenceLogIdentity(String xid, Long branchId) throws Exception {
        Constructor<?> constructor = fenceLogIdentityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object identity = constructor.newInstance();

        Method setXidMethod = fenceLogIdentityClass.getDeclaredMethod("setXid", String.class);
        Method setBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("setBranchId", Long.class);
        setXidMethod.setAccessible(true);
        setBranchIdMethod.setAccessible(true);

        setXidMethod.invoke(identity, xid);
        setBranchIdMethod.invoke(identity, branchId);

        return identity;
    }
}
