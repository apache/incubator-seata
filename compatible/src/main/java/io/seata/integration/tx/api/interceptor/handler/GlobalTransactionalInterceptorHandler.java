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

import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.util.ClassUtils;
import org.apache.seata.rm.GlobalLockExecutor;
import org.apache.seata.spring.annotation.GlobalLock;
import org.apache.seata.spring.annotation.GlobalTransactional;
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
            final GlobalTransactional globalTransactionalAnnotationOld = getAnnotation(specificMethod, targetClass, GlobalTransactional.class);
            final org.apache.seata.spring.annotation.GlobalTransactional globalTransactionalAnnotationNew = getAnnotation(specificMethod, targetClass, org.apache.seata.spring.annotation.GlobalTransactional.class);
            final GlobalLock globalLockAnnotationOld = getAnnotation(specificMethod, targetClass, GlobalLock.class);
            final org.apache.seata.spring.annotation.GlobalLock globalLockAnnotationNew = getAnnotation(specificMethod, targetClass, org.apache.seata.spring.annotation.GlobalLock.class);

            boolean localDisable = disable || (ATOMIC_DEGRADE_CHECK.get() && degradeNum >= degradeCheckAllowTimes);
            if (!localDisable) {
                if (globalTransactionalAnnotationOld != null || globalTransactionalAnnotationNew != null || this.aspectTransactional != null) {
                    AspectTransactional transactional;
                    if (globalTransactionalAnnotationOld != null) {
                        transactional = new AspectTransactional(globalTransactionalAnnotationOld.timeoutMills(),
                                globalTransactionalAnnotationOld.name(), globalTransactionalAnnotationOld.rollbackFor(),
                                globalTransactionalAnnotationOld.rollbackForClassName(),
                                globalTransactionalAnnotationOld.noRollbackFor(),
                                globalTransactionalAnnotationOld.noRollbackForClassName(),
                                globalTransactionalAnnotationOld.propagation(),
                                globalTransactionalAnnotationOld.lockRetryInterval(),
                                globalTransactionalAnnotationOld.lockRetryTimes(),
                                globalTransactionalAnnotationOld.lockStrategyMode());
                    } else if (globalTransactionalAnnotationNew != null) {
                        transactional = new AspectTransactional(globalTransactionalAnnotationNew.timeoutMills(),
                                globalTransactionalAnnotationNew.name(), globalTransactionalAnnotationNew.rollbackFor(),
                                globalTransactionalAnnotationNew.rollbackForClassName(),
                                globalTransactionalAnnotationNew.noRollbackFor(),
                                globalTransactionalAnnotationNew.noRollbackForClassName(),
                                globalTransactionalAnnotationNew.propagation(),
                                globalTransactionalAnnotationNew.lockRetryInterval(),
                                globalTransactionalAnnotationNew.lockRetryTimes(),
                                globalTransactionalAnnotationNew.lockStrategyMode());
                    } else {
                        transactional = this.aspectTransactional;
                    }
                    return handleGlobalTransaction(invocation, transactional);
                } else if (globalLockAnnotationOld != null || globalLockAnnotationNew != null) {
                    if (globalLockAnnotationOld != null) {
                        return handleGlobalLockOld(invocation, globalLockAnnotationOld);
                    } else {
                        return handleGlobalLock(invocation, globalLockAnnotationNew);
                    }
                }
            }

        }
        return invocation.proceed();
    }


    private Object handleGlobalLockOld(final InvocationWrapper methodInvocation, final GlobalLock globalLockAnno) throws Throwable {
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
