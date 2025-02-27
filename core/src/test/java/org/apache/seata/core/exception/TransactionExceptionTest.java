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

public class TransactionExceptionTest {

    @Test
    public void testConstructorWithCode() {
        TransactionException exception = new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist);
        assertEquals(TransactionExceptionCode.GlobalTransactionNotExist, exception.getCode());
    }

    @Test
    public void testConstructorWithCodeAndCause() {
        Throwable cause = new RuntimeException("test");
        TransactionException exception = new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, cause);
        assertEquals(TransactionExceptionCode.GlobalTransactionNotExist, exception.getCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessage() {
        TransactionException exception = new TransactionException("test message");
        assertEquals("test message", exception.getMessage());
    }

    @Test
    public void testConstructorWithCodeAndMessage() {
        TransactionException exception = new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, "test message");
        assertEquals(TransactionExceptionCode.GlobalTransactionNotExist, exception.getCode());
        assertEquals("test message", exception.getMessage());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new RuntimeException("test");
        TransactionException exception = new TransactionException(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("test");
        TransactionException exception = new TransactionException("test message", cause);
        assertEquals("test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testConstructorWithCodeMessageAndCause() {
        Throwable cause = new RuntimeException("test");
        TransactionException exception = new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, "test message", cause);
        assertEquals(TransactionExceptionCode.GlobalTransactionNotExist, exception.getCode());
        assertEquals("test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}