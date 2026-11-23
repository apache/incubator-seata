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

import org.apache.seata.common.executor.Callback;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Action interceptor handler test.
 *
 */
public class ActionInterceptorHandlerTest {

    /**
     * The Action interceptor handler.
     */
    protected ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    @AfterEach
    public void tearDown() {
        BusinessActionContextUtil.clear();
    }

    /**
     * Test business action context.
     *
     * @throws NoSuchMethodException the no such method exception
     */
    @Test
    public void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = TestAction.class.getDeclaredMethod(
                "prepare", BusinessActionContext.class, int.class, List.class, TestParam.class);
        List<Object> list = new ArrayList<>();
        list.add("b");
        TestParam tccParam = new TestParam(1, "abc@ali.com");

        Map<String, Object> paramContext = actionInterceptorHandler.fetchActionRequestContext(
                prepareMethod, new Object[] {null, 10, list, tccParam});
        System.out.println(paramContext);

        Assertions.assertEquals(10, paramContext.get("a"));
        Assertions.assertEquals("b", paramContext.get("b"));
        Assertions.assertEquals("abc@ali.com", paramContext.get("email"));
    }

    @Test
    public void testProceedTracksActionContextMutation() throws Throwable {
        Method prepareMethod = TestAction.class.getDeclaredMethod(
                "prepare", BusinessActionContext.class, int.class, List.class, TestParam.class);
        List<Object> list = new ArrayList<>();
        list.add("b");
        TestParam tccParam = new TestParam(1, "abc@ali.com");

        TwoPhaseBusinessActionParam businessActionParam = mock(TwoPhaseBusinessActionParam.class);
        org.mockito.Mockito.doReturn("prepare").when(businessActionParam).getActionName();
        org.mockito.Mockito.doReturn(BranchType.TCC).when(businessActionParam).getBranchType();
        org.mockito.Mockito.doReturn(false).when(businessActionParam).getDelayReport();
        org.mockito.Mockito.doReturn(false).when(businessActionParam).getUseCommonFence();
        org.mockito.Mockito.doReturn(Collections.emptyMap())
                .when(businessActionParam)
                .getBusinessActionContext();

        DefaultResourceManager resourceManager = mock(DefaultResourceManager.class);
        AtomicReference<BusinessActionContext> observedContext = new AtomicReference<>();
        ArgumentCaptor<String> applicationDataCaptor = ArgumentCaptor.forClass(String.class);

        try (MockedStatic<DefaultResourceManager> mocked = mockStatic(DefaultResourceManager.class)) {
            mocked.when(DefaultResourceManager::get).thenReturn(resourceManager);
            when(resourceManager.branchRegister(
                            eq(BranchType.TCC), eq("prepare"), isNull(), eq("test-xid"), anyString(), isNull()))
                    .thenReturn(1L);

            Callback<Object> callback = () -> {
                BusinessActionContext currentContext = BusinessActionContextUtil.getContext();
                Assertions.assertNotNull(currentContext);
                Assertions.assertNull(currentContext.getUpdated());
                currentContext.getActionContext().put("biz", "value");
                Assertions.assertTrue(currentContext.getUpdated());
                observedContext.set(currentContext);
                return null;
            };

            Object result = actionInterceptorHandler.proceed(
                    prepareMethod, new Object[] {null, 10, list, tccParam}, "test-xid", businessActionParam, callback);

            Assertions.assertNull(result);
        }

        Assertions.assertNotNull(observedContext.get());
        Assertions.assertNull(observedContext.get().getUpdated());
        verify(resourceManager)
                .branchReport(
                        eq(BranchType.TCC),
                        eq("test-xid"),
                        eq(1L),
                        eq(BranchStatus.Registered),
                        applicationDataCaptor.capture());
        Assertions.assertTrue(applicationDataCaptor.getValue().contains("biz"));
        Assertions.assertTrue(applicationDataCaptor.getValue().contains("value"));
    }
}
