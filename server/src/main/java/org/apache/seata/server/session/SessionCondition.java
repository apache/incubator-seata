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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The type Session condition.
 *
 */
public class SessionCondition {
    public enum ScanContinuation {
        UNSET,
        RESUMABLE,
        EXHAUSTED
    }

    private Long transactionId;
    private String xid;
    private GlobalStatus status;
    private GlobalStatus[] statuses;
    private Long overTimeAliveMills;
    private Integer limit;
    private Integer scanLimit;
    private boolean lazyLoadBranch;
    private byte[] statusScanCursor;
    private byte[] nextStatusScanCursor;
    private ScanContinuation statusScanContinuation = ScanContinuation.UNSET;
    private Map<GlobalStatus, byte[]> statusScanCursors = Collections.emptyMap();
    private Map<GlobalStatus, byte[]> nextStatusScanCursors = Collections.emptyMap();
    private Long maxTimeoutDeadlineMillis;
    private byte[] timeoutScanCursor;
    private byte[] nextTimeoutScanCursor;
    private ScanContinuation timeoutScanContinuation = ScanContinuation.UNSET;
    private SessionScanStats scanStats = SessionScanStats.empty();

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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getScanLimit() {
        return scanLimit;
    }

    public void setScanLimit(Integer scanLimit) {
        this.scanLimit = scanLimit;
    }

    public boolean isLazyLoadBranch() {
        return lazyLoadBranch;
    }

    public void setLazyLoadBranch(boolean lazyLoadBranch) {
        this.lazyLoadBranch = lazyLoadBranch;
    }

    public byte[] getStatusScanCursor() {
        return statusScanCursor == null ? null : Arrays.copyOf(statusScanCursor, statusScanCursor.length);
    }

    public void setStatusScanCursor(byte[] statusScanCursor) {
        this.statusScanCursor =
                statusScanCursor == null ? null : Arrays.copyOf(statusScanCursor, statusScanCursor.length);
    }

    public byte[] getNextStatusScanCursor() {
        return nextStatusScanCursor == null ? null : Arrays.copyOf(nextStatusScanCursor, nextStatusScanCursor.length);
    }

    public void setNextStatusScanCursor(byte[] nextStatusScanCursor) {
        this.nextStatusScanCursor =
                nextStatusScanCursor == null ? null : Arrays.copyOf(nextStatusScanCursor, nextStatusScanCursor.length);
        statusScanContinuation = nextStatusScanCursor == null ? ScanContinuation.EXHAUSTED : ScanContinuation.RESUMABLE;
    }

    public ScanContinuation getStatusScanContinuation() {
        return statusScanContinuation;
    }

    public void clearNextStatusScanCursor() {
        nextStatusScanCursor = null;
        statusScanContinuation = ScanContinuation.UNSET;
    }

    public Map<GlobalStatus, byte[]> getStatusScanCursors() {
        return copyStatusScanCursors(statusScanCursors);
    }

    public void setStatusScanCursors(Map<GlobalStatus, byte[]> statusScanCursors) {
        this.statusScanCursors = copyStatusScanCursors(statusScanCursors);
    }

    public Map<GlobalStatus, byte[]> getNextStatusScanCursors() {
        return copyStatusScanCursors(nextStatusScanCursors);
    }

    public void setNextStatusScanCursors(Map<GlobalStatus, byte[]> nextStatusScanCursors) {
        setNextStatusScanCursors(
                nextStatusScanCursors,
                nextStatusScanCursors == null || nextStatusScanCursors.isEmpty()
                        ? ScanContinuation.EXHAUSTED
                        : ScanContinuation.RESUMABLE);
    }

    public void setNextStatusScanCursors(
            Map<GlobalStatus, byte[]> nextStatusScanCursors, ScanContinuation statusScanContinuation) {
        this.nextStatusScanCursors = copyStatusScanCursors(nextStatusScanCursors);
        this.statusScanContinuation = statusScanContinuation;
    }

    public void clearNextStatusScanCursors() {
        nextStatusScanCursors = Collections.emptyMap();
        statusScanContinuation = ScanContinuation.UNSET;
    }

    public Long getMaxTimeoutDeadlineMillis() {
        return maxTimeoutDeadlineMillis;
    }

    public void setMaxTimeoutDeadlineMillis(Long maxTimeoutDeadlineMillis) {
        this.maxTimeoutDeadlineMillis = maxTimeoutDeadlineMillis;
    }

    public byte[] getTimeoutScanCursor() {
        return timeoutScanCursor == null ? null : Arrays.copyOf(timeoutScanCursor, timeoutScanCursor.length);
    }

    public void setTimeoutScanCursor(byte[] timeoutScanCursor) {
        this.timeoutScanCursor =
                timeoutScanCursor == null ? null : Arrays.copyOf(timeoutScanCursor, timeoutScanCursor.length);
    }

    public byte[] getNextTimeoutScanCursor() {
        return nextTimeoutScanCursor == null
                ? null
                : Arrays.copyOf(nextTimeoutScanCursor, nextTimeoutScanCursor.length);
    }

    public void setNextTimeoutScanCursor(byte[] nextTimeoutScanCursor) {
        this.nextTimeoutScanCursor = nextTimeoutScanCursor == null
                ? null
                : Arrays.copyOf(nextTimeoutScanCursor, nextTimeoutScanCursor.length);
        timeoutScanContinuation =
                nextTimeoutScanCursor == null ? ScanContinuation.EXHAUSTED : ScanContinuation.RESUMABLE;
    }

    public ScanContinuation getTimeoutScanContinuation() {
        return timeoutScanContinuation;
    }

    public void clearNextTimeoutScanCursor() {
        nextTimeoutScanCursor = null;
        timeoutScanContinuation = ScanContinuation.UNSET;
    }

    public SessionScanStats getScanStats() {
        return scanStats;
    }

    public void setScanStats(SessionScanStats scanStats) {
        this.scanStats = scanStats == null ? SessionScanStats.empty() : scanStats;
    }

    public void clearScanStats() {
        this.scanStats = SessionScanStats.empty();
    }

    private static Map<GlobalStatus, byte[]> copyStatusScanCursors(Map<GlobalStatus, byte[]> cursors) {
        if (cursors == null || cursors.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<GlobalStatus, byte[]> copies = new EnumMap<>(GlobalStatus.class);
        for (Map.Entry<GlobalStatus, byte[]> entry : cursors.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                byte[] cursor = entry.getValue();
                copies.put(entry.getKey(), Arrays.copyOf(cursor, cursor.length));
            }
        }
        return copies.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copies);
    }
}
