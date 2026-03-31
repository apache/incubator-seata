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
package org.apache.seata.saga.engine.exception;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link ForwardInvalidException}
 */
public class ForwardInvalidExceptionTest {

    @Test
    public void defaultConstructorTest() {
        ForwardInvalidException e = new ForwardInvalidException();
        assertNotNull(e);
    }

    @Test
    public void constructorWithErrorCodeTest() {
        ForwardInvalidException e = new ForwardInvalidException(FrameworkErrorCode.UnknownAppError);
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void constructorWithMessageTest() {
        ForwardInvalidException e = new ForwardInvalidException("forward invalid");
        assertEquals("forward invalid", e.getMessage());
    }

    @Test
    public void constructorWithMessageAndErrorCodeTest() {
        ForwardInvalidException e = new ForwardInvalidException("forward invalid", FrameworkErrorCode.UnknownAppError);
        assertEquals("forward invalid", e.getMessage());
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void constructorWithCauseMessageAndErrorCodeTest() {
        Throwable cause = new RuntimeException("root");
        ForwardInvalidException e =
                new ForwardInvalidException(cause, "forward invalid", FrameworkErrorCode.UnknownAppError);
        assertSame(cause, e.getCause());
        assertEquals("forward invalid", e.getMessage());
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void constructorWithCauseTest() {
        Throwable cause = new RuntimeException("root");
        ForwardInvalidException e = new ForwardInvalidException(cause);
        assertSame(cause, e.getCause());
    }

    @Test
    public void constructorWithCauseAndMessageTest() {
        Throwable cause = new RuntimeException("root");
        ForwardInvalidException e = new ForwardInvalidException(cause, "forward invalid");
        assertEquals("forward invalid", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void inheritanceFromEngineExecutionExceptionTest() {
        ForwardInvalidException e = new ForwardInvalidException("test");
        e.setStateName("TestState");
        e.setStateMachineName("TestMachine");
        assertEquals("TestState", e.getStateName());
        assertEquals("TestMachine", e.getStateMachineName());
    }
}
