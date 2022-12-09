package io.seata.commonapi.interceptor;

import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class DefaultInvocationWrapper implements InvocationWrapper {
    private Object proxy;
    private Object delegate;
    private Method method;
    private Object[] args;

    public DefaultInvocationWrapper(Object proxy, Object delegate, Method method, Object[] args) {
        this.proxy = proxy;
        this.delegate = delegate;
        this.method = method;
        this.args = args;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object getProxy() {
        return proxy;
    }

    @Override
    public Object getTarget() {
        return delegate;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object proceed() {
        try {
            return method.invoke(delegate, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
