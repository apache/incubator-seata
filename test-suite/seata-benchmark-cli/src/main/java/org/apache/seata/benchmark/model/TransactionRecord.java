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
package org.apache.seata.benchmark.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Transaction execution record
 */
public class TransactionRecord {

    private final String xid;
    private final String status;
    private final long duration;
    private final int branchCount;
    private final boolean success;
    private final long timestamp;

    public TransactionRecord(String xid, String status, long duration, int branchCount, boolean success) {
        this.xid = xid;
        this.status = status;
        this.duration = duration;
        this.branchCount = branchCount;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

    public String getXid() {
        return xid;
    }

    public String getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    public int getBranchCount() {
        return branchCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date(timestamp));
    }

    public String getResultSymbol() {
        return success ? "✓" : "✗";
    }

    @Override
    public String toString() {
        return String.format(
                "[%s] XID: %s | Status: %s | Duration: %dms | Branches: %d | Result: %s",
                getFormattedTime(), xid, status, duration, branchCount, getResultSymbol());
    }
}
