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
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test cases for StateInstanceImpl.
 */
public class StateInstanceImplTest {

    private StateInstanceImpl stateInstance;
    private org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheStateInstance;

    @BeforeEach
    public void setUp() {
        apacheStateInstance = new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        stateInstance = StateInstanceImpl.wrap(apacheStateInstance);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateInstanceImpl.class.isAnnotationPresent(Deprecated.class),
                "StateInstanceImpl should be marked as @Deprecated");
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apache =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        StateInstanceImpl wrapped = StateInstanceImpl.wrap(apache);
        assertNotNull(wrapped);
        assertSame(apache, wrapped.unwrap());
    }

    @Test
    public void testUnwrap() {
        assertSame(apacheStateInstance, stateInstance.unwrap());
    }

    @Test
    public void testGetSetId() {
        String id = "test-id-123";
        stateInstance.setId(id);
        assertEquals(id, stateInstance.getId());
        assertEquals(id, apacheStateInstance.getId());
    }

    @Test
    public void testGetSetMachineInstanceId() {
        String machineInstanceId = "machine-instance-123";
        stateInstance.setMachineInstanceId(machineInstanceId);
        assertEquals(machineInstanceId, stateInstance.getMachineInstanceId());
        assertEquals(machineInstanceId, apacheStateInstance.getMachineInstanceId());
    }

    @Test
    public void testGetSetName() {
        String name = "test-state";
        stateInstance.setName(name);
        assertEquals(name, stateInstance.getName());
        assertEquals(name, apacheStateInstance.getName());
    }

    @Test
    public void testGetSetType() {
        apacheStateInstance.setType(org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK);
        StateType type = stateInstance.getType();
        assertNotNull(type);
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK, type.unwrap());

        StateType newType = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.CHOICE);
        stateInstance.setType(newType);
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.CHOICE, apacheStateInstance.getType());
    }

    @Test
    public void testSetTypeNull() {
        stateInstance.setType(null);
        assertNull(apacheStateInstance.getType());
    }

    @Test
    public void testGetSetServiceName() {
        String serviceName = "testService";
        stateInstance.setServiceName(serviceName);
        assertEquals(serviceName, stateInstance.getServiceName());
        assertEquals(serviceName, apacheStateInstance.getServiceName());
    }

    @Test
    public void testGetSetServiceMethod() {
        String serviceMethod = "testMethod";
        stateInstance.setServiceMethod(serviceMethod);
        assertEquals(serviceMethod, stateInstance.getServiceMethod());
        assertEquals(serviceMethod, apacheStateInstance.getServiceMethod());
    }

    @Test
    public void testGetSetServiceType() {
        String serviceType = "testType";
        stateInstance.setServiceType(serviceType);
        assertEquals(serviceType, stateInstance.getServiceType());
        assertEquals(serviceType, apacheStateInstance.getServiceType());
    }

    @Test
    public void testGetSetBusinessKey() {
        String businessKey = "business-key-123";
        stateInstance.setBusinessKey(businessKey);
        assertEquals(businessKey, stateInstance.getBusinessKey());
        assertEquals(businessKey, apacheStateInstance.getBusinessKey());
    }

    @Test
    public void testGetSetGmtStarted() {
        Date gmtStarted = new Date();
        stateInstance.setGmtStarted(gmtStarted);
        assertEquals(gmtStarted, stateInstance.getGmtStarted());
        assertEquals(gmtStarted, apacheStateInstance.getGmtStarted());
    }

    @Test
    public void testGetSetGmtUpdated() {
        Date gmtUpdated = new Date();
        stateInstance.setGmtUpdated(gmtUpdated);
        assertEquals(gmtUpdated, stateInstance.getGmtUpdated());
        assertEquals(gmtUpdated, apacheStateInstance.getGmtUpdated());
    }

    @Test
    public void testGetSetGmtEnd() {
        Date gmtEnd = new Date();
        stateInstance.setGmtEnd(gmtEnd);
        assertEquals(gmtEnd, stateInstance.getGmtEnd());
        assertEquals(gmtEnd, apacheStateInstance.getGmtEnd());
    }

    @Test
    public void testIsSetForUpdate() {
        stateInstance.setForUpdate(true);
        assertTrue(stateInstance.isForUpdate());
        assertTrue(apacheStateInstance.isForUpdate());

        stateInstance.setForUpdate(false);
        assertFalse(stateInstance.isForUpdate());
        assertFalse(apacheStateInstance.isForUpdate());
    }

    @Test
    public void testGetSetStateIdCompensatedFor() {
        String stateId = "compensated-state-123";
        stateInstance.setStateIdCompensatedFor(stateId);
        assertEquals(stateId, stateInstance.getStateIdCompensatedFor());
        assertEquals(stateId, apacheStateInstance.getStateIdCompensatedFor());
    }

    @Test
    public void testGetSetStateIdRetriedFor() {
        String stateId = "retried-state-123";
        stateInstance.setStateIdRetriedFor(stateId);
        assertEquals(stateId, stateInstance.getStateIdRetriedFor());
        assertEquals(stateId, apacheStateInstance.getStateIdRetriedFor());
    }

    @Test
    public void testGetSetException() {
        Exception exception = new RuntimeException("test exception");
        stateInstance.setException(exception);
        assertEquals(exception, stateInstance.getException());
        assertEquals(exception, apacheStateInstance.getException());
    }

    @Test
    public void testGetSetInputParams() {
        Object inputParams = new Object();
        stateInstance.setInputParams(inputParams);
        assertEquals(inputParams, stateInstance.getInputParams());
        assertEquals(inputParams, apacheStateInstance.getInputParams());
    }

    @Test
    public void testGetSetOutputParams() {
        Object outputParams = new Object();
        stateInstance.setOutputParams(outputParams);
        assertEquals(outputParams, stateInstance.getOutputParams());
        assertEquals(outputParams, apacheStateInstance.getOutputParams());
    }

    @Test
    public void testGetSetStatus() {
        apacheStateInstance.setStatus(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU);
        ExecutionStatus status = stateInstance.getStatus();
        assertNotNull(status);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU, status.unwrap());

        ExecutionStatus newStatus = ExecutionStatus.wrap(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA);
        stateInstance.setStatus(newStatus);
        assertEquals(org.apache.seata.saga.statelang.domain.ExecutionStatus.FA, apacheStateInstance.getStatus());
    }

    @Test
    public void testSetStatusNull() {
        stateInstance.setStatus(null);
        assertNull(apacheStateInstance.getStatus());
    }

    @Test
    public void testGetSetCompensationState() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheCompensationState =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheStateInstance.setCompensationState(apacheCompensationState);

        StateInstanceImpl compensationState = (StateInstanceImpl) stateInstance.getCompensationState();
        assertNotNull(compensationState);
        assertSame(apacheCompensationState, compensationState.unwrap());

        StateInstanceImpl newCompensationState =
                StateInstanceImpl.wrap(new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl());
        stateInstance.setCompensationState(newCompensationState);
        assertSame(newCompensationState.unwrap(), apacheStateInstance.getCompensationState());
    }

    @Test
    public void testGetSetStateMachineInstance() {
        StateMachineInstance mockInstance = mock(StateMachineInstance.class);
        stateInstance.setStateMachineInstance(mockInstance);
        assertSame(mockInstance, stateInstance.getStateMachineInstance());
    }

    @Test
    public void testIsSetIgnoreStatus() {
        stateInstance.setIgnoreStatus(true);
        assertTrue(stateInstance.isIgnoreStatus());
        assertTrue(apacheStateInstance.isIgnoreStatus());

        stateInstance.setIgnoreStatus(false);
        assertFalse(stateInstance.isIgnoreStatus());
        assertFalse(apacheStateInstance.isIgnoreStatus());
    }

    @Test
    public void testIsForCompensation() {
        assertFalse(stateInstance.isForCompensation());
    }

    @Test
    public void testGetSetSerializedInputParams() {
        Object serializedInputParams = "serialized-input";
        stateInstance.setSerializedInputParams(serializedInputParams);
        assertEquals(serializedInputParams, stateInstance.getSerializedInputParams());
        assertEquals(serializedInputParams, apacheStateInstance.getSerializedInputParams());
    }

    @Test
    public void testGetSetSerializedOutputParams() {
        Object serializedOutputParams = "serialized-output";
        stateInstance.setSerializedOutputParams(serializedOutputParams);
        assertEquals(serializedOutputParams, stateInstance.getSerializedOutputParams());
        assertEquals(serializedOutputParams, apacheStateInstance.getSerializedOutputParams());
    }

    @Test
    public void testGetSetSerializedException() {
        Object serializedException = "serialized-exception";
        stateInstance.setSerializedException(serializedException);
        assertEquals(serializedException, stateInstance.getSerializedException());
        assertEquals(serializedException, apacheStateInstance.getSerializedException());
    }

    @Test
    public void testGetCompensationStatus() {
        apacheStateInstance.setStatus(org.apache.seata.saga.statelang.domain.ExecutionStatus.SU);
        ExecutionStatus compensationStatus = stateInstance.getCompensationStatus();
        assertNull(compensationStatus);
    }
}
