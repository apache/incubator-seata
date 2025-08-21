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
package org.apache.seata.common.monitor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SqlMonitor {

    private static final SqlMonitor INSTANCE = new SqlMonitor();
    private volatile long slowThreshold = 1000;
    private volatile int maxSlowEntries = 100;
    private volatile int maxAllRecord = 1000;
    private final Deque<SqlExecutionEntry> slowSqlQueue = new ConcurrentLinkedDeque<>();
    private final Deque<SqlExecutionEntry> allRecord = new ConcurrentLinkedDeque<>();
    private final Lock slowLock = new ReentrantLock();
    private final Lock recordLock = new ReentrantLock();

    private SqlMonitor() {
    }

    public static SqlMonitor getInstance() {
        return INSTANCE;
    }

    public void record(String sql, long execMs, long holdMs) {
        Instant now = Instant.now();
        SqlExecutionEntry entry = new SqlExecutionEntry(sql, execMs, holdMs, now);

        recordLock.lock();
        // record all SQL executions for histogram
        try {
            allRecord.addLast(entry);
            while (allRecord.size() > maxAllRecord) {
                allRecord.removeFirst();
            }
        } finally {
            recordLock.unlock();
        }

        // record the slow sql
        if (execMs > slowThreshold) {
            slowLock.lock();
            try {
                slowSqlQueue.addLast(entry);
                while (slowSqlQueue.size() > maxSlowEntries) {
                    slowSqlQueue.removeFirst();
                }
            } finally {
                slowLock.unlock();
            }
        }
    }

    public List<SqlExecutionEntry> getSlowSqlList() {
        slowLock.lock();
        try {
            return new ArrayList<>(slowSqlQueue);
        } finally {
            slowLock.unlock();
        }
    }

    public List<SqlExecutionEntry> getAllRecords() {
        recordLock.lock();
        try {
            return new ArrayList<>(allRecord);
        } finally {
            recordLock.unlock();
        }
    }

    public void clearOldRecords(Instant beforeTime) {
        recordLock.lock();
        try {
            allRecord.removeIf(record -> record.getTimestamp().isBefore(beforeTime));
        } finally {
            recordLock.unlock();
        }

        slowLock.lock();
        try {
            slowSqlQueue.removeIf(entry -> entry.getTimestamp().isBefore(beforeTime));
        } finally {
            slowLock.unlock();
        }
    }


    /**
     * Set the execution time threshold for slow SQL.
     * Intended for use by dynamic configuration (e.g. Nacos or application.yml binding).
     */
    public void setSlowThreshold(long threshold) {
        this.slowThreshold = threshold;
    }

    /**
     * Set the max entries for slow SQL
     * Intended for use by dunamic configuration (e.g. Nacos or application.yml binding)
     */
    public void setMaxSlowEntries(int maxEntries) {
        this.maxSlowEntries = maxEntries;
    }

    public long getSlowThreshold() {
        return slowThreshold;
    }

    public int getMaxSlowEntries() {
        return maxSlowEntries;
    }

    public int getMaxAllRecord() {
        return maxAllRecord;
    }

    public int getSlowSqlCount() {
        return slowSqlQueue.size();
    }

    public int getAllRecordCount() {
        return allRecord.size();
    }


    /**
     * Reset all internal states, only for testing purpose.
     */
    public void resetForTest() {
        slowLock.lock();
        try {
            slowSqlQueue.clear();
        } finally {
            slowLock.unlock();
        }

        recordLock.lock();
        try {
            allRecord.clear();
        } finally {
            recordLock.unlock();
        }
    }


}
