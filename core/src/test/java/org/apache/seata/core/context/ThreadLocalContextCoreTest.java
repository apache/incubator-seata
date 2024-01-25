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
package org.apache.seata.core.context;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The type Thread local context core test.
 */
public class ThreadLocalContextCoreTest {
    private static ThreadLocalContextCore contextCore ;


    @BeforeAll
    public static void setUp() {
        contextCore = new ThreadLocalContextCore();
    }
    @Test
    public void testPutAndGet() {
        // Test putting and getting a value
        contextCore.put("key", "value");
        assertEquals("value", contextCore.get("key"));
        contextCore.remove("key");
    }

    @Test
    public void testRemove() {
        // Test putting and removing a value
        contextCore.put("key", "value");
        assertEquals("value", contextCore.remove("key"));
        assertNull(contextCore.get("key"));
    }

    @Test
    public void testEntries() {
        // Test getting all entries
        contextCore.put("key1", "value1");
        contextCore.put("key2", "value2");
        contextCore.put("key3", "value3");
        assertEquals(3, contextCore.entries().size());
        assertTrue(contextCore.entries().containsKey("key1"));
        assertTrue(contextCore.entries().containsKey("key2"));
        assertTrue(contextCore.entries().containsKey("key3"));
        contextCore.remove("key1");
        contextCore.remove("key2");
        contextCore.remove("key3");
        assertNull(contextCore.get("key1"));
        assertNull(contextCore.get("key2"));
        assertNull(contextCore.get("key3"));
    }
}
