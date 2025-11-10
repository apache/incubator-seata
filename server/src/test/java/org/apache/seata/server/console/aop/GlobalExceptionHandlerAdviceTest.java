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
package org.apache.seata.server.console.aop;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.exception.ConsoleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlobalExceptionHandlerAdviceTest {

    private GlobalExceptionHandlerAdvice advice;

    @BeforeEach
    public void setUp() {
        advice = new GlobalExceptionHandlerAdvice();
    }

    @Test
    public void testHandlerConsoleException() {
        RuntimeException cause = new RuntimeException("Original cause");
        String logMessage = "Console error occurred";
        ConsoleException exception = new ConsoleException(cause, logMessage);

        SingleResult<Void> result = advice.handlerConsoleException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(exception.getMessage(), result.getMessage());
    }

    @Test
    public void testHandlerConsoleException_WithNullLogMessage() {
        RuntimeException cause = new RuntimeException("Original cause");
        ConsoleException exception = new ConsoleException(cause, null);

        SingleResult<Void> result = advice.handlerConsoleException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandlerIllegalArgumentException() {
        String errorMessage = "Invalid argument provided";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        SingleResult<Void> result = advice.handlerIllegalArgumentException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }

    @Test
    public void testHandlerIllegalArgumentException_WithNullMessage() {
        IllegalArgumentException exception = new IllegalArgumentException();

        SingleResult<Void> result = advice.handlerIllegalArgumentException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandlerIllegalStateException() {
        String errorMessage = "Illegal state detected";
        IllegalStateException exception = new IllegalStateException(errorMessage);

        SingleResult<Void> result = advice.handlerIllegalStateException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }

    @Test
    public void testHandlerIllegalStateException_WithNullMessage() {
        IllegalStateException exception = new IllegalStateException();

        SingleResult<Void> result = advice.handlerIllegalStateException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandlerShouldNeverHappenException() {
        String errorMessage = "This should never happen";
        ShouldNeverHappenException exception = new ShouldNeverHappenException(errorMessage);

        SingleResult<Void> result = advice.handlerShouldNeverHappenException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }

    @Test
    public void testHandlerShouldNeverHappenException_WithCause() {
        RuntimeException cause = new RuntimeException("Cause");
        ShouldNeverHappenException exception = new ShouldNeverHappenException(cause);

        SingleResult<Void> result = advice.handlerShouldNeverHappenException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandlerFrameworkException() {
        String errorMessage = "Framework error occurred";
        FrameworkException exception = new FrameworkException(errorMessage);

        SingleResult<Void> result = advice.handlerFrameworkException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }

    @Test
    public void testHandlerFrameworkException_WithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        String errorMessage = "Framework error with cause";
        FrameworkException exception = new FrameworkException(cause, errorMessage);

        SingleResult<Void> result = advice.handlerFrameworkException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandleException() {
        String errorMessage = "Generic exception occurred";
        Exception exception = new Exception(errorMessage);

        SingleResult<Void> result = advice.handleException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }

    @Test
    public void testHandleException_WithNullMessage() {
        Exception exception = new Exception();

        SingleResult<Void> result = advice.handleException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    public void testHandleException_RuntimeException() {
        String errorMessage = "Runtime exception";
        RuntimeException exception = new RuntimeException(errorMessage);

        SingleResult<Void> result = advice.handleException(exception);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(errorMessage, result.getMessage());
    }
}
