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

import io.seata.saga.statelang.domain.RecoverStrategy;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for StateMachineImpl.
 */
public class StateMachineImplTest {

    private StateMachineImpl stateMachine;
    private org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apacheStateMachine;

    @BeforeEach
    public void setUp() {
        apacheStateMachine = new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        stateMachine = StateMachineImpl.wrap(apacheStateMachine);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateMachineImpl.class.isAnnotationPresent(Deprecated.class),
                "StateMachineImpl should be marked as @Deprecated");
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apache =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        StateMachineImpl wrapped = StateMachineImpl.wrap(apache);
        assertNotNull(wrapped);
        assertSame(apache, wrapped.unwrap());
    }

    @Test
    public void testWrapNull() {
        assertNull(StateMachineImpl.wrap(null));
    }

    @Test
    public void testUnwrap() {
        assertSame(apacheStateMachine, stateMachine.unwrap());
    }

    @Test
    public void testGetName() {
        apacheStateMachine.setName("test-state-machine");
        assertEquals("test-state-machine", stateMachine.getName());
    }

    @Test
    public void testGetComment() {
        apacheStateMachine.setComment("test comment");
        assertEquals("test comment", stateMachine.getComment());
    }

    @Test
    public void testGetSetStartState() {
        stateMachine.setStartState("StartState");
        assertEquals("StartState", stateMachine.getStartState());
        assertEquals("StartState", apacheStateMachine.getStartState());
    }

    @Test
    public void testGetSetVersion() {
        stateMachine.setVersion("1.0.0");
        assertEquals("1.0.0", stateMachine.getVersion());
        assertEquals("1.0.0", apacheStateMachine.getVersion());
    }

    @Test
    public void testGetStates() {
        Map<String, org.apache.seata.saga.statelang.domain.State> apacheStates = new LinkedHashMap<>();
        org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl();
        apacheState1.setName("State1");
        org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl apacheState2 =
                new org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl();
        apacheState2.setName("State2");
        apacheStates.put("State1", apacheState1);
        apacheStates.put("State2", apacheState2);

        apacheStateMachine.setStates(apacheStates);

        Map<String, State> states = stateMachine.getStates();
        assertNotNull(states);
        assertEquals(2, states.size());
        assertTrue(states.containsKey("State1"));
        assertTrue(states.containsKey("State2"));
    }

    @Test
    public void testGetStatesNull() {
        apacheStateMachine.setStates(null);
        assertNull(stateMachine.getStates());
    }

    @Test
    public void testGetState() {
        org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl apacheState =
                new org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl();
        apacheState.setName("TestState");

        Map<String, org.apache.seata.saga.statelang.domain.State> apacheStates = new LinkedHashMap<>();
        apacheStates.put("TestState", apacheState);
        apacheStateMachine.setStates(apacheStates);

        State state = stateMachine.getState("TestState");
        assertNotNull(state);
        assertEquals("TestState", state.getName());
    }

    @Test
    public void testGetSetId() {
        stateMachine.setId("sm-123");
        assertEquals("sm-123", stateMachine.getId());
        assertEquals("sm-123", apacheStateMachine.getId());
    }

    @Test
    public void testGetSetTenantId() {
        stateMachine.setTenantId("tenant-1");
        assertEquals("tenant-1", stateMachine.getTenantId());
        assertEquals("tenant-1", apacheStateMachine.getTenantId());
    }

    @Test
    public void testGetAppName() {
        apacheStateMachine.setAppName("test-app");
        assertEquals("test-app", stateMachine.getAppName());
    }

    @Test
    public void testGetType() {
        apacheStateMachine.setType("STATE_LANG");
        assertEquals("STATE_LANG", stateMachine.getType());
    }

    @Test
    public void testGetStatus() {
        apacheStateMachine.setStatus(org.apache.seata.saga.statelang.domain.StateMachine.Status.AC);
        StateMachine.Status status = stateMachine.getStatus();
        assertNotNull(status);
        assertEquals(org.apache.seata.saga.statelang.domain.StateMachine.Status.AC, status.unwrap());
    }

    @Test
    public void testGetSetRecoverStrategy() {
        RecoverStrategy strategy = RecoverStrategy.wrap(org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward);
        stateMachine.setRecoverStrategy(strategy);
        assertEquals(
                org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward,
                stateMachine.getRecoverStrategy().unwrap());
        assertEquals(
                org.apache.seata.saga.statelang.domain.RecoverStrategy.Forward,
                apacheStateMachine.getRecoverStrategy());
    }

    @Test
    public void testIsPersist() {
        apacheStateMachine.setPersist(true);
        assertTrue(stateMachine.isPersist());

        apacheStateMachine.setPersist(false);
        assertFalse(stateMachine.isPersist());
    }

    @Test
    public void testIsRetryPersistModeUpdate() {
        apacheStateMachine.setRetryPersistModeUpdate(true);
        assertTrue(stateMachine.isRetryPersistModeUpdate());

        apacheStateMachine.setRetryPersistModeUpdate(false);
        assertFalse(stateMachine.isRetryPersistModeUpdate());
    }

    @Test
    public void testIsCompensatePersistModeUpdate() {
        apacheStateMachine.setCompensatePersistModeUpdate(true);
        assertTrue(stateMachine.isCompensatePersistModeUpdate());

        apacheStateMachine.setCompensatePersistModeUpdate(false);
        assertFalse(stateMachine.isCompensatePersistModeUpdate());
    }

    @Test
    public void testGetSetContent() {
        String content = "{\"name\":\"test-machine\"}";
        stateMachine.setContent(content);
        assertEquals(content, stateMachine.getContent());
        assertEquals(content, apacheStateMachine.getContent());
    }

    @Test
    public void testGetSetGmtCreate() {
        Date gmtCreate = new Date();
        stateMachine.setGmtCreate(gmtCreate);
        assertEquals(gmtCreate, stateMachine.getGmtCreate());
        assertEquals(gmtCreate, apacheStateMachine.getGmtCreate());
    }
}
