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

    // Unknown
    UnKnown,

    // PHASE 1: can accept new branch registering.
    Begin,


    /** PHASE 2: Running Status: may be changed any time. */

    // Committing.
    Committing,

    // Retrying commit after a recoverable failure.
    CommitRetrying,

    // Rollbacking
    Rollbacking,

    // Retrying rollback after a recoverable failure.
    RollbackRetrying,

    // Rollbacking since timeout
    TimeoutRollbacking,

    // Retrying rollback (since timeout) after a recoverable failure.
    TimeoutRollbackRetrying,


    /** PHASE 2: Final Status: will NOT change any more. */

    // Finally: global transaction is successfully committed.
    Committed,

    // Finally: failed to commit
    CommitFailed,

    // Finally: global transaction is successfully rollbacked.
    Rollbacked,

    // Finally: failed to rollback
    RollbackFailed,

    // Finally: global transaction is successfully rollbacked since timeout.
    TimeoutRollbacked,

    // Finally: failed to rollback since timeout
    TimeoutRollbackFailed,

    // Not managed in session MAP any more
    Finished;

    private static final Map<Integer, GlobalStatus> MAP = new HashMap<>(values().length);

    static {
        for (GlobalStatus status : values()) {
            MAP.put(status.ordinal(), status);
        }
    }

    public static GlobalStatus get(byte ordinal) {
        return get((int) ordinal);
    }

    public static GlobalStatus get(int ordinal) {
        GlobalStatus status = MAP.get(ordinal);

        if (null == status) {
            throw new IllegalArgumentException("Unknown GlobalStatus[" + ordinal + "]");
        }

        return status;
    }
}
