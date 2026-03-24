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
package org.apache.seata.saga.rm;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.fence.hook.TccHook;
import org.apache.seata.integration.tx.api.fence.hook.TccHookManager;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SagaAnnotationResourceManager.
 *
 * Focus areas:
 * 1. branchRollback with TccHook before/after callbacks
 * 2. Hook exception handling (should not break rollback)
 * 3. Different compensation return types (boolean, TwoPhaseResult, null)
 * 4. Compensation exception handling
 */
public class SagaAnnotationResourceManagerTest {

    static {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
    }

    private SagaAnnotationResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        TccHookManager.clear();
        resourceManager = new SagaAnnotationResourceManager();
    }

    @AfterEach
    void tearDown() {
        TccHookManager.clear();
        BusinessActionContextUtil.clear();
    }

    // ---- Helper classes ----

    public static class TestCompensationTarget {
        public boolean compensate(BusinessActionContext context) {
            return true;
        }

        public boolean compensateFail(BusinessActionContext context) {
            return false;
        }

        public Boolean compensateReturnNull(BusinessActionContext context) {
            return null;
        }

        public TwoPhaseResult compensateWithResultSuccess(BusinessActionContext context) {
            return new TwoPhaseResult(true, "ok");
        }

        public TwoPhaseResult compensateWithResultFail(BusinessActionContext context) {
            return new TwoPhaseResult(false, "fail");
        }

        public boolean compensateThrow(BusinessActionContext context) {
            throw new RuntimeException("compensation error");
        }
    }

    public static class TrackingTccHook implements TccHook {
        boolean beforeRollbackCalled = false;
        boolean afterRollbackCalled = false;
        boolean shouldThrowInBefore = false;
        boolean shouldThrowInAfter = false;

        @Override
        public void beforeTccPrepare(String xid, Long branchId, String actionName, BusinessActionContext context) {}

        @Override
        public void afterTccPrepare(String xid, Long branchId, String actionName, BusinessActionContext context) {}

        @Override
        public void beforeTccCommit(String xid, Long branchId, String actionName, BusinessActionContext context) {}

        @Override
        public void afterTccCommit(String xid, Long branchId, String actionName, BusinessActionContext context) {}

        @Override
        public void beforeTccRollback(String xid, Long branchId, String actionName, BusinessActionContext context) {
            beforeRollbackCalled = true;
            if (shouldThrowInBefore) {
                throw new RuntimeException("hook error in beforeTccRollback");
            }
        }

        @Override
        public void afterTccRollback(String xid, Long branchId, String actionName, BusinessActionContext context) {
            afterRollbackCalled = true;
            if (shouldThrowInAfter) {
                throw new RuntimeException("hook error in afterTccRollback");
            }
        }
    }

    private SagaAnnotationResource createResource(String actionName, String methodName) throws NoSuchMethodException {
        SagaAnnotationResource resource = new SagaAnnotationResource();
        resource.setActionName(actionName);
        resource.setTargetBean(new TestCompensationTarget());
        resource.setCompensationMethod(
                TestCompensationTarget.class.getDeclaredMethod(methodName, BusinessActionContext.class));
        resource.setCompensationArgsClasses(new Class<?>[] {BusinessActionContext.class});
        resource.setPhaseTwoCompensationKeys(new String[] {"unused"});
        return resource;
    }

    // ---- Tests for hook invocation in branchRollback ----

    @Test
    void testBranchRollbackWithHooksInvoked() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        TccHookManager.registerHook(hook);

        SagaAnnotationResource resource = createResource("testAction", "compensate");
        resourceManager.getManagedResources().put("testAction", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 1L, "testAction", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertTrue(hook.beforeRollbackCalled, "beforeTccRollback should be called");
        assertTrue(hook.afterRollbackCalled, "afterTccRollback should be called");
    }

    @Test
    void testBranchRollbackHookExceptionInBeforeDoesNotBreakRollback() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        hook.shouldThrowInBefore = true;
        TccHookManager.registerHook(hook);

        SagaAnnotationResource resource = createResource("testAction2", "compensate");
        resourceManager.getManagedResources().put("testAction2", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 2L, "testAction2", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertTrue(hook.beforeRollbackCalled);
        assertTrue(hook.afterRollbackCalled, "afterTccRollback should still be called even if before throws");
    }

    @Test
    void testBranchRollbackHookExceptionInAfterDoesNotBreakRollback() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        hook.shouldThrowInAfter = true;
        TccHookManager.registerHook(hook);

        SagaAnnotationResource resource = createResource("testAction3", "compensate");
        resourceManager.getManagedResources().put("testAction3", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 3L, "testAction3", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertTrue(hook.beforeRollbackCalled);
        assertTrue(hook.afterRollbackCalled);
    }

    @Test
    void testBranchRollbackWithoutHooks() throws Exception {
        SagaAnnotationResource resource = createResource("testAction4", "compensate");
        resourceManager.getManagedResources().put("testAction4", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 4L, "testAction4", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
    }

    // ---- Tests for different compensation return types ----

    @Test
    void testBranchRollbackCompensationReturnsFalse() throws Exception {
        SagaAnnotationResource resource = createResource("testAction5", "compensateFail");
        resourceManager.getManagedResources().put("testAction5", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 5L, "testAction5", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
    }

    @Test
    void testBranchRollbackCompensationReturnsNull() throws Exception {
        SagaAnnotationResource resource = createResource("testAction6", "compensateReturnNull");
        resourceManager.getManagedResources().put("testAction6", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 6L, "testAction6", null);

        // null return is treated as success
        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
    }

    @Test
    void testBranchRollbackCompensationReturnsTwoPhaseResultSuccess() throws Exception {
        SagaAnnotationResource resource = createResource("testAction7", "compensateWithResultSuccess");
        resourceManager.getManagedResources().put("testAction7", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 7L, "testAction7", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
    }

    @Test
    void testBranchRollbackCompensationReturnsTwoPhaseResultFail() throws Exception {
        SagaAnnotationResource resource = createResource("testAction8", "compensateWithResultFail");
        resourceManager.getManagedResources().put("testAction8", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 8L, "testAction8", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
    }

    @Test
    void testBranchRollbackCompensationThrowsException() throws Exception {
        SagaAnnotationResource resource = createResource("testAction9", "compensateThrow");
        resourceManager.getManagedResources().put("testAction9", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 9L, "testAction9", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
    }

    // ---- Tests for hook invocation with compensation failure ----

    @Test
    void testBranchRollbackWithHooksWhenCompensationFails() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        TccHookManager.registerHook(hook);

        SagaAnnotationResource resource = createResource("testAction10", "compensateFail");
        resourceManager.getManagedResources().put("testAction10", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 10L, "testAction10", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
        assertTrue(hook.beforeRollbackCalled, "beforeTccRollback should be called even when compensation fails");
        assertTrue(hook.afterRollbackCalled, "afterTccRollback should be called in finally block");
    }

    @Test
    void testBranchRollbackWithHooksWhenCompensationThrows() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        TccHookManager.registerHook(hook);

        SagaAnnotationResource resource = createResource("testAction11", "compensateThrow");
        resourceManager.getManagedResources().put("testAction11", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 11L, "testAction11", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
        assertTrue(hook.beforeRollbackCalled);
        assertTrue(hook.afterRollbackCalled, "afterTccRollback should be called in finally block even on exception");
    }

    // ---- Tests for multiple hooks ----

    @Test
    void testBranchRollbackWithMultipleHooks() throws Exception {
        TrackingTccHook hook1 = new TrackingTccHook();
        TrackingTccHook hook2 = new TrackingTccHook();
        TccHookManager.registerHook(hook1);
        TccHookManager.registerHook(hook2);

        SagaAnnotationResource resource = createResource("testAction12", "compensate");
        resourceManager.getManagedResources().put("testAction12", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 12L, "testAction12", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertTrue(hook1.beforeRollbackCalled);
        assertTrue(hook1.afterRollbackCalled);
        assertTrue(hook2.beforeRollbackCalled);
        assertTrue(hook2.afterRollbackCalled);
    }

    @Test
    void testBranchRollbackFirstHookThrowsSecondStillCalled() throws Exception {
        TrackingTccHook hook1 = new TrackingTccHook();
        hook1.shouldThrowInBefore = true;
        TrackingTccHook hook2 = new TrackingTccHook();
        TccHookManager.registerHook(hook1);
        TccHookManager.registerHook(hook2);

        SagaAnnotationResource resource = createResource("testAction13", "compensate");
        resourceManager.getManagedResources().put("testAction13", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 13L, "testAction13", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertTrue(hook1.beforeRollbackCalled);
        // hook2.beforeRollbackCalled is NOT guaranteed because hook1 throws
        // the loop iterates hooks sequentially, and the exception breaks the loop
        assertTrue(hook2.afterRollbackCalled, "afterTccRollback should still call all hooks");
    }
}
