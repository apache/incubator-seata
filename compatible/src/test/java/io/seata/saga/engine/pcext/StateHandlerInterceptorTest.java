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
package io.seata.saga.engine.pcext;

import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.interceptors.ServiceTaskHandlerInterceptor;
import org.apache.seata.saga.engine.sequence.SeqGenerator;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for StateHandlerInterceptor interface compatibility wrapper.
 */
public class StateHandlerInterceptorTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateHandlerInterceptor.class.isAnnotationPresent(Deprecated.class),
                "StateHandlerInterceptor should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(StateHandlerInterceptor.class.isInterface(), "StateHandlerInterceptor should be an interface");
    }

    @Test
    public void testExtendsApacheStateHandlerInterceptor() {
        assertTrue(
                org.apache.seata.saga.engine.pcext.StateHandlerInterceptor.class.isAssignableFrom(
                        StateHandlerInterceptor.class),
                "StateHandlerInterceptor should extend org.apache.seata.saga.engine.pcext.StateHandlerInterceptor");
    }

    @Test
    public void preProcessWhenLoopElementPresent_SetsExtensionParamsTest() {
        HierarchicalProcessContext context = mock(HierarchicalProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        StateMachineInstance stateMachineInstance = mock(StateMachineInstance.class);
        StateMachineConfig stateMachineConfig = mock(StateMachineConfig.class);
        StateMachine stateMachine = mock(StateMachine.class);
        ServiceTaskStateImpl state = mock(ServiceTaskStateImpl.class);
        SeqGenerator seqGenerator = mock(SeqGenerator.class);

        when(context.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getState(context)).thenReturn(state);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST)).thenReturn(stateMachineInstance);
        when(context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).thenReturn(stateMachineConfig);

        when(stateMachineInstance.getGmtUpdated()).thenReturn(new Date());
        when(stateMachineConfig.getTransOperationTimeout()).thenReturn(Math.toIntExact(100000L));
        when(stateMachineInstance.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.isPersist()).thenReturn(false);

        when(state.getName()).thenReturn("LoopTask");
        when(state.isForCompensation()).thenReturn(false);

        Object mockLoopElement = "testLoopElementValue";
        when(context.getVariable(DomainConstants.VAR_NAME_LOOP_ELEMENT)).thenReturn(mockLoopElement);

        when(stateMachineConfig.getSeqGenerator()).thenReturn(seqGenerator);
        when(seqGenerator.generate(anyString())).thenReturn("SEQ_1001");

        ServiceTaskHandlerInterceptor interceptor = new ServiceTaskHandlerInterceptor();
        assertDoesNotThrow(() -> interceptor.preProcess(context));

        ArgumentCaptor<StateInstance> captor = ArgumentCaptor.forClass(StateInstance.class);
        verify(context).setVariableLocally(eq(DomainConstants.VAR_NAME_STATE_INST), captor.capture());

        StateInstance capturedInstance = captor.getValue();
        Assertions.assertNotNull(capturedInstance);
        Assertions.assertNotNull(capturedInstance.getExtensionParams());

        @SuppressWarnings("unchecked")
        Map<String, Object> extensionParams = (Map<String, Object>) capturedInstance.getExtensionParams();
        Assertions.assertEquals(mockLoopElement, extensionParams.get(DomainConstants.VAR_NAME_LOOP_ELEMENT));
    }
}
