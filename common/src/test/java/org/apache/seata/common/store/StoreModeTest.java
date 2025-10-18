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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StoreModeTest {
    @Test
    void testGetName() {
        Assertions.assertEquals("file", StoreMode.FILE.getName());
        Assertions.assertEquals("db", StoreMode.DB.getName());
        Assertions.assertEquals("redis", StoreMode.REDIS.getName());
        Assertions.assertEquals("raft", StoreMode.RAFT.getName());
    }

    @Test
    void testGet() {
        Assertions.assertEquals(StoreMode.FILE, StoreMode.get("file"));
        Assertions.assertEquals(StoreMode.FILE, StoreMode.get("FILE"));
        Assertions.assertEquals(StoreMode.FILE, StoreMode.get("FiLe"));
        Assertions.assertEquals(StoreMode.DB, StoreMode.get("db"));
        Assertions.assertEquals(StoreMode.DB, StoreMode.get("DB"));
        Assertions.assertEquals(StoreMode.REDIS, StoreMode.get("redis"));
        Assertions.assertEquals(StoreMode.REDIS, StoreMode.get("REDIS"));
        Assertions.assertEquals(StoreMode.RAFT, StoreMode.get("raft"));
        Assertions.assertEquals(StoreMode.RAFT, StoreMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StoreMode.get("unknown"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StoreMode.get(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StoreMode.get(null));
    }

    @Test
    void testContainsValidMode() {
        Assertions.assertTrue(StoreMode.contains("file"));
        Assertions.assertTrue(StoreMode.contains("FILE"));
        Assertions.assertTrue(StoreMode.contains("FiLe"));
        Assertions.assertTrue(StoreMode.contains("db"));
        Assertions.assertTrue(StoreMode.contains("redis"));
        Assertions.assertTrue(StoreMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        Assertions.assertFalse(StoreMode.contains("unknown"));
        Assertions.assertFalse(StoreMode.contains(""));
        Assertions.assertFalse(StoreMode.contains(null));
    }
}
