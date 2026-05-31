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
package org.apache.seata.saga.engine.pcext.handlers;

import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.StateMachineEngine;
import org.apache.seata.saga.engine.pcext.StateHandlerInterceptor;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.SubStateMachineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link SubStateMachineHandler}
 */
public class SubStateMachineHandlerTest {

    private SubStateMachineHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new SubStateMachineHandler();
    }

    @Test
    public void addInterceptorAddToListTest() {
        StateHandlerInterceptor interceptor = mock(StateHandlerInterceptor.class);
        handler.addInterceptor(interceptor);
        assertTrue(handler.getInterceptors().contains(interceptor));
    }

    @Test
    public void addInterceptorDuplicateInterceptorNotAddTest() {
        StateHandlerInterceptor interceptor = mock(StateHandlerInterceptor.class);
        handler.addInterceptor(interceptor);
        handler.addInterceptor(interceptor);
        assertEquals(1, handler.getInterceptors().size());
    }

    @Test
    public void addInterceptorMultipleDifferentInterceptorsTest() {
        StateHandlerInterceptor interceptor1 = mock(StateHandlerInterceptor.class);
        StateHandlerInterceptor interceptor2 = mock(StateHandlerInterceptor.class);
        handler.addInterceptor(interceptor1);
        handler.addInterceptor(interceptor2);
        assertEquals(2, handler.getInterceptors().size());
        assertTrue(handler.getInterceptors().contains(interceptor1));
        assertTrue(handler.getInterceptors().contains(interceptor2));
    }

    @Test
    public void getInterceptorsReturnListTest() {
        List<StateHandlerInterceptor> interceptors = handler.getInterceptors();
        assertNotNull(interceptors);
    }

    @Test
    public void setInterceptorsTest() {
        List<StateHandlerInterceptor> interceptors = new ArrayList<>();
        StateHandlerInterceptor interceptor = mock(StateHandlerInterceptor.class);
        interceptors.add(interceptor);

        handler.setInterceptors(interceptors);

        assertSame(interceptors, handler.getInterceptors());
    }

    @Test
    public void setInterceptorsWithNullTest() {
        handler.setInterceptors(null);
        assertNull(handler.getInterceptors());
    }

    @Test
    public void addInterceptorWhenInterceptorsIsNullDoNothingTest() {
        handler.setInterceptors(null);
        StateHandlerInterceptor interceptor = mock(StateHandlerInterceptor.class);

        // Should not throw
        assertDoesNotThrow(() -> handler.addInterceptor(interceptor));
    }

    @Test
    public void handlerNotNullTest() {
        assertNotNull(handler);
    }

    // ========== Existing Static Method Tests ==========

    @Test
    public void decideStatusForwardWithSuccessReturnSUTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.SU);

        ExecutionStatus result = invokeDecideStatus(instance, true);

        assertEquals(ExecutionStatus.SU, result);
    }

    @Test
    public void decideStatusForwardWithFailureReturnInstanceStatusTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.FA);
        when(instance.getCompensationStatus()).thenReturn(null);

        ExecutionStatus result = invokeDecideStatus(instance, true);

        assertEquals(ExecutionStatus.FA, result);
    }

    @Test
    public void decideStatusWhenCompensationStatusIsNullReturnInstanceStatusTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.FA);
        when(instance.getCompensationStatus()).thenReturn(null);

        ExecutionStatus result = invokeDecideStatus(instance, false);

        assertEquals(ExecutionStatus.FA, result);
    }

    @Test
    public void decideStatusWhenCompensationStatusIsFAReturnInstanceStatusTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getStatus()).thenReturn(ExecutionStatus.FA);
        when(instance.getCompensationStatus()).thenReturn(ExecutionStatus.FA);

        ExecutionStatus result = invokeDecideStatus(instance, false);

        assertEquals(ExecutionStatus.FA, result);
    }

    @Test
    public void decideStatusWhenCompensationStatusIsSUReturnFATest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getCompensationStatus()).thenReturn(ExecutionStatus.SU);

        ExecutionStatus result = invokeDecideStatus(instance, false);

        assertEquals(ExecutionStatus.FA, result);
    }

    @Test
    public void decideStatusWhenCompensationStatusIsUNReturnUNTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getCompensationStatus()).thenReturn(ExecutionStatus.UN);

        ExecutionStatus result = invokeDecideStatus(instance, false);

        assertEquals(ExecutionStatus.UN, result);
    }

    @Test
    public void decideStatusWhenCompensationStatusIsRUReturnUNTest() throws Exception {
        StateMachineInstance instance = mock(StateMachineInstance.class);
        when(instance.getCompensationStatus()).thenReturn(ExecutionStatus.RU);

        ExecutionStatus result = invokeDecideStatus(instance, false);

        assertEquals(ExecutionStatus.UN, result);
    }

    /**
     * Invoke private static method decideStatus via reflection
     */
    private ExecutionStatus invokeDecideStatus(StateMachineInstance instance, boolean isForward) throws Exception {
        Method method = SubStateMachineHandler.class.getDeclaredMethod(
                "decideStatus", StateMachineInstance.class, boolean.class);
        method.setAccessible(true);
        return (ExecutionStatus) method.invoke(null, instance, isForward);
    }

    // ========== New Process Tests ==========

    @Test
    public void processStartNewSubMachineTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        SubStateMachineImpl subStateMachine = mock(SubStateMachineImpl.class);
        StateMachineEngine engine = mock(StateMachineEngine.class);
        StateMachineInstance parentSmInstance = mock(StateMachineInstance.class);
        StateInstance stateInstance = mock(StateInstance.class);
        StateMachineInstance subSmInstance = mock(StateMachineInstance.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(subStateMachine);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_ENGINE)).thenReturn(engine);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(parentSmInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);

        // Input params
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("a", 1);
        when(context.getVariable(DomainConstants.VAR_NAME_INPUT_PARAMS)).thenReturn(inputParams);

        // Mock SubStateMachine
        when(subStateMachine.getStateMachineName()).thenReturn("subMachine");
        when(subStateMachine.getName()).thenReturn("subState");

        // Mock Engine Start
        when(engine.start(eq("subMachine"), any(), any())).thenReturn(subSmInstance);

        // Mock SubStateMachineInstance Result
        Map<String, Object> endParams = new HashMap<>();
        endParams.put("result", "success");
        when(subSmInstance.getEndParams()).thenReturn(endParams);
        when(subSmInstance.getStatus()).thenReturn(ExecutionStatus.SU);
        when(subSmInstance.getCompensationStatus()).thenReturn(null);

        // Mock StateInstance
        when(stateInstance.getStateMachineInstance()).thenReturn(parentSmInstance);

        handler.process(context);

        verify(engine).start(eq("subMachine"), any(), any());
        verify(stateInstance).setOutputParams(endParams);
        verify(context).setVariable(eq(DomainConstants.VAR_NAME_OUTPUT_PARAMS), eq(endParams));
        verify(stateInstance).setStatus(ExecutionStatus.SU);
    }

    @Test
    public void processForwardExistingSubMachineTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        SubStateMachineImpl subStateMachine = mock(SubStateMachineImpl.class);
        StateMachineEngine engine = mock(StateMachineEngine.class);
        StateMachineInstance parentSmInstance = mock(StateMachineInstance.class);
        StateInstance stateInstance = mock(StateInstance.class);
        StateMachineInstance subSmInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        StateLogStore stateLogStore = mock(StateLogStore.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(subStateMachine);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_ENGINE)).thenReturn(engine);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(parentSmInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);

        // Use Mockito to mock boolean check for forward flag
        when(context.getVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD))
                .thenReturn(true);

        when(config.getStateLogStore()).thenReturn(stateLogStore);

        // Correctly set up the retry scenario so validation passes
        when(stateInstance.getStateIdRetriedFor()).thenReturn("originalStateId");
        when(stateInstance.getMachineInstanceId()).thenReturn("machineId1");

        StateInstance originalStateInst = mock(StateInstance.class);
        when(originalStateInst.getId()).thenReturn("originalStateId");
        when(originalStateInst.getMachineInstanceId()).thenReturn("machineId1");
        // Must return null to break the do-while loop
        when(originalStateInst.getStateIdRetriedFor()).thenReturn(null);

        when(stateLogStore.getStateInstance(eq("originalStateId"), any())).thenReturn(originalStateInst);

        List<StateMachineInstance> existingSubInsts = Collections.singletonList(subSmInstance);
        when(stateLogStore.queryStateMachineInstanceByParentId(any())).thenReturn(existingSubInsts);

        when(subSmInstance.getId()).thenReturn("subInstId");
        when(subSmInstance.getEndParams()).thenReturn(new HashMap<>());
        when(subSmInstance.getStatus()).thenReturn(ExecutionStatus.SU);

        // Mock Engine Forward
        when(engine.forward(eq("subInstId"), any())).thenReturn(subSmInstance);

        handler.process(context);

        verify(engine).forward(eq("subInstId"), any());
        verify(context).removeVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD);
    }
}
