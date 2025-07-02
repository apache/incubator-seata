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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SqlMonitor {

    private static final SqlMonitor INSTANCE = new SqlMonitor();
    private volatile long slowThreshold = 1000;
    private volatile int maxSlowEntries = 50;
    private final Deque<SlowSqlEntry> slowSqlQueue = new ConcurrentLinkedDeque<>();
    private final Lock slowLock = new ReentrantLock();
    private final Map<String, Integer> txnHistogram = new ConcurrentHashMap<>();
    private final Map<String, Integer> holdHistogram = new ConcurrentHashMap<>();

    private SqlMonitor() {
    }

    public static SqlMonitor getInstance() {
        return INSTANCE;
    }

    public void record(String sql, long execMs, long holdMs) {
        // slow sql
        if (execMs > slowThreshold) {
            slowLock.lock();
            try {
                slowSqlQueue.addLast(new SlowSqlEntry(sql, execMs, Instant.now()));
                if (slowSqlQueue.size() > maxSlowEntries) {
                    slowSqlQueue.removeFirst();
                }
            } finally {
                slowLock.unlock();
            }
        }

        // transaction histogram
        String bin = chooseTxnBucket(execMs);
        txnHistogram.merge(bin, 1, Integer::sum);

        // connection hold time histogram
        String bin2 = chooseHoldBucket(holdMs);
        holdHistogram.merge(bin2, 1, Integer::sum);
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


    public List<SlowSqlEntry> getSlowSqlList() {
        return new ArrayList<>(slowSqlQueue);
    }

    public Map<String, Integer> getTxnHistogram() {
        return new LinkedHashMap<>(txnHistogram);
    }

    public Map<String, Integer> getHoldHistogram() {
        return new LinkedHashMap<>(holdHistogram);
    }


    private String chooseTxnBucket(long ms) {
        if (ms <= 50) {
            return "0-50ms";
        } else if (ms <= 200) {
            return "50-200ms";
        } else if (ms <= 500) {
            return "200-500ms";
        } else if (ms <= 1000) {
            return "500ms-1s";
        } else if (ms <= 3000) {
            return "1s-3s";
        } else {
            return "3s+";
        }
    }


    private String chooseHoldBucket(long ms) {
        if (ms <= 50) {
            return "0-50ms";
        } else if (ms <= 200) {
            return "50-200ms";
        } else if (ms <= 500) {
            return "200-500ms";
        } else if (ms <= 1000) {
            return "500ms-1s";
        } else {
            return "1s+";
        }
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
        txnHistogram.clear();
        holdHistogram.clear();
    }
}

