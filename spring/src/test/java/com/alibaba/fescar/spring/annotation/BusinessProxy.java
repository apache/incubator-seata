package com.alibaba.fescar.spring.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class BusinessProxy implements InvocationHandler {
    private Object proxy;

    public BusinessProxy(Object proxy) {
        this.proxy = proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("before");
        Object result = null;
        try {
            result = method.invoke(proxy, args);
        } catch (Exception e) {

        }
        System.out.println("after");
        return result;
    }
}
