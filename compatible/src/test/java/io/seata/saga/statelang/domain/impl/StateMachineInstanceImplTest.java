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

import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for StateMachineInstanceImpl.
 */
public class StateMachineInstanceImplTest {

    private StateMachineInstanceImpl stateMachineInstance;
    private org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance;

    @BeforeEach
    public void setUp() {
        apacheInstance = new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        stateMachineInstance = StateMachineInstanceImpl.wrap(apacheInstance);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateMachineInstanceImpl.class.isAnnotationPresent(Deprecated.class),
                "StateMachineInstanceImpl should be marked as @Deprecated");
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apache =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        StateMachineInstanceImpl wrapped = StateMachineInstanceImpl.wrap(apache);
        assertNotNull(wrapped);
        assertSame(apache, wrapped.unwrap());
    }

    @Test
    public void testUnwrap() {
        assertSame(apacheInstance, stateMachineInstance.unwrap());
    }

    @Test
    public void testGetSetId() {
        String id = "instance-123";
        stateMachineInstance.setId(id);
        assertEquals(id, stateMachineInstance.getId());
        assertEquals(id, apacheInstance.getId());
    }

    @Test
    public void testGetSetMachineId() {
        String machineId = "machine-456";
        stateMachineInstance.setMachineId(machineId);
        assertEquals(machineId, stateMachineInstance.getMachineId());
        assertEquals(machineId, apacheInstance.getMachineId());
    }

    @Test
    public void testGetSetTenantId() {
        String tenantId = "tenant-789";
        stateMachineInstance.setTenantId(tenantId);
        assertEquals(tenantId, stateMachineInstance.getTenantId());
        assertEquals(tenantId, apacheInstance.getTenantId());
    }

    @Test
    public void testGetSetParentId() {
        String parentId = "parent-123";
        stateMachineInstance.setParentId(parentId);
        assertEquals(parentId, stateMachineInstance.getParentId());
        assertEquals(parentId, apacheInstance.getParentId());
    }

    @Test
    public void testGetSetGmtStarted() {
        Date gmtStarted = new Date();
        stateMachineInstance.setGmtStarted(gmtStarted);
        assertEquals(gmtStarted, stateMachineInstance.getGmtStarted());
        assertEquals(gmtStarted, apacheInstance.getGmtStarted());
    }

    @Test
    public void testGetSetGmtEnd() {
        Date gmtEnd = new Date();
        stateMachineInstance.setGmtEnd(gmtEnd);
        assertEquals(gmtEnd, stateMachineInstance.getGmtEnd());
        assertEquals(gmtEnd, apacheInstance.getGmtEnd());
    }

    @Test
    public void testPutStateInstance() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheStateInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheStateInstance.setId("state-1");
        StateInstanceImpl stateInstance = StateInstanceImpl.wrap(apacheStateInstance);

        stateMachineInstance.putStateInstance("state-1", stateInstance);

        // Verify through the apache instance
        List<org.apache.seata.saga.statelang.domain.StateInstance> stateList = apacheInstance.getStateList();
        assertNotNull(stateList);
    }

    @Test
    public void testGetSetStatus() {
        apacheInstance.setStatus(org.apache.seata.saga.statelang.domain.ExecutionStatus.RU);
        ExecutionStatus status = stateMachineInstance.getStatus();
        assertNotNull(status);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.RU, status.unwrap());

        ExecutionStatus newStatus = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU);
        stateMachineInstance.setStatus(newStatus);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU, apacheInstance.getStatus());
    }

    @Test
    public void testSetStatusNull() {
        stateMachineInstance.setStatus(null);
        assertNull(apacheInstance.getStatus());
    }

    @Test
    public void testGetSetCompensationStatus() {
        apacheInstance.setCompensationStatus(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU);
        ExecutionStatus compensationStatus = stateMachineInstance.getCompensationStatus();
        assertNotNull(compensationStatus);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU, compensationStatus.unwrap());

        ExecutionStatus newStatus = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA);
        stateMachineInstance.setCompensationStatus(newStatus);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA, apacheInstance.getCompensationStatus());
    }

    @Test
    public void testSetCompensationStatusNull() {
        stateMachineInstance.setCompensationStatus(null);
        assertNull(apacheInstance.getCompensationStatus());
    }

    @Test
    public void testIsSetRunning() {
        stateMachineInstance.setRunning(true);
        assertTrue(stateMachineInstance.isRunning());
        assertTrue(apacheInstance.isRunning());

        stateMachineInstance.setRunning(false);
        assertFalse(stateMachineInstance.isRunning());
        assertFalse(apacheInstance.isRunning());
    }

    @Test
    public void testGetSetGmtUpdated() {
        Date gmtUpdated = new Date();
        stateMachineInstance.setGmtUpdated(gmtUpdated);
        assertEquals(gmtUpdated, stateMachineInstance.getGmtUpdated());
        assertEquals(gmtUpdated, apacheInstance.getGmtUpdated());
    }

    @Test
    public void testGetSetBusinessKey() {
        String businessKey = "business-key-123";
        stateMachineInstance.setBusinessKey(businessKey);
        assertEquals(businessKey, stateMachineInstance.getBusinessKey());
        assertEquals(businessKey, apacheInstance.getBusinessKey());
    }

    @Test
    public void testGetSetException() {
        Exception exception = new RuntimeException("test exception");
        stateMachineInstance.setException(exception);
        assertEquals(exception, stateMachineInstance.getException());
        assertEquals(exception, apacheInstance.getException());
    }

    @Test
    public void testGetSetStartParams() {
        Map<String, Object> startParams = new HashMap<>();
        startParams.put("param1", "value1");
        startParams.put("param2", 123);

        stateMachineInstance.setStartParams(startParams);
        assertEquals(startParams, stateMachineInstance.getStartParams());
        assertEquals(startParams, apacheInstance.getStartParams());
    }

    @Test
    public void testGetSetEndParams() {
        Map<String, Object> endParams = new HashMap<>();
        endParams.put("result", "success");
        endParams.put("code", 200);

        stateMachineInstance.setEndParams(endParams);
        assertEquals(endParams, stateMachineInstance.getEndParams());
        assertEquals(endParams, apacheInstance.getEndParams());
    }

    @Test
    public void testGetSetContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");

        stateMachineInstance.setContext(context);
        assertEquals(context, stateMachineInstance.getContext());
        assertEquals(context, apacheInstance.getContext());
    }

    @Test
    public void testGetSetStateMachine() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl apacheStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        apacheStateMachine.setName("test-state-machine");
        apacheInstance.setStateMachine(apacheStateMachine);

        StateMachine stateMachine = stateMachineInstance.getStateMachine();
        assertNotNull(stateMachine);
        assertEquals("test-state-machine", stateMachine.getName());

        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl newApacheStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        newApacheStateMachine.setName("new-machine");
        StateMachineImpl newStateMachine = StateMachineImpl.wrap(newApacheStateMachine);
        stateMachineInstance.setStateMachine(newStateMachine);
        assertEquals("new-machine", apacheInstance.getStateMachine().getName());
    }

    @Test
    public void testGetSetStateList() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState1.setId("state-1");
        apacheState1.setName("State1");

        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState2 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState2.setId("state-2");
        apacheState2.setName("State2");

        List<org.apache.seata.saga.statelang.domain.StateInstance> apacheStateList = new ArrayList<>();
        apacheStateList.add(apacheState1);
        apacheStateList.add(apacheState2);

        apacheInstance.setStateList(apacheStateList);

        List<StateInstance> stateList = stateMachineInstance.getStateList();
        assertNotNull(stateList);
        assertEquals(2, stateList.size());
        assertEquals("state-1", stateList.get(0).getId());
        assertEquals("state-2", stateList.get(1).getId());

        // Test setting state list
        List<StateInstance> newStateList = new ArrayList<>();
        StateInstanceImpl newState =
                StateInstanceImpl.wrap(new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl());
        newState.unwrap().setId("state-3");
        newStateList.add(newState);

        stateMachineInstance.setStateList(newStateList);
        assertEquals(1, apacheInstance.getStateList().size());
        assertEquals("state-3", apacheInstance.getStateList().get(0).getId());
    }

    @Test
    public void testGetSetStateMap() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState1.setId("state-1");
        apacheState1.setName("State1");

        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState2 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState2.setId("state-2");
        apacheState2.setName("State2");

        List<org.apache.seata.saga.statelang.domain.StateInstance> apacheStateList = new ArrayList<>();
        apacheStateList.add(apacheState1);
        apacheStateList.add(apacheState2);

        apacheInstance.setStateList(apacheStateList);

        Map<String, StateInstance> stateMap = stateMachineInstance.getStateMap();
        assertNotNull(stateMap);
        assertEquals(2, stateMap.size());
        assertTrue(stateMap.containsKey("state-1"));
        assertTrue(stateMap.containsKey("state-2"));

        // Test setting state map
        Map<String, StateInstance> newStateMap = new HashMap<>();
        StateInstanceImpl newState =
                StateInstanceImpl.wrap(new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl());
        newState.unwrap().setId("state-3");
        newStateMap.put("state-3", newState);

        stateMachineInstance.setStateMap(newStateMap);
        Map<String, org.apache.seata.saga.statelang.domain.StateInstance> apacheStateMap = apacheInstance.getStateMap();
        assertNotNull(apacheStateMap);
        assertTrue(apacheStateMap.containsKey("state-3"));
    }

    @Test
    public void testGetSetSerializedStartParams() {
        Object serializedStartParams = "{\"param\":\"value\"}";
        stateMachineInstance.setSerializedStartParams(serializedStartParams);
        assertEquals(serializedStartParams, stateMachineInstance.getSerializedStartParams());
        assertEquals(serializedStartParams, apacheInstance.getSerializedStartParams());
    }

    @Test
    public void testGetSetSerializedEndParams() {
        Object serializedEndParams = "{\"result\":\"success\"}";
        stateMachineInstance.setSerializedEndParams(serializedEndParams);
        assertEquals(serializedEndParams, stateMachineInstance.getSerializedEndParams());
        assertEquals(serializedEndParams, apacheInstance.getSerializedEndParams());
    }

    @Test
    public void testGetSetSerializedException() {
        Object serializedException = "serialized exception data";
        stateMachineInstance.setSerializedException(serializedException);
        assertEquals(serializedException, stateMachineInstance.getSerializedException());
        assertEquals(serializedException, apacheInstance.getSerializedException());
    }

    @Test
    public void testStateListStateMachineInstanceReference() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState1.setId("state-1");

        List<org.apache.seata.saga.statelang.domain.StateInstance> apacheStateList = new ArrayList<>();
        apacheStateList.add(apacheState1);
        apacheInstance.setStateList(apacheStateList);

        List<StateInstance> stateList = stateMachineInstance.getStateList();
        assertNotNull(stateList);
        assertEquals(1, stateList.size());
        // Verify that the state instance has reference to the state machine instance
        assertSame(stateMachineInstance, stateList.get(0).getStateMachineInstance());
    }

    @Test
    public void testStateMapStateMachineInstanceReference() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState1.setId("state-1");

        List<org.apache.seata.saga.statelang.domain.StateInstance> apacheStateList = new ArrayList<>();
        apacheStateList.add(apacheState1);
        apacheInstance.setStateList(apacheStateList);

        Map<String, StateInstance> stateMap = stateMachineInstance.getStateMap();
        assertNotNull(stateMap);
        assertEquals(1, stateMap.size());
        // Verify that the state instance has reference to the state machine instance
        assertSame(stateMachineInstance, stateMap.get("state-1").getStateMachineInstance());
    }
}
