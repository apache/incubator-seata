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

class SessionModeTest {
    @Test
    void testGetName() {
        Assertions.assertEquals("file", SessionMode.FILE.getName());
        Assertions.assertEquals("db", SessionMode.DB.getName());
        Assertions.assertEquals("redis", SessionMode.REDIS.getName());
        Assertions.assertEquals("raft", SessionMode.RAFT.getName());
    }

    @Test
    void testGet() {
        Assertions.assertEquals(SessionMode.FILE, SessionMode.get("file"));
        Assertions.assertEquals(SessionMode.FILE, SessionMode.get("FILE"));
        Assertions.assertEquals(SessionMode.FILE, SessionMode.get("FiLe"));
        Assertions.assertEquals(SessionMode.DB, SessionMode.get("db"));
        Assertions.assertEquals(SessionMode.DB, SessionMode.get("DB"));
        Assertions.assertEquals(SessionMode.REDIS, SessionMode.get("redis"));
        Assertions.assertEquals(SessionMode.REDIS, SessionMode.get("REDIS"));
        Assertions.assertEquals(SessionMode.RAFT, SessionMode.get("raft"));
        Assertions.assertEquals(SessionMode.RAFT, SessionMode.get("Raft"));
    }

    @Test
    void testGetUnknown() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SessionMode.get("unknown"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SessionMode.get(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SessionMode.get(null));
    }

    @Test
    void testContainsValid() {
        Assertions.assertTrue(SessionMode.contains("file"));
        Assertions.assertTrue(SessionMode.contains("FILE"));
        Assertions.assertTrue(SessionMode.contains("FiLe"));
        Assertions.assertTrue(SessionMode.contains("db"));
        Assertions.assertTrue(SessionMode.contains("redis"));
        Assertions.assertTrue(SessionMode.contains("raft"));
    }

    @Test
    void testContainsInvalid() {
        Assertions.assertFalse(SessionMode.contains("unknown"));
        Assertions.assertFalse(SessionMode.contains(""));
        Assertions.assertFalse(SessionMode.contains(null));
    }
}
