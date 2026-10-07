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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SqlMonitorTest {
    private SqlMonitor monitor;

    @BeforeEach
    public void setUp() {
        monitor = SqlMonitor.getInstance();
        monitor.resetForTest();
    }

    @Test
    public void testRecordSlowSqlEntry() {
        monitor.record("SELECT * FROM users", 1500, 100);
        List<SlowSqlEntry> slowSqlList = monitor.getSlowSqlList();
        assertEquals(1, slowSqlList.size());
        SlowSqlEntry entry = slowSqlList.get(0);
        assertEquals("SELECT * FROM users", entry.getSql());
        assertTrue(entry.getExecutionTimeMillis() >= 1500);
    }

    @Test
    public void testRecordMultiSlowEntry() {
        monitor.record("SELECT * FROM student", 1200, 100);
        monitor.record("SELECT * FROM school", 1300, 100);
        List<SlowSqlEntry> slowSqlList = monitor.getSlowSqlList();
        assertEquals(2, slowSqlList.size());
        SlowSqlEntry entry1 = slowSqlList.get(0);
        SlowSqlEntry entry2 = slowSqlList.get(1);
        assertEquals("SELECT * FROM student", entry1.getSql());
        assertEquals("SELECT * FROM school", entry2.getSql());
    }

    @Test
    public void testMaxSlowSqlQueueSize() {
        // maxSlowEntries = 50 by default
        for (int i = 0; i < 55; i++) {
            monitor.record("SELECT * FROM orders WHERE id = " + i, 1500, 200);
        }

        List<SlowSqlEntry> slowSqlList = monitor.getSlowSqlList();
        assertEquals(50, slowSqlList.size());

        // Should not contain the first 5 entries
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            assertFalse(
                    slowSqlList.stream()
                            .map(SlowSqlEntry::getSql)
                            .anyMatch(sql -> sql.equals("SELECT * FROM orders WHERE id = " + finalI)),
                    "Entry with id = " + finalI + " should have been evicted");
        }
    }

    @Test
    public void testRecordForFastSql() {
        monitor.record("SELECT 1", 100, 50);
        List<SlowSqlEntry> slowSqlList = monitor.getSlowSqlList();
        assertTrue(slowSqlList.isEmpty());
    }

    @Test
    public void testTxnHistogramBuckets() {
        monitor.record("SELECT * FROM t1", 30, 0);
        monitor.record("SELECT * FROM t2", 150, 0);
        monitor.record("SELECT * FROM t3", 300, 0);
        monitor.record("SELECT * FROM t4", 800, 0);
        monitor.record("SELECT * FROM t5", 2000, 0);
        monitor.record("SELECT * FROM t6", 4000, 0);

        Map<String, Integer> histogram = monitor.getTxnHistogram();
        assertEquals(1, histogram.get("0-50ms"));
        assertEquals(1, histogram.get("50-200ms"));
        assertEquals(1, histogram.get("200-500ms"));
        assertEquals(1, histogram.get("500ms-1s"));
        assertEquals(1, histogram.get("1s-3s"));
        assertEquals(1, histogram.get("3s+"));
    }

    @Test
    public void testHoldHistogramBuckets() {
        monitor.record("SELECT * FROM hold1", 0, 30);
        monitor.record("SELECT * FROM hold2", 0, 150);
        monitor.record("SELECT * FROM hold3", 0, 300);
        monitor.record("SELECT * FROM hold4", 0, 800);
        monitor.record("SELECT * FROM hold5", 0, 2000);

        Map<String, Integer> histogram = monitor.getHoldHistogram();
        assertEquals(1, histogram.get("0-50ms"));
        assertEquals(1, histogram.get("50-200ms"));
        assertEquals(1, histogram.get("200-500ms"));
        assertEquals(1, histogram.get("500ms-1s"));
        assertEquals(1, histogram.get("1s+"));
    }
}
