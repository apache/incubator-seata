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
package io.seata.core.exception;

/**
 * The type BranchTransaction exception.
 *
 * @author will
 */
public class BranchTransactionException extends TransactionException{

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     */
    public BranchTransactionException(TransactionExceptionCode code) {
        super(code);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param cause the cause
     */
    public BranchTransactionException(TransactionExceptionCode code, Throwable cause) {
        super(code, cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     */
    public BranchTransactionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param message the message
     */
    public BranchTransactionException(TransactionExceptionCode code, String message) {
        super(code, message);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param cause the cause
     */
    public BranchTransactionException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public BranchTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param message the message
     * @param cause the cause
     */
    public BranchTransactionException(TransactionExceptionCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
