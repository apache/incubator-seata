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


import co.faao.plugin.transmission.response.MessageCoreResult;
import co.faao.plugin.transmission.response.base.Result;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.constants.Seata;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.transaction.Propagation;
import io.seata.tm.api.transaction.SuspendedResourcesHolder;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import io.seata.tm.api.transaction.TransactionInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.seata.tm.api.TransactionalExecutor.Code.RollbackDone;

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
        if (!Seata.EWELL_SEATA_STATE_IS_ON) {
            return business.execute();
        }
        // 1 get transactionInfo
        TransactionInfo txInfo = business.getTransactionInfo();
        if (txInfo == null) {
            throw new ShouldNeverHappenException("transactionInfo does not exist");
        }
        // 1.1 get or create a transaction
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        // 1.2 Handle the Transaction propatation and the branchType
        Propagation propagation = txInfo.getPropagation();
        SuspendedResourcesHolder suspendedResourcesHolder = null;
        try {
            switch (propagation) {
                case NOT_SUPPORTED:
                    suspendedResourcesHolder = tx.suspend(true);
                    return business.execute();
                case REQUIRES_NEW:
                    suspendedResourcesHolder = tx.suspend(true);
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
                        throw new TransactionException(
                                String.format("Existing transaction found for transaction marked with propagation 'never',xid = %s"
                                        ,RootContext.getXID()));
                    } else {
                        return business.execute();
                    }
                case MANDATORY:
                    if (!existingTransaction()) {
                        throw new TransactionException("No existing transaction found for transaction marked with propagation 'mandatory'");
                    }
                    break;
                default:
                    throw new TransactionException("Not Supported Propagation:" + propagation);
            }


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
                //code   add ccg ewell
                Integer status = null;
                String errorMessage = null;
                if (rs != null && (rs instanceof MessageCoreResult || rs instanceof Result)) {
                    if(rs instanceof MessageCoreResult) {
                        status = ((MessageCoreResult) rs).getStatus();
                        errorMessage = ((MessageCoreResult) rs).getMessage();
                    } else if(rs instanceof Result){
                        status = ((Result) rs).getStatus();
                        errorMessage = ((Result) rs).getMessage();
                    }
                    if (status != null && status.equals(0)) {
                        try {
                            completeTransactionAfterThrowing(txInfo, tx, new Exception(errorMessage));
                        } catch (TransactionalExecutor.ExecutionException e) {
                            TransactionalExecutor.Code code = e.getCode();
                            if (!code.toString().equals(RollbackDone.toString())) {
                                throw new TransactionalExecutor.ExecutionException(tx, e, TransactionalExecutor.Code.RollbackFailure,e);
                            }
                        }
                    } else {
                        // 4. everything is fine, commit.
                        commitTransaction(tx);
                    }
                } else {
                    // 4. everything is fine, commit.
                    commitTransaction(tx);
                }

                return rs;
            } finally {
                //5. clear
                triggerAfterCompletion();
                cleanUp();
            }
        } finally {
            tx.resume(suspendedResourcesHolder);
        }

    }

    public boolean existingTransaction() {
        return StringUtils.isNotEmpty(RootContext.getXID());

    }



    private void completeTransactionAfterThrowing(TransactionInfo txInfo, GlobalTransaction tx, Throwable originalException) throws TransactionalExecutor.ExecutionException {
        //roll back
        if (txInfo != null && txInfo.rollbackOn(originalException)) {
            try {
                rollbackTransaction(tx, originalException);
            } catch (TransactionException txe) {
                // Failed to rollback
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                        TransactionalExecutor.Code.RollbackFailure, originalException);
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

    private void rollbackTransaction(GlobalTransaction tx, Throwable originalException) throws TransactionException, TransactionalExecutor.ExecutionException {
        triggerBeforeRollback();
        tx.rollback();
        triggerAfterRollback();
        // 3.1 Successfully rolled back
        throw new TransactionalExecutor.ExecutionException(tx, GlobalStatus.RollbackRetrying.equals(tx.getLocalStatus())
            ? TransactionalExecutor.Code.RollbackRetrying : TransactionalExecutor.Code.RollbackDone, originalException);
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
