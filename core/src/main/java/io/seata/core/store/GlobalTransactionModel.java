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

import java.util.Date;

/**
 * Global Transaction model
 *
 * @author wang.liang
 */
interface GlobalTransactionModel {

    /**
     * Gets xid.
     *
     * @return the xid
     */
    String getXid();

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    void setXid(String xid);

    /**
     * Gets status.
     *
     * @return the status
     */
    int getStatus();

    /**
     * Sets status.
     *
     * @param status the status
     */
    void setStatus(int status);

    /**
     * Gets application id.
     *
     * @return the application id
     */
    String getApplicationId();

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    void setApplicationId(String applicationId);

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    String getTransactionServiceGroup();

    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    void setTransactionServiceGroup(String transactionServiceGroup);

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    String getTransactionName();

    /**
     * Sets transaction name.
     *
     * @param transactionName the transaction name
     */
    void setTransactionName(String transactionName);

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    int getTimeout();

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    void setTimeout(int timeout);

    /**
     * Gets begin time.
     *
     * @return the begin time
     */
    long getBeginTime();

    /**
     * Sets begin time.
     *
     * @param beginTime the begin time
     */
    void setBeginTime(long beginTime);

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    long getTransactionId();

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    void setTransactionId(long transactionId);

    /**
     * Gets application data.
     *
     * @return the application data
     */
    String getApplicationData();

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    void setApplicationData(String applicationData);

    /**
     * Gets gmt create.
     *
     * @return the gmt create
     */
    Date getGmtCreate();

    /**
     * Sets gmt create.
     *
     * @param gmtCreate the gmt create
     */
    void setGmtCreate(Date gmtCreate);

    /**
     * Gets gmt modified.
     *
     * @return the gmt modified
     */
    Date getGmtModified();

    /**
     * Sets gmt modified.
     *
     * @param gmtModified the gmt modified
     */
    void setGmtModified(Date gmtModified);
}
