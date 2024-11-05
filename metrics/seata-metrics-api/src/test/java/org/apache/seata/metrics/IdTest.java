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
package org.apache.seata.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class IdTest {
    private Id id;

    @BeforeEach
    public void setUp() {
        id = new Id("test");
    }

    @Test
    public void testGetId() {
        assertNotNull(id.getId());
    }

    @Test
    public void testGetName() {
        assertEquals("test", id.getName());
    }

    @Test
    public void testGetTags() {
        assertFalse(( id.getTags()).iterator().hasNext());
    }

    @Test
    public void testGetTagCount() {
        assertEquals(0, id.getTagCount());
    }

    @Test
    public void testWithTag() {
        id.withTag("key", "value");
        assertEquals(1, id.getTagCount());
    }

    @Test
    public void testGetMeterKey() {
        id.withTag("key", "value");
        assertEquals("test(value)", id.getMeterKey());
    }

    @Test
    public void testToString() {
        id.withTag("key", "value");
        assertEquals("test(key=value)", id.toString());
    }
}
