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

import java.lang.reflect.Method;

import io.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author leezongjie
 * @date 2023/2/11
 */
public class AdapterInvocationWrapper implements InvocationWrapper {

    private MethodInvocation invocation;

    public AdapterInvocationWrapper(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Method getMethod() {
        return invocation.getMethod();
    }

    @Override
    public Object getProxy() {
        return null;
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
    public Object proceed() {
        try {
            return invocation.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException("try to proceed invocation error", throwable);
        }
    }
}
