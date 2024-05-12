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
package io.seata.core.exception;

/**
 * The type Transaction exception.
 */
@Deprecated
public class TransactionException extends org.apache.seata.core.exception.TransactionException {

    public TransactionException(TransactionExceptionCode code) {
        super(code.convertTransactionExceptionCode());
    }

    public TransactionException(TransactionExceptionCode code, Throwable cause) {
        super(code.convertTransactionExceptionCode(), cause);
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(TransactionExceptionCode code, String message) {
        super(code.convertTransactionExceptionCode(), message);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionException(TransactionExceptionCode code, String message, Throwable cause) {
        super(code.convertTransactionExceptionCode(), message, cause);
    }
}
