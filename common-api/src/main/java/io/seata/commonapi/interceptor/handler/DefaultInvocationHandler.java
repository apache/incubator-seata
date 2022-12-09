package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.interceptor.DefaultInvocationWrapper;
import io.seata.commonapi.interceptor.InvocationWrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class DefaultInvocationHandler implements InvocationHandler {

    private ProxyInvocationHandler proxyInvocationHandler;
    private Object delegate;

    public DefaultInvocationHandler(ProxyInvocationHandler proxyInvocationHandler, Object delegate) {
        this.proxyInvocationHandler = proxyInvocationHandler;
        this.delegate = delegate;
    }

    /**
     * 动态代理调用方法
     *
     * @param proxy  生成的代理对象
     * @param method 代理的方法
     * @param args   方法参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //TODO log
        System.out.println("bytebuddy proxy before");
        InvocationWrapper invocation = new DefaultInvocationWrapper(proxy, delegate, method, args);
        Object result;
        if (proxyInvocationHandler != null) {
            result = proxyInvocationHandler.invoke(invocation);
        } else {
            result = invocation.proceed();
        }
        System.out.println("bytebuddy proxy after");
        return result;
    }
}
