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
package io.seata.spring.interceptor;

import io.seata.spring.annotation.AtTransactional;
import io.seata.spring.annotation.HandleGlobalTransaction;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author funkye
 */
public class AtGlobalTransactionalInterceptor implements MethodInterceptor {
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();
    private static final AtTransactional DEFAULT_AT_TRANSACTIONAL = new AtTransactional();
    private final HandleGlobalTransaction handleGlobalTransaction = new HandleGlobalTransaction();
    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final FailureHandler failureHandler;
    private final AtTransactional atTransactional;

    public AtGlobalTransactionalInterceptor(FailureHandler failureHandler, AtTransactional atTransactional) {
        this.failureHandler = null == failureHandler ? DEFAULT_FAIL_HANDLER : failureHandler;
        this.atTransactional = null == atTransactional ? DEFAULT_AT_TRANSACTIONAL : atTransactional;
    }

    public AtGlobalTransactionalInterceptor() {
        this.failureHandler = DEFAULT_FAIL_HANDLER;
        this.atTransactional = DEFAULT_AT_TRANSACTIONAL;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return handleGlobalTransaction.runTransaction(invocation, atTransactional, failureHandler,
            transactionalTemplate);
    }
}
