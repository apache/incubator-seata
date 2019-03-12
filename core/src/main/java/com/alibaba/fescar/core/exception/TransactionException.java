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
package com.alibaba.fescar.core.exception;

/**
 * The type Transaction exception.
 */
public class TransactionException extends Exception {

    /**
     * The Code.
     */
    protected TransactionExceptionCode code = TransactionExceptionCode.Unknown;

    /**
     * Gets code.
     *
     * @return the code
     */
    public TransactionExceptionCode getCode() {
        return code;
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     */
    public TransactionException(TransactionExceptionCode code) {
        this.code = code;
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code  the code
     * @param cause the cause
     */
    public TransactionException(TransactionExceptionCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     */
    public TransactionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code    the code
     * @param message the message
     */
    public TransactionException(TransactionExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param cause the cause
     */
    public TransactionException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code    the code
     * @param message the message
     * @param cause   the cause
     */
    public TransactionException(TransactionExceptionCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
