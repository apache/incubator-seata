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
 * Test cases for RecoverStrategy enum compatibility wrapper.
 */
public class RecoverStrategyTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                RecoverStrategy.class.isAnnotationPresent(Deprecated.class),
                "RecoverStrategy should be marked as @Deprecated");
    }

    @Test
    public void testEnumValues() {
        RecoverStrategy[] values = RecoverStrategy.values();
        assertEquals(2, values.length, "Should have 2 enum values");
    }

    @Test
    public void testCompensate() {
        assertNotNull(RecoverStrategy.Compensate);
        assertEquals("Compensate", RecoverStrategy.Compensate.name());
    }

    @Test
    public void testForward() {
        assertNotNull(RecoverStrategy.Forward);
        assertEquals("Forward", RecoverStrategy.Forward.name());
    }

    @Test
    public void testWrapNull() {
        assertNull(RecoverStrategy.wrap(null), "Wrapping null should return null");
    }

    @Test
    public void testWrapCompensate() {
        RecoverStrategy wrapped =
                RecoverStrategy.wrap(org.apache.seata.saga.statelang.domain.RecoverStrategy.Compensate);
        assertEquals(RecoverStrategy.Compensate, wrapped);
    }

    @Test
    public void testWrapForward() {
        RecoverStrategy wrapped = RecoverStrategy.wrap(org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward);
        assertEquals(RecoverStrategy.Forward, wrapped);
    }

    @Test
    public void testUnwrapCompensate() {
        org.apache.seata.saga.statelang.domain.RecoverStrategy unwrapped = RecoverStrategy.Compensate.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.RecoverStrategy.Compensate, unwrapped);
    }

    @Test
    public void testUnwrapForward() {
        org.apache.seata.saga.statelang.domain.RecoverStrategy unwrapped = RecoverStrategy.Forward.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward, unwrapped);
    }

    @Test
    public void testWrapAndUnwrapRoundTrip() {
        for (org.apache.seata.saga.statelang.domain.RecoverStrategy apache :
                org.apache.seata.saga.statelang.domain.RecoverStrategy.values()) {
            RecoverStrategy wrapped = RecoverStrategy.wrap(apache);
            assertNotNull(wrapped);
            org.apache.seata.saga.statelang.domain.RecoverStrategy unwrapped = wrapped.unwrap();
            assertEquals(apache, unwrapped, "Round trip should preserve value");
        }
    }
}
