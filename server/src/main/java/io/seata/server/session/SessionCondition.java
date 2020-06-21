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

/**
 * The type Session condition.
 *
 * @author slievrly
 */
public class SessionCondition {
    private Long transactionId;
    private GlobalStatus[] statuses;
    private Long overTimeAliveMills;
    private GlobalTableSortField sortField;
    private SortOrder sortOrder;

    /**
     * Instantiates a new Session condition.
     */
    public SessionCondition() {
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param status the status
     */
    public SessionCondition(GlobalStatus status) {
        statuses = new GlobalStatus[] {status};
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     */
    public SessionCondition(GlobalStatus[] statuses) {
        this.statuses = statuses;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public SessionCondition(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public GlobalStatus[] getStatuses() {
        return statuses;
    }

    public void setStatuses(GlobalStatus[] statuses) {
        this.statuses = statuses;
    }

    public Long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    public void setOverTimeAliveMills(Long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    public GlobalTableSortField getSortField() {
        return sortField;
    }

    public void setSortField(GlobalTableSortField sortField) {
        this.sortField = sortField;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
