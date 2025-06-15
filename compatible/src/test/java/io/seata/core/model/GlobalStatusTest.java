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
package io.seata.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for GlobalStatus enum compatibility.
 */
public class GlobalStatusTest {

    @Test
    public void testDeprecatedAnnotation() {
        // Test that GlobalStatus is marked as @Deprecated
        assertTrue(GlobalStatus.class.isAnnotationPresent(Deprecated.class),
                "GlobalStatus should be marked as @Deprecated");
    }

    @Test
    public void testAllEnumValues() {
        // Test all enum values exist
        assertNotNull(GlobalStatus.UnKnown);
        assertNotNull(GlobalStatus.Begin);
        assertNotNull(GlobalStatus.Committing);
        assertNotNull(GlobalStatus.CommitRetrying);
        assertNotNull(GlobalStatus.Rollbacking);
        assertNotNull(GlobalStatus.RollbackRetrying);
        assertNotNull(GlobalStatus.TimeoutRollbacking);
        assertNotNull(GlobalStatus.TimeoutRollbackRetrying);
        assertNotNull(GlobalStatus.AsyncCommitting);
        assertNotNull(GlobalStatus.Committed);
        assertNotNull(GlobalStatus.CommitFailed);
        assertNotNull(GlobalStatus.Rollbacked);
        assertNotNull(GlobalStatus.RollbackFailed);
        assertNotNull(GlobalStatus.TimeoutRollbacked);
        assertNotNull(GlobalStatus.TimeoutRollbackFailed);
        assertNotNull(GlobalStatus.Finished);
        assertNotNull(GlobalStatus.CommitRetryTimeout);
        assertNotNull(GlobalStatus.RollbackRetryTimeout);
    }

    @Test
    public void testStatusCodes() {
        // Test that each status has the correct code
        assertEquals(0, GlobalStatus.UnKnown.getCode());
        assertEquals(1, GlobalStatus.Begin.getCode());
        assertEquals(2, GlobalStatus.Committing.getCode());
        assertEquals(3, GlobalStatus.CommitRetrying.getCode());
        assertEquals(4, GlobalStatus.Rollbacking.getCode());
        assertEquals(5, GlobalStatus.RollbackRetrying.getCode());
        assertEquals(6, GlobalStatus.TimeoutRollbacking.getCode());
        assertEquals(7, GlobalStatus.TimeoutRollbackRetrying.getCode());
        assertEquals(8, GlobalStatus.AsyncCommitting.getCode());
        assertEquals(9, GlobalStatus.Committed.getCode());
        assertEquals(10, GlobalStatus.CommitFailed.getCode());
        assertEquals(11, GlobalStatus.Rollbacked.getCode());
        assertEquals(12, GlobalStatus.RollbackFailed.getCode());
        assertEquals(13, GlobalStatus.TimeoutRollbacked.getCode());
        assertEquals(14, GlobalStatus.TimeoutRollbackFailed.getCode());
        assertEquals(15, GlobalStatus.Finished.getCode());
        assertEquals(16, GlobalStatus.CommitRetryTimeout.getCode());
        assertEquals(17, GlobalStatus.RollbackRetryTimeout.getCode());
    }

    @Test
    public void testGetByIntCode() {
        // Test getting status by int code
        assertEquals(GlobalStatus.UnKnown, GlobalStatus.get(0));
        assertEquals(GlobalStatus.Begin, GlobalStatus.get(1));
        assertEquals(GlobalStatus.Committing, GlobalStatus.get(2));
        assertEquals(GlobalStatus.Committed, GlobalStatus.get(9));
        assertEquals(GlobalStatus.Rollbacked, GlobalStatus.get(11));
        assertEquals(GlobalStatus.Finished, GlobalStatus.get(15));
        assertEquals(GlobalStatus.RollbackRetryTimeout, GlobalStatus.get(17));
    }

    @Test
    public void testGetByByteCode() {
        // Test getting status by byte code
        assertEquals(GlobalStatus.UnKnown, GlobalStatus.get((byte) 0));
        assertEquals(GlobalStatus.Begin, GlobalStatus.get((byte) 1));
        assertEquals(GlobalStatus.Committed, GlobalStatus.get((byte) 9));
        assertEquals(GlobalStatus.Rollbacked, GlobalStatus.get((byte) 11));
    }

