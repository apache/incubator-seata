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
 * The type GlobalTransaction condition.
 *
 * @author wang.liang
 */
public class GlobalTransactionCondition extends AbstractQuerier<GlobalTransactionModel> {

    //region Fields

    /**
     * condition: status in (?, ?, ..., ?)
     */
    protected GlobalStatus[] statuses;

    /**
     * condition: begin_time < currentTimeMillis() - overTimeAliveMills
     */
    protected long overTimeAliveMills = 0;

    /**
     * condition: filter is timeout or not timeout
     * -  null: all
     * -  true: timeout data.     begin_time  < System.currentTimeMillis() - timeout
     * - false: not timeout data. begin_time >= System.currentTimeMillis() - timeout
     */
    protected Boolean timeoutData;

    //endregion

    //region Constructor

    /**
     * Instantiates a new condition.
     */
    public GlobalTransactionCondition() {
    }

    /**
     * Instantiates a new condition.
     *
     * @param statuses the statuses
     */
    public GlobalTransactionCondition(GlobalStatus... statuses) {
        this.statuses = statuses;
    }

    /**
     * Instantiates a new condition.
     *
     * @param status    the status
     * @param pageIndex the page index
     * @param pageSize  the page size
     */
    public GlobalTransactionCondition(GlobalStatus status, int pageIndex, int pageSize) {
        this(new GlobalStatus[]{status}, pageIndex, pageSize);
    }

    /**
     * Instantiates a new condition.
     *
     * @param statuses  the statuses
     * @param pageIndex the page index
     * @param pageSize  the page size
     */
    public GlobalTransactionCondition(GlobalStatus[] statuses, int pageIndex, int pageSize) {
        this.statuses = statuses;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    /**
     * Instantiates a new condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public GlobalTransactionCondition(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    //endregion

    //region Override Querier

    /**
     * Match data.
     *
     * @param globalTransaction the global transaction
     * @return the boolean
     */
    @Override
    public <D extends GlobalTransactionModel> boolean isMatch(D globalTransaction) {
        if (globalTransaction == null) {
            return false;
        }

        // where
        // status in (?, ?, ?)
        if (statuses != null && statuses.length > 0) {
            if (!this.hasStatus(globalTransaction.getStatus())) {
                return false; // un match
            }
        }
        // begin_time < System.currentTimeMillis() - ?
        if (overTimeAliveMills > 0) {
            if (globalTransaction.getBeginTime() >= System.currentTimeMillis() - overTimeAliveMills) {
                return false; // un match
            }
        }
        //  true: begin_time  < System.currentTimeMillis() - timeout
        // false: begin_time >= System.currentTimeMillis() - timeout
        if (timeoutData != null) {
            boolean isTimeout = globalTransaction.getBeginTime() < System.currentTimeMillis() - globalTransaction.getTimeout();
            return timeoutData ? isTimeout : !isTimeout;
        }

        return true;
    }

    /**
     * do sort
     *
     * @param globalTransactionDOs the global transactions
     * @return the after sort list
     */
    @Override
    public <D extends GlobalTransactionModel> List<D> doSort(List<D> globalTransactionDOs) {
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
                case APPLICATION_DATA:
                    return this.compareTo(a.getApplicationData(), b.getApplicationData());
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

    private boolean hasStatus(int statusCode) {
        for (GlobalStatus status : statuses) {
            if (status.getCode() == statusCode) {
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

    public void setStatuses(GlobalStatus... statuses) {
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

    //endregion
}
