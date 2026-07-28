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
package org.apache.seata.integration.tx.api.interceptor;

import org.apache.seata.common.Constants;
import org.apache.seata.common.executor.Callback;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.fence.hook.TccHookManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for action status report functionality in ActionInterceptorHandler.
 *
 * Covers:
 * 1. reportActionStatus method (success/failed/exception paths)
 * 2. ENABLE_ACTION_STATUS_REPORT conditional branches in proceed method
 */
public class ActionInterceptorHandlerReportTest {

    private MockedStatic<BusinessActionContextUtil> mockedContextUtil;
    private boolean originalEnableActionStatusReport;

    @BeforeEach
    void setUp() throws Exception {
        TccHookManager.clear();
        mockedContextUtil = Mockito.mockStatic(BusinessActionContextUtil.class);
        mockedContextUtil
                .when(() -> BusinessActionContextUtil.reportContext(any()))
                .thenReturn(true);

        // Enable action status report via reflection
        originalEnableActionStatusReport = setEnableActionStatusReport(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedContextUtil.close();
        TccHookManager.clear();

        // Restore original value
        setEnableActionStatusReport(originalEnableActionStatusReport);
    }

    private boolean setEnableActionStatusReport(boolean value) throws Exception {
        Field field = ActionInterceptorHandler.class.getDeclaredField("ENABLE_ACTION_STATUS_REPORT");
        field.setAccessible(true);

        // Use sun.misc.Unsafe to modify static final field (works on all Java versions)
        Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);

        Object base = unsafe.staticFieldBase(field);
        long offset = unsafe.staticFieldOffset(field);
        boolean original = unsafe.getBoolean(base, offset);
        unsafe.putBoolean(base, offset, value);
        return original;
    }

    private BusinessActionContext createActionContext(String xid, long branchId) {
        BusinessActionContext context = new BusinessActionContext();
        context.setXid(xid);
        context.setBranchId(branchId);
        context.setBranchType(BranchType.AT);
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);
        return context;
    }

    private TwoPhaseBusinessActionParam createParam(String actionName) {
        TwoPhaseBusinessActionParam param = new TwoPhaseBusinessActionParam();
        param.setActionName(actionName);
        param.setDelayReport(Boolean.TRUE);
        param.setBranchType(BranchType.AT);
        param.setUseCommonFence(false);
        return param;
    }

    // ---- Test reportActionStatus directly ----

    @Test
    void testReportActionStatusSuccess() {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();
        BusinessActionContext context = createActionContext("xid1", 1L);

        handler.reportActionStatus(context, Constants.ACTION_STATUS_SUCCESS);

        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus());
        mockedContextUtil.verify(() -> BusinessActionContextUtil.reportContext(context));
    }

    @Test
    void testReportActionStatusFailed() {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();
        BusinessActionContext context = createActionContext("xid2", 2L);

        handler.reportActionStatus(context, Constants.ACTION_STATUS_FAILED);

        assertEquals(Constants.ACTION_STATUS_FAILED, context.getActionStatus());
    }

    @Test
    void testReportActionStatusExceptionHandledGracefully() {
        ActionInterceptorHandler handler = new ActionInterceptorHandler();
        BusinessActionContext context = createActionContext("xid3", 3L);

        mockedContextUtil
                .when(() -> BusinessActionContextUtil.reportContext(any()))
                .thenThrow(new RuntimeException("report failed"));

        assertDoesNotThrow(() -> handler.reportActionStatus(context, Constants.ACTION_STATUS_SUCCESS));
        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus());
    }

    // ---- Test ENABLE_ACTION_STATUS_REPORT conditional in proceed ----

    @Test
    void testProceedReportsSuccessWhenCallbackSucceeds() throws Throwable {
        ActionInterceptorHandler handler = Mockito.spy(new ActionInterceptorHandler());
        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid100", 100L);
        Object[] arguments = new Object[] {context};
        TwoPhaseBusinessActionParam param = createParam("testAction");

        Mockito.doReturn("branch123")
                .when(handler)
                .doTxActionLogStore(
                        any(Method.class),
                        any(),
                        any(TwoPhaseBusinessActionParam.class),
                        any(BusinessActionContext.class));

        Callback<Object> successCallback = () -> "ok";
        handler.proceed(method, arguments, "xid100", param, successCallback);

        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus());
        mockedContextUtil.verify(() -> BusinessActionContextUtil.reportContext(any()), Mockito.atLeast(1));
    }

    @Test
    void testProceedReportsFailedWhenCallbackThrows() throws Throwable {
        ActionInterceptorHandler handler = Mockito.spy(new ActionInterceptorHandler());
        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid200", 200L);
        Object[] arguments = new Object[] {context};
        TwoPhaseBusinessActionParam param = createParam("testAction");

        Mockito.doReturn("branch456")
                .when(handler)
                .doTxActionLogStore(
                        any(Method.class),
                        any(),
                        any(TwoPhaseBusinessActionParam.class),
                        any(BusinessActionContext.class));

        Callback<Object> failCallback = () -> {
            throw new RuntimeException("business error");
        };

        try {
            handler.proceed(method, arguments, "xid200", param, failCallback);
        } catch (RuntimeException e) {
            // expected
        }

        assertEquals(Constants.ACTION_STATUS_FAILED, context.getActionStatus());
    }

    // ---- Helper class for method reference ----

    public static class TestTarget {
        public Object execute(BusinessActionContext context) {
            return "result";
        }
    }
}
