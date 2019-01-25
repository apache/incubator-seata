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

package com.alibaba.fescar.core.model;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;

import java.util.HashMap;
import java.util.Map;

/**
 * Status of branch transaction.
 */
public enum BranchStatus {

    // Unknown
    Unknown,

    // Registered to TC.
    Registered,

    // Branch logic is successfully done at phase one.
    PhaseOne_Done,

    // Branch logic is failed at phase one.
    PhaseOne_Failed,

    // Branch logic is NOT reported for a timeout.
    PhaseOne_Timeout,

    // Commit logic is successfully done at phase two.
    PhaseTwo_Committed,

    // Commit logic is failed but retryable.
    PhaseTwo_CommitFailed_Retryable,

    // Commit logic is failed and NOT retryable.
    PhaseTwo_CommitFailed_Unretryable,

    // Rollback logic is successfully done at phase two.
    PhaseTwo_Rollbacked,

    // Rollback logic is failed but retryable.
    PhaseTwo_RollbackFailed_Retryable,

    // Rollback logic is failed but NOT retryable.
    PhaseTwo_RollbackFailed_Unretryable;

    private static final Map<Integer, BranchStatus> MAP = new HashMap<>(values().length);

    static {
        for (BranchStatus status : values()) {
            MAP.put(status.ordinal(), status);
        }
    }

    public static BranchStatus get(byte ordinal) {
        return get((int) ordinal);
    }

    public static BranchStatus get(int ordinal) {
        BranchStatus status = MAP.get(ordinal);

        if (null == status) {
            throw new ShouldNeverHappenException("Unknown BranchStatus[" + ordinal + "]");
        }

        return status;
    }
}
