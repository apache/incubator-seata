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
package io.seata.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TransactionExceptionCode enum compatibility.
 */
public class TransactionExceptionCodeTest {

    @Test
    public void testDeprecatedAnnotation() {
        // Test that TransactionExceptionCode is marked as @Deprecated
        assertTrue(TransactionExceptionCode.class.isAnnotationPresent(Deprecated.class),
                "TransactionExceptionCode should be marked as @Deprecated");
    }

    @Test
    public void testAllEnumValues() {
        // Test all enum values exist
        assertNotNull(TransactionExceptionCode.Unknown);
        assertNotNull(TransactionExceptionCode.BeginFailed);
        assertNotNull(TransactionExceptionCode.LockKeyConflict);
        assertNotNull(TransactionExceptionCode.IO);
        assertNotNull(TransactionExceptionCode.BranchRollbackFailed_Retriable);
        assertNotNull(TransactionExceptionCode.BranchRollbackFailed_Unretriable);
        assertNotNull(TransactionExceptionCode.BranchRegisterFailed);
        assertNotNull(TransactionExceptionCode.BranchReportFailed);
        assertNotNull(TransactionExceptionCode.LockableCheckFailed);
        assertNotNull(TransactionExceptionCode.BranchTransactionNotExist);
        assertNotNull(TransactionExceptionCode.GlobalTransactionNotExist);
        assertNotNull(TransactionExceptionCode.GlobalTransactionNotActive);
        assertNotNull(TransactionExceptionCode.GlobalTransactionStatusInvalid);
        assertNotNull(TransactionExceptionCode.FailedToSendBranchCommitRequest);
        assertNotNull(TransactionExceptionCode.FailedToSendBranchRollbackRequest);
        assertNotNull(TransactionExceptionCode.FailedToAddBranch);
        assertNotNull(TransactionExceptionCode.FailedLockGlobalTranscation);
        assertNotNull(TransactionExceptionCode.FailedWriteSession);
        assertNotNull(TransactionExceptionCode.FailedStore);
        assertNotNull(TransactionExceptionCode.NotRaftLeader);
        assertNotNull(TransactionExceptionCode.LockKeyConflictFailFast);
        assertNotNull(TransactionExceptionCode.TransactionTimeout);
        assertNotNull(TransactionExceptionCode.CommitHeuristic);
        assertNotNull(TransactionExceptionCode.Broken);
    }

