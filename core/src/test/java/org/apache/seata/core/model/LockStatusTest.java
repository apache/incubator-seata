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
package org.apache.seata.core.model;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link LockStatus}.
 */
public class LockStatusTest {

    @Test
    public void testGetCode() {
        assertEquals(0, LockStatus.Locked.getCode());
        assertEquals(1, LockStatus.Rollbacking.getCode());
    }

    @Test
    public void testGetWithByteCode() {
        assertEquals(LockStatus.Locked, LockStatus.get((byte) 0));
        assertEquals(LockStatus.Rollbacking, LockStatus.get((byte) 1));
    }

    @Test
    public void testGetWithIntCode() {
        assertEquals(LockStatus.Locked, LockStatus.get(0));
        assertEquals(LockStatus.Rollbacking, LockStatus.get(1));
    }

    @Test
    public void testGetWithInvalidCodeThrowsException() {
        assertThrows(ShouldNeverHappenException.class, () -> LockStatus.get(99));
        assertThrows(ShouldNeverHappenException.class, () -> LockStatus.get(-1));
    }

    @Test
    public void testAllEnumValuesHaveUniqueCode() {
        LockStatus[] values = LockStatus.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                if (values[i].getCode() == values[j].getCode()) {
                    throw new AssertionError("Duplicate code found: " + values[i] + " and " + values[j]);
                }
            }
        }
    }

    @Test
    public void testValuesCount() {
        assertEquals(2, LockStatus.values().length);
    }

    @Test
    public void testValueOf() {
        assertEquals(LockStatus.Locked, LockStatus.valueOf("Locked"));
        assertEquals(LockStatus.Rollbacking, LockStatus.valueOf("Rollbacking"));
    }

    @Test
    public void testValueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> LockStatus.valueOf("Invalid"));
    }
}
