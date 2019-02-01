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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;

/**
 * Status of branch transaction.
 */
public enum BranchStatus {

    /**
     * Unknown branch status.
     */
    // Unknown
    Unknown,

    /**
     * The Registered.
     */
    // Registered to TC.
    Registered,

    /**
     * The Phase one done.
     */
    // Branch logic is successfully done at phase one.
    PhaseOne_Done,

    /**
     * The Phase one failed.
     */
    // Branch logic is failed at phase one.
    PhaseOne_Failed,

    /**
     * The Phase one timeout.
     */
    // Branch logic is NOT reported for a timeout.
    PhaseOne_Timeout,

    /**
     * The Phase two committed.
     */
    // Commit logic is successfully done at phase two.
    PhaseTwo_Committed,

    /**
     * The Phase two commit failed retryable.
     */
    // Commit logic is failed but retryable.
    PhaseTwo_CommitFailed_Retryable,

    /**
     * The Phase two commit failed unretryable.
     */
    // Commit logic is failed and NOT retryable.
    PhaseTwo_CommitFailed_Unretryable,

    /**
     * The Phase two rollbacked.
     */
    // Rollback logic is successfully done at phase two.
    PhaseTwo_Rollbacked,

    /**
     * The Phase two rollback failed retryable.
     */
    // Rollback logic is failed but retryable.
    PhaseTwo_RollbackFailed_Retryable,

    /**
     * The Phase two rollback failed unretryable.
     */
    // Rollback logic is failed but NOT retryable.
    PhaseTwo_RollbackFailed_Unretryable;

    private static final Map<Integer, BranchStatus> MAP = new HashMap<>(values().length);

    static {
        for (BranchStatus status : values()) {
            MAP.put(status.ordinal(), status);
        }
    }

    /**
     * Get branch status.
     *
     * @param ordinal the ordinal
     * @return the branch status
     */
    public static BranchStatus get(byte ordinal) {
        return get((int) ordinal);
    }

    /**
     * Get branch status.
     *
     * @param ordinal the ordinal
     * @return the branch status
     */
    public static BranchStatus get(int ordinal) {
        BranchStatus status = MAP.get(ordinal);

        if (null == status) {
            throw new ShouldNeverHappenException("Unknown BranchStatus[" + ordinal + "]");
        }

        return status;
    }
}