    @Test
    public void testEnumOrdinals() {
        // Test that ordinals are consistent (important for serialization)
        TransactionExceptionCode[] values = TransactionExceptionCode.values();
        
        assertEquals(0, TransactionExceptionCode.Unknown.ordinal());
        assertEquals(1, TransactionExceptionCode.BeginFailed.ordinal());
        assertEquals(2, TransactionExceptionCode.LockKeyConflict.ordinal());
        assertEquals(3, TransactionExceptionCode.IO.ordinal());
        
        // Test that ordinals are sequential
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal(),
                    "Ordinal should be sequential for index: " + i);
        }
    }

    @Test
    public void testGetByIntOrdinal() {
        // Test getting exception code by int ordinal
        TransactionExceptionCode[] values = TransactionExceptionCode.values();
        
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], TransactionExceptionCode.get(i),
                    "Should get correct enum value for ordinal: " + i);
        }
    }

    @Test
    public void testGetByByteOrdinal() {
        // Test getting exception code by byte ordinal
        assertEquals(TransactionExceptionCode.Unknown, TransactionExceptionCode.get((byte) 0));
        assertEquals(TransactionExceptionCode.BeginFailed, TransactionExceptionCode.get((byte) 1));
        assertEquals(TransactionExceptionCode.LockKeyConflict, TransactionExceptionCode.get((byte) 2));
        assertEquals(TransactionExceptionCode.IO, TransactionExceptionCode.get((byte) 3));
    }

    @Test
    public void testGetInvalidOrdinal() {
        // Test that invalid ordinals throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> TransactionExceptionCode.get(-1),
                "Should throw IllegalArgumentException for negative ordinal");
        assertThrows(IllegalArgumentException.class, () -> TransactionExceptionCode.get(100),
                "Should throw IllegalArgumentException for ordinal > max value");
        assertThrows(IllegalArgumentException.class, 
                () -> TransactionExceptionCode.get(TransactionExceptionCode.values().length),
                "Should throw IllegalArgumentException for ordinal >= values length");
    }

    @Test
    public void testConvertTransactionExceptionCode() {
        // Test conversion to Apache Seata TransactionExceptionCode
        for (TransactionExceptionCode code : TransactionExceptionCode.values()) {
            org.apache.seata.core.exception.TransactionExceptionCode converted = 
                    code.convertTransactionExceptionCode();
            assertNotNull(converted, "Converted exception code should not be null for: " + code);
            assertEquals(code.ordinal(), converted.ordinal(),
                    "Converted exception code should have same ordinal for: " + code);
        }
    }

    @Test
    public void testEnumValueCount() {
        // Test that we have the expected number of enum values
        TransactionExceptionCode[] values = TransactionExceptionCode.values();
        assertEquals(24, values.length, "Should have 24 TransactionExceptionCode enum values");
    }

    @Test
    public void testSpecificExceptionCodes() {
        // Test some specific important exception codes
        assertEquals("Unknown", TransactionExceptionCode.Unknown.name());
        assertEquals("BeginFailed", TransactionExceptionCode.BeginFailed.name());
        assertEquals("LockKeyConflict", TransactionExceptionCode.LockKeyConflict.name());
        assertEquals("BranchRollbackFailed_Retriable", TransactionExceptionCode.BranchRollbackFailed_Retriable.name());
        assertEquals("BranchRollbackFailed_Unretriable", TransactionExceptionCode.BranchRollbackFailed_Unretriable.name());
        assertEquals("GlobalTransactionNotExist", TransactionExceptionCode.GlobalTransactionNotExist.name());
        assertEquals("TransactionTimeout", TransactionExceptionCode.TransactionTimeout.name());
    }

    @Test
    public void testExceptionCodeCategories() {
        // Test that different categories of exceptions exist
        
        // Connection/IO related
        assertNotNull(TransactionExceptionCode.IO);
        assertNotNull(TransactionExceptionCode.FailedWriteSession);
        assertNotNull(TransactionExceptionCode.FailedStore);
        
        // Transaction lifecycle
        assertNotNull(TransactionExceptionCode.BeginFailed);
        assertNotNull(TransactionExceptionCode.TransactionTimeout);
        assertNotNull(TransactionExceptionCode.CommitHeuristic);
        
        // Branch operations
        assertNotNull(TransactionExceptionCode.BranchRegisterFailed);
        assertNotNull(TransactionExceptionCode.BranchReportFailed);
        assertNotNull(TransactionExceptionCode.BranchRollbackFailed_Retriable);
        assertNotNull(TransactionExceptionCode.BranchRollbackFailed_Unretriable);
        
        // Lock operations
        assertNotNull(TransactionExceptionCode.LockKeyConflict);
        assertNotNull(TransactionExceptionCode.LockKeyConflictFailFast);
        assertNotNull(TransactionExceptionCode.LockableCheckFailed);
        
        // Global transaction states
        assertNotNull(TransactionExceptionCode.GlobalTransactionNotExist);
        assertNotNull(TransactionExceptionCode.GlobalTransactionNotActive);
        assertNotNull(TransactionExceptionCode.GlobalTransactionStatusInvalid);
    }

    @Test
    public void testAllValuesHaveNames() {
        // Test that all enum values have proper names
        for (TransactionExceptionCode code : TransactionExceptionCode.values()) {
            assertNotNull(code.name(), "Exception code name should not be null for: " + code);
            assertTrue(code.name().length() > 0, "Exception code name should not be empty for: " + code);
        }
    }

    @Test
    public void testBidirectionalCompatibility() {
        // Test that conversion is bidirectional compatible
        for (TransactionExceptionCode originalCode : TransactionExceptionCode.values()) {
            org.apache.seata.core.exception.TransactionExceptionCode apacheCode = 
                    originalCode.convertTransactionExceptionCode();
            
            // Convert back using ordinal
            TransactionExceptionCode backConverted = TransactionExceptionCode.get(apacheCode.ordinal());
            
            assertEquals(originalCode, backConverted,
                    "Bidirectional conversion should work for: " + originalCode);
        }
    }
} 