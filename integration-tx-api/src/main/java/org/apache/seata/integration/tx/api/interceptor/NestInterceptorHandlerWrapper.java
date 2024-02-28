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
package org.apache.seata.integration.tx.api.interceptor;

import java.lang.reflect.Method;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;


public class NestInterceptorHandlerWrapper implements InvocationWrapper {

    private ProxyInvocationHandler proxyInvocationHandler;

    private InvocationWrapper invocation;

    public NestInterceptorHandlerWrapper(ProxyInvocationHandler proxyInvocationHandler, InvocationWrapper invocation) {
        this.proxyInvocationHandler = proxyInvocationHandler;
        this.invocation = invocation;
    }

    @Override
    public Method getMethod() {
        return invocation.getMethod();
    }

    @Override
    public Object getProxy() {
        return invocation.getProxy();
    }

    @Override
    public Object getTarget() {
        return invocation.getTarget();
    }

    @Override
    public Object[] getArguments() {
        return invocation.getArguments();
    }

    @Override
    public Object proceed() throws Throwable {
        return proxyInvocationHandler.invoke(invocation);
    }
}
