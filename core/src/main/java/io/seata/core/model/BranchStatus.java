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
package io.seata.core.model;

import java.util.HashMap;
import java.util.Map;

import io.seata.common.exception.ShouldNeverHappenException;

/**
 * Status of branch transaction.
 *
 * @author sharajava
 */
public enum BranchStatus {

    /**
     * Unknown branch status.
     */
    // Unknown
    Unknown(0),

    /**
     * The Registered.
     */
    // Registered to TC.
    Registered(1),

    /**
     * The Phase one done.
     */
    // Branch logic is successfully done at phase one.
    PhaseOne_Done(2),

    /**
     * The Phase one failed.
     */
    // Branch logic is failed at phase one.
    PhaseOne_Failed(3),

    /**
     * The Phase one timeout.
     */
    // Branch logic is NOT reported for a timeout.
    PhaseOne_Timeout(4),

    /**
     * The Phase two committed.
     */
    // Commit logic is successfully done at phase two.
    PhaseTwo_Committed(5),

    /**
     * The Phase two commit failed retryable.
     */
    // Commit logic is failed but retryable.
    PhaseTwo_CommitFailed_Retryable(6),

    /**
     * The Phase two commit failed unretryable.
     */
    // Commit logic is failed and NOT retryable.
    PhaseTwo_CommitFailed_Unretryable(7),

    /**
     * The Phase two rollbacked.
     */
    // Rollback logic is successfully done at phase two.
    PhaseTwo_Rollbacked(8),

    /**
     * The Phase two rollback failed retryable.
     */
    // Rollback logic is failed but retryable.
    PhaseTwo_RollbackFailed_Retryable(9),

    /**
     * The Phase two rollback failed unretryable.
     */
    // Rollback logic is failed but NOT retryable.
    PhaseTwo_RollbackFailed_Unretryable(10);

    private int code;

    BranchStatus(int code) {
        this.code = code;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    private static final Map<Integer, BranchStatus> MAP = new HashMap<>(values().length);

    static {
        for (BranchStatus status : values()) {
            MAP.put(status.getCode(), status);
        }
    }

    /**
     * Get branch status.
     *
     * @param code the code
     * @return the branch status
     */
    public static BranchStatus get(byte code) {
        return get((int)code);
    }

    /**
     * Get branch status.
     *
     * @param code the code
     * @return the branch status
     */
    public static BranchStatus get(int code) {
        BranchStatus status = MAP.get(code);

        if (null == status) {
            throw new ShouldNeverHappenException("Unknown BranchStatus[" + code + "]");
        }

        return status;
    }
}
