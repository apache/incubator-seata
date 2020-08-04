package io.seata.rm.hook;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GlobalTransactionHookTest {

    public static String xid="test";
    @BeforeAll
    public static void initHook(){
        RootContext.bind(xid);
    }

    @Test
    public void testCallBack() {
        GlobalTransactionHookManager.registerHook(new GlobalTransactionHook(){
            @Override
            public void afterCommit() {
                System.out.println("afterCommit1");
            }
        });

        GlobalTransactionHookManager.registerHook(new GlobalTransactionHook(){
            @Override
            public void afterCommit() {
                System.out.println("afterCommit2");
            }
        });

        GlobalTransactionHookManager.registerHook(new GlobalTransactionHook(){
            @Override
            public void afterRollback() {
                System.out.println("afterRollback");
            }
        });

        for(GlobalTransactionHook hook : GlobalTransactionHookManager.getHooks(xid)){
            hook.afterCommit();
            hook.afterRollback();
        }


    }

}
