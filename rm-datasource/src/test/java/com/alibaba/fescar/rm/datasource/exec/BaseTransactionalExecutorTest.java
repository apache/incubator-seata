package com.alibaba.fescar.rm.datasource.exec;

import java.sql.Statement;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fescar.rm.GlobalLockTemplate;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.StatementProxy;

public class BaseTransactionalExecutorTest {

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testExecuteWithGlobalLockSet() throws Exception {
        
        //initial objects
        ConnectionProxy connectionProxy = new ConnectionProxy(null,null,null);
        StatementProxy statementProxy = new StatementProxy<>(connectionProxy, null);
       
        BaseTransactionalExecutor<Object,Statement> baseTransactionalExecutor = new BaseTransactionalExecutor<Object,Statement>(statementProxy, null, null) {
            @Override
            protected Object doExecute(Object... args) throws Throwable {
                return null;
            }
        };
        GlobalLockTemplate<Object> globalLockLocalTransactionalTemplate = new GlobalLockTemplate<>();
        
        // not in global lock context
        new Callable<Object>() {
            
            @Override
            public Object call() throws Exception {
                try {
                    baseTransactionalExecutor.execute(new Object());
                    Assert.assertTrue("conectionContext set!",!connectionProxy.isGlobalLockRequire());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }.call();
        
        
        //in global lock context
        globalLockLocalTransactionalTemplate.execute(new Callable<Object>() {
            
            @Override
            public Object call() throws Exception {
                try {
                    baseTransactionalExecutor.execute(new Object());
                    Assert.assertTrue("conectionContext not set!",connectionProxy.isGlobalLockRequire());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        
        
    }
    
}
