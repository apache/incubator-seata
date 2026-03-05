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
 * Test class for {@link EngineExecutionException}
 */
public class EngineExecutionExceptionTest {

    @Test
    public void defaultConstructorTest() {
        EngineExecutionException e = new EngineExecutionException();
        assertNotNull(e);
        // Parent class FrameworkException default constructor sets the default message
        assertNotNull(e.getMessage());
    }

    @Test
    public void constructorWithErrorCodeTest() {
        EngineExecutionException e = new EngineExecutionException(FrameworkErrorCode.UnknownAppError);
        assertNotNull(e);
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void constructorWithMessageTest() {
        EngineExecutionException e = new EngineExecutionException("Test error message");
        assertEquals("Test error message", e.getMessage());
    }

    @Test
    public void constructorWithMessageAndErrorCodeTest() {
        EngineExecutionException e = new EngineExecutionException("Test error", FrameworkErrorCode.UnknownAppError);
        assertEquals("Test error", e.getMessage());
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void constructorWithCauseTest() {
        Throwable cause = new RuntimeException("root cause");
        EngineExecutionException e = new EngineExecutionException(cause);
        assertSame(cause, e.getCause());
    }

    @Test
    public void constructorWithCauseAndMessageTest() {
        Throwable cause = new RuntimeException("root cause");
        EngineExecutionException e = new EngineExecutionException(cause, "wrapper message");
        assertEquals("wrapper message", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void constructorWithCauseMessageAndErrorCodeTest() {
        Throwable cause = new RuntimeException("root cause");
        EngineExecutionException e =
                new EngineExecutionException(cause, "wrapper message", FrameworkErrorCode.UnknownAppError);
        assertEquals("wrapper message", e.getMessage());
        assertSame(cause, e.getCause());
        assertEquals(FrameworkErrorCode.UnknownAppError, e.getErrcode());
    }

    @Test
    public void stateNameGetterSetterTest() {
        EngineExecutionException e = new EngineExecutionException();
        e.setStateName("TestState");
        assertEquals("TestState", e.getStateName());
    }

    @Test
    public void stateMachineNameGetterSetterTest() {
        EngineExecutionException e = new EngineExecutionException();
        e.setStateMachineName("TestMachine");
        assertEquals("TestMachine", e.getStateMachineName());
    }

    @Test
    public void stateMachineInstanceIdGetterSetterTest() {
        EngineExecutionException e = new EngineExecutionException();
        e.setStateMachineInstanceId("instance-123");
        assertEquals("instance-123", e.getStateMachineInstanceId());
    }

    @Test
    public void stateInstanceIdGetterSetterTest() {
        EngineExecutionException e = new EngineExecutionException();
        e.setStateInstanceId("state-456");
        assertEquals("state-456", e.getStateInstanceId());
    }
}
