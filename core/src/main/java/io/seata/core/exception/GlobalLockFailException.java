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
 * @author zhang.siyu
 */
public class GlobalLockFailException extends BranchTransactionException {

    public GlobalLockFailException(TransactionExceptionCode code) {
        super(code);
    }

    public GlobalLockFailException(TransactionExceptionCode code, Throwable cause) {
        super(code, cause);
    }

    public GlobalLockFailException(String message) {
        super(message);
    }

    public GlobalLockFailException(TransactionExceptionCode code, String message) {
        super(code, message);
    }

    public GlobalLockFailException(Throwable cause) {
        super(cause);
    }

    public GlobalLockFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public GlobalLockFailException(TransactionExceptionCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
