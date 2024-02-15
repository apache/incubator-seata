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
package io.seata.integration.tx.api.interceptor.handler;

import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.util.ClassUtils;
import org.apache.seata.rm.GlobalLockExecutor;
import org.apache.seata.tm.api.FailureHandler;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * The type Global transactional interceptor handler.
 */
public class GlobalTransactionalInterceptorHandler extends org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler {


    public GlobalTransactionalInterceptorHandler(FailureHandler failureHandler, Set<String> methodsToProxy) {
        super(failureHandler, methodsToProxy);
    }

    public GlobalTransactionalInterceptorHandler(FailureHandler failureHandler, Set<String> methodsToProxy, AspectTransactional aspectTransactional) {
        super(failureHandler, methodsToProxy, aspectTransactional);
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        Class<?> targetClass = invocation.getTarget().getClass();
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        if (specificMethod != null && !specificMethod.getDeclaringClass().equals(Object.class)) {
            final GlobalTransactional globalTransactionalAnnotation = getAnnotation(specificMethod, targetClass, GlobalTransactional.class);
            final GlobalLock globalLockAnnotation = getAnnotation(specificMethod, targetClass, GlobalLock.class);
            boolean localDisable = disable || (ATOMIC_DEGRADE_CHECK.get() && degradeNum >= degradeCheckAllowTimes);
            if (!localDisable) {
                if (globalTransactionalAnnotation != null || this.aspectTransactional != null) {
                    AspectTransactional transactional;
                    if (globalTransactionalAnnotation != null) {
                        transactional = new AspectTransactional(globalTransactionalAnnotation.timeoutMills(),
                                globalTransactionalAnnotation.name(), globalTransactionalAnnotation.rollbackFor(),
                                globalTransactionalAnnotation.rollbackForClassName(),
                                globalTransactionalAnnotation.noRollbackFor(),
                                globalTransactionalAnnotation.noRollbackForClassName(),
                                globalTransactionalAnnotation.propagation(),
                                globalTransactionalAnnotation.lockRetryInterval(),
                                globalTransactionalAnnotation.lockRetryTimes(),
                                globalTransactionalAnnotation.lockStrategyMode());
                    } else {
                        transactional = this.aspectTransactional;
                    }
                    return handleGlobalTransaction(invocation, transactional);
                } else if (globalLockAnnotation != null) {
                    return handleGlobalLock(invocation, globalLockAnnotation);
                }
            }
        }
        return invocation.proceed();
    }


    private Object handleGlobalLock(final InvocationWrapper methodInvocation, final GlobalLock globalLockAnno) throws Throwable {
        return globalLockTemplate.execute(new GlobalLockExecutor() {
            @Override
            public Object execute() throws Throwable {
                return methodInvocation.proceed();
            }

            @Override
            public GlobalLockConfig getGlobalLockConfig() {
                GlobalLockConfig config = new GlobalLockConfig();
                config.setLockRetryInterval(globalLockAnno.lockRetryInterval());
                config.setLockRetryTimes(globalLockAnno.lockRetryTimes());
                return config;
            }
        });
    }
}
