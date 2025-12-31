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
package org.apache.seata.server.console.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConsoleException Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsoleException Test")
class ConsoleExceptionTest {

    private ConsoleException consoleException;
    private Throwable cause;
    private String logMessage;

    @BeforeEach
    void setUp() {
        cause = new RuntimeException("Test cause");
        logMessage = "Test log message";
        consoleException = new ConsoleException(cause, logMessage);
    }

    @Test
    @DisplayName("test constructor initializes with cause and message")
    void testConstructorInitializesWithCauseAndMessage() {
        assertNotNull(consoleException);
        assertEquals(logMessage, consoleException.getMessage());
        assertEquals(logMessage, consoleException.getLogMessage());
    }

    @Test
    @DisplayName("test getMessage returns log message")
    void testGetMessageReturnsLogMessage() {
        assertEquals(logMessage, consoleException.getMessage());
    }

    @Test
    @DisplayName("test getLogMessage returns log message")
    void testGetLogMessageReturnsLogMessage() {
        assertEquals(logMessage, consoleException.getLogMessage());
    }

    @Test
    @DisplayName("test setLogMessage updates message")
    void testSetLogMessageUpdatesMessage() {
        String newMessage = "New message";
        consoleException.setLogMessage(newMessage);
        
        assertEquals(newMessage, consoleException.getLogMessage());
        assertEquals(newMessage, consoleException.getMessage());
    }

    @Test
    @DisplayName("test ConsoleException extends RuntimeException")
    void testConsoleExceptionExtendsRuntimeException() {
        assertTrue(consoleException instanceof RuntimeException);
    }

    @Test
    @DisplayName("test exception with null cause")
    void testExceptionWithNullCause() {
        ConsoleException exception = new ConsoleException(null, "message");
        assertNotNull(exception);
        assertEquals("message", exception.getMessage());
    }

    @Test
    @DisplayName("test exception with null log message")
    void testExceptionWithNullLogMessage() {
        ConsoleException exception = new ConsoleException(cause, null);
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("test exception with empty log message")
    void testExceptionWithEmptyLogMessage() {
        ConsoleException exception = new ConsoleException(cause, "");
        assertNotNull(exception);
        assertEquals("", exception.getMessage());
    }

    @Test
    @DisplayName("test getCause returns original cause")
    void testGetCauseReturnsOriginalCause() {
        assertEquals(cause, consoleException.getCause());
    }

    @Test
    @DisplayName("test exception can be thrown and caught")
    void testExceptionCanBeThrownAndCaught() {
        assertThrows(ConsoleException.class, () -> {
            throw new ConsoleException(new RuntimeException("cause"), "Test exception");
        });
    }

    @Test
    @DisplayName("test exception message format")
    void testExceptionMessageFormat() {
        String message = "Error occurred";
        ConsoleException exception = new ConsoleException(cause, message);
        
        assertNotNull(exception.toString());
        assertTrue(exception.toString().contains(message) || exception.toString().contains("ConsoleException"));
    }

    @Test
    @DisplayName("test multiple setLogMessage calls")
    void testMultipleSetLogMessageCalls() {
        consoleException.setLogMessage("Message 1");
        assertEquals("Message 1", consoleException.getMessage());
        
        consoleException.setLogMessage("Message 2");
        assertEquals("Message 2", consoleException.getMessage());
        
        consoleException.setLogMessage("Message 3");
        assertEquals("Message 3", consoleException.getMessage());
    }
}
