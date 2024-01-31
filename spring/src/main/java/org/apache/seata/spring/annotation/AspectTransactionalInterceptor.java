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

import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.apache.seata.integration.tx.api.interceptor.DefaultInvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler;
import org.apache.seata.tm.api.FailureHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

/**
 * Aspect transactional interceptor. 
 *
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

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        InvocationWrapper invocationWrapper = new DefaultInvocationWrapper(null, invocation.getThis(), specificMethod, invocation.getArguments());
        return this.globalTransactionalInterceptorHandler.invoke(invocationWrapper);
    }
}
