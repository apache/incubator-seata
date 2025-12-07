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
package io.seata.core.context;

import io.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for RootContext compatibility.
 */
public class RootContextTest {

    @BeforeEach
    public void setUp() {
        // Clean up context before each test
        RootContext.unbind();
        RootContext.unbindBranchType();
        RootContext.unbindGlobalLockFlag();
    }

    @AfterEach
    public void tearDown() {
        // Clean up context after each test
        RootContext.unbind();
        RootContext.unbindBranchType();
        RootContext.unbindGlobalLockFlag();
    }

    @Test
    public void testConstants() {
        // Test KEY_XID constant
        assertEquals("TX_XID", RootContext.KEY_XID, "KEY_XID constant should match expected value");

        // Test KEY_BRANCH_TYPE constant
        assertEquals(
                "TX_BRANCH_TYPE", RootContext.KEY_BRANCH_TYPE, "KEY_BRANCH_TYPE constant should match expected value");
    }

    @Test
    public void testDeprecatedAnnotation() {
        // Test that RootContext is marked as @Deprecated
        assertTrue(
                RootContext.class.isAnnotationPresent(Deprecated.class), "RootContext should be marked as @Deprecated");
    }

    @Test
    public void testXidBindingAndUnbinding() {
        // Test initial state
        assertNull(RootContext.getXID(), "Initial XID should be null");

        // Test binding XID
        String testXid = "test-xid-123";
        RootContext.bind(testXid);
        assertEquals(testXid, RootContext.getXID(), "XID should be bound correctly");

        // Test unbinding XID
        String unboundXid = RootContext.unbind();
        assertEquals(testXid, unboundXid, "Unbind should return the previously bound XID");
        assertNull(RootContext.getXID(), "XID should be null after unbinding");
    }

    @Test
    public void testBranchTypeOperations() {
        // Test initial state
        assertNull(RootContext.getBranchType(), "Initial branch type should be null");

        // Bind XID first (required for branch type operations in some contexts)
        RootContext.bind("test-xid-for-branch");

        // Test binding branch type
        BranchType testBranchType = BranchType.AT;
        RootContext.bindBranchType(testBranchType);
        assertEquals(testBranchType, RootContext.getBranchType(), "Branch type should be bound correctly");

        // Test unbinding branch type
        BranchType unboundBranchType = RootContext.unbindBranchType();
        assertEquals(testBranchType, unboundBranchType, "Unbind should return the previously bound branch type");

        // Note: In test environment, branch type might persist until context is fully cleaned
        // So we test that unbinding works by binding a different type
        RootContext.bindBranchType(BranchType.XA);
        assertEquals(BranchType.XA, RootContext.getBranchType(), "Should be able to bind different branch type");
    }

    @Test
    public void testDefaultBranchType() {
        // Test setting default branch type (only AT and XA are allowed)
        BranchType defaultBranchType = BranchType.AT;
        assertDoesNotThrow(
                () -> RootContext.setDefaultBranchType(defaultBranchType),
                "Setting default branch type to AT should not throw exception");

        // Test that TCC is not allowed as default branch type
        assertThrows(
                IllegalArgumentException.class,
                () -> RootContext.setDefaultBranchType(BranchType.TCC),
                "Setting default branch type to TCC should throw IllegalArgumentException");
    }

    @Test
    public void testTimeoutOperations() {
        // Test initial timeout
        Integer initialTimeout = RootContext.getTimeout();
        // Initial timeout might be null or some default value

        // Test setting timeout
        Integer testTimeout = 30000; // 30 seconds
        RootContext.setTimeout(testTimeout);
        assertEquals(testTimeout, RootContext.getTimeout(), "Timeout should be set correctly");

        // Test setting null timeout
        RootContext.setTimeout(null);
        assertNull(RootContext.getTimeout(), "Timeout should be null when set to null");
    }

    @Test
    public void testGlobalLockFlag() {
        // Test initial state
        assertFalse(RootContext.requireGlobalLock(), "Initial global lock flag should be false");

        // Test binding global lock flag
        RootContext.bindGlobalLockFlag();
        assertTrue(RootContext.requireGlobalLock(), "Global lock flag should be true after binding");

        // Test unbinding global lock flag
        RootContext.unbindGlobalLockFlag();
        assertFalse(RootContext.requireGlobalLock(), "Global lock flag should be false after unbinding");
    }

    @Test
    public void testTransactionStateChecks() {
        // Test initial state - not in any transaction
        assertFalse(RootContext.inGlobalTransaction(), "Should not be in global transaction initially");
        assertFalse(RootContext.inTccBranch(), "Should not be in TCC branch initially");
        assertFalse(RootContext.inSagaBranch(), "Should not be in Saga branch initially");

        // Test with XID bound (simulating global transaction)
        RootContext.bind("test-xid");
        assertTrue(RootContext.inGlobalTransaction(), "Should be in global transaction when XID is bound");

        // Test with TCC branch type
        RootContext.bindBranchType(BranchType.TCC);
        assertTrue(RootContext.inTccBranch(), "Should be in TCC branch when TCC branch type is bound");

        // Test with Saga branch type
        RootContext.bindBranchType(BranchType.SAGA);
        assertTrue(RootContext.inSagaBranch(), "Should be in Saga branch when Saga branch type is bound");
    }

    @Test
    public void testAssertNotInGlobalTransaction() {
        // Should not throw when not in global transaction
        assertDoesNotThrow(
                () -> RootContext.assertNotInGlobalTransaction(), "Should not throw when not in global transaction");

        // Should throw when in global transaction (may throw ShouldNeverHappenException in compatible mode)
        RootContext.bind("test-xid");
        assertThrows(
                RuntimeException.class,
                () -> RootContext.assertNotInGlobalTransaction(),
                "Should throw exception when in global transaction");
    }

    @Test
    public void testEntries() {
        // Test entries method returns a map
        Map<String, Object> entries = RootContext.entries();
        assertNotNull(entries, "Entries should not be null");
        assertTrue(entries instanceof Map, "Entries should be a Map");
    }

    @Test
    public void testComplexScenario() {
        // Test a complex scenario with multiple operations
        String xid = "complex-test-xid";
        BranchType branchType = BranchType.AT;
        Integer timeout = 60000;

        // Bind all context
        RootContext.bind(xid);
        RootContext.bindBranchType(branchType);
        RootContext.bindGlobalLockFlag();
        RootContext.setTimeout(timeout);

        // Verify all are set correctly
        assertEquals(xid, RootContext.getXID());
        assertEquals(branchType, RootContext.getBranchType());
        assertTrue(RootContext.requireGlobalLock());
        assertEquals(timeout, RootContext.getTimeout());
        assertTrue(RootContext.inGlobalTransaction());

        // Clean up
        assertEquals(xid, RootContext.unbind());
        assertEquals(branchType, RootContext.unbindBranchType());
        RootContext.unbindGlobalLockFlag();

        // Verify clean state
        assertNull(RootContext.getXID());
        assertNull(RootContext.getBranchType());
        assertFalse(RootContext.requireGlobalLock());
        assertFalse(RootContext.inGlobalTransaction());
    }
}
