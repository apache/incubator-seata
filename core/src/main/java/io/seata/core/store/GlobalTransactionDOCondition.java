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
package io.seata.core.store;

import io.seata.common.util.CollectionUtils;
import io.seata.core.model.GlobalStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * The type GlobalTransactionDO condition.
 *
 * @author wang.liang
 */
public class GlobalTransactionDOCondition {

    //region Fields

    /**
     * condition: in status
     */
    protected GlobalStatus[] statuses;

    /**
     * condition: begin_time < currentTimeMillis() - overTimeAliveMills
     */
    protected long overTimeAliveMills = 0;
    /**
     * condition: filter is timeout or not timeout
     * null: all
     * true: timeout data
     * false: not timeout data
     */
    protected Boolean timeoutData;

    protected GlobalTableSortField sortField;
    protected SortOrder sortOrder;

    protected int limit = 0;

    //endregion

    //region Constructor

    /**
     * Instantiates a new Session condition.
     */
    public GlobalTransactionDOCondition() {
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     */
    public GlobalTransactionDOCondition(GlobalStatus... statuses) {
        this.statuses = statuses;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param status the status
     * @param limit  the limit
     */
    public GlobalTransactionDOCondition(GlobalStatus status, int limit) {
        this.statuses = new GlobalStatus[]{status};
        this.limit = limit;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     * @param limit    the limit
     */
    public GlobalTransactionDOCondition(GlobalStatus[] statuses, int limit) {
        this.statuses = statuses;
        this.limit = limit;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public GlobalTransactionDOCondition(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    //endregion

    //region Public

    /**
     * Match data.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    public boolean isMatch(GlobalTransactionDO globalTransactionDO) {
        if (globalTransactionDO == null) {
            return false;
        }

        // where
        // status in (?, ?, ?)
        if (statuses != null && statuses.length > 0) {
            if (!this.hasStatus(globalTransactionDO.getStatus())) {
                return false; // un match
            }
        }
        // begin_time < System.currentTimeMillis() - ?
        if (overTimeAliveMills > 0) {
            if (globalTransactionDO.getBeginTime() >= System.currentTimeMillis() - overTimeAliveMills) {
                return false; // un match
            }
        }
        //  true: begin_time  < System.currentTimeMillis() - timeout
        // false: begin_time >= System.currentTimeMillis() - timeout
        if (timeoutData != null) {
            boolean isTimeout = globalTransactionDO.getBeginTime() < System.currentTimeMillis() - globalTransactionDO.getTimeout();
            return timeoutData ? isTimeout : !isTimeout;
        }

        return true;
    }

    public <T extends GlobalTransactionDO> List<T> filter(List<T> globalTransactionDOs) {
        List<T> found = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(globalTransactionDOs)) {
            for (T globalTransactionDO : globalTransactionDOs) {
                if (this.isMatch(globalTransactionDO)) {
                    found.add(globalTransactionDO);
                }
            }
        }
        return found;
    }

    public <T extends GlobalTransactionDO> List<T> sort(List<T> globalTransactionDOs) {
        if (CollectionUtils.isEmpty(globalTransactionDOs)) {
            return new ArrayList<>();
        }

        if (sortField == null) {
            return globalTransactionDOs;
        }

        globalTransactionDOs.sort((a, b) -> {
            switch (sortField) {
                case XID:
                    return this.compareTo(a.getXid(), b.getXid());
                case TRANSACTION_ID:
                    return this.compareTo(a.getTransactionId(), b.getTransactionId());
                case STATUS:
                    return this.compareTo(a.getStatus(), b.getStatus());
                case APPLICATION_ID:
                    return this.compareTo(a.getApplicationId(), b.getApplicationId());
                case TRANSACTION_SERVICE_GROUP:
                    return this.compareTo(a.getTransactionServiceGroup(), b.getTransactionServiceGroup());
                case TRANSACTION_NAME:
                    return this.compareTo(a.getTransactionName(), b.getTransactionName());
                case TIMEOUT:
                    return this.compareTo(a.getTimeout(), b.getTimeout());
                case BEGIN_TIME:
                    return this.compareTo(a.getBeginTime(), b.getBeginTime());
                //case APPLICATION_DATA:
                //    return this.compareTo(a.getApplicationData(), b.getApplicationData());
                case GMT_CREATE:
                    return this.compareTo(a.getGmtCreate(), b.getGmtCreate());
                case GMT_MODIFIED:
                    return this.compareTo(a.getGmtModified(), b.getGmtModified());
                default:
                    return 0;
            }
        });
        return globalTransactionDOs;
    }

    //endregion

    //region Private

    private int compareTo(Comparable a, Comparable b) {
        int ret;
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                ret = -1;
            }
        } else {
            if (b == null) {
                ret = 1;
            } else {
                ret = a.compareTo(b);
                if (ret == 0) {
                    return 0;
                }
            }
        }

        if (sortOrder == SortOrder.DESC) {
            if (ret > 0) {
                return -1;
            } else {
                return 1;
            }
        }

        return ret;
    }

    private boolean hasStatus(GlobalStatus status0) {
        for (GlobalStatus status : statuses) {
            if (status == status0) {
                return true;
            }
        }
        return false;
    }

    //endregion

    //region Gets and Sets

    public GlobalStatus[] getStatuses() {
        return statuses;
    }

    public void setStatuses(GlobalStatus[] statuses) {
        this.statuses = statuses;
    }

    public long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    public void setOverTimeAliveMills(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    public Boolean getTimeoutData() {
        return timeoutData;
    }

    public void setTimeoutData(Boolean timeoutData) {
        this.timeoutData = timeoutData;
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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    //endregion
}
