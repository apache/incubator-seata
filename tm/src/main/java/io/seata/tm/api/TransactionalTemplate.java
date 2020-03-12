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
package io.seata.tm.api;


import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.transaction.Propagation;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import io.seata.tm.api.transaction.TransactionInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template of executing business logic with a global transaction.
 *
 * @author sharajava
 */
public class TransactionalTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTemplate.class);


    /**
     * Execute object.
     *
     * @param business the business
     * @return the object
     * @throws TransactionalExecutor.ExecutionException the execution exception
     */
    public Object execute(TransactionalExecutor business) throws Throwable {
        // 1 get transactionInfo
        TransactionInfo txInfo = business.getTransactionInfo();
        if (txInfo == null) {
            throw new ShouldNeverHappenException("transactionInfo does not exist");
        }
        Propagation propagation = txInfo.getPropagation();
        String suspendedXid = null;
        try {
            switch (propagation) {
                case NOT_SUPPORTED:
                    suspendedXid = suspend();
                    return business.execute();
                case REQUIRES_NEW:
                    suspendedXid = suspend();
                    break;
                case SUPPORTS:
                    if (!existingTransaction()) {
                        return business.execute();
                    }
                    break;
                case REQUIRED:
                    break;
                case NEVER:
                    if (existingTransaction()) {
                        throw new IllegalArgumentException("Existing transaction found for transaction marked with propagation 'never'");
                    } else {
                        return business.execute();
                    }
                case MANDATORY:
                    if (!existingTransaction()) {
                        throw new IllegalArgumentException("No existing transaction found for transaction marked with propagation 'mandatory'");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Not Supported Propagation:" + propagation);
            }

            // 1.1 get or create a transaction
            GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

            try {

                // 2. begin transaction
                beginTransaction(txInfo, tx);

                Object rs = null;
                try {

                    // Do Your Business
                    rs = business.execute();

                } catch (Throwable ex) {

                    // 3.the needed business exception to rollback.
                    completeTransactionAfterThrowing(txInfo, tx, ex);
                    throw ex;
                }

                // 4. everything is fine, commit.
                commitTransaction(tx);

                return rs;
            } finally {
                //5. clear
                triggerAfterCompletion();
                cleanUp();
            }
        } finally {
            resume(suspendedXid);
        }

    }

    public boolean existingTransaction() {
        return !StringUtils.isEmpty(RootContext.getXID());

    }

    /**
     * Suspend the current transaction
     * @return the transaction xid has suspended
     */
    private String suspend() {
        String xid = RootContext.getXID();
        if (!StringUtils.isEmpty(xid)) {
            xid = RootContext.unbind();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Suspending current transaction,xid = {}",xid);
        }
        return xid;
    }

    /**
     * Resume the given transaction
     * @param suspendedXid the transaction xid to resume
     */
    private void resume(String suspendedXid) {
        if (!StringUtils.isEmpty(suspendedXid)) {
            RootContext.bind(suspendedXid);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Resumimg the transaction,xid = {}",suspendedXid);
            }
        }
    }

    private void completeTransactionAfterThrowing(TransactionInfo txInfo, GlobalTransaction tx, Throwable ex) throws TransactionalExecutor.ExecutionException {
        //roll back
        if (txInfo != null && txInfo.rollbackOn(ex)) {
            try {
                rollbackTransaction(tx, ex);
            } catch (TransactionException txe) {
                // Failed to rollback
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                        TransactionalExecutor.Code.RollbackFailure, ex);
            }
        } else {
            // not roll back on this exception, so commit
            commitTransaction(tx);
        }
    }

    private void commitTransaction(GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeCommit();
            tx.commit();
            triggerAfterCommit();
        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.CommitFailure);
        }
    }

    private void rollbackTransaction(GlobalTransaction tx, Throwable ex) throws TransactionException, TransactionalExecutor.ExecutionException {
        triggerBeforeRollback();
        tx.rollback();
        triggerAfterRollback();
        // 3.1 Successfully rolled back
        throw new TransactionalExecutor.ExecutionException(tx, GlobalStatus.RollbackRetrying.equals(tx.getLocalStatus())
            ? TransactionalExecutor.Code.RollbackRetrying : TransactionalExecutor.Code.RollbackDone, ex);
    }

    private void beginTransaction(TransactionInfo txInfo, GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeBegin();
            tx.begin(txInfo.getTimeOut(), txInfo.getName());
            triggerAfterBegin();
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.BeginFailure);

        }
    }

    private void triggerBeforeBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeBegin in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerAfterBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterBegin in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerBeforeRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeRollback in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerAfterRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterRollback in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerBeforeCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeCommit in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerAfterCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCommit in hook {}",e.getMessage(),e);
            }
        }
    }

    private void triggerAfterCompletion() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCompletion();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCompletion in hook {}",e.getMessage(),e);
            }
        }
    }

    private void cleanUp() {
        TransactionHookManager.clear();
    }

    private List<TransactionHook> getCurrentHooks() {
        return TransactionHookManager.getHooks();
    }

}
