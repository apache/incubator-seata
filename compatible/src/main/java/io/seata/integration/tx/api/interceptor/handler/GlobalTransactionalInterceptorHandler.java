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
import org.apache.seata.spring.annotation.GlobalLock;
import org.apache.seata.spring.annotation.GlobalTransactional;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * The type Global transactional interceptor handler.
 */
@Deprecated
public class GlobalTransactionalInterceptorHandler extends org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler {

    public GlobalTransactionalInterceptorHandler(org.apache.seata.tm.api.FailureHandler failureHandler, Set<String> methodsToProxy) {
        super(failureHandler, methodsToProxy);
    }

    public GlobalTransactionalInterceptorHandler(org.apache.seata.tm.api.FailureHandler failureHandler, Set<String> methodsToProxy,
        AspectTransactional aspectTransactional) {
        super(failureHandler, methodsToProxy, aspectTransactional);
    }

    @Override
    public GlobalLockConfig getGlobalLockConfig(Method method, Class<?> targetClass) {
        final GlobalLock globalLockAnno = getAnnotation(method, targetClass, GlobalLock.class);
        if (globalLockAnno != null) {
            GlobalLockConfig config = new GlobalLockConfig();
            config.setLockRetryInterval(globalLockAnno.lockRetryInterval());
            config.setLockRetryTimes(globalLockAnno.lockRetryTimes());
            return config;
        } else {
            return null;
        }
    }

    @Override
    public AspectTransactional getAspectTransactional(Method method, Class<?> targetClass) {
        final GlobalTransactional globalTransactionalAnnotation =
            getAnnotation(method, targetClass, GlobalTransactional.class);
        return globalTransactionalAnnotation != null ? new AspectTransactional(
            globalTransactionalAnnotation.timeoutMills(), globalTransactionalAnnotation.name(),
            globalTransactionalAnnotation.rollbackFor(), globalTransactionalAnnotation.rollbackForClassName(),
            globalTransactionalAnnotation.noRollbackFor(), globalTransactionalAnnotation.noRollbackForClassName(),
            globalTransactionalAnnotation.propagation(), globalTransactionalAnnotation.lockRetryInterval(),
            globalTransactionalAnnotation.lockRetryTimes(), globalTransactionalAnnotation.lockStrategyMode()) : null;
    }

}