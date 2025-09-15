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
package org.apache.seata.common.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionModeTest {
    @Test
    void testGetName() {
        assertEquals("file", SessionMode.FILE.getName());
        assertEquals("db", SessionMode.DB.getName());
        assertEquals("redis", SessionMode.REDIS.getName());
        assertEquals("raft", SessionMode.RAFT.getName());
    }

    @Test
    void testGet() {
        assertEquals(SessionMode.FILE, SessionMode.get("file"));
        assertEquals(SessionMode.FILE, SessionMode.get("FILE"));
        assertEquals(SessionMode.FILE, SessionMode.get("FiLe"));
        assertEquals(SessionMode.DB, SessionMode.get("db"));
        assertEquals(SessionMode.DB, SessionMode.get("DB"));
        assertEquals(SessionMode.REDIS, SessionMode.get("redis"));
        assertEquals(SessionMode.REDIS, SessionMode.get("REDIS"));
        assertEquals(SessionMode.RAFT, SessionMode.get("raft"));
        assertEquals(SessionMode.RAFT, SessionMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        assertThrows(IllegalArgumentException.class, () -> SessionMode.get("unknown"));
        assertThrows(IllegalArgumentException.class, () -> SessionMode.get(""));
        assertThrows(IllegalArgumentException.class, () -> SessionMode.get(null));
    }

    @Test
    void testContainsValid() {
        assertTrue(SessionMode.contains("file"));
        assertTrue(SessionMode.contains("FILE"));
        assertTrue(SessionMode.contains("FiLe"));
        assertTrue(SessionMode.contains("db"));
        assertTrue(SessionMode.contains("redis"));
        assertTrue(SessionMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        assertFalse(SessionMode.contains("unknown"));
        assertFalse(SessionMode.contains(""));
        assertFalse(SessionMode.contains(null));
    }
}