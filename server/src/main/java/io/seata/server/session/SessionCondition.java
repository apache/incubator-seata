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
package io.seata.server.session;

import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalTransactionDOCondition;

/**
 * The type Session condition.
 *
 * @author slievrly
 */
public class SessionCondition extends GlobalTransactionDOCondition {

    private Boolean withBranchSessions;

    /**
     * Instantiates a new Session condition.
     */
    public SessionCondition() {
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     */
    public SessionCondition(GlobalStatus... statuses) {
        super(statuses);
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param status the status
     * @param limit  the limit
     */
    public SessionCondition(GlobalStatus status, int limit) {
        super(status, limit);
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     * @param limit    the limit
     */
    public SessionCondition(GlobalStatus[] statuses, int limit) {
        super(statuses, limit);
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public SessionCondition(long overTimeAliveMills) {
        super(overTimeAliveMills);
    }


    public Boolean getWithBranchSessions() {
        return withBranchSessions;
    }

    public void setWithBranchSessions(Boolean withBranchSessions) {
        this.withBranchSessions = withBranchSessions;
    }
}
