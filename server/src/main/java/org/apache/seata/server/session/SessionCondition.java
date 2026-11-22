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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.GlobalStatus;

import java.util.Date;

/**
 * The type Session condition.
 *
 */
public class SessionCondition {
    private Long transactionId;
    private String xid;
    private GlobalStatus status;
    private GlobalStatus[] statuses;
    private Long overTimeAliveMills;
    private boolean lazyLoadBranch;
    private Date cursorGmtModified;
    private String cursorXid;
    private Integer pageSize;
    private Integer offset;

    /**
     * Instantiates a new Session condition.
     */
    public SessionCondition() {}

    /**
     * Instantiates a new Session condition.
     *
     * @param xid the xid
     */
    public SessionCondition(String xid) {
        this.xid = xid;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param status the status
     */
    public SessionCondition(GlobalStatus status) {
        this.status = status;
        this.statuses = new GlobalStatus[] {status};
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     */
    public SessionCondition(GlobalStatus... statuses) {
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

    /**
     * Gets status.
     *
     * @return the status
     */
    public GlobalStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(GlobalStatus status) {
        this.status = status;
        this.statuses = new GlobalStatus[] {status};
    }

    /**
     * Gets over time alive mills.
     *
     * @return the over time alive mills
     */
    public Long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    /**
     * Sets over time alive mills.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public void setOverTimeAliveMills(Long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public GlobalStatus[] getStatuses() {
        return statuses;
    }

    public void setStatuses(GlobalStatus... statuses) {
        this.statuses = statuses;
    }

    public boolean isLazyLoadBranch() {
        return lazyLoadBranch;
    }

    public void setLazyLoadBranch(boolean lazyLoadBranch) {
        this.lazyLoadBranch = lazyLoadBranch;
    }

    /**
     * Gets cursor gmt modified.
     *
     * @return the cursor gmt modified
     */
    public Date getCursorGmtModified() {
        return cursorGmtModified;
    }

    /**
     * Sets cursor gmt modified.
     *
     * @param cursorGmtModified the cursor gmt modified
     */
    public void setCursorGmtModified(Date cursorGmtModified) {
        this.cursorGmtModified = cursorGmtModified;
    }

    /**
     * Gets cursor xid.
     *
     * @return the cursor xid
     */
    public String getCursorXid() {
        return cursorXid;
    }

    /**
     * Sets cursor xid.
     *
     * @param cursorXid the cursor xid
     */
    public void setCursorXid(String cursorXid) {
        this.cursorXid = cursorXid;
    }

    /**
     * Gets page size.
     *
     * @return the page size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets page size.
     *
     * @param pageSize the page size
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets offset.
     *
     * @return the offset
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Sets offset.
     *
     * @param offset the offset
     */
    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    /**
     * Whether a complete composite keyset cursor {@code (cursorGmtModified, cursorXid)} is present.
     *
     * @return {@code true} if both cursor fields are set
     */
    public boolean hasCursor() {
        return cursorGmtModified != null && StringUtils.isNotBlank(cursorXid);
    }

    /**
     * Whether any pagination or cursor field is configured on this condition.
     *
     * @return {@code true} if pagination/cursor querying is requested
     */
    public boolean isPagedQuery() {
        return cursorGmtModified != null || StringUtils.isNotBlank(cursorXid) || pageSize != null || offset != null;
    }
}
