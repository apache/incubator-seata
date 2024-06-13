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
package org.apache.seata.sqlparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SQLParsingExceptionTest {
    @Test
    public void testConstructorWithMessage() {
        String message = "Test message";
        SQLParsingException exception = new SQLParsingException(message);
        assertEquals(message, exception.getMessage(), "Message should match");
        assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test message";
        Throwable cause = new IllegalArgumentException("Test cause");
        SQLParsingException exception = new SQLParsingException(message, cause);
        assertEquals(message, exception.getMessage(), "Message should match");
        assertEquals(cause, exception.getCause(), "Cause should match");
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new IllegalArgumentException("Test cause");
        SQLParsingException exception = new SQLParsingException(cause);
        assertEquals(cause.toString(), exception.getMessage(), "Message should be cause's toString");
        assertEquals(cause, exception.getCause(), "Cause should match");
    }
}
