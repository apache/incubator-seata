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
package io.seata.saga.statelang.domain.impl;

import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for StateImpl.
 */
public class StateImplTest {

    private StateImpl state;
    private org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl apacheState;

    @BeforeEach
    public void setUp() {
        apacheState = new org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl();
        state = StateImpl.wrap(apacheState);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(StateImpl.class.isAnnotationPresent(Deprecated.class), "StateImpl should be marked as @Deprecated");
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl apache =
                new org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl();
        StateImpl wrapped = StateImpl.wrap(apache);
        assertNotNull(wrapped);
        assertSame(apache, wrapped.unwrap());
    }

    @Test
    public void testWrapNull() {
        assertNull(StateImpl.wrap(null));
    }

    @Test
    public void testUnwrap() {
        assertSame(apacheState, state.unwrap());
    }

    @Test
    public void testGetName() {
        apacheState.setName("test-state");
        assertEquals("test-state", state.getName());
    }

    @Test
    public void testGetNameNull() {
        apacheState.setName(null);
        assertNull(state.getName());
    }

    @Test
    public void testGetComment() {
        apacheState.setComment("test comment");
        assertEquals("test comment", state.getComment());
    }

    @Test
    public void testGetCommentNull() {
        apacheState.setComment(null);
        assertNull(state.getComment());
    }

    @Test
    public void testGetType() {
        StateType type = state.getType();
        assertNotNull(type);
        // ServiceTaskStateImpl 默认类型是 SERVICE_TASK
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK, type.unwrap());
    }

    @Test
    public void testGetNext() {
        apacheState.setNext("next-state");
        assertEquals("next-state", state.getNext());
    }

    @Test
    public void testGetNextNull() {
        apacheState.setNext(null);
        assertNull(state.getNext());
    }

    @Test
    public void testGetExtensions() {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("key1", "value1");
        extensions.put("key2", 123);
        extensions.put("key3", true);
        apacheState.setExtensions(extensions);

        Map<String, Object> result = state.getExtensions();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
        assertEquals(true, result.get("key3"));
    }

    @Test
    public void testGetExtensionsEmpty() {
        Map<String, Object> extensions = new HashMap<>();
        apacheState.setExtensions(extensions);

        Map<String, Object> result = state.getExtensions();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetExtensionsNull() {
        apacheState.setExtensions(null);
        assertNull(state.getExtensions());
    }

    @Test
    public void testGetStateMachine() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apacheStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        apacheStateMachine.setName("test-machine");
        apacheState.setStateMachine(apacheStateMachine);

        StateMachine stateMachine = state.getStateMachine();
        assertNotNull(stateMachine);
        assertEquals("test-machine", stateMachine.getName());
        assertSame(apacheStateMachine, ((StateMachineImpl) stateMachine).unwrap());
    }

    @Test
    public void testGetStateMachineNull() {
        apacheState.setStateMachine(null);
        assertNull(state.getStateMachine());
    }

    @Test
    public void testGetStateMachineWithComplexSetup() {
        // 创建一个完整的状态机
        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apacheStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        apacheStateMachine.setName("complex-machine");
        apacheStateMachine.setVersion("1.0.0");
        apacheStateMachine.setStartState("StartState");

        // 设置到状态上
        apacheState.setStateMachine(apacheStateMachine);

        // 验证包装后的状态机
        StateMachine stateMachine = state.getStateMachine();
        assertNotNull(stateMachine);
        assertEquals("complex-machine", stateMachine.getName());
        assertEquals("1.0.0", stateMachine.getVersion());
        assertEquals("StartState", stateMachine.getStartState());
    }

    @Test
    public void testMultipleGettersWithSameState() {
        // 设置多个属性
        apacheState.setName("multi-test-state");
        apacheState.setComment("multi-test comment");
        apacheState.setNext("next-multi-state");

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("priority", 1);
        apacheState.setExtensions(extensions);

        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apacheStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        apacheStateMachine.setName("multi-test-machine");
        apacheState.setStateMachine(apacheStateMachine);

        // 验证所有属性
        assertEquals("multi-test-state", state.getName());
        assertEquals("multi-test comment", state.getComment());
        assertEquals("next-multi-state", state.getNext());
        assertNotNull(state.getType());
        assertEquals(
                org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK,
                state.getType().unwrap());
        assertNotNull(state.getExtensions());
        assertEquals(1, state.getExtensions().get("priority"));
        assertNotNull(state.getStateMachine());
        assertEquals("multi-test-machine", state.getStateMachine().getName());
    }
}
