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
import java.util.LinkedHashSet;
import java.util.Set;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.spring.util.GlobalTransactionalCheck;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalTemplate;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author funkye
 */
public class HandleGlobalTransaction {

    public Object runTransaction(final MethodInvocation methodInvocation, final Object globalTrxAnno,
        final FailureHandler failureHandler, final TransactionalTemplate transactionalTemplate) throws Throwable {
        boolean succeed = true;
        try {
            return transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    return methodInvocation.proceed();
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    return getInfo(globalTrxAnno, methodInvocation.getMethod());
                }

            });
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            switch (code) {
                case RollbackDone:
                    throw e.getOriginalException();
                case BeginFailure:
                    succeed = false;
                    failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case CommitFailure:
                    succeed = false;
                    failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackFailure:
                    failureHandler.onRollbackFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackRetrying:
                    failureHandler.onRollbackRetrying(e.getTransaction(), e.getCause());
                    throw e.getCause();
                default:
                    throw new ShouldNeverHappenException(String.format("Unknown TransactionalExecutor.Code: %s", code));
            }
        } finally {
            GlobalTransactionalCheck.onDegradeCheck(succeed);
        }
    }

    private TransactionInfo getInfo(Object globalTrxAnno, Method method) {
        TransactionInfo transactionInfo = new TransactionInfo();
        AtTransactional globalTrx = convertAtTransactional(globalTrxAnno);
        transactionInfo.setTimeOut(globalTrx.getTimeoutMills());
        String name = globalTrx.getName();
        if (StringUtils.isNullOrEmpty(name)) {
            name = formatMethod(method);
        }
        transactionInfo.setName(name);
        transactionInfo.setPropagation(globalTrx.getPropagation());
        Set<RollbackRule> rollbackRules = new LinkedHashSet<>();
        for (Class<?> rbRule : globalTrx.getRollbackFor()) {
            rollbackRules.add(new RollbackRule(rbRule));
        }
        for (String rbRule : globalTrx.getRollbackForClassName()) {
            rollbackRules.add(new RollbackRule(rbRule));
        }
        for (Class<?> rbRule : globalTrx.getNoRollbackFor()) {
            rollbackRules.add(new NoRollbackRule(rbRule));
        }
        for (String rbRule : globalTrx.getNoRollbackForClassName()) {
            rollbackRules.add(new NoRollbackRule(rbRule));
        }
        transactionInfo.setRollbackRules(rollbackRules);
        return transactionInfo;
    }

    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        int in = 0;
        for (Class<?> clazz : params) {
            sb.append(clazz.getName());
            if (++in < params.length) {
                sb.append(", ");
            }
        }
        return sb.append(")").toString();
    }

    private AtTransactional convertAtTransactional(Object globalTrxAnno) {
        if (globalTrxAnno instanceof AtTransactional) {
            return (AtTransactional)globalTrxAnno;
        }

        if (globalTrxAnno instanceof GlobalTransactional) {
            GlobalTransactional globalTransactional = (GlobalTransactional)globalTrxAnno;
            AtTransactional atTransactional = new AtTransactional();
            atTransactional.setName(globalTransactional.name());
            atTransactional.setNoRollbackFor(globalTransactional.noRollbackFor());
            atTransactional.setPropagation(globalTransactional.propagation());
            atTransactional.setTimeoutMills(globalTransactional.timeoutMills());
            atTransactional.setNoRollbackForClassName(globalTransactional.noRollbackForClassName());
            atTransactional.setRollbackFor(globalTransactional.rollbackFor());
            atTransactional.setRollbackForClassName(globalTransactional.rollbackForClassName());
            return atTransactional;
        }

        return null;
    }

}
