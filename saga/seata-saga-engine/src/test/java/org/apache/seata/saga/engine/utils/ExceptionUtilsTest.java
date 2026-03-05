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
package org.apache.seata.saga.engine.utils;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link ExceptionUtils}
 */
public class ExceptionUtilsTest {

    @Test
    public void createEngineExecutionExceptionWithAllParamsTest() {
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachine sm = mock(StateMachine.class);
        StateInstance stateInstance = mock(StateInstance.class);

        when(smInstance.getStateMachine()).thenReturn(sm);
        when(sm.getAppName()).thenReturn("testApp");
        when(smInstance.getId()).thenReturn("sm-123");
        when(stateInstance.getName()).thenReturn("testState");
        when(stateInstance.getId()).thenReturn("state-456");

        Exception cause = new RuntimeException("test");
        EngineExecutionException result = ExceptionUtils.createEngineExecutionException(
                cause, FrameworkErrorCode.UnknownAppError, "test message", smInstance, stateInstance);

        assertEquals("test message", result.getMessage());
        assertEquals("testApp", result.getStateMachineName());
        assertEquals("sm-123", result.getStateMachineInstanceId());
        assertEquals("testState", result.getStateName());
        assertEquals("state-456", result.getStateInstanceId());
    }

    @Test
    public void createEngineExecutionExceptionWithNullStateMachineInstanceTest() {
        EngineExecutionException result = ExceptionUtils.createEngineExecutionException(
                null, FrameworkErrorCode.UnknownAppError, "test", null, (StateInstance) null);

        assertNull(result.getStateMachineName());
        assertNull(result.getStateMachineInstanceId());
    }

    @Test
    public void createEngineExecutionExceptionWithStateNameTest() {
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachine sm = mock(StateMachine.class);

        when(smInstance.getStateMachine()).thenReturn(sm);
        when(sm.getAppName()).thenReturn("testApp");
        when(smInstance.getId()).thenReturn("sm-123");

        Exception cause = new RuntimeException("test");
        EngineExecutionException result = ExceptionUtils.createEngineExecutionException(
                cause, FrameworkErrorCode.UnknownAppError, "test message", smInstance, "testStateName");

        assertEquals("test message", result.getMessage());
        assertEquals("testApp", result.getStateMachineName());
        assertEquals("sm-123", result.getStateMachineInstanceId());
        assertEquals("testStateName", result.getStateName());
    }

    @Test
    public void getNetExceptionTypeSocketTimeoutExceptionConnectTimeoutTest() {
        SocketTimeoutException e = new SocketTimeoutException("connect timed out");
        assertEquals(ExceptionUtils.NetExceptionType.CONNECT_TIMEOUT_EXCEPTION, ExceptionUtils.getNetExceptionType(e));
    }

    @Test
    public void getNetExceptionTypeSocketTimeoutExceptionReadTimeoutTest() {
        SocketTimeoutException e = new SocketTimeoutException("read timed out");
        assertEquals(ExceptionUtils.NetExceptionType.READ_TIMEOUT_EXCEPTION, ExceptionUtils.getNetExceptionType(e));
    }

    @Test
    public void getNetExceptionTypeConnectExceptionTest() {
        ConnectException e = new ConnectException("Connection refused");
        assertEquals(ExceptionUtils.NetExceptionType.CONNECT_EXCEPTION, ExceptionUtils.getNetExceptionType(e));
    }

    @Test
    public void getNetExceptionTypeNestedNetworkExceptionTest() {
        ConnectException innerException = new ConnectException();
        RuntimeException outerException = new RuntimeException("wrapper", innerException);
        assertEquals(
                ExceptionUtils.NetExceptionType.CONNECT_EXCEPTION, ExceptionUtils.getNetExceptionType(outerException));
    }

    @Test
    public void getNetExceptionTypeNotNetExceptionTest() {
        RuntimeException e = new RuntimeException("not a network error");
        assertEquals(ExceptionUtils.NetExceptionType.NOT_NET_EXCEPTION, ExceptionUtils.getNetExceptionType(e));
    }

    @Test
    public void getNetExceptionTypeMaxCauseDepthExceededTest() {
        Exception current = new RuntimeException("base");
        for (int i = 0; i < 25; i++) {
            current = new RuntimeException("level-" + i, current);
        }
        assertEquals(ExceptionUtils.NetExceptionType.NOT_NET_EXCEPTION, ExceptionUtils.getNetExceptionType(current));
    }

    @Test
    public void isNetExceptionWhenIsNetworkExceptionReturnTrueTest() {
        ConnectException e = new ConnectException();
        assertTrue(ExceptionUtils.isNetException(e));
    }

    @Test
    public void isNetExceptionWhenNotNetworkExceptionReturnFalseTest() {
        RuntimeException e = new RuntimeException("not network");
        assertFalse(ExceptionUtils.isNetException(e));
    }
}
