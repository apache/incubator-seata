package io.seata.commonapi.interceptor.handler;

import io.seata.common.util.CollectionUtils;
import io.seata.commonapi.interceptor.InvocationWrapper;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public abstract class AbstractProxyInvocationHandler implements ProxyInvocationHandler {

    protected abstract Object doInvoke(InvocationWrapper invocation) throws Throwable;

    @Override
    public Object invoke(InvocationWrapper invocation) throws Throwable {
        if (CollectionUtils.isNotEmpty(getMethodsToProxy()) && !getMethodsToProxy().contains(invocation.getMethod().getName())) {
            return invocation.proceed();
        }
        return doInvoke(invocation);
    }

}
