/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
