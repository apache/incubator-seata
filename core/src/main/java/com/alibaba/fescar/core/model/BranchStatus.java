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

    // Commit logic is failed but retriable.
    PhaseTwo_CommitFailed_Retriable,

    // Commit logic is failed and NOT retriable.
    PhaseTwo_CommitFailed_Unretriable,

    // Rollback logic is successfully done at phase two.
    PhaseTwo_Rollbacked,

    // Rollback logic is failed but retriable.
    PhaseTwo_RollbackFailed_Retriable,

    // Rollback logic is failed but NOT retriable.
    PhaseTwo_RollbackFailed_Unretriable;

    public static BranchStatus get(byte ordinal) {
        return get((int) ordinal);
    }

    public static BranchStatus get(int ordinal) {
        for (BranchStatus branchStatus : BranchStatus.values()) {
            if (branchStatus.ordinal() == ordinal) {
                return branchStatus;
            }
        }
        throw new ShouldNeverHappenException("Unknown BranchStatus[" + ordinal + "]");
    }
}
