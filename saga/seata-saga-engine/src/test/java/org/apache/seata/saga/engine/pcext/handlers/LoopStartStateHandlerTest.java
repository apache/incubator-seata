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
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.utils.LoopContextHolder;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState.LoopImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LoopStartStateHandler}
 */
public class LoopStartStateHandlerTest {

    private LoopStartStateHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new LoopStartStateHandler();
    }

    @Test
    public void processWhenLoopConfigNullSetTemporaryStateTest() {
        ProcessContext context = mock(ProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        AbstractTaskState state = mock(AbstractTaskState.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(instruction.getStateName()).thenReturn("testState");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(smInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);

        when(state.getType()).thenReturn(StateType.SERVICE_TASK);
        when(state.getLoop()).thenReturn(null);

        handler.process(context);

        verify(instruction).setTemporaryState(null);
        verify(instruction).setTemporaryState(state);
    }

    @Test
    public void processHandlerNotNullTest() {
        assertNotNull(handler);
    }

    @Test
    public void processCleanupContextVariablesInFinallyBlockTest() {
        ProcessContext context = mock(ProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        AbstractTaskState state = mock(AbstractTaskState.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(instruction.getStateName()).thenReturn("testState");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(smInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);
        when(state.getType()).thenReturn(StateType.SERVICE_TASK);
        when(state.getLoop()).thenReturn(null);

        handler.process(context);

        verify(context).removeVariable(DomainConstants.LOOP_SEMAPHORE);
        verify(context).removeVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE);
        verify(context).removeVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);
    }

    @Test
    public void processWhenLoopContextHolderIsNullNotThrowExceptionTest() {
        ProcessContext context = mock(ProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        AbstractTaskState state = mock(AbstractTaskState.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(instruction.getStateName()).thenReturn("testState");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(smInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(null);
        when(state.getType()).thenReturn(StateType.SERVICE_TASK);
        when(state.getLoop()).thenReturn(null);

        assertDoesNotThrow(() -> handler.process(context));
    }

    @Test
    public void processWhenLoopContextHolderExistsWithFailEndFalseTest() {
        ProcessContext context = mock(ProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        AbstractTaskState state = mock(AbstractTaskState.class);

        LoopContextHolder holder = new LoopContextHolder();
        holder.setFailEnd(false);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(instruction.getStateName()).thenReturn("testState");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(smInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);
        when(state.getType()).thenReturn(StateType.SERVICE_TASK);
        when(state.getLoop()).thenReturn(null);

        handler.process(context);

        assertFalse(holder.isFailEnd());
    }

    @Test
    public void processAsyncLoopExecutionHappyPathTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateMachineConfig config = mock(StateMachineConfig.class);
        AbstractTaskState state = mock(AbstractTaskState.class);
        ProcessCtrlEventPublisher publisher = mock(ProcessCtrlEventPublisher.class);
        ExpressionResolver resolver = mock(ExpressionResolver.class);

        LoopImpl loop = new LoopImpl();
        loop.setParallel(2);
        loop.setCollection("$.users");
        loop.setElementVariableName("user");

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(instruction.getStateName()).thenReturn("loopState");
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(smInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(config);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT)).thenReturn(new HashMap<>());

        LoopContextHolder holder = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        when(state.getType()).thenReturn(StateType.SERVICE_TASK);
        when(state.getLoop()).thenReturn(loop);

        Expression expression = mock(Expression.class);
        List<String> userList = Arrays.asList("user1", "user2", "user3");
        when(expression.getValue(any())).thenReturn(userList);
        when(resolver.getExpression(any())).thenReturn(expression);
        when(config.getExpressionResolver()).thenReturn(resolver);

        when(config.isEnableAsync()).thenReturn(true);
        when(config.getAsyncProcessCtrlEventPublisher()).thenReturn(publisher);

        final Semaphore[] capturedSemaphore = new Semaphore[1];
        doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object value = invocation.getArgument(1);
                    if (DomainConstants.LOOP_SEMAPHORE.equals(key)) {
                        capturedSemaphore[0] = (Semaphore) value;
                    }
                    return null;
                })
                .when(context)
                .setVariable(eq(DomainConstants.LOOP_SEMAPHORE), any());

        // Merged doAnswer handling both StateInstance injection AND Semaphore release
        doAnswer(invocation -> {
                    ProcessContext ctx = invocation.getArgument(0);

                    StateInstance mockStateInst = mock(StateInstance.class);
                    when(mockStateInst.getOutputParams()).thenReturn(new HashMap<>());

                    // Crucial Fix: Use setVariableLocally to prevent delegation to mocked parent
                    if (ctx instanceof HierarchicalProcessContext) {
                        ((HierarchicalProcessContext) ctx)
                                .setVariableLocally(DomainConstants.VAR_NAME_STATE_INST, mockStateInst);
                    } else {
                        ctx.setVariable(DomainConstants.VAR_NAME_STATE_INST, mockStateInst);
                    }

                    if (capturedSemaphore[0] != null) {
                        capturedSemaphore[0].release();
                    }
                    return null;
                })
                .when(publisher)
                .publish(any());

        handler.process(context);

        verify(publisher, times(3)).publish(any());
        verify(context).removeVariable(DomainConstants.LOOP_SEMAPHORE);
    }
}
