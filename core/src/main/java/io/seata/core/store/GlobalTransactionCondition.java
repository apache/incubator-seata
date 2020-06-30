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

import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_STATUS;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_XID;

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

        if (!super.isNeedSort()) {
            return globalTransactionDOs;
        }

        globalTransactionDOs.sort((a, b) -> {
            int ret = 0;
            for (SortParam sortParam : super.getSortParams()) {
                switch (sortParam.getSortFieldName()) {
                    case GLOBAL_TABLE_XID:
                        ret = this.compareTo(a.getXid(), b.getXid(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_TRANSACTION_ID:
                        ret = this.compareTo(a.getTransactionId(), b.getTransactionId(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_STATUS:
                        ret = this.compareTo(a.getStatus(), b.getStatus(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_APPLICATION_ID:
                        ret = this.compareTo(a.getApplicationId(), b.getApplicationId(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP:
                        ret = this.compareTo(a.getTransactionServiceGroup(), b.getTransactionServiceGroup(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_TRANSACTION_NAME:
                        ret = this.compareTo(a.getTransactionName(), b.getTransactionName(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_TIMEOUT:
                        ret = this.compareTo(a.getTimeout(), b.getTimeout(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_BEGIN_TIME:
                        ret = this.compareTo(a.getBeginTime(), b.getBeginTime(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_APPLICATION_DATA:
                        ret = this.compareTo(a.getApplicationData(), b.getApplicationData(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_GMT_CREATE:
                        ret = this.compareTo(a.getGmtCreate(), b.getGmtCreate(), sortParam.getSortOrder());
                        break;
                    case GLOBAL_TABLE_GMT_MODIFIED:
                        ret = this.compareTo(a.getGmtModified(), b.getGmtModified(), sortParam.getSortOrder());
                        break;
                    default:
                        break;
                }
                if (ret != 0) {
                    return ret;
                }
            }
            return ret;
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

    /**
     * Sets sort fields, and all fields use SortOrder.ASC
     *
     * @param sortFields the sort fields
     */
    public void setSortFields(GlobalTableField... sortFields) {
        if (sortFields.length == 0) {
            return;
        }
        SortParam[] sortParams = new SortParam[sortFields.length];
        for (int i = 0, l = sortFields.length; i < l; ++i) {
            sortParams[i] = new SortParam(sortFields[i].getFieldName());
        }
        super.setSortParams(sortParams);
    }

    //endregion
}
