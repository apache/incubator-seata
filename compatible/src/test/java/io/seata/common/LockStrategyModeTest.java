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
package io.seata.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for LockStrategyMode enum.
 */
public class LockStrategyModeTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                LockStrategyMode.class.isAnnotationPresent(Deprecated.class),
                "LockStrategyMode should be marked as @Deprecated");
    }

    @Test
    public void testIsEnum() {
        assertTrue(LockStrategyMode.class.isEnum(), "LockStrategyMode should be an enum");
    }

    @Test
    public void testEnumValues() {
        LockStrategyMode[] values = LockStrategyMode.values();
        assertEquals(2, values.length, "Should have exactly 2 enum values");
    }

    @Test
    public void testOptimisticMode() {
        LockStrategyMode mode = LockStrategyMode.OPTIMISTIC;
        assertNotNull(mode);
        assertEquals("OPTIMISTIC", mode.name());
        assertEquals(0, mode.ordinal());
    }

    @Test
    public void testPessimisticMode() {
        LockStrategyMode mode = LockStrategyMode.PESSIMISTIC;
        assertNotNull(mode);
        assertEquals("PESSIMISTIC", mode.name());
        assertEquals(1, mode.ordinal());
    }

    @Test
    public void testValueOf() {
        assertEquals(LockStrategyMode.OPTIMISTIC, LockStrategyMode.valueOf("OPTIMISTIC"));
        assertEquals(LockStrategyMode.PESSIMISTIC, LockStrategyMode.valueOf("PESSIMISTIC"));
    }

    @Test
    public void testToString() {
        assertEquals("OPTIMISTIC", LockStrategyMode.OPTIMISTIC.toString());
        assertEquals("PESSIMISTIC", LockStrategyMode.PESSIMISTIC.toString());
    }

    @Test
    public void testSwitchStatement() {
        LockStrategyMode mode = LockStrategyMode.OPTIMISTIC;
        String result;

        switch (mode) {
            case OPTIMISTIC:
                result = "optimistic";
                break;
            case PESSIMISTIC:
                result = "pessimistic";
                break;
            default:
                result = "unknown";
        }

        assertEquals("optimistic", result);
    }
}
