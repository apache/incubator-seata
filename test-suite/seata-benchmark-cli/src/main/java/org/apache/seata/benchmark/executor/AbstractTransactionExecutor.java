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
package org.apache.seata.benchmark.executor;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.benchmark.model.TransactionRecord;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.apache.seata.tm.api.TransactionalExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.apache.seata.tm.api.transaction.Propagation;
import org.apache.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base class for transaction executors implementing common transaction handling logic
 */
public abstract class AbstractTransactionExecutor implements TransactionExecutor {

    protected final BenchmarkConfig config;

    protected AbstractTransactionExecutor(BenchmarkConfig config) {
        this.config = config;
    }

    /**
     * Get the transaction name for this executor
     *
     * @return transaction name
     */
    protected abstract String getTransactionName();

    /**
     * Get the number of branches for this transaction
     *
     * @return number of branches
     */
    protected abstract int getBranchCount();

    /**
     * Execute the business logic for this transaction
     *
     * @throws Exception if execution fails
     */
    protected abstract void executeBusinessLogic() throws Exception;

    /**
     * Get the logger for the concrete executor class
     *
     * @return logger instance
     */
    protected abstract Logger getLogger();

    @Override
    public final TransactionRecord execute() {
        long startTime = System.currentTimeMillis();
        final String[] xidHolder = new String[1]; // Use array to capture xid in lambda
        String status = BenchmarkConstants.STATUS_UNKNOWN;
        int branchCount = getBranchCount();
        boolean success = false;

        TransactionalTemplate template = new TransactionalTemplate();

        try {
            template.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    // Capture XID after transaction begins
                    GlobalTransaction tx = GlobalTransactionContext.getCurrent();
                    if (tx != null) {
                        xidHolder[0] = tx.getXid();
                    }

                    // Execute business logic
                    executeBusinessLogic();

                    // Simulate rollback by throwing exception if configured
                    if (shouldRollback()) {
                        throw new BenchmarkRollbackException("Simulated rollback for benchmark");
                    }

                    return null;
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    TransactionInfo txInfo = new TransactionInfo();
                    txInfo.setTimeOut(BenchmarkConstants.TRANSACTION_TIMEOUT_MS);
                    txInfo.setName(getTransactionName());
                    txInfo.setPropagation(Propagation.REQUIRED);
                    return txInfo;
                }
            });

            status = BenchmarkConstants.STATUS_COMMITTED;
            success = true;

        } catch (BenchmarkRollbackException e) {
            // Simulated rollback - this is expected behavior
            status = BenchmarkConstants.STATUS_ROLLBACKED;
            getLogger().debug("Transaction rolled back as configured");
        } catch (Throwable e) {
            // Actual exception - transaction failed
            status = BenchmarkConstants.STATUS_FAILED;
            getLogger().warn("Transaction execution failed: {}", e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        return new TransactionRecord(xidHolder[0], status, duration, branchCount, success);
    }

    /**
     * Determine if the transaction should be rolled back based on rollback percentage
     *
     * @return true if should rollback
     */
    protected boolean shouldRollback() {
        return ThreadLocalRandom.current().nextInt(100) < config.getRollbackPercentage();
    }

    /**
     * Custom exception for simulated rollback in benchmark
     */
    private static class BenchmarkRollbackException extends RuntimeException {
        public BenchmarkRollbackException(String message) {
            super(message);
        }
    }
}
