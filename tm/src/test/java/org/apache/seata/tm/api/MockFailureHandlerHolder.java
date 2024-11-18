package org.apache.seata.tm.api;

public class MockFailureHandlerHolder implements FailureHandler{
    @Override
    public void onBeginFailure(BaseTransaction tx, Throwable cause) {

    }

    @Override
    public void onCommitFailure(BaseTransaction tx, Throwable cause) {

    }

    @Override
    public void onRollbackFailure(BaseTransaction tx, Throwable originalException) {

    }

    @Override
    public void onRollbacking(BaseTransaction tx, Throwable originalException) {

    }
}
