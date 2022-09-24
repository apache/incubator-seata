package io.seata.commonapi.interceptor;

import io.seata.common.executor.Callback;

public class EmptyCallBack implements Callback<Object> {

    @Override
    public Object execute() throws Throwable {
        return null;
    }
}
