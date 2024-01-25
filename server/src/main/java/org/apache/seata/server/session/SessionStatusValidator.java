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
package org.apache.seata.server.session;

import org.apache.seata.core.model.GlobalStatus;

/**
 * The type change status validator.
 *
 */
public class SessionStatusValidator {

    /**
     * is timeout global status
     *
     * @param status the global session
     */
    public static boolean isTimeoutGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.TimeoutRollbacked
                || status == GlobalStatus.TimeoutRollbackFailed
                || status == GlobalStatus.TimeoutRollbacking
                || status == GlobalStatus.TimeoutRollbackRetrying;
    }

    /**
     * is rollback global status
     *
     * @param status the global session
     */
    public static boolean isRollbackGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.Rollbacking
                || status == GlobalStatus.RollbackRetrying
                || status == GlobalStatus.Rollbacked
                || status == GlobalStatus.RollbackFailed
                || status == GlobalStatus.RollbackRetryTimeout;
    }

    /**
     * is commit global status
     *
     * @param status the global session
     */
    public static boolean isCommitGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.Committing
                || status == GlobalStatus.AsyncCommitting
                || status == GlobalStatus.CommitRetrying
                || status == GlobalStatus.Committed
                || status == GlobalStatus.CommitFailed
                || status == GlobalStatus.CommitRetryTimeout;
    }

    /**
     * check the relation of before status and after status
     *
     * @param before the global session
     * @param after the global session
     */
    public static boolean validateUpdateStatus(GlobalStatus before, GlobalStatus after) {
        if (isTimeoutGlobalStatus(before) && isCommitGlobalStatus(after)) {
            return false;
        }
        if (isCommitGlobalStatus(before) && isTimeoutGlobalStatus(after)) {
            return false;
        }
        if (isRollbackGlobalStatus(before) && isCommitGlobalStatus(after)) {
            return false;
        }
        if (isCommitGlobalStatus(before) && isRollbackGlobalStatus(after)) {
            return false;
        }
        return true;
    }
}
