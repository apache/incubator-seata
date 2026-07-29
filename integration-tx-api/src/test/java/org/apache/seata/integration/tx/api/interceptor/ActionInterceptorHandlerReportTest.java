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
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.fence.hook.TccHook;
import org.apache.seata.integration.tx.api.fence.hook.TccHookManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for the action status state machine in ActionInterceptorHandler.
 *
 * Covers the none/running/success/failed status transitions and the SAGA_ANNOTATION-only gating,
 * replacing the previous sun.misc.Unsafe-based static field manipulation with an overridable method.
 */
public class ActionInterceptorHandlerReportTest {

    private MockedStatic<BusinessActionContextUtil> mockedContextUtil;

    @BeforeEach
    void setUp() {
        TccHookManager.clear();
        mockedContextUtil = Mockito.mockStatic(BusinessActionContextUtil.class);
        mockedContextUtil
                .when(() -> BusinessActionContextUtil.reportContext(any()))
                .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        mockedContextUtil.close();
        TccHookManager.clear();
    }

    private BusinessActionContext createActionContext(String xid, long branchId) {
        BusinessActionContext context = new BusinessActionContext();
        context.setXid(xid);
        context.setBranchId(branchId);
        context.setBranchType(BranchType.SAGA_ANNOTATION);
        Map<String, Object> actionContext = new HashMap<>();
        context.setActionContext(actionContext);
        return context;
    }

    private TwoPhaseBusinessActionParam createParam(String actionName) {
        TwoPhaseBusinessActionParam param = new TwoPhaseBusinessActionParam();
        param.setActionName(actionName);
        param.setDelayReport(Boolean.TRUE);
        param.setBranchType(BranchType.SAGA_ANNOTATION);
        param.setUseCommonFence(false);
        return param;
    }

    private ActionInterceptorHandler spyHandlerWithReportEnabled() {
        ActionInterceptorHandler handler = Mockito.spy(new ActionInterceptorHandler());
        Mockito.doReturn(true).when(handler).isActionStatusReportEnabled(any());
        Mockito.doReturn("branch1")
                .when(handler)
                .doTxActionLogStore(
                        any(Method.class),
                        any(),
                        any(TwoPhaseBusinessActionParam.class),
                        any(BusinessActionContext.class));
        return handler;
    }

    @Test
    void testProceedReportsRunningThenSuccess() throws Throwable {
        ActionInterceptorHandler handler = spyHandlerWithReportEnabled();
        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid1", 1L);

        handler.proceed(method, new Object[] {context}, "xid1", createParam("testAction"), () -> "ok");

        assertEquals(Constants.ACTION_STATUS_SUCCESS, context.getActionStatus());
        // running reported immediately before execute, plus the final report in finally
        mockedContextUtil.verify(() -> BusinessActionContextUtil.reportContext(any()), Mockito.atLeast(2));
    }

    @Test
    void testProceedReportsFailedWhenCallbackThrows() throws Throwable {
        ActionInterceptorHandler handler = spyHandlerWithReportEnabled();
        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid2", 2L);

        try {
            handler.proceed(method, new Object[] {context}, "xid2", createParam("testAction"), () -> {
                throw new RuntimeException("business error");
            });
        } catch (RuntimeException e) {
            // expected
        }

        assertEquals(Constants.ACTION_STATUS_FAILED, context.getActionStatus());
    }

    @Test
    void testProceedReportsNoneWhenBeforePrepareHookThrows() throws Throwable {
        ActionInterceptorHandler handler = spyHandlerWithReportEnabled();
        TccHook throwingHook = Mockito.mock(TccHook.class);
        Mockito.doThrow(new RuntimeException("prepare hook error"))
                .when(throwingHook)
                .beforeTccPrepare(any(), any(), any(), any());
        TccHookManager.registerHook(throwingHook);

        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid3", 3L);

        try {
            handler.proceed(method, new Object[] {context}, "xid3", createParam("testAction"), () -> "ok");
        } catch (RuntimeException e) {
            // expected: before-prepare hook failure rethrown
        }

        assertEquals(Constants.ACTION_STATUS_NONE, context.getActionStatus());
    }

    @Test
    void testProceedDoesNotReportWhenBranchTypeIsTcc() throws Throwable {
        ActionInterceptorHandler handler = Mockito.spy(new ActionInterceptorHandler());
        Mockito.doReturn(false).when(handler).isActionStatusReportEnabled(any());
        Mockito.doReturn("branch4")
                .when(handler)
                .doTxActionLogStore(
                        any(Method.class),
                        any(),
                        any(TwoPhaseBusinessActionParam.class),
                        any(BusinessActionContext.class));

        Method method = TestTarget.class.getDeclaredMethod("execute", BusinessActionContext.class);
        BusinessActionContext context = createActionContext("xid4", 4L);
        TwoPhaseBusinessActionParam param = createParam("testAction");
        param.setBranchType(BranchType.TCC);

        handler.proceed(method, new Object[] {context}, "xid4", param, () -> "ok");

        assertNull(context.getActionStatus());
    }

    public static class TestTarget {
        public Object execute(BusinessActionContext context) {
            return "result";
        }
    }
}
