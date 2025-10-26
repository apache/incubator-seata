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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TransactionException compatibility layer.
 */
public class TransactionExceptionTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                TransactionException.class.isAnnotationPresent(Deprecated.class),
                "TransactionException should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataException() {
        assertTrue(
                org.apache.seata.core.exception.TransactionException.class.isAssignableFrom(TransactionException.class),
                "TransactionException should extend org.apache.seata.core.exception.TransactionException");
    }

    @Test
    public void testConstructorWithCode() {
        TransactionException exception = new TransactionException(TransactionExceptionCode.BeginFailed);

        assertNotNull(exception);
        assertEquals(org.apache.seata.core.exception.TransactionExceptionCode.BeginFailed, exception.getCode());
    }

    @Test
    public void testConstructorWithCodeAndCause() {
        Throwable cause = new RuntimeException("Test cause");
        TransactionException exception =
                new TransactionException(TransactionExceptionCode.FailedToSendBranchCommitRequest, cause);

        assertNotNull(exception);
        assertEquals(
                org.apache.seata.core.exception.TransactionExceptionCode.FailedToSendBranchCommitRequest,
                exception.getCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessage() {
        String message = "Test exception message";
        TransactionException exception = new TransactionException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        // Apache Seata sets default Unknown code when only message is provided
        assertEquals(org.apache.seata.core.exception.TransactionExceptionCode.Unknown, exception.getCode());
    }

    @Test
    public void testConstructorWithCodeAndMessage() {
        String message = "Rollback failed due to timeout";
        TransactionException exception =
                new TransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable, message);

        assertNotNull(exception);
        assertEquals(
                org.apache.seata.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable,
                exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new IllegalStateException("Invalid state");
        TransactionException exception = new TransactionException(cause);

        assertNotNull(exception);
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Transaction failed";
        Throwable cause = new IllegalArgumentException("Invalid argument");
        TransactionException exception = new TransactionException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithCodeMessageAndCause() {
        String message = "Global lock acquire failed";
        Throwable cause = new InterruptedException("Lock timeout");
        TransactionException exception =
                new TransactionException(TransactionExceptionCode.LockKeyConflict, message, cause);

        assertNotNull(exception);
        assertEquals(org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflict, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testAllExceptionCodes() {
        // Test that all exception codes can be properly converted via ordinal
        for (TransactionExceptionCode code : TransactionExceptionCode.values()) {
            TransactionException exception = new TransactionException(code);
            assertNotNull(exception);
            assertNotNull(exception.getCode());
            // Compare by ordinal since conversion is based on ordinal, not name
            // (io.seata has typo "FailedLockGlobalTranscation" vs Apache's "FailedLockGlobalTransaction")
            assertEquals(code.ordinal(), exception.getCode().ordinal());
        }
    }

    @Test
    public void testExceptionCodeConversion() {
        // Test specific code conversions
        TransactionException unknownException = new TransactionException(TransactionExceptionCode.Unknown);
        assertEquals(org.apache.seata.core.exception.TransactionExceptionCode.Unknown, unknownException.getCode());

        TransactionException beginException = new TransactionException(TransactionExceptionCode.BeginFailed);
        assertEquals(org.apache.seata.core.exception.TransactionExceptionCode.BeginFailed, beginException.getCode());

        TransactionException commitException =
                new TransactionException(TransactionExceptionCode.FailedToSendBranchCommitRequest);
        assertEquals(
                org.apache.seata.core.exception.TransactionExceptionCode.FailedToSendBranchCommitRequest,
                commitException.getCode());

        TransactionException rollbackException =
                new TransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable);
        assertEquals(
                org.apache.seata.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable,
                rollbackException.getCode());
    }
}
