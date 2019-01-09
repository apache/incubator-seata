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

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.thread.NamedThreadFactory;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.rm.RMClientAT;
import com.alibaba.fescar.tm.TMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demo code for API usage.
 */
public class DemoCode {

    private void init() {
        String applicationId = "my_app";
        String transactionServiceGroup = "my_tx_group";
        TMClient.init(applicationId, transactionServiceGroup);
        RMClientAT.init(applicationId, transactionServiceGroup);

    }

    /**
     * Demo code for High Level API (TransactionTemplate) usage.
     *
     * @throws Throwable business exception
     */
    public void demoByHighLevelAPI() throws Throwable {
        // 0. init
        init();

        // 0.1 prepare for the template instance
        TransactionalTemplate transactionalTemplate = new TransactionalTemplate();

        // 0.2 prepare for the failure handler (this is optional)
        FailureHandler failureHandler = new MyFailureHandler();

        try {
            // run you business in template
            transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    // Do Your BusinessService
                    businessCall1();
                    businessCall2();
                    businessCall3();
                    return null;
                }

                @Override
                public int timeout() {
                    return 30000;
                }

                @Override
                public String name() {
                    return "my_tx_instance";
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

    private static class MyFailureHandler implements FailureHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(MyFailureHandler.class);

        @Override
        public void onBeginFailure(GlobalTransaction tx, Throwable cause) {
            LOGGER.warn("Failed to begin transaction. ", cause);

        }

        @Override
        public void onCommitFailure(final GlobalTransaction tx, Throwable cause) {
            LOGGER.warn("Failed to commit transaction[" + tx.getXid() + "]", cause);
            final ScheduledExecutorService schedule = new ScheduledThreadPoolExecutor(1,
                new NamedThreadFactory("BusinessRetryCommit", 1, true));
            schedule.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        tx.commit();
                        schedule.shutdownNow();

                    } catch (TransactionException ignore) {

                    }

                }
            }, 0, 5, TimeUnit.SECONDS);


        }

        @Override
        public void onRollbackFailure(final GlobalTransaction tx, Throwable cause) {
            LOGGER.warn("Failed to begin transaction[" + tx.getXid() + "]", cause);
            final ScheduledExecutorService schedule = new ScheduledThreadPoolExecutor(1,
                new NamedThreadFactory("BusinessRetryRollback", 1, true));
            schedule.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        tx.rollback();
                        schedule.shutdownNow();

                    } catch (TransactionException ignore) {

                    }

                }
            }, 0, 5, TimeUnit.SECONDS);

        }

    }

    /**
     * Demo code for Low Level API (GlobalTransaction) usage.
     *
     * @throws Throwable business exception
     */
    public void demoByLowLevelAPI() throws Throwable {
        // 0. init
        init();

        // 1. get or create a transaction
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        // 2. begin transaction
        try {
            tx.begin(30000, "my_tx_instance");

        } catch (TransactionException txe) {
            // TODO: Handle the transaction begin failure.

        }

        Object rs = null;
        try {

            // Do Your BusinessService
            businessCall1();
            businessCall2();
            businessCall3();

        } catch (Throwable ex) {

            // 3. any business exception, rollback.
            try {
                tx.rollback();

                // 3.1 throw the business exception out.
                throw ex;

            } catch (TransactionException txe) {
                // TODO: Handle the transaction rollback failure.

            }

        }

        // 4. everything is fine, commit.
        try {
            tx.commit();

        } catch (TransactionException txe) {
            // TODO: Handle the transaction rollback failure.

        }
    }

    private static void businessCall1() {

    }

    private static void businessCall2() {

    }

    private static void businessCall3() {

    }
}
