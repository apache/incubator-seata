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
package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.interceptor.DefaultInvocationWrapper;
import io.seata.commonapi.interceptor.InvocationWrapper;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class DefaultInvocationHandler implements InvocationHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DefaultInvocationHandler.class);

    private ProxyInvocationHandler proxyInvocationHandler;
    private Object delegate;

    public DefaultInvocationHandler(ProxyInvocationHandler proxyInvocationHandler, Object delegate) {
        this.proxyInvocationHandler = proxyInvocationHandler;
        this.delegate = delegate;
    }

    /**
     * Dynamic proxy calls methods
     *
     * @param proxy  The generated proxy object
     * @param method Method of agency
     * @param args   Method parameter
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOGGER.info("bytebuddy proxy before");
        InvocationWrapper invocation = new DefaultInvocationWrapper(proxy, delegate, method, args);
        Object result;
        if (proxyInvocationHandler != null) {
            result = proxyInvocationHandler.invoke(invocation);
        } else {
            result = invocation.proceed();
        }
        LOGGER.info("bytebuddy proxy after");
        return result;
    }
}
