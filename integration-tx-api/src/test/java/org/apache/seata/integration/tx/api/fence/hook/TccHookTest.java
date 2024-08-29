package org.apache.seata.integration.tx.api.fence.hook;


import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TccHookTest {
    private TccHook tccHook;
    private String xid;
    private Long branchId;
    private String actionName;
    private BusinessActionContext context;

    @BeforeEach
    public void setUp() {
        tccHook = mock(TccHook.class);
        xid = "test-xid";
        branchId = 12345L;
        actionName = "testAction";
        context = new BusinessActionContext();
        TccHookManager.clear();
        TccHookManager.registerHook(tccHook);
    }

    @Test
    public void testBeforeTccPrepare() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccPrepare(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccPrepare(xid, branchId, actionName, context);
    }

    @Test
    public void testAfterTccPrepare() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccPrepare(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccPrepare(xid, branchId, actionName, context);
    }

    @Test
    public void testBeforeTccCommit() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccCommit(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccCommit(xid, branchId, actionName, context);
    }

    @Test
    public void testAfterTccCommit() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccCommit(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccCommit(xid, branchId, actionName, context);
    }

    @Test
    public void testBeforeTccRollback() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.beforeTccRollback(xid, branchId, actionName, context);
        }
        verify(tccHook).beforeTccRollback(xid, branchId, actionName, context);
    }

    @Test
    public void testAfterTccRollback() {
        for (TccHook hook : TccHookManager.getHooks()) {
            hook.afterTccRollback(xid, branchId, actionName, context);
        }
        verify(tccHook).afterTccRollback(xid, branchId, actionName, context);
    }

    @Test
    public void testTccPrepareFinally() {
        Assertions.assertThrowsExactly(TccHookTestException.class, () -> {
            try {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.beforeTccPrepare(xid, branchId, actionName, context);
                }
                verify(tccHook).beforeTccPrepare(xid, branchId, actionName, context);
                throw new TccHookTestException("tcc hook test exception!");
            } finally {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.afterTccPrepare(xid, branchId, actionName, context);
                }
                verify(tccHook).afterTccPrepare(xid, branchId, actionName, context);
            }
        });
    }

    @Test
    public void testTccCommitFinally() {
        Assertions.assertThrowsExactly(TccHookTestException.class, () -> {
            try {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.beforeTccCommit(xid, branchId, actionName, context);
                }
                verify(tccHook).beforeTccCommit(xid, branchId, actionName, context);
                throw new TccHookTestException("tcc hook test exception!");
            } finally {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.afterTccCommit(xid, branchId, actionName, context);
                }
                verify(tccHook).afterTccCommit(xid, branchId, actionName, context);
            }
        });
    }

    @Test
    public void testTccRollbackFinally() {
        Assertions.assertThrowsExactly(TccHookTestException.class, () -> {
            try {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.beforeTccRollback(xid, branchId, actionName, context);
                }
                verify(tccHook).beforeTccRollback(xid, branchId, actionName, context);
                throw new TccHookTestException("tcc hook test exception!");
            } finally {
                for (TccHook hook : TccHookManager.getHooks()) {
                    hook.afterTccRollback(xid, branchId, actionName, context);
                }
                verify(tccHook).afterTccRollback(xid, branchId, actionName, context);
            }
        });
    }

    public static class TccHookTestException extends RuntimeException {
        public TccHookTestException(String message) {
            super(message);
        }
    }
}
