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
package io.seata.commonapi.interceptor;

import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
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
    public Object proceed() {
        try {
            return method.invoke(delegate, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
