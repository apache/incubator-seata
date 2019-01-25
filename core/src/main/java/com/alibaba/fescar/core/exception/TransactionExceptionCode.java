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

import java.util.HashMap;
import java.util.Map;

public enum TransactionExceptionCode {

    //
    Unknown,

    //
    LockKeyConflict,

    //
    IO,

    //
    BranchRollbackFailed_Retriable,

    //
    BranchRollbackFailed_Unretriable,

    //
    BranchRegisterFailed,

    //
    BranchReportFailed,

    //
    LockableCheckFailed,

    //
    BranchTransactionNotExist,

    //
    GlobalTransactionNotExist,

    //
    GlobalTransactionNotActive,

    //
    GlobalTransactionStatusInvalid,

    //
    FailedToSendBranchCommitRequest,

    //
    FailedToSendBranchRollbackRequest,

    //
    FailedToAddBranch,


    ;

    private static final Map<Integer, TransactionExceptionCode> MAP = new HashMap<>(values().length);

    static {
        for (TransactionExceptionCode code : values()) {
            MAP.put(code.ordinal(), code);
        }
    }

    public static TransactionExceptionCode get(byte ordinal) {
        return get((int) ordinal);
    }

    public static TransactionExceptionCode get(int ordinal) {
        TransactionExceptionCode code = MAP.get(ordinal);

        if (null == code) {
            throw new IllegalArgumentException("Unknown TransactionExceptionCode[" + ordinal + "]");
        }

        return code;
    }

}
