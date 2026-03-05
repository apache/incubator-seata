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
package org.apache.seata.saga.engine.pcext.interceptors;

import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import org.apache.seata.saga.engine.pcext.utils.LoopContextHolder;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.TaskState.Loop;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LoopTaskHandlerInterceptor}
 */
public class LoopTaskHandlerInterceptorTest {

    private LoopTaskHandlerInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = new LoopTaskHandlerInterceptor();
    }

    @Test
    public void matchForServiceTaskHandlerReturnTrueTest() {
        assertTrue(interceptor.match(ServiceTaskStateHandler.class));
    }

    @Test
    public void matchForSubStateMachineHandlerReturnTrueTest() {
        assertTrue(interceptor.match(SubStateMachineHandler.class));
    }

    @Test
    public void matchForNullClassReturnFalseTest() {
        assertFalse(interceptor.match(null));
    }

    @Test
    public void preProcessWhenNotLoopStateDoNothingTest() {
        ProcessContext context = mock(ProcessContext.class);
        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(false);

        // Should not throw and do nothing when not a loop state
        assertDoesNotThrow(() -> interceptor.preProcess(context));
    }

    @Test
    public void postProcessWhenNotLoopStateDoNothingTest() {
        ProcessContext context = mock(ProcessContext.class);
        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(false);

        // Should not throw and do nothing when not a loop state
        assertDoesNotThrow(() -> interceptor.postProcess(context, null));
    }

    @Test
    public void interceptorNotNullTest() {
        assertNotNull(interceptor);
    }

    // ========== Additional Tests: Coverage for preProcess Core Logic ==========

    @Test
    public void preProcessWhenIsLoopStateSetContextVariablesTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        AbstractTaskState taskState = mock(AbstractTaskState.class);
        Loop loop = mock(Loop.class);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.hasVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE))
                .thenReturn(false);
        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(taskState);
        when(taskState.getLoop()).thenReturn(loop);
        when(context.getVariable(DomainConstants.LOOP_COUNTER)).thenReturn(0);

        // Set loop configuration
        when(loop.getElementIndexName()).thenReturn("loopIndex");
        when(loop.getElementVariableName()).thenReturn("loopElement");

        // Set LoopContextHolder
        LoopContextHolder holder = new LoopContextHolder();
        List<String> collection = Arrays.asList("item1", "item2", "item3");
        holder.setCollection(collection);
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        // Set stateMachineContext
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("existingKey", "existingValue");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT)).thenReturn(contextVariables);

        // Execute
        assertDoesNotThrow(() -> interceptor.preProcess(context));

        // Verify setVariableLocally is called
        verify(context).setVariableLocally(eq(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT), any(Map.class));
    }

    // ========== Additional Tests: Coverage for postProcess Core Logic ==========

    @Test
    public void postProcessWhenStateSuccessIncrementCompletedInstancesTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstance stateInstance = mock(StateInstance.class);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);
        when(stateInstance.getStatus()).thenReturn(ExecutionStatus.SU);

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);
        when(context.getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION))
                .thenReturn(null);

        // Execute
        interceptor.postProcess(context, null);

        // Verify nrOfCompletedInstances has been incremented
        assertEquals(1, holder.getNrOfCompletedInstances().get());
        // Verify nrOfActiveInstances has been decremented
        assertEquals(-1, holder.getNrOfActiveInstances().get());
    }

    @Test
    public void postProcessWhenStateFailedSetFailEndTrueTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstance stateInstance = mock(StateInstance.class);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);
        when(stateInstance.getStatus()).thenReturn(ExecutionStatus.FA);

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);
        when(context.getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION))
                .thenReturn(null);

        // Execute
        interceptor.postProcess(context, null);

        // Verify failEnd is set to true
        assertTrue(holder.isFailEnd());
    }

    @Test
    public void postProcessWhenExceptionOccursReleaseSemaphoreTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstance stateInstance = mock(StateInstance.class);
        Semaphore semaphore = new Semaphore(0);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);
        when(stateInstance.getStatus()).thenReturn(ExecutionStatus.SU);

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);
        when(context.getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION))
                .thenReturn(null);
        when(context.hasVariable(DomainConstants.LOOP_SEMAPHORE)).thenReturn(true);
        when(context.getVariable(DomainConstants.LOOP_SEMAPHORE)).thenReturn(semaphore);

        Exception testException = new RuntimeException("test exception");

        // Execute
        interceptor.postProcess(context, testException);

        // Verify semaphore is released
        assertEquals(1, semaphore.availablePermits());
        // Verify failEnd is set to true (due to exception)
        assertTrue(holder.isFailEnd());
    }

    @Test
    public void postProcessWhenStateInstanceIsNullDoNotSetFailEndTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(null);

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);
        when(context.getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION))
                .thenReturn(null);

        // Execute
        interceptor.postProcess(context, null);

        // Verify failEnd remains false
        assertFalse(holder.isFailEnd());
    }

    @Test
    public void postProcessWhenLocalExceptionExistsSetFailEndTrueTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstance stateInstance = mock(StateInstance.class);

        when(context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)).thenReturn(true);
        when(context.getVariable(DomainConstants.VAR_NAME_STATE_INST)).thenReturn(stateInstance);
        when(stateInstance.getStatus()).thenReturn(ExecutionStatus.SU);

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        // Local exception exists
        Exception localException = new RuntimeException("local exception");
        when(context.getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION))
                .thenReturn(localException);

        // Execute
        interceptor.postProcess(context, null);

        // Verify failEnd is set to true
        assertTrue(holder.isFailEnd());
    }
}
