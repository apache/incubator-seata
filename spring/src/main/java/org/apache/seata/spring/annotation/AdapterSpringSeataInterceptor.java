/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.annotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.seata.integration.tx.api.interceptor.SeataInterceptor;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;


public class AdapterSpringSeataInterceptor implements MethodInterceptor, SeataInterceptor, Ordered {

    private ProxyInvocationHandler proxyInvocationHandler;

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
        return proxyInvocationHandler.getOrder();
    }

    @Override
    public void setOrder(int order) {
        proxyInvocationHandler.setOrder(order);
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
