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

/**
 * Callback for executing business logic in a global transaction.
 */
public interface TransactionalExecutor {

    /**
     * Execute the business logic here.
     *
     * @return What the business logic returns.
     * @throws Throwable Any throwable during executing.
     */
    Object execute() throws Throwable;

    /**
     * Global transacton timeout in MILLISECONDS.
     * @return timeout in MILLISECONDS.
     */
    int timeout();

    /**
     * Given name of the global transaction instance.
     * @return Given name.
     */
    String name();

    enum Code {

        //
        BeginFailure,

        //
        CommitFailure,

        //
        RollbackFailure,

        //
        RollbackDone
    }

    class ExecutionException extends Exception {

        private GlobalTransaction transaction;

        private Code code;

        private Throwable originalException;

        public ExecutionException(GlobalTransaction transaction, Throwable cause, Code code) {
            this(transaction, cause, code, null);
        }

        public ExecutionException(GlobalTransaction transaction, Code code, Throwable originalException) {
            this(transaction, null, code, originalException);
        }
        public ExecutionException(GlobalTransaction transaction, Throwable cause, Code code, Throwable originalException) {
            this(transaction, null, cause, code, originalException);
        }


        public ExecutionException(GlobalTransaction transaction, String message, Throwable cause, Code code, Throwable originalException) {
            super(message, cause);
            this.transaction = transaction;
            this.code = code;
            this.originalException = originalException;
        }

        public GlobalTransaction getTransaction() {
            return transaction;
        }

        public void setTransaction(GlobalTransaction transaction) {
            this.transaction = transaction;
        }

        public Code getCode() {
            return code;
        }

        public void setCode(Code code) {
            this.code = code;
        }

        public Throwable getOriginalException() {
            return originalException;
        }

        public void setOriginalException(Throwable originalException) {
            this.originalException = originalException;
        }
    }
}
