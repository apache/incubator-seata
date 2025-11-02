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
 * Test cases for ExecutionStatus enum compatibility wrapper.
 */
public class ExecutionStatusTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ExecutionStatus.class.isAnnotationPresent(Deprecated.class),
                "ExecutionStatus should be marked as @Deprecated");
    }

    @Test
    public void testEnumValues() {
        ExecutionStatus[] values = ExecutionStatus.values();
        assertEquals(5, values.length, "Should have 5 enum values");
    }

    @Test
    public void testRunning() {
        assertEquals("Running", ExecutionStatus.RU.getStatusString());
    }

    @Test
    public void testSucceed() {
        assertEquals("Succeed", ExecutionStatus.SU.getStatusString());
    }

    @Test
    public void testFailed() {
        assertEquals("Failed", ExecutionStatus.FA.getStatusString());
    }

    @Test
    public void testUnknown() {
        assertEquals("Unknown", ExecutionStatus.UN.getStatusString());
    }

    @Test
    public void testSkipped() {
        assertEquals("Skipped", ExecutionStatus.SK.getStatusString());
    }

    @Test
    public void testWrapNull() {
        assertNull(ExecutionStatus.wrap(null), "Wrapping null should return null");
    }

    @Test
    public void testWrapRunning() {
        ExecutionStatus wrapped = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.RU);
        assertEquals(ExecutionStatus.RU, wrapped);
    }

    @Test
    public void testWrapSucceed() {
        ExecutionStatus wrapped = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU);
        assertEquals(ExecutionStatus.SU, wrapped);
    }

    @Test
    public void testWrapFailed() {
        ExecutionStatus wrapped = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA);
        assertEquals(ExecutionStatus.FA, wrapped);
    }

    @Test
    public void testWrapUnknown() {
        ExecutionStatus wrapped = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.UN);
        assertEquals(ExecutionStatus.UN, wrapped);
    }

    @Test
    public void testWrapSkipped() {
        ExecutionStatus wrapped = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.SK);
        assertEquals(ExecutionStatus.SK, wrapped);
    }

    @Test
    public void testUnwrapRunning() {
        org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = ExecutionStatus.RU.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.RU, unwrapped);
    }

    @Test
    public void testUnwrapSucceed() {
        org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = ExecutionStatus.SU.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU, unwrapped);
    }

    @Test
    public void testUnwrapFailed() {
        org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = ExecutionStatus.FA.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA, unwrapped);
    }

    @Test
    public void testUnwrapUnknown() {
        org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = ExecutionStatus.UN.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.UN, unwrapped);
    }

    @Test
    public void testUnwrapSkipped() {
        org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = ExecutionStatus.SK.unwrap();
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.SK, unwrapped);
    }

    @Test
    public void testWrapAndUnwrapRoundTrip() {
        for (org.apache.seata.saga.statelang.domain.ExecutionStatus apache :
                org.apache.seata.saga.statelang.domain.ExecutionStatus.values()) {
            ExecutionStatus wrapped = ExecutionStatus.wrap(apache);
            assertNotNull(wrapped);
            org.apache.seata.saga.statelang.domain.ExecutionStatus unwrapped = wrapped.unwrap();
            assertEquals(apache, unwrapped, "Round trip should preserve value");
        }
    }
}
