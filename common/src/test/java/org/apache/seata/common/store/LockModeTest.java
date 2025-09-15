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

class LockModeTest {
    @Test
    void testGetName() {
        assertEquals("file", LockMode.FILE.getName());
        assertEquals("db", LockMode.DB.getName());
        assertEquals("redis", LockMode.REDIS.getName());
        assertEquals("raft", LockMode.RAFT.getName());
    }

    @Test
    void testGet() {
        assertEquals(LockMode.FILE, LockMode.get("file"));
        assertEquals(LockMode.FILE, LockMode.get("FILE"));
        assertEquals(LockMode.FILE, LockMode.get("FiLe"));
        assertEquals(LockMode.DB, LockMode.get("db"));
        assertEquals(LockMode.DB, LockMode.get("DB"));
        assertEquals(LockMode.REDIS, LockMode.get("redis"));
        assertEquals(LockMode.REDIS, LockMode.get("REDIS"));
        assertEquals(LockMode.RAFT, LockMode.get("raft"));
        assertEquals(LockMode.RAFT, LockMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        assertThrows(IllegalArgumentException.class, () -> LockMode.get("unknown"));
        assertThrows(IllegalArgumentException.class, () -> LockMode.get(""));
        assertThrows(IllegalArgumentException.class, () -> LockMode.get(null));
    }

    @Test
    void testContainsValid() {
        assertTrue(LockMode.contains("file"));
        assertTrue(LockMode.contains("FILE"));
        assertTrue(LockMode.contains("FiLe"));
        assertTrue(LockMode.contains("db"));
        assertTrue(LockMode.contains("redis"));
        assertTrue(LockMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        assertFalse(LockMode.contains("unknown"));
        assertFalse(LockMode.contains(""));
        assertFalse(LockMode.contains(null));
    }
}