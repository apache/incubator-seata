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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


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
    public Object proceed() throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = t.getCause();
            }
            throw t;
        }

    }
}
