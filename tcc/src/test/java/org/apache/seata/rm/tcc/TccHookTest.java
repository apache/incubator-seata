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
package org.apache.seata.rm.tcc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.executor.Callback;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.fence.hook.TccHook;
import org.apache.seata.integration.tx.api.fence.hook.TccHookManager;
import org.apache.seata.integration.tx.api.interceptor.ActionInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.core.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TccHookTest {
    private MyTccHook tccHook;
    private String xid;
    private Long branchId;
    private String actionName;
    private BusinessActionContext context;
    private TestActionInterceptorHandler actionInterceptorHandler;
    private TCCResourceManager tccResourceManager;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        tccHook = Mockito.spy(new MyTccHook());
        xid = "test-xid";
        branchId = 12345L;
        actionName = "testAction";
        context = new BusinessActionContext();
        TccHookManager.clear();
        TccHookManager.registerHook(tccHook);

        actionInterceptorHandler = Mockito.spy(new TestActionInterceptorHandler());
        TCCResourceManager tccResourceManagerObject = new TCCResourceManager();
        TCCResource tccResource = mock(TCCResource.class);
        Mockito.doReturn(actionName)
                .when(tccResource).getResourceId();
        Mockito.doReturn(actionName)
                .when(tccResource).getActionName();
        TestTccThreePhaseHandler testTccThreePhaseHandler = new TestTccThreePhaseHandler();
        Mockito.doReturn(testTccThreePhaseHandler)
                .when(tccResource).getTargetBean();

        Mockito.doReturn(new String[0])
                .when(tccResource).getPhaseTwoCommitKeys();
        Mockito.doReturn(new Class[0])
                .when(tccResource).getCommitArgsClasses();

        Mockito.doReturn(new String[0])
                .when(tccResource).getPhaseTwoRollbackKeys();
        Mockito.doReturn(new Class[0])
                .when(tccResource).getRollbackArgsClasses();

        Method commitMethod = testTccThreePhaseHandler.getClass().getMethod("commit");
        Mockito.doReturn(commitMethod)
                .when(tccResource).getCommitMethod();

        Method rollbackMethod = testTccThreePhaseHandler.getClass().getMethod("rollback");
        Mockito.doReturn(rollbackMethod)
                .when(tccResource).getRollbackMethod();

        Map<String, Resource> tccResourceCache = new ConcurrentHashMap<>();
        tccResourceCache.put(actionName, tccResource);
        setPrivateField(tccResourceManagerObject, "tccResourceCache", tccResourceCache);
        tccResourceManager = Mockito.spy(tccResourceManagerObject);
    }

    @Test
    public void testBeforeTccPrepare() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccPrepare(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccPrepare(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testAfterTccPrepare() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccPrepare(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccPrepare(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testBeforeTccCommit() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccCommit(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccCommit(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testAfterTccCommit() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccCommit(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccCommit(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testBeforeTccRollback() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccRollback(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccRollback(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testAfterTccRollback() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccRollback(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccRollback(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testTccPrepareHook() throws Throwable {
        TestTccThreePhaseHandler testTccThreePhaseHandler = new TestTccThreePhaseHandler();
        Method method = testTccThreePhaseHandler.getClass().getMethod("prepare");
        TwoPhaseBusinessActionParam twoPhaseBusinessActionParam = Mockito.mock(TwoPhaseBusinessActionParam.class);
        Callback<Object> callback = Mockito.mock(Callback.class, Mockito.withSettings().defaultAnswer(Mockito.RETURNS_DEFAULTS));
        Mockito.doReturn(actionName)
                .when(twoPhaseBusinessActionParam).getActionName();
        actionInterceptorHandler.proceed(method, null, xid, twoPhaseBusinessActionParam, callback);
        verify(tccHook).beforeTccPrepare(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        verify(tccHook).afterTccPrepare(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testTccCommitHook() throws TransactionException {
        tccResourceManager.branchCommit(BranchType.TCC, xid, branchId, actionName, null);
        verify(tccHook).beforeTccCommit(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        verify(tccHook).afterTccCommit(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testTccRollbackHook() throws TransactionException {
        tccResourceManager.branchRollback(BranchType.TCC, xid, branchId, actionName, null);
        verify(tccHook).beforeTccRollback(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        verify(tccHook).afterTccRollback(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    public class TestActionInterceptorHandler extends ActionInterceptorHandler {
        @Override
        public BusinessActionContext getOrCreateActionContextAndResetToArguments(Class<?>[] parameterTypes, Object[] arguments) {
            return context;
        }

        @Override
        public String doTxActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessActionParam businessActionParam,
                                         BusinessActionContext actionContext) {
            return String.valueOf(branchId);
        }
    }

    public class TestTccThreePhaseHandler {
        public void prepare() {
        }
        public void commit() {
        }
        public void rollback() {
        }
    }

    public class MyTccHook implements TccHook {
        private final Logger LOGGER = LoggerFactory.getLogger(MyTccHook.class);
        @Override
        public void beforeTccPrepare(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations before tcc prepare");
        }

        @Override
        public void afterTccPrepare(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations after tcc prepare");
        }

        @Override
        public void beforeTccCommit(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations before tcc commit");
        }

        @Override
        public void afterTccCommit(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations after tcc commit");
        }

        @Override
        public void beforeTccRollback(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations before tcc rollback");
        }

        @Override
        public void afterTccRollback(String xid, Long branchId, String actionName, BusinessActionContext context) {
            LOGGER.info("do some business operations after tcc rollback");
        }
    }
}
