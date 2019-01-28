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

/**
 * Status of global transaction.
 */
public enum GlobalStatus {

    /**
     * Un known global status.
     */
    // Unknown
    UnKnown,

    /**
     * The Begin.
     */
    // PHASE 1: can accept new branch registering.
    Begin,

    /**
     * PHASE 2: Running Status: may be changed any time.
     */
    // Committing.
    Committing,

    /**
     * The Commit retrying.
     */
    // Retrying commit after a recoverable failure.
    CommitRetrying,

    /**
     * Rollbacking global status.
     */
    // Rollbacking
    Rollbacking,

    /**
     * The Rollback retrying.
     */
    // Retrying rollback after a recoverable failure.
    RollbackRetrying,

    /**
     * The Timeout rollbacking.
     */
    // Rollbacking since timeout
    TimeoutRollbacking,

    /**
     * The Timeout rollback retrying.
     */
    // Retrying rollback (since timeout) after a recoverable failure.
    TimeoutRollbackRetrying,

    /**
     * PHASE 2: Final Status: will NOT change any more.
     */
    // Finally: global transaction is successfully committed.
    Committed,

    /**
     * The Commit failed.
     */
    // Finally: failed to commit
    CommitFailed,

    /**
     * The Rollbacked.
     */
    // Finally: global transaction is successfully rollbacked.
    Rollbacked,

    /**
     * The Rollback failed.
     */
    // Finally: failed to rollback
    RollbackFailed,

    /**
     * The Timeout rollbacked.
     */
    // Finally: global transaction is successfully rollbacked since timeout.
    TimeoutRollbacked,

    /**
     * The Timeout rollback failed.
     */
    // Finally: failed to rollback since timeout
    TimeoutRollbackFailed,

    /**
     * The Finished.
     */
    // Not managed in session MAP any more
    Finished;

    private static final Map<Integer, GlobalStatus> MAP = new HashMap<>(values().length);

    static {
        for (GlobalStatus status : values()) {
            MAP.put(status.ordinal(), status);
        }
    }

    /**
     * Get global status.
     *
     * @param ordinal the ordinal
     * @return the global status
     */
    public static GlobalStatus get(byte ordinal) {
        return get((int) ordinal);
    }

    /**
     * Get global status.
     *
     * @param ordinal the ordinal
     * @return the global status
     */
    public static GlobalStatus get(int ordinal) {
        GlobalStatus status = MAP.get(ordinal);

        if (null == status) {
            throw new IllegalArgumentException("Unknown GlobalStatus[" + ordinal + "]");
        }

        return status;
    }
}
