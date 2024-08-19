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

import java.lang.reflect.Method;

import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;


public class AdapterInvocationWrapper implements InvocationWrapper {

    private final MethodInvocation invocation;
    private final Object proxy;
    public AdapterInvocationWrapper(MethodInvocation invocation) {
        this.invocation = invocation;
        if (invocation instanceof ReflectiveMethodInvocation) {
            ReflectiveMethodInvocation reflectiveInvocation = (ReflectiveMethodInvocation) invocation;
            this.proxy = reflectiveInvocation.getProxy();
        } else {
            this.proxy = null;
        }
    }

    @Override
    public Method getMethod() {
        return invocation.getMethod();
    }

    @Override
    public Object getProxy() {
        return this.proxy;
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
    public Object proceed() throws Throwable {
        return invocation.proceed();
    }
}
