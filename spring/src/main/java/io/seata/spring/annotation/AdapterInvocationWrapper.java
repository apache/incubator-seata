package io.seata.spring.annotation;

import java.lang.reflect.Method;

import io.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author leezongjie
 * @date 2023/2/11
 */
public class AdapterInvocationWrapper implements InvocationWrapper {

    private MethodInvocation invocation;

    public AdapterInvocationWrapper(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Method getMethod() {
        return invocation.getMethod();
    }

    @Override
    public Object getProxy() {
        return null;
    }

    @Override
    public Object getTarget() {
        return invocation.getThis();
    }

    @Override
    public Object[] getArguments() {
        return invocation.getArguments();
    }

    @Override
    public Object proceed() {
        try {
            return invocation.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException("try to proceed invocation error", throwable);
        }
    }
}
