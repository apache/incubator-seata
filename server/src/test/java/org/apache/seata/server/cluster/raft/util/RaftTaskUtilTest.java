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
package org.apache.seata.server.cluster.raft.util;

import org.apache.seata.core.exception.GlobalTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RaftTaskUtil focusing on future handling and exception management.
 */
public class RaftTaskUtilTest {

    @Test
    public void testFutureGetWithSuccessfulCompletion() throws TransactionException {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);

        boolean result = RaftTaskUtil.futureGet(future);

        assertTrue(result);
    }

    @Test
    public void testFutureGetWithFalseResult() throws TransactionException {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(false);

        boolean result = RaftTaskUtil.futureGet(future);

        assertFalse(result);
    }

    @Test
    public void testFutureGetWithInterruptedException() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeExceptionally(new InterruptedException("Test interruption"));

        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.FailedWriteSession, exception.getCode());
        assertTrue(exception.getMessage().contains("Fail to store global session"));
    }

    @Test
    public void testFutureGetWithTransactionExceptionInExecutionException() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        TransactionException cause =
                new TransactionException(TransactionExceptionCode.BranchRegisterFailed, "Branch registration failed");
        future.completeExceptionally(cause);

        TransactionException exception = assertThrows(TransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.BranchRegisterFailed, exception.getCode());
        assertTrue(exception.getMessage().contains("Branch registration failed"));
    }

    @Test
    public void testFutureGetWithGlobalTransactionExceptionInExecutionException() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        GlobalTransactionException cause = new GlobalTransactionException(
                TransactionExceptionCode.GlobalTransactionNotExist, "Global transaction does not exist");
        future.completeExceptionally(cause);

        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.GlobalTransactionNotExist, exception.getCode());
        assertTrue(exception.getMessage().contains("Global transaction does not exist"));
    }

    @Test
    public void testFutureGetWithNonTransactionExceptionInExecutionException() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RuntimeException cause = new RuntimeException("Unexpected error");
        future.completeExceptionally(cause);

        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.FailedWriteSession, exception.getCode());
        assertTrue(exception.getMessage().contains("Fail to store global session"));
    }

    @Test
    public void testFutureGetWithExecutionExceptionWithNullCause() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeExceptionally(new ExecutionException("Error message", null));

        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.FailedWriteSession, exception.getCode());
    }

    @Test
    public void testFutureGetWithMultipleCompletions() throws TransactionException {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);

        // First get
        boolean result1 = RaftTaskUtil.futureGet(future);
        assertTrue(result1);

        // Second get should return the same result
        boolean result2 = RaftTaskUtil.futureGet(future);
        assertTrue(result2);
    }

    @Test
    public void testFutureGetPreservesTransactionExceptionCode() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        TransactionException cause =
                new TransactionException(TransactionExceptionCode.LockKeyConflict, "Lock conflict detected");
        future.completeExceptionally(cause);

        TransactionException exception = assertThrows(TransactionException.class, () -> RaftTaskUtil.futureGet(future));

        assertEquals(TransactionExceptionCode.LockKeyConflict, exception.getCode());
        assertEquals("Lock conflict detected", exception.getMessage());
    }

    @Test
    public void testFutureGetWithDifferentTransactionExceptionTypes() {
        // Test with various TransactionException subclasses
        CompletableFuture<Boolean> future1 = new CompletableFuture<>();
        future1.completeExceptionally(
                new GlobalTransactionException(TransactionExceptionCode.FailedToSendBranchCommitRequest));

        assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future1));

        CompletableFuture<Boolean> future2 = new CompletableFuture<>();
        future2.completeExceptionally(new TransactionException(TransactionExceptionCode.FailedToAddBranch));

        assertThrows(TransactionException.class, () -> RaftTaskUtil.futureGet(future2));
    }

    @Test
    public void testFutureGetWithAsyncCompletion() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Complete asynchronously
        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(100);
                future.complete(true);
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });
        completer.start();

        boolean result = RaftTaskUtil.futureGet(future);

        assertTrue(result);
        completer.join();
    }

    @Test
    public void testFutureGetExceptionMessageContainsDetails() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeExceptionally(new InterruptedException("Specific interruption reason"));

        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        // The exception message should contain reference to the failure
        assertTrue(exception.getMessage().contains("Fail to store global session"));
        assertTrue(exception.getMessage().contains("Specific interruption reason"));
    }

    @Test
    public void testFutureGetWithNestedExecutionException() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ExecutionException nested =
                new ExecutionException(new TransactionException(TransactionExceptionCode.BeginFailed, "Begin failed"));
        future.completeExceptionally(nested);

        // The ExecutionException's cause is another ExecutionException,
        // which is not a TransactionException, so should be wrapped
        GlobalTransactionException exception =
                assertThrows(GlobalTransactionException.class, () -> RaftTaskUtil.futureGet(future));

        // Should wrap the ExecutionException since it's not TransactionException
        assertEquals(TransactionExceptionCode.FailedWriteSession, exception.getCode());
    }
}
