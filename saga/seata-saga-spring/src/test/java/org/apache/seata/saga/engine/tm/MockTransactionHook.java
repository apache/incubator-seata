package org.apache.seata.saga.engine.tm;

import org.apache.seata.tm.api.transaction.TransactionHook;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class MockTransactionHook implements TransactionHook {
    @Override
    public void beforeBegin() {

    }

    @Override
    public void afterBegin() {

    }

    @Override
    public void beforeCommit() {

    }

    @Override
    public void afterCommit() {

    }

    @Override
    public void beforeRollback() {

    }

    @Override
    public void afterRollback() {

    }

    @Override
    public void afterCompletion() {

    }
}
