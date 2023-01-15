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

import io.seata.integrationapi.annotation.AspectTransactional;
import io.seata.integrationapi.interceptor.DefaultInvocationWrapper;
import io.seata.integrationapi.interceptor.InvocationWrapper;
import io.seata.integrationapi.interceptor.handler.GlobalTransactionalInterceptorHandler;
import io.seata.tm.api.FailureHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

/**
 * @author ruishansun
 */
public class AspectTransactionalInterceptor implements MethodInterceptor {

    private final FailureHandler failureHandler;
    private final AspectTransactional aspectTransactional;
    private final GlobalTransactionalInterceptorHandler globalTransactionalInterceptorHandler;

    private static final AspectTransactional DEFAULT_ASPECT_TRANSACTIONAL = new AspectTransactional();

    public AspectTransactionalInterceptor() {
        this(DEFAULT_ASPECT_TRANSACTIONAL);
    }

    public AspectTransactionalInterceptor(AspectTransactional aspectTransactional) {
        this(null, aspectTransactional);
    }

    public AspectTransactionalInterceptor(FailureHandler failureHandler) {
        this(failureHandler, DEFAULT_ASPECT_TRANSACTIONAL);
    }

    public AspectTransactionalInterceptor(FailureHandler failureHandler, AspectTransactional aspectTransactional) {
        this.failureHandler = failureHandler;
        this.aspectTransactional = aspectTransactional;
        this.globalTransactionalInterceptorHandler = new GlobalTransactionalInterceptorHandler(this.failureHandler, null, this.aspectTransactional);
    }

    @Nullable
    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        InvocationWrapper invocationWrapper = new DefaultInvocationWrapper(null, invocation.getThis(), specificMethod, invocation.getArguments());

        return this.globalTransactionalInterceptorHandler.invoke(invocationWrapper);
    }
}
