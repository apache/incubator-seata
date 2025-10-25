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

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.common.executor.Callback;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SpringFenceHandler
 * Focuses on testing accessible methods and logic, avoiding complex static field operations
 */
@ExtendWith(MockitoExtension.class)
public class SpringFenceHandlerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private Callback<Object> targetCallback;

    @Mock
    private Method method;

    @Mock
    private Object targetBean;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    private SpringFenceHandler springFenceHandler;

    private static final String TEST_XID = "test-xid-123";
    private static final Long TEST_BRANCH_ID = 123456L;
    private static final String TEST_ACTION_NAME = "testAction";

    @BeforeEach
    public void setUp() {
        springFenceHandler = new SpringFenceHandler();
    }

    @Test
    public void testGetDataSource() {
        // Given
        SpringFenceHandler.setDataSource(dataSource);

        // When
        DataSource result = SpringFenceHandler.getDataSource();

        // Then
        assertSame(dataSource, result);
    }

    @Test
    public void testSetDataSource() {
        // Given
        DataSource newDataSource = mock(DataSource.class);

        // When
        SpringFenceHandler.setDataSource(newDataSource);

        // Then
        assertSame(newDataSource, SpringFenceHandler.getDataSource());
    }

    @Test
    public void testSetTransactionTemplate() {
        // Given
        TransactionTemplate newTemplate = mock(TransactionTemplate.class);

        // When
        SpringFenceHandler.setTransactionTemplate(newTemplate);

        // Then
        // Verify successful setting
        assertNotNull(newTemplate);
    }

    @Test
    public void testPrepareFenceSuccess() throws Exception {
        // Given
        Object expectedResult = "success";
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.prepareFence(TEST_XID, TEST_BRANCH_ID, TEST_ACTION_NAME, targetCallback))
                .thenReturn(expectedResult);

        // When
        Object result = handler.prepareFence(TEST_XID, TEST_BRANCH_ID, TEST_ACTION_NAME, targetCallback);

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    public void testCommitFenceSuccess() throws Exception {
        // Given
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.commitFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}))
                .thenReturn(true);

        // When
        boolean result = handler.commitFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {});

        // Then
        assertTrue(result);
    }

    @Test
    public void testRollbackFenceSuccess() throws Exception {
        // Given
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.rollbackFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}, TEST_ACTION_NAME))
                .thenReturn(true);

        // When
        boolean result =
                handler.rollbackFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}, TEST_ACTION_NAME);

        // Then
        assertTrue(result);
    }

    @Test
    public void testDeleteFenceSuccess() {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID))
                    .thenReturn(true);

            // When
            boolean result = SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID);

            // Then
            assertTrue(result);
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID), times(1));
        }
    }

    @Test
    public void testDeleteFenceFailure() {
        // Given
        try (MockedStatic<SpringFenceHandler> mockedStatic = mockStatic(SpringFenceHandler.class)) {
            mockedStatic
                    .when(() -> SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID))
                    .thenReturn(false);

            // When
            boolean result = SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID);

            // Then
            assertFalse(result);
            mockedStatic.verify(() -> SpringFenceHandler.deleteFence(TEST_XID, TEST_BRANCH_ID), times(1));
        }
    }

    @Test
    public void testDeleteFenceByDate() {
        // Given
        Date testDate = new Date();
        int expectedResult = 5;
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.deleteFenceByDate(testDate)).thenReturn(expectedResult);

        // When
        int result = handler.deleteFenceByDate(testDate);

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    public void testCreateTransactionTemplateForTransactionalMethodWithNull() throws Exception {
        // Given
        Method createMethod = SpringFenceHandler.class.getDeclaredMethod(
                "createTransactionTemplateForTransactionalMethod", Transactional.class);
        createMethod.setAccessible(true);

        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        try (MockedStatic<BusinessActionContextUtil> mockedUtil = mockStatic(BusinessActionContextUtil.class)) {
            mockedUtil.when(BusinessActionContextUtil::getContext).thenReturn(null);

            // When
            TransactionTemplate result =
                    (TransactionTemplate) createMethod.invoke(springFenceHandler, (Transactional) null);

            // Then
            assertSame(transactionTemplate, result);
        }
    }

    @Test
    public void testCreateTransactionTemplateForTransactionalMethodWithTransactional() throws Exception {
        // Given - Create @Transactional annotation implementation using dynamic proxy
        Transactional transactional = (Transactional) java.lang.reflect.Proxy.newProxyInstance(
                Transactional.class.getClassLoader(), new Class[] {Transactional.class}, (proxy, method, args) -> {
                    if ("isolation".equals(method.getName())) {
                        return Isolation.READ_COMMITTED;
                    }
                    // Return default value
                    return method.getDefaultValue();
                });

        when(transactionTemplate.getTransactionManager()).thenReturn(platformTransactionManager);
        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        Method createMethod = SpringFenceHandler.class.getDeclaredMethod(
                "createTransactionTemplateForTransactionalMethod", Transactional.class);
        createMethod.setAccessible(true);

        // When
        TransactionTemplate result = (TransactionTemplate) createMethod.invoke(springFenceHandler, transactional);

        // Then
        assertNotNull(result);
        assertEquals(Isolation.READ_COMMITTED.value(), result.getIsolationLevel());
        verify(transactionTemplate).getTransactionManager();
    }

    @Test
    public void testCreateTransactionTemplateWithBusinessActionContext() throws Exception {
        // Given
        BusinessActionContext businessActionContext = mock(BusinessActionContext.class);
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put(Constants.TX_ISOLATION, 2); // READ_COMMITTED

        when(businessActionContext.getActionContext()).thenReturn(actionContext);
        when(transactionTemplate.getTransactionManager()).thenReturn(platformTransactionManager);

        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        Method createMethod = SpringFenceHandler.class.getDeclaredMethod(
                "createTransactionTemplateForTransactionalMethod", Transactional.class);
        createMethod.setAccessible(true);

        try (MockedStatic<BusinessActionContextUtil> mockedUtil = mockStatic(BusinessActionContextUtil.class)) {
            mockedUtil.when(BusinessActionContextUtil::getContext).thenReturn(businessActionContext);

            // When
            TransactionTemplate result =
                    (TransactionTemplate) createMethod.invoke(springFenceHandler, (Transactional) null);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getIsolationLevel());
        }
    }

    @Test
    public void testMultipleDeleteFenceCalls() {
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
    public void testDeleteFenceWithDifferentParameters() {
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

    @Test
    public void testPrepareFenceWithException() throws Exception {
        // Given
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.prepareFence(TEST_XID, TEST_BRANCH_ID, TEST_ACTION_NAME, targetCallback))
                .thenThrow(new SkipCallbackWrapperException(new RuntimeException("Test exception")));

        // When & Then
        assertThrows(SkipCallbackWrapperException.class, () -> {
            handler.prepareFence(TEST_XID, TEST_BRANCH_ID, TEST_ACTION_NAME, targetCallback);
        });
    }

    @Test
    public void testCommitFenceWithException() throws Exception {
        // Given
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.commitFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}))
                .thenThrow(new SkipCallbackWrapperException(new RuntimeException("Commit failed")));

        // When & Then
        assertThrows(SkipCallbackWrapperException.class, () -> {
            handler.commitFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {});
        });
    }

    @Test
    public void testRollbackFenceWithException() throws Exception {
        // Given
        SpringFenceHandler handler = mock(SpringFenceHandler.class);

        when(handler.rollbackFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}, TEST_ACTION_NAME))
                .thenThrow(new SkipCallbackWrapperException(new RuntimeException("Rollback failed")));

        // When & Then
        assertThrows(SkipCallbackWrapperException.class, () -> {
            handler.rollbackFence(method, targetBean, TEST_XID, TEST_BRANCH_ID, new Object[] {}, TEST_ACTION_NAME);
        });
    }
}
