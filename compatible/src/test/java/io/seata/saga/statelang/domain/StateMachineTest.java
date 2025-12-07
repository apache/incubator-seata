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
package io.seata.saga.statelang.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for StateMachine interface and Status enum compatibility wrapper.
 */
public class StateMachineTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateMachine.class.isAnnotationPresent(Deprecated.class),
                "StateMachine should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(StateMachine.class.isInterface(), "StateMachine should be an interface");
    }

    @Test
    public void testStatusEnumValues() {
        StateMachine.Status[] values = StateMachine.Status.values();
        assertEquals(2, values.length, "Should have 2 status values");
    }

    @Test
    public void testStatusActive() {
        assertEquals("Active", StateMachine.Status.AC.getStatusString());
    }

    @Test
    public void testStatusInactive() {
        assertEquals("Inactive", StateMachine.Status.IN.getStatusString());
    }

    @Test
    public void testStatusWrapNull() {
        assertNull(StateMachine.Status.wrap(null), "Wrapping null should return null");
    }

    @Test
    public void testStatusWrapActive() {
        StateMachine.Status wrapped =
                StateMachine.Status.wrap(org.apache.seata.saga.statelang.domain.StateMachine.Status.AC);
        assertEquals(StateMachine.Status.AC, wrapped);
    }

    @Test
    public void testStatusWrapInactive() {
        StateMachine.Status wrapped =
                StateMachine.Status.wrap(org.apache.seata.saga.statelang.domain.StateMachine.Status.IN);
        assertEquals(StateMachine.Status.IN, wrapped);
    }

    @Test
    public void testStatusUnwrapActive() {
        org.apache.seata.saga.statelang.domain.StateMachine.Status unwrapped = StateMachine.Status.AC.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.StateMachine.Status.AC, unwrapped);
    }

    @Test
    public void testStatusUnwrapInactive() {
        org.apache.seata.saga.statelang.domain.StateMachine.Status unwrapped = StateMachine.Status.IN.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.StateMachine.Status.IN, unwrapped);
    }

    @Test
    public void testStatusWrapAndUnwrapRoundTrip() {
        for (org.apache.seata.saga.statelang.domain.StateMachine.Status apache :
                org.apache.seata.saga.statelang.domain.StateMachine.Status.values()) {
            StateMachine.Status wrapped = StateMachine.Status.wrap(apache);
            assertNotNull(wrapped);
            org.apache.seata.saga.statelang.domain.StateMachine.Status unwrapped = wrapped.unwrap();
            assertEquals(apache, unwrapped, "Round trip should preserve value");
        }
    }
}
