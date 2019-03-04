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

package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.tm.api.transaction.TransactionHook;
import com.alibaba.fescar.tm.api.transaction.TransactionHookExecuteException;
import com.alibaba.fescar.tm.api.transaction.TransactionHookManager;

import java.util.List;

/**
 * Template of executing business logic with a global transaction.
 */
public class TransactionalTemplate {

    /**
     * Execute object.
     *
     * @param business the business
     * @return the object
     * @throws TransactionalExecutor.ExecutionException the execution exception
     */
    public Object execute(TransactionalExecutor business) throws TransactionalExecutor.ExecutionException {

        // 1. get or create a transaction
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {

            // 2. begin transaction
            try {
                triggerBeforeBegin();
                tx.begin(business.timeout(), business.name());
                triggerAfterBegin();
            } catch (TransactionException txe) {
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                        TransactionalExecutor.Code.BeginFailure);

            }
            Object rs = null;
            try {

                // Do Your Business
                rs = business.execute();

            } catch (Throwable ex) {

                // 3. any business exception, rollback.
                try {
                    tx.rollback();

                    // 3.1 Successfully rolled back
                    throw new TransactionalExecutor.ExecutionException(tx, TransactionalExecutor.Code.RollbackDone, ex);

                } catch (TransactionException txe) {
                    // 3.2 Failed to rollback
                    throw new TransactionalExecutor.ExecutionException(tx, txe,
                            TransactionalExecutor.Code.RollbackFailure, ex);

                }

            }
            // 4. everything is fine, commit.
            try {
                triggerBeforeCommit();
                tx.commit();

            } catch (TransactionException txe) {
                // 4.1 Failed to commit
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                        TransactionalExecutor.Code.CommitFailure);

            }
            try {
                triggerAfterCommit();
            } catch (Exception e) {
                throw new TransactionHookExecuteException(e);
            }
            return rs;
        }finally {
            cleanupAfterCommit();
        }
    }


    private void triggerBeforeBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            hook.beforeBegin();
        }
    }

    private void triggerAfterBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            hook.afterBegin();
        }
    }
    private void triggerBeforeCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            hook.beforeCommit();
        }
    }

    private void triggerAfterCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            hook.afterCommit();
        }
    }

    private void cleanupAfterCommit() {
        TransactionHookManager.clear();
    }

    private List<TransactionHook> getCurrentHooks() {
        return TransactionHookManager.getHooks();
    }

}
