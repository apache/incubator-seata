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

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;

/**
 * Template of executing business logic with a global transaction.
 */
public class TransactionalTemplate {

    /**
     * Execute object.
     *
     * @param business the business
     * @return the object
     * @throws Throwable the execution exception, maybe business exception OR com.alibaba.fescar.tm.api.TransactionalExecutor.ExecutionException
     */
    public Object execute(TransactionalExecutor business) throws Throwable {

        Object rs;
        // someone has already start a global transaction, just execute business
        if (null != RootContext.getXID()) {
            rs = business.execute();
        } else {
            if(null != GlobalTransactionContext.getCurrent()) {
                throw new Exception("GlobalTransactionContext ThreadLocal variables was leaked!");
            }
            // 1. bind a transaction to current thread
            GlobalTransaction tx = GlobalTransactionContext.createNew();

            // 2. begin transaction
            try {
                tx.begin(business.timeout(), business.name());

            } catch (TransactionException txe) {
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.BeginFailure);

            }

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

                } finally {
                    GlobalTransactionContext.clean();
                }

            }

            // 4. everything is fine, commit.
            try {
                tx.commit();

            } catch (TransactionException txe) {
                // 4.1 Failed to commit
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.CommitFailure);

            } finally {
                GlobalTransactionContext.clean();
            }
        }
        return rs;
    }

}
