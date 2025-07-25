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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlMonitorTest {
    private SqlMonitor monitor;

    @BeforeEach
    public void setUp() {
        monitor = SqlMonitor.getInstance();
        monitor.resetForTest();
        monitor.setSlowThreshold(1000);
        monitor.setMaxSlowEntries(100);
    }

    @Test
    public void recordSlowSql_goesToBothQueues() {
        monitor.record("SELECT * FROM users", 1500, 10);
        List<SqlExecutionEntry> slow = monitor.getSlowSqlList();
        List<SqlExecutionEntry> all = monitor.getAllRecords();

        assertEquals(1, slow.size());
        assertEquals(1, all.size());

        SqlExecutionEntry e = slow.get(0);
        assertEquals("SELECT * FROM users", e.getSql());
        assertTrue(e.getExecutionTimeMillis() >= 1500);
        assertEquals(e, all.get(0));
    }

    @Test
    public void recordFastSql_onlyInAllRecords() {
        monitor.record("SELECT * FROM student", 100, 5);
        assertTrue(monitor.getSlowSqlList().isEmpty());
        assertEquals(1, monitor.getAllRecordCount());
        assertEquals("SELECT * FROM student", monitor.getAllRecords().get(0).getSql());
    }

    @Test
    public void slowQueueOverCapacity() {
        // decrease the capacity for test
        monitor.setMaxSlowEntries(5);
        for (int i = 0; i < 7; i++) {
            monitor.record("SLOW " + i, 2000, 10);
        }

        List<SqlExecutionEntry> slow = monitor.getSlowSqlList();
        assertEquals(5, slow.size());

        // Should not contain the first 2 entries
        for (int i = 0; i < 2; i++) {
            String sql = "SLOW " + i;
            assertFalse(slow.stream().anyMatch(e -> e.getSql().equals(sql)),
                    "Entry " + sql + " should have been evicted");
        }

        for (int i = 2; i < 7; i++) {
            String sql = "SLOW " + i;
            assertTrue(slow.stream().anyMatch(e -> e.getSql().equals(sql)),
                    "Entry " + sql + " should remain");
        }
    }

    @Test
    public void allRecordOverCapacity() {
        int cap = monitor.getMaxAllRecord();

        // all set to fast SQL
        for (int i = 0; i < cap + 3; i++) {
            monitor.record("SQL " + i, 10, 1);
        }

        assertEquals(cap, monitor.getAllRecordCount());

        List<SqlExecutionEntry> all = monitor.getAllRecords();

        for (int i = 0; i < 3; i++) {
            String sql = "SQL " + i;
            assertFalse(all.stream().anyMatch(e -> e.getSql().equals(sql)),
                    "Entry " + sql + " should have been evicted from allRecord");
        }
    }


    @Test
    public void clearOldRecords_removeBeforeGivenTime() throws InterruptedException {
        monitor.record("old slow", 2000, 1);
        monitor.record("old fast", 10, 1);

        Thread.sleep(5);
        Instant cutoff = Instant.now();
        monitor.record("new slow", 2000, 1);
        monitor.record("new fast", 10, 1);

        monitor.clearOldRecords(cutoff);
        assertEquals(1, monitor.getSlowSqlCount());
        assertEquals("new slow", monitor.getSlowSqlList().get(0).getSql());

        assertEquals(2, monitor.getAllRecordCount());
        assertTrue(monitor.getAllRecords().stream().anyMatch(e -> e.getSql().equals("new slow")));
        assertTrue(monitor.getAllRecords().stream().anyMatch(e -> e.getSql().equals("new fast")));
    }



    @Test
    public void gettersReturnInternalState() {
        monitor.record("A", 2000, 1);
        monitor.record("B", 10, 1);
        assertEquals(1, monitor.getSlowSqlCount());
        assertEquals(2, monitor.getAllRecordCount());
        assertEquals(1000, monitor.getMaxAllRecord());
        assertEquals(100,  monitor.getMaxSlowEntries());
        assertEquals(1000L, monitor.getSlowThreshold());
    }
}
