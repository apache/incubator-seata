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

class StoreModeTest {
    @Test
    void testGetName() {
        assertEquals("file", StoreMode.FILE.getName());
        assertEquals("db", StoreMode.DB.getName());
        assertEquals("redis", StoreMode.REDIS.getName());
        assertEquals("raft", StoreMode.RAFT.getName());
    }

    @Test
    void testGet() {
        assertEquals(StoreMode.FILE, StoreMode.get("file"));
        assertEquals(StoreMode.FILE, StoreMode.get("FILE"));
        assertEquals(StoreMode.FILE, StoreMode.get("FiLe"));
        assertEquals(StoreMode.DB, StoreMode.get("db"));
        assertEquals(StoreMode.DB, StoreMode.get("DB"));
        assertEquals(StoreMode.REDIS, StoreMode.get("redis"));
        assertEquals(StoreMode.REDIS, StoreMode.get("REDIS"));
        assertEquals(StoreMode.RAFT, StoreMode.get("raft"));
        assertEquals(StoreMode.RAFT, StoreMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        assertThrows(IllegalArgumentException.class, () -> StoreMode.get("unknown"));
        assertThrows(IllegalArgumentException.class, () -> StoreMode.get(""));
        assertThrows(IllegalArgumentException.class, () -> StoreMode.get(null));
    }

    @Test
    void testContainsValidMode() {
        assertTrue(StoreMode.contains("file"));
        assertTrue(StoreMode.contains("FILE"));
        assertTrue(StoreMode.contains("FiLe"));
        assertTrue(StoreMode.contains("db"));
        assertTrue(StoreMode.contains("redis"));
        assertTrue(StoreMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        assertFalse(StoreMode.contains("unknown"));
        assertFalse(StoreMode.contains(""));
        assertFalse(StoreMode.contains(null));
    }
}