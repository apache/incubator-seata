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
package org.apache.seata.integration.tx.api.interceptor.handler;

import org.apache.seata.integration.tx.api.interceptor.DefaultInvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class DefaultInvocationHandler implements InvocationHandler {

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
        InvocationWrapper invocation = new DefaultInvocationWrapper(proxy, delegate, method, args);
        Object result;
        if (proxyInvocationHandler != null) {
            result = proxyInvocationHandler.invoke(invocation);
        } else {
            result = invocation.proceed();
        }
        return result;
    }
}
