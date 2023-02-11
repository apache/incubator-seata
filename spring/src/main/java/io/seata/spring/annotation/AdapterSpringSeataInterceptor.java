package io.seata.spring.annotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.Assert;

/**
 * @author leezongjie
 * @date 2023/2/11
 */
public class AdapterSpringSeataInterceptor implements MethodInterceptor, SeataInterceptor {

    private ProxyInvocationHandler proxyInvocationHandler;
    private int order;

    public AdapterSpringSeataInterceptor(ProxyInvocationHandler proxyInvocationHandler) {
        Assert.notNull(proxyInvocationHandler, "proxyInvocationHandler must not be null");
        this.proxyInvocationHandler = proxyInvocationHandler;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        AdapterInvocationWrapper adapterInvocationWrapper = new AdapterInvocationWrapper(invocation);
        Object result = proxyInvocationHandler.invoke(adapterInvocationWrapper);
        return result;
    }

    @Override
    public int getOrder() {
        if (SeataInterceptorPosition.Any == proxyInvocationHandler.getPosition()) {
            return proxyInvocationHandler.getOrder();
        }
        return order;
    }

    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return proxyInvocationHandler.getPosition();
    }

    @Override
    public String toString() {
        return proxyInvocationHandler.getClass().getName();
    }
}
