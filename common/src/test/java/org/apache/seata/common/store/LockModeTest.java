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

class LockModeTest {
    @Test
    void testGetName() {
        Assertions.assertEquals("file", LockMode.FILE.getName());
        Assertions.assertEquals("db", LockMode.DB.getName());
        Assertions.assertEquals("redis", LockMode.REDIS.getName());
        Assertions.assertEquals("raft", LockMode.RAFT.getName());
    }

    @Test
    void testGet() {
        Assertions.assertEquals(LockMode.FILE, LockMode.get("file"));
        Assertions.assertEquals(LockMode.FILE, LockMode.get("FILE"));
        Assertions.assertEquals(LockMode.FILE, LockMode.get("FiLe"));
        Assertions.assertEquals(LockMode.DB, LockMode.get("db"));
        Assertions.assertEquals(LockMode.DB, LockMode.get("DB"));
        Assertions.assertEquals(LockMode.REDIS, LockMode.get("redis"));
        Assertions.assertEquals(LockMode.REDIS, LockMode.get("REDIS"));
        Assertions.assertEquals(LockMode.RAFT, LockMode.get("raft"));
        Assertions.assertEquals(LockMode.RAFT, LockMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> LockMode.get("unknown"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LockMode.get(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LockMode.get(null));
    }

    @Test
    void testContainsValid() {
        Assertions.assertTrue(LockMode.contains("file"));
        Assertions.assertTrue(LockMode.contains("FILE"));
        Assertions.assertTrue(LockMode.contains("FiLe"));
        Assertions.assertTrue(LockMode.contains("db"));
        Assertions.assertTrue(LockMode.contains("redis"));
        Assertions.assertTrue(LockMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        Assertions.assertFalse(LockMode.contains("unknown"));
        Assertions.assertFalse(LockMode.contains(""));
        Assertions.assertFalse(LockMode.contains(null));
    }
}
