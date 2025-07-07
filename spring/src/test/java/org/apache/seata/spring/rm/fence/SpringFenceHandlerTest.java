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
package org.apache.seata.spring.rm.fence;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.common.executor.Callback;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceDO;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceStore;
import org.apache.seata.rm.fence.SpringFenceHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SpringFenceHandlerTest {

    private DataSource dataSource;
    private Connection connection;
    private CommonFenceStore fenceStore;
    private TransactionTemplate transactionTemplate;

    private final SpringFenceHandler handler = new SpringFenceHandler();

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        SpringFenceHandler.setDataSource(dataSource);

        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        transactionTemplate = spy(new TransactionTemplate(txManager));
        doAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                })
                .when(transactionTemplate)
                .execute(any());

        SpringFenceHandler.setTransactionTemplate(transactionTemplate);

        fenceStore = mock(CommonFenceStore.class);
        setStaticFinalField(SpringFenceHandler.class, "COMMON_FENCE_DAO", fenceStore);
    }

    @Test
    void testPrepareFence_success() throws Throwable {
        when(fenceStore.insertCommonFenceDO(any(), any())).thenReturn(true);

        Callback<Object> callback = mock(Callback.class);
        when(callback.execute()).thenReturn("OK");

        Object result = handler.prepareFence("xid123", 1L, "action", callback);
        assertEquals("OK", result);
    }

    @Test
    void testPrepareFence_duplicateKey() {
        when(fenceStore.insertCommonFenceDO(any(), any()))
                .thenThrow(new CommonFenceException(FrameworkErrorCode.DuplicateKeyException));

        assertThrows(
                SkipCallbackWrapperException.class,
                () -> handler.prepareFence("xid", 1L, "action", mock(Callback.class)));
    }

    @Test
    void testCommitFence_success() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_TRIED);

        when(fenceStore.queryCommonFenceDO(any(), eq("xid"), eq(100L))).thenReturn(fenceDO);
        when(fenceStore.updateCommonFenceDO(
                        any(),
                        eq("xid"),
                        eq(100L),
                        eq(CommonFenceConstant.STATUS_COMMITTED),
                        eq(CommonFenceConstant.STATUS_TRIED)))
                .thenReturn(true);

        Method method = TestTCC.class.getMethod("commitMethod");
        boolean result = handler.commitFence(method, new TestTCC(), "xid", 100L, new Object[0]);
        assertTrue(result);
    }

    @Test
    void testCommitFence_alreadyCommitted() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_COMMITTED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);

        Method method = TestTCC.class.getMethod("commitMethod");
        boolean result = handler.commitFence(method, new TestTCC(), "xid", 200L, new Object[0]);
        assertTrue(result);
    }

    @Test
    void testRollbackFence_success() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_TRIED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);
        when(fenceStore.updateCommonFenceDO(
                        any(),
                        anyString(),
                        anyLong(),
                        eq(CommonFenceConstant.STATUS_ROLLBACKED),
                        eq(CommonFenceConstant.STATUS_TRIED)))
                .thenReturn(true);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 300L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testRollbackFence_insertSuspended() throws Exception {
        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(null);
        when(fenceStore.insertCommonFenceDO(any(), any())).thenReturn(true);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 400L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testRollbackFence_alreadyRollbacked() throws Exception {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setStatus(CommonFenceConstant.STATUS_ROLLBACKED);

        when(fenceStore.queryCommonFenceDO(any(), anyString(), anyLong())).thenReturn(fenceDO);

        Method method = TestTCC.class.getMethod("rollbackMethod");
        boolean result = handler.rollbackFence(method, new TestTCC(), "xid", 500L, new Object[0], "action");
        assertTrue(result);
    }

    @Test
    void testDeleteFenceByDate() throws Exception {
        Date now = new Date();
        Set<String> xidSet = new HashSet<>();
        xidSet.add("xid-1");
        xidSet.add("xid-2");

        when(fenceStore.queryEndStatusXidsByDate(any(), eq(now), anyInt()))
                .thenReturn(xidSet)
                .thenReturn(Collections.emptySet());

        when(fenceStore.deleteTCCFenceDO(any(), anyList())).thenReturn(xidSet.size());

        int deleted = handler.deleteFenceByDate(now);
        assertEquals(xidSet.size(), deleted);
    }

    static void setStaticFinalField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        field.set(null, value);
    }

    public static class TestTCC {
        public boolean commitMethod() {
            return true;
        }

        public boolean rollbackMethod() {
            return true;
        }
    }
}
