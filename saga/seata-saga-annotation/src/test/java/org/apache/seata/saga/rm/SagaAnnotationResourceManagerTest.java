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

import org.apache.seata.common.Constants;
import org.apache.seata.common.json.JsonUtil;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SagaAnnotationResourceManager.
 *
 * Focus areas:
 * 1. branchRollback with TccHook before/after callbacks
 * 2. before-rollback hook failure blocks compensation and returns retryable
 * 3. Action status driven rollback decision (none/running/success/failed/null)
 * 4. Different compensation return types (boolean, TwoPhaseResult, null)
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
        int compensateCount = 0;

        public boolean compensate(BusinessActionContext context) {
            compensateCount++;
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
        BranchType capturedBranchType = null;

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
            capturedBranchType = context.getBranchType();
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
        return createResource(actionName, methodName, new TestCompensationTarget());
    }

    private SagaAnnotationResource createResource(String actionName, String methodName, TestCompensationTarget target)
            throws NoSuchMethodException {
        SagaAnnotationResource resource = new SagaAnnotationResource();
        resource.setActionName(actionName);
        resource.setTargetBean(target);
        resource.setCompensationMethod(
                TestCompensationTarget.class.getDeclaredMethod(methodName, BusinessActionContext.class));
        resource.setCompensationArgsClasses(new Class<?>[] {BusinessActionContext.class});
        resource.setPhaseTwoCompensationKeys(new String[] {"unused"});
        return resource;
    }

    private String buildApplicationData(String actionStatus) {
        Map<String, Object> inner = new HashMap<>();
        if (actionStatus != null) {
            inner.put(Constants.ACTION_STATUS, actionStatus);
        }
        Map<String, Object> outer = new HashMap<>();
        outer.put(Constants.TX_ACTION_CONTEXT, inner);
        return JsonUtil.toJSONString(outer);
    }

    // ---- Tests for hook invocation in branchRollback (legacy: null action status) ----

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
        assertEquals(BranchType.SAGA_ANNOTATION, hook.capturedBranchType, "branchType should be set on context");
    }

    @Test
    void testBranchRollbackHookExceptionInBeforeReturnsRetryable() throws Exception {
        TrackingTccHook hook = new TrackingTccHook();
        hook.shouldThrowInBefore = true;
        TccHookManager.registerHook(hook);

        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("testAction2", "compensate", target);
        resourceManager.getManagedResources().put("testAction2", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 2L, "testAction2", null);

        assertEquals(
                BranchStatus.PhaseTwo_RollbackFailed_Retryable,
                status,
                "before-hook failure should block compensation and return retryable");
        assertEquals(0, target.compensateCount, "compensation should not execute when before-hook fails");
        assertTrue(hook.afterRollbackCalled, "afterTccRollback should still be called in finally");
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

        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("testAction13", "compensate", target);
        resourceManager.getManagedResources().put("testAction13", resource);

        BranchStatus status =
                resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid123", 13L, "testAction13", null);

        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, status);
        assertTrue(hook1.beforeRollbackCalled);
        assertTrue(
                hook2.beforeRollbackCalled,
                "second hook beforeTccRollback should still be called (hook exceptions are caught per hook)");
        assertTrue(hook2.afterRollbackCalled, "afterTccRollback should still call all hooks");
        assertEquals(0, target.compensateCount, "compensation should be skipped when a before-hook fails");
    }

    // ---- Tests for action status driven rollback decision ----

    @Test
    void testBranchRollbackEmptyRollbackWhenActionStatusNone() throws Exception {
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actNone", "compensate", target);
        resourceManager.getManagedResources().put("actNone", resource);

        BranchStatus status = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION, "xid", 1L, "actNone", buildApplicationData(Constants.ACTION_STATUS_NONE));

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status, "none status -> empty rollback");
        assertEquals(0, target.compensateCount, "compensation should be skipped on empty rollback");
    }

    @Test
    void testBranchRollbackRetryWhenActionStatusRunning() throws Exception {
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actRunning", "compensate", target);
        resourceManager.getManagedResources().put("actRunning", resource);

        BranchStatus status = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION,
                "xid",
                1L,
                "actRunning",
                buildApplicationData(Constants.ACTION_STATUS_RUNNING));

        assertEquals(
                BranchStatus.PhaseTwo_RollbackFailed_Retryable, status, "running status -> retry (anti-suspension)");
        assertEquals(0, target.compensateCount, "compensation should be skipped while phase one is running");
    }

    @Test
    void testBranchRollbackEmptyRollbackWhenActionStatusFailed() throws Exception {
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actFailed", "compensate", target);
        resourceManager.getManagedResources().put("actFailed", resource);

        BranchStatus status = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION,
                "xid",
                1L,
                "actFailed",
                buildApplicationData(Constants.ACTION_STATUS_FAILED));

        assertEquals(
                BranchStatus.PhaseTwo_Rollbacked, status, "failed status -> empty rollback (business self-handled)");
        assertEquals(0, target.compensateCount);
    }

    @Test
    void testBranchRollbackCompensateWhenActionStatusSuccess() throws Exception {
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actSuccess", "compensate", target);
        resourceManager.getManagedResources().put("actSuccess", resource);

        BranchStatus status = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION,
                "xid",
                1L,
                "actSuccess",
                buildApplicationData(Constants.ACTION_STATUS_SUCCESS));

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status, "success status -> execute compensation");
        assertEquals(1, target.compensateCount);
    }

    @Test
    void testBranchRollbackLegacyWhenActionStatusNull() throws Exception {
        // action status report disabled -> getActionStatus returns null -> compensate unconditionally (legacy)
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actNull", "compensate", target);
        resourceManager.getManagedResources().put("actNull", resource);

        BranchStatus status = resourceManager.branchRollback(BranchType.SAGA_ANNOTATION, "xid", 1L, "actNull", null);

        assertEquals(BranchStatus.PhaseTwo_Rollbacked, status);
        assertEquals(1, target.compensateCount);
    }

    // ---- End-to-end retry flow: null -> retry -> reported -> retry with updated data ----

    @Test
    void testBranchRollbackRetryFlowFromNullToReportedStatus() throws Exception {
        // Simulates the TC-driven retry sequence:
        // 1. initial rollback with no status reported yet (null) -> legacy compensation would run, but here we
        //    emulate the "phase one still running" window by reporting running on retry.
        TestCompensationTarget target = new TestCompensationTarget();
        SagaAnnotationResource resource = createResource("actE2e", "compensate", target);
        resourceManager.getManagedResources().put("actE2e", resource);

        // 1st retry: phase one still running -> retryable, no compensation
        BranchStatus first = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION, "xid", 1L, "actE2e", buildApplicationData(Constants.ACTION_STATUS_RUNNING));
        assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, first);
        assertEquals(0, target.compensateCount);

        // 2nd retry: phase one finally reported success -> compensation executes
        BranchStatus second = resourceManager.branchRollback(
                BranchType.SAGA_ANNOTATION, "xid", 1L, "actE2e", buildApplicationData(Constants.ACTION_STATUS_SUCCESS));
        assertEquals(BranchStatus.PhaseTwo_Rollbacked, second);
        assertEquals(1, target.compensateCount);
    }
}
