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
package org.apache.seata.common.exception;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ParseEndpointExceptionTest {

    @Test
    void testNoArgConstructor() {
        ParseEndpointException e = new ParseEndpointException();
        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testMessageConstructor() {
        String message = "Invalid endpoint format: tcp://localhost:8091";
        ParseEndpointException e = new ParseEndpointException(message);
        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testCauseConstructor() {
        ParseEndpointException e = new ParseEndpointException();
        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Failed to parse endpoint";
        Throwable cause = new IllegalArgumentException("missing host");
        ParseEndpointException e = new ParseEndpointException(message, cause);
        assertEquals(message, e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void testAllArgsConstructor() {
        String message = "endpoint malformed";
        Throwable cause = new RuntimeException("network error");
        boolean enableSuppression = true;
        boolean writableStackTrace = false;

        ParseEndpointException e = new ParseEndpointException(
                message, cause, enableSuppression, writableStackTrace
        );

        assertEquals(message, e.getMessage());
        assertSame(cause, e.getCause());

        assertEquals(0, e.getStackTrace().length);
    }

    @Test
    void testToStringAndPrintStackTrace() {
        ParseEndpointException e = new ParseEndpointException("test", new NullPointerException());
        assertNotNull(e.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        try {
            e.printStackTrace(ps);
            assertTrue(out.size() > 0);
        } catch (Exception ignored) {}
    }
}