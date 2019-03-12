/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.rm.GlobalLockTemplate;
import com.alibaba.fescar.tm.api.DefaultFailureHandlerImpl;
import com.alibaba.fescar.tm.api.FailureHandler;
import com.alibaba.fescar.tm.api.TransactionalExecutor;
import com.alibaba.fescar.tm.api.TransactionalTemplate;

/**
 * The type Global transactional interceptor.
 */
public class GlobalTransactionalInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptor.class);
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final GlobalLockTemplate<Object> globalLockTemplate = new GlobalLockTemplate<>();
    private final FailureHandler failureHandler;

    /**
     * Instantiates a new Global transactional interceptor.
     *
     * @param failureHandler the failure handler
     */
    public GlobalTransactionalInterceptor(FailureHandler failureHandler) {
        if (null == failureHandler) {
            failureHandler = DEFAULT_FAIL_HANDLER;
        }
        this.failureHandler = failureHandler;
    }

    @Override
    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        final GlobalTransactional globalTrxAnno = getAnnotation(methodInvocation.getMethod(),
            GlobalTransactional.class);
        final GlobalLock globalLockAnno = getAnnotation(methodInvocation.getMethod(), GlobalLock.class);
        if (globalTrxAnno != null) {
            return handleGlobalTransaction(methodInvocation, globalTrxAnno);
        } else if (globalLockAnno != null) {
            return handleGlobalLock(methodInvocation);
        } else {
            return methodInvocation.proceed();
        }
    }

    private Object handleGlobalLock(final MethodInvocation methodInvocation) throws Exception {
        return globalLockTemplate.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    return methodInvocation.proceed();
                } catch (Throwable e) {
                    if (e instanceof Exception) {
                        throw (Exception)e;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private Object handleGlobalTransaction(final MethodInvocation methodInvocation,
                                           final GlobalTransactional globalTrxAnno) throws Throwable {
        try {
            return transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    return methodInvocation.proceed();
                }

                @Override
                public int timeout() {
                    return globalTrxAnno.timeoutMills();
                }

                @Override
                public String name() {
                    String name = globalTrxAnno.name();
                    if (!StringUtils.isNullOrEmpty(name)) {
                        return name;
                    }
                    return formatMethod(methodInvocation.getMethod());
                }
            });
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            switch (code) {
                case RollbackDone:
                    throw e.getOriginalException();
                case BeginFailure:
                    failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case CommitFailure:
                    failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackFailure:
                    failureHandler.onRollbackFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                default:
                    throw new ShouldNeverHappenException("Unknown TransactionalExecutor.Code: " + code);

            }
        }
    }

    private <T extends Annotation> T getAnnotation(Method method, Class<T> clazz) {
        if (method == null) {
            return null;
        }
        return method.getAnnotation(clazz);
    }

    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder();

        String methodName = method.getName();
        Class<?>[] params = method.getParameterTypes();
        sb.append(methodName);
        sb.append("(");

        int paramPos = 0;
        for (Class<?> clazz : params) {
            sb.append(clazz.getName());
            if (++paramPos < params.length) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
