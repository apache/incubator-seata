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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConsoleExceptionTest {

    @Test
    public void testConsoleException_WithCauseAndLogMessage() {
        RuntimeException cause = new RuntimeException("Test cause");
        String logMessage = "Test log message";

        ConsoleException exception = new ConsoleException(cause, logMessage);

        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertEquals(logMessage, exception.getLogMessage());
    }

    @Test
    public void testConsoleException_GetAndSetLogMessage() {
        RuntimeException cause = new RuntimeException("Test cause");
        String logMessage = "Original log message";

        ConsoleException exception = new ConsoleException(cause, logMessage);
        Assertions.assertEquals(logMessage, exception.getLogMessage());

        String newLogMessage = "New log message";
        exception.setLogMessage(newLogMessage);
        Assertions.assertEquals(newLogMessage, exception.getLogMessage());
    }

    @Test
    public void testConsoleException_WithNullCause() {
        String logMessage = "Test log message";

        ConsoleException exception = new ConsoleException(null, logMessage);

        Assertions.assertNull(exception.getCause());
        Assertions.assertEquals(logMessage, exception.getLogMessage());
    }

    @Test
    public void testConsoleException_WithNullLogMessage() {
        RuntimeException cause = new RuntimeException("Test cause");

        ConsoleException exception = new ConsoleException(cause, null);

        Assertions.assertEquals(cause, exception.getCause());
        Assertions.assertNull(exception.getLogMessage());
    }

    @Test
    public void testConsoleException_IsRuntimeException() {
        RuntimeException cause = new RuntimeException("Test cause");
        String logMessage = "Test log message";

        ConsoleException exception = new ConsoleException(cause, logMessage);

        Assertions.assertTrue(exception instanceof RuntimeException);
    }
}
