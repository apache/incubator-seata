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
     * Gets offset for Redis list-based pagination (ignored by DB stores).
     *
     * @return the offset, or {@code null} if unset
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Sets the zero-based start offset for Redis list-based pagination (ignored by DB stores;
     * negative values are clamped to {@code 0}).
     *
     * <p><strong>Redis best-effort contract:</strong> Because Redis status lists mutate concurrently
     * via {@code LREM}/{@code RPUSH} on status transitions and session removals, offset-based paging
     * across multiple calls is best-effort: removals ahead of {@code offset} can cause rows to be
     * skipped in the current scan (caught on a subsequent tick), re-appends to the tail can cause
     * duplicates, and concurrently deleted session hashes can yield a page shorter than
     * {@code pageSize}. Callers MUST be idempotent and MUST terminate only on an empty page
     * ({@code page.isEmpty()}), not on a short page.
     *
     * @param offset the zero-based list offset
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
     * Whether exactly one of {@code cursorGmtModified} or {@code cursorXid} is set (incomplete cursor).
     *
     * @return {@code true} if only one cursor component is populated
     */
    public boolean hasPartialCursor() {
        return (cursorGmtModified != null) ^ StringUtils.isNotBlank(cursorXid);
    }

    /**
     * Whether this condition requests composite keyset pagination for DB stores.
     *
     * <p>DB stores honor {@code cursorGmtModified}, {@code cursorXid}, and {@code pageSize},
     * and ignore {@code offset}.
     *
     * @return {@code true} if DB keyset pagination is requested
     */
    public boolean isDbPagedQuery() {
        return cursorGmtModified != null || StringUtils.isNotBlank(cursorXid) || pageSize != null;
    }

    /**
     * Whether this condition requests offset/page-size pagination for Redis stores.
     *
     * <p>Redis stores honor {@code offset} and {@code pageSize}, and ignore DB keyset cursor fields.
     *
     * @return {@code true} if Redis offset pagination is requested
     */
    public boolean isRedisPagedQuery() {
        return offset != null || pageSize != null;
    }

    /**
     * Whether any pagination or cursor field is configured on this condition
     * ({@link #isDbPagedQuery()} or {@link #isRedisPagedQuery()}).
     *
     * <p>Storage-specific dispatch rules:
     * <ul>
     *   <li>DB stores check {@link #isDbPagedQuery()} ({@code cursorGmtModified}, {@code cursorXid},
     *       {@code pageSize}) and ignore {@code offset}.</li>
     *   <li>Redis stores check {@link #isRedisPagedQuery()} ({@code offset}, {@code pageSize})
     *       and ignore DB keyset cursor fields.</li>
     * </ul>
     *
     * @return {@code true} if pagination/cursor querying is requested for any store
     */
    public boolean isPagedQuery() {
        return isDbPagedQuery() || isRedisPagedQuery();
    }
}
