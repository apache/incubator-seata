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
package io.seata.saga.engine.store.impl;

import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test cases for StateLogStoreImpl.
 */
@ExtendWith(MockitoExtension.class)
public class StateLogStoreImplTest {

    @Mock
    private org.apache.seata.saga.engine.store.StateLogStore mockApacheStateLogStore;

    private StateLogStore stateLogStore;

    @BeforeEach
    public void setUp() {
        stateLogStore = StateLogStoreImpl.wrap(mockApacheStateLogStore);
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.engine.store.StateLogStore apacheStore =
                mock(org.apache.seata.saga.engine.store.StateLogStore.class);
        StateLogStore wrapped = StateLogStoreImpl.wrap(apacheStore);
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof StateLogStoreImpl);
    }

    @Test
    public void testUnwrap() {
        StateLogStoreImpl impl = (StateLogStoreImpl) stateLogStore;
        assertSame(mockApacheStateLogStore, impl.unwrap());
    }

    @Test
    public void testRecordStateMachineStarted() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        StateMachineInstance machineInstance = StateMachineInstanceImpl.wrap(apacheInstance);

        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.recordStateMachineStarted(machineInstance, context);

        verify(mockApacheStateLogStore, times(1)).recordStateMachineStarted(eq(apacheInstance), eq(apacheContext));
    }

    @Test
    public void testRecordStateMachineFinished() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        StateMachineInstance machineInstance = StateMachineInstanceImpl.wrap(apacheInstance);

        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.recordStateMachineFinished(machineInstance, context);

        verify(mockApacheStateLogStore, times(1)).recordStateMachineFinished(eq(apacheInstance), eq(apacheContext));
    }

    @Test
    public void testRecordStateMachineRestarted() {
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        StateMachineInstance machineInstance = StateMachineInstanceImpl.wrap(apacheInstance);

        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.recordStateMachineRestarted(machineInstance, context);

        verify(mockApacheStateLogStore, times(1)).recordStateMachineRestarted(eq(apacheInstance), eq(apacheContext));
    }

    @Test
    public void testRecordStateStarted() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheStateInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        StateInstance stateInstance = StateInstanceImpl.wrap(apacheStateInstance);

        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.recordStateStarted(stateInstance, context);

        verify(mockApacheStateLogStore, times(1)).recordStateStarted(eq(apacheStateInstance), eq(apacheContext));
    }

    @Test
    public void testRecordStateFinished() {
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheStateInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        StateInstance stateInstance = StateInstanceImpl.wrap(apacheStateInstance);

        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.recordStateFinished(stateInstance, context);

        verify(mockApacheStateLogStore, times(1)).recordStateFinished(eq(apacheStateInstance), eq(apacheContext));
    }

    @Test
    public void testGetStateMachineInstance() {
        String stateMachineInstanceId = "instance-123";
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        apacheInstance.setId(stateMachineInstanceId);

        when(mockApacheStateLogStore.getStateMachineInstance(stateMachineInstanceId))
                .thenReturn(apacheInstance);

        StateMachineInstance result = stateLogStore.getStateMachineInstance(stateMachineInstanceId);

        assertNotNull(result);
        assertEquals(stateMachineInstanceId, result.getId());
        verify(mockApacheStateLogStore, times(1)).getStateMachineInstance(stateMachineInstanceId);
    }

    @Test
    public void testGetStateMachineInstanceByBusinessKey() {
        String businessKey = "biz-key-123";
        String tenantId = "tenant-1";
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        apacheInstance.setBusinessKey(businessKey);
        apacheInstance.setTenantId(tenantId);

        when(mockApacheStateLogStore.getStateMachineInstanceByBusinessKey(businessKey, tenantId))
                .thenReturn(apacheInstance);

        StateMachineInstance result = stateLogStore.getStateMachineInstanceByBusinessKey(businessKey, tenantId);

        assertNotNull(result);
        assertEquals(businessKey, result.getBusinessKey());
        assertEquals(tenantId, result.getTenantId());
        verify(mockApacheStateLogStore, times(1)).getStateMachineInstanceByBusinessKey(businessKey, tenantId);
    }

    @Test
    public void testQueryStateMachineInstanceByParentId() {
        String parentId = "parent-123";
        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance1 =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        apacheInstance1.setId("child-1");
        apacheInstance1.setParentId(parentId);

        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl apacheInstance2 =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        apacheInstance2.setId("child-2");
        apacheInstance2.setParentId(parentId);

        List<org.apache.seata.saga.statelang.domain.StateMachineInstance> apacheList = new ArrayList<>();
        apacheList.add(apacheInstance1);
        apacheList.add(apacheInstance2);

        when(mockApacheStateLogStore.queryStateMachineInstanceByParentId(parentId))
                .thenReturn(apacheList);

        List<StateMachineInstance> result = stateLogStore.queryStateMachineInstanceByParentId(parentId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("child-1", result.get(0).getId());
        assertEquals("child-2", result.get(1).getId());
        verify(mockApacheStateLogStore, times(1)).queryStateMachineInstanceByParentId(parentId);
    }

    @Test
    public void testQueryStateMachineInstanceByParentIdEmptyList() {
        String parentId = "parent-123";
        when(mockApacheStateLogStore.queryStateMachineInstanceByParentId(parentId))
                .thenReturn(new ArrayList<>());

        List<StateMachineInstance> result = stateLogStore.queryStateMachineInstanceByParentId(parentId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockApacheStateLogStore, times(1)).queryStateMachineInstanceByParentId(parentId);
    }

    @Test
    public void testQueryStateMachineInstanceByParentIdNull() {
        String parentId = "parent-123";
        when(mockApacheStateLogStore.queryStateMachineInstanceByParentId(parentId))
                .thenReturn(null);

        List<StateMachineInstance> result = stateLogStore.queryStateMachineInstanceByParentId(parentId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockApacheStateLogStore, times(1)).queryStateMachineInstanceByParentId(parentId);
    }

    @Test
    public void testGetStateInstance() {
        String stateInstanceId = "state-123";
        String machineInstId = "machine-456";
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheStateInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheStateInstance.setId(stateInstanceId);
        apacheStateInstance.setMachineInstanceId(machineInstId);

        when(mockApacheStateLogStore.getStateInstance(stateInstanceId, machineInstId))
                .thenReturn(apacheStateInstance);

        StateInstance result = stateLogStore.getStateInstance(stateInstanceId, machineInstId);

        assertNotNull(result);
        assertEquals(stateInstanceId, result.getId());
        assertEquals(machineInstId, result.getMachineInstanceId());
        verify(mockApacheStateLogStore, times(1)).getStateInstance(stateInstanceId, machineInstId);
    }

    @Test
    public void testGetStateInstanceNull() {
        String stateInstanceId = "state-123";
        String machineInstId = "machine-456";
        when(mockApacheStateLogStore.getStateInstance(stateInstanceId, machineInstId))
                .thenReturn(null);

        StateInstance result = stateLogStore.getStateInstance(stateInstanceId, machineInstId);

        assertNull(result);
        verify(mockApacheStateLogStore, times(1)).getStateInstance(stateInstanceId, machineInstId);
    }

    @Test
    public void testQueryStateInstanceListByMachineInstanceId() {
        String stateMachineInstanceId = "machine-123";
        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState1 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState1.setId("state-1");
        apacheState1.setMachineInstanceId(stateMachineInstanceId);

        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl apacheState2 =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        apacheState2.setId("state-2");
        apacheState2.setMachineInstanceId(stateMachineInstanceId);

        List<org.apache.seata.saga.statelang.domain.StateInstance> apacheList = new ArrayList<>();
        apacheList.add(apacheState1);
        apacheList.add(apacheState2);

        when(mockApacheStateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId))
                .thenReturn(apacheList);

        List<StateInstance> result = stateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("state-1", result.get(0).getId());
        assertEquals("state-2", result.get(1).getId());
        verify(mockApacheStateLogStore, times(1)).queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
    }

    @Test
    public void testQueryStateInstanceListByMachineInstanceIdEmptyList() {
        String stateMachineInstanceId = "machine-123";
        when(mockApacheStateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId))
                .thenReturn(new ArrayList<>());

        List<StateInstance> result = stateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockApacheStateLogStore, times(1)).queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
    }

    @Test
    public void testQueryStateInstanceListByMachineInstanceIdNull() {
        String stateMachineInstanceId = "machine-123";
        when(mockApacheStateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId))
                .thenReturn(null);

        List<StateInstance> result = stateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockApacheStateLogStore, times(1)).queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
    }

    @Test
    public void testClearUp() {
        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl context = ProcessContextImpl.wrap(apacheContext);

        stateLogStore.clearUp(context);

        verify(mockApacheStateLogStore, times(1)).clearUp(eq(apacheContext));
    }
}
