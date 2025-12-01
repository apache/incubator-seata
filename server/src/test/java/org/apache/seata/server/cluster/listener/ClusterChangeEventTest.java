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
package org.apache.seata.server.cluster.listener;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterChangeEventTest {

    @Test
    public void testConstructorWithFullParameters() {
        Object source = new Object();
        String group = "test-group";
        long term = 123L;
        boolean leader = true;

        ClusterChangeEvent event = new ClusterChangeEvent(source, group, term, leader);

        assertEquals(source, event.getSource());
        assertEquals(group, event.getGroup());
        assertEquals(term, event.getTerm());
        assertTrue(event.isLeader());
    }

    @Test
    public void testConstructorWithSourceAndGroup() {
        Object source = new Object();
        String group = "test-group-2";

        ClusterChangeEvent event = new ClusterChangeEvent(source, group);

        assertEquals(source, event.getSource());
        assertEquals(group, event.getGroup());
        assertEquals(0L, event.getTerm());
        assertFalse(event.isLeader());
    }

    @Test
    public void testConstructorWithClock() {
        Object source = new Object();
        Clock clock = Clock.systemUTC();

        ClusterChangeEvent event = new ClusterChangeEvent(source, clock);

        assertEquals(source, event.getSource());
        assertNull(event.getGroup());
        assertEquals(0L, event.getTerm());
        assertFalse(event.isLeader());
    }

    @Test
    public void testSetAndGetGroup() {
        ClusterChangeEvent event = new ClusterChangeEvent(new Object(), "initial-group");
        event.setGroup("updated-group");
        assertEquals("updated-group", event.getGroup());
    }

    @Test
    public void testSetAndGetTerm() {
        ClusterChangeEvent event = new ClusterChangeEvent(new Object(), Clock.systemUTC());
        event.setTerm(999L);
        assertEquals(999L, event.getTerm());
    }

    @Test
    public void testSetAndGetLeader() {
        ClusterChangeEvent event = new ClusterChangeEvent(new Object(), "group");
        event.setLeader(true);
        assertTrue(event.isLeader());

        event.setLeader(false);
        assertFalse(event.isLeader());
    }

    @Test
    public void testLeaderStatusChange() {
        Object source = new Object();
        ClusterChangeEvent event = new ClusterChangeEvent(source, "group", 1L, false);
        assertFalse(event.isLeader());

        event.setLeader(true);
        assertTrue(event.isLeader());
    }
}
