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

/**
 * Session query scan statistics populated by storage implementations that can expose scan costs.
 */
public class SessionScanStats {

    private static final SessionScanStats EMPTY = new SessionScanStats(0L, 0L, 0L, 0L, 0L, false);

    private final long rowsScanned;
    private final long rowsReturned;
    private final long pointReads;
    private final long sessionsReturned;
    private final long elapsedMillis;
    private final boolean limitReached;

    public SessionScanStats(
            long rowsScanned,
            long rowsReturned,
            long pointReads,
            long sessionsReturned,
            long elapsedMillis,
            boolean limitReached) {
        this.rowsScanned = rowsScanned;
        this.rowsReturned = rowsReturned;
        this.pointReads = pointReads;
        this.sessionsReturned = sessionsReturned;
        this.elapsedMillis = elapsedMillis;
        this.limitReached = limitReached;
    }

    public static SessionScanStats empty() {
        return EMPTY;
    }

    public long getRowsScanned() {
        return rowsScanned;
    }

    public long getRowsReturned() {
        return rowsReturned;
    }

    public long getPointReads() {
        return pointReads;
    }

    public long getSessionsReturned() {
        return sessionsReturned;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public boolean isLimitReached() {
        return limitReached;
    }
}