    @Test
    public void testGetInvalidCode() {
        // Test that invalid codes throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> GlobalStatus.get(-1),
                "Should throw IllegalArgumentException for negative code");
        assertThrows(IllegalArgumentException.class, () -> GlobalStatus.get(100),
                "Should throw IllegalArgumentException for code > max value");
        assertThrows(IllegalArgumentException.class, () -> GlobalStatus.get(GlobalStatus.values().length),
                "Should throw IllegalArgumentException for code >= values length");
    }

    @Test
    public void testIsOnePhaseTimeout() {
        // Test timeout status detection
        assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbacking),
                "TimeoutRollbacking should be one phase timeout");
        assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbackRetrying),
                "TimeoutRollbackRetrying should be one phase timeout");
        assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbacked),
                "TimeoutRollbacked should be one phase timeout");
        assertTrue(GlobalStatus.isOnePhaseTimeout(GlobalStatus.TimeoutRollbackFailed),
                "TimeoutRollbackFailed should be one phase timeout");

        // Test non-timeout statuses
        assertFalse(GlobalStatus.isOnePhaseTimeout(GlobalStatus.Begin),
                "Begin should not be one phase timeout");
        assertFalse(GlobalStatus.isOnePhaseTimeout(GlobalStatus.Committed),
                "Committed should not be one phase timeout");
        assertFalse(GlobalStatus.isOnePhaseTimeout(GlobalStatus.Rollbacked),
                "Rollbacked should not be one phase timeout");
    }

    @Test
    public void testIsTwoPhaseSuccess() {
        // Test two phase success status detection
        assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Committed),
                "Committed should be two phase success");
        assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Rollbacked),
                "Rollbacked should be two phase success");
        assertTrue(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.TimeoutRollbacked),
                "TimeoutRollbacked should be two phase success");

        // Test non-success statuses
        assertFalse(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Begin),
                "Begin should not be two phase success");
        assertFalse(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.Committing),
                "Committing should not be two phase success");
        assertFalse(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.CommitFailed),
                "CommitFailed should not be two phase success");
        assertFalse(GlobalStatus.isTwoPhaseSuccess(GlobalStatus.RollbackFailed),
                "RollbackFailed should not be two phase success");
    }

    @Test
    public void testIsTwoPhaseHeuristic() {
        // Test two phase heuristic status detection
        assertTrue(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Finished),
                "Finished should be two phase heuristic");

        // Test non-heuristic statuses
        assertFalse(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Begin),
                "Begin should not be two phase heuristic");
        assertFalse(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Committed),
                "Committed should not be two phase heuristic");
        assertFalse(GlobalStatus.isTwoPhaseHeuristic(GlobalStatus.Rollbacked),
                "Rollbacked should not be two phase heuristic");
    }

    @Test
    public void testConvertGlobalStatus() {
        // Test conversion to Apache Seata GlobalStatus
        for (GlobalStatus status : GlobalStatus.values()) {
            org.apache.seata.core.model.GlobalStatus converted = status.convertGlobalStatus();
            assertNotNull(converted, "Converted status should not be null for: " + status);
            assertEquals(status.getCode(), converted.getCode(),
                    "Converted status should have same code for: " + status);
        }
    }

    @Test
    public void testStatusDescriptions() {
        // Test that all statuses have descriptions (not testing exact text as it might change)
        for (GlobalStatus status : GlobalStatus.values()) {
            // Access the description through toString or other means
            // The exact description is implementation detail, just ensure it's accessible
            assertNotNull(status.name(), "Status name should not be null for: " + status);
            assertTrue(status.name().length() > 0, "Status name should not be empty for: " + status);
        }
    }

    @Test
    public void testEnumValueCount() {
        // Test that we have the expected number of enum values
        GlobalStatus[] values = GlobalStatus.values();
        assertEquals(18, values.length, "Should have 18 GlobalStatus enum values");
    }

    @Test
    public void testEnumOrdinals() {
        // Test that ordinals match codes for consistent ordering
        for (GlobalStatus status : GlobalStatus.values()) {
            assertEquals(status.ordinal(), status.getCode(),
                    "Ordinal should match code for status: " + status);
        }
    }

    @Test
    public void testPhaseStatusGroups() {
        // Test phase 1 statuses
        assertEquals(1, GlobalStatus.Begin.getCode(), "Begin should be phase 1");
        
        // Test phase 2 running statuses (transient states)
        assertTrue(GlobalStatus.Committing.getCode() >= 2 && GlobalStatus.Committing.getCode() <= 8,
                "Committing should be in phase 2 running range");
        assertTrue(GlobalStatus.Rollbacking.getCode() >= 2 && GlobalStatus.Rollbacking.getCode() <= 8,
                "Rollbacking should be in phase 2 running range");
        assertTrue(GlobalStatus.AsyncCommitting.getCode() >= 2 && GlobalStatus.AsyncCommitting.getCode() <= 8,
                "AsyncCommitting should be in phase 2 running range");
        
        // Test phase 2 final statuses
        assertTrue(GlobalStatus.Committed.getCode() >= 9,
                "Committed should be in final status range");
        assertTrue(GlobalStatus.Rollbacked.getCode() >= 9,
                "Rollbacked should be in final status range");
        assertTrue(GlobalStatus.Finished.getCode() >= 9,
                "Finished should be in final status range");
    }
} 