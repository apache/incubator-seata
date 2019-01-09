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

public class TransactionException extends Exception {

    protected TransactionExceptionCode code = TransactionExceptionCode.Unknown;

    public TransactionExceptionCode getCode() {
        return code;
    }

    public TransactionException(TransactionExceptionCode code) {
        this.code = code;
    }
    public TransactionException(TransactionExceptionCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(TransactionExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionException(TransactionExceptionCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
