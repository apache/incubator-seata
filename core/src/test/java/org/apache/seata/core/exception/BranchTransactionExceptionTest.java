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
package org.apache.seata.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BranchTransactionExceptionTest {

    @Test
    public void testConstructorWithCode() {
        BranchTransactionException exception = new BranchTransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable);
        assertEquals(TransactionExceptionCode.BranchRollbackFailed_Retriable, exception.getCode());
    }

    @Test
    public void testConstructorWithCodeAndCause() {
        Throwable cause = new RuntimeException("test");
        BranchTransactionException exception = new BranchTransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable, cause);
        assertEquals(TransactionExceptionCode.BranchRollbackFailed_Retriable, exception.getCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessage() {
        BranchTransactionException exception = new BranchTransactionException("test message");
        assertEquals("test message", exception.getMessage());
    }

    @Test
    public void testConstructorWithCodeAndMessage() {
        BranchTransactionException exception = new BranchTransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable, "test message");
        assertEquals(TransactionExceptionCode.BranchRollbackFailed_Retriable, exception.getCode());
        assertEquals("test message", exception.getMessage());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new RuntimeException("test");
        BranchTransactionException exception = new BranchTransactionException(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("test");
        BranchTransactionException exception = new BranchTransactionException("test message", cause);
        assertEquals("test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithCodeMessageAndCause() {
        Throwable cause = new RuntimeException("test");
        BranchTransactionException exception = new BranchTransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable, "test message", cause);
        assertEquals(TransactionExceptionCode.BranchRollbackFailed_Retriable, exception.getCode());
        assertEquals("test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}