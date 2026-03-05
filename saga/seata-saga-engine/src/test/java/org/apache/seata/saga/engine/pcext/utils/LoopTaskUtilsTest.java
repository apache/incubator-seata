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
package org.apache.seata.saga.engine.pcext.utils;

import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.impl.ProcessContextImpl;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LoopTaskUtils}
 */
public class LoopTaskUtilsTest {

    @Test
    public void matchLoopWhenStateTypeIsServiceTaskReturnTrueTest() {
        State state = mock(State.class);
        when(state.getType()).thenReturn(StateType.SERVICE_TASK);

        assertTrue(LoopTaskUtils.matchLoop(state));
    }

    @Test
    public void matchLoopWhenStateTypeIsScriptTaskReturnTrueTest() {
        State state = mock(State.class);
        when(state.getType()).thenReturn(StateType.SCRIPT_TASK);

        assertTrue(LoopTaskUtils.matchLoop(state));
    }

    @Test
    public void matchLoopWhenStateTypeIsSubStateMachineReturnTrueTest() {
        State state = mock(State.class);
        when(state.getType()).thenReturn(StateType.SUB_STATE_MACHINE);

        assertTrue(LoopTaskUtils.matchLoop(state));
    }

    @Test
    public void matchLoopWhenStateTypeIsChoiceReturnFalseTest() {
        State state = mock(State.class);
        when(state.getType()).thenReturn(StateType.CHOICE);

        assertFalse(LoopTaskUtils.matchLoop(state));
    }

    @Test
    public void matchLoopWhenStateIsNullReturnFalseTest() {
        assertFalse(LoopTaskUtils.matchLoop(null));
    }

    @Test
    public void reloadLoopCounterFromValidStateNameExtractCounterTest() {
        String stateName = "myState" + LoopTaskUtils.LOOP_STATE_NAME_PATTERN + "10";
        int counter = LoopTaskUtils.reloadLoopCounter(stateName);

        assertEquals(10, counter);
    }

    @Test
    public void reloadLoopCounterFromInvalidStateNameReturnNegativeOneTest() {
        String stateName = "myState";
        int counter = LoopTaskUtils.reloadLoopCounter(stateName);

        // Source code returns -1 when pattern is not found
        assertEquals(-1, counter);
    }

    @Test
    public void reloadLoopCounterWithZeroCounterTest() {
        String stateName = "myState" + LoopTaskUtils.LOOP_STATE_NAME_PATTERN + "0";
        int counter = LoopTaskUtils.reloadLoopCounter(stateName);

        assertEquals(0, counter);
    }

    @Test
    public void reloadLoopCounterWithNullStateNameReturnNegativeOneTest() {
        int counter = LoopTaskUtils.reloadLoopCounter(null);

        assertEquals(-1, counter);
    }

    @Test
    public void reloadLoopCounterWithEmptyStateNameReturnNegativeOneTest() {
        int counter = LoopTaskUtils.reloadLoopCounter("");

        assertEquals(-1, counter);
    }

    @Test
    public void loopStateNamePatternConstantTest() {
        assertEquals("-loop-", LoopTaskUtils.LOOP_STATE_NAME_PATTERN);
    }

    // ========== Additional Tests: Coverage for createLoopCounterContext ==========

    @Test
    public void createLoopCounterContextPushCountersToStackTest() {
        ProcessContext context = mock(ProcessContext.class);
        LoopContextHolder holder = new LoopContextHolder();
        List<String> collection = Arrays.asList("a", "b", "c");
        holder.setCollection(collection);

        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        LoopTaskUtils.createLoopCounterContext(context);

        assertEquals(3, holder.getNrOfInstances().get());
        // Stack should contain 2, 1, 0 (pushed in descending order)
        Stack<Integer> stack = holder.getLoopCounterStack();
        assertEquals(3, stack.size());
        assertEquals(Integer.valueOf(0), stack.pop());
        assertEquals(Integer.valueOf(1), stack.pop());
        assertEquals(Integer.valueOf(2), stack.pop());
    }

    // ========== Additional Tests: Coverage for generateLoopStateName ==========

    @Test
    public void generateLoopStateNameWithValidNameAppendPatternTest() {
        ProcessContext context = mock(ProcessContext.class);
        when(context.getVariable(DomainConstants.LOOP_COUNTER)).thenReturn(5);

        String result = LoopTaskUtils.generateLoopStateName(context, "testState");

        assertEquals("testState-loop-5", result);
    }

    @Test
    public void generateLoopStateNameWithBlankNameReturnOriginalTest() {
        ProcessContext context = mock(ProcessContext.class);

        String result = LoopTaskUtils.generateLoopStateName(context, "");

        assertEquals("", result);
    }

    @Test
    public void generateLoopStateNameWithNullNameReturnNullTest() {
        ProcessContext context = mock(ProcessContext.class);

        String result = LoopTaskUtils.generateLoopStateName(context, null);

        assertNull(result);
    }

    // ========== Additional Tests: Coverage for acquireNextLoopCounter ==========

    @Test
    public void acquireNextLoopCounterPopFromStackTest() {
        ProcessContext context = mock(ProcessContext.class);
        LoopContextHolder holder = new LoopContextHolder();
        holder.getLoopCounterStack().push(10);
        holder.getLoopCounterStack().push(20);

        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        int counter = LoopTaskUtils.acquireNextLoopCounter(context);

        assertEquals(20, counter);
    }

    @Test
    public void acquireNextLoopCounterWhenStackEmptyReturnNegativeOneTest() {
        ProcessContext context = mock(ProcessContext.class);
        LoopContextHolder holder = new LoopContextHolder();
        // Empty stack

        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(holder);

        int counter = LoopTaskUtils.acquireNextLoopCounter(context);

        assertEquals(-1, counter);
    }

    // ========== Additional Tests: Coverage for createLoopEventContext ==========

    @Test
    public void createLoopEventContextWithPositiveCounterSetCounterDirectlyTest() {
        ProcessContext parentContext = mock(ProcessContext.class);
        StateInstruction instruction = mock(StateInstruction.class);
        when(parentContext.getInstruction(StateInstruction.class)).thenReturn(instruction);
        when(instruction.getStateMachineName()).thenReturn("testMachine");
        when(instruction.getTenantId()).thenReturn("tenant1");
        when(instruction.getStateName()).thenReturn("testState");
        when(instruction.getTemporaryState()).thenReturn(null);

        ProcessContext childContext = LoopTaskUtils.createLoopEventContext(parentContext, 5);

        assertNotNull(childContext);
        assertEquals(5, childContext.getVariable(DomainConstants.LOOP_COUNTER));
    }

    @Test
    public void createLoopEventContextWithNegativeCounterAcquireFromStackTest() {
        ProcessContextImpl parentContext = new ProcessContextImpl();
        StateInstruction instruction = new StateInstruction();
        instruction.setStateMachineName("testMachine");
        instruction.setTenantId("tenant1");
        instruction.setStateName("testState");
        parentContext.setInstruction(instruction);

        LoopContextHolder holder = new LoopContextHolder();
        holder.getLoopCounterStack().push(7);
        parentContext.setVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER, holder);

        ProcessContext childContext = LoopTaskUtils.createLoopEventContext(parentContext, -1);

        assertNotNull(childContext);
        assertEquals(7, childContext.getVariable(DomainConstants.LOOP_COUNTER));
    }

    // ========== Additional Tests: Coverage for findOutLastRetriedStateInstance ==========

    @Test
    public void findOutLastRetriedStateInstanceWhenFoundReturnStateTest() {
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateInstance state1 = mock(StateInstance.class);
        StateInstance state2 = mock(StateInstance.class);

        when(state1.getName()).thenReturn("state-loop-0");
        when(state2.getName()).thenReturn("state-loop-1");

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(state1);
        stateList.add(state2);

        when(smInstance.getStateList()).thenReturn(stateList);

        StateInstance result = LoopTaskUtils.findOutLastRetriedStateInstance(smInstance, "state-loop-1");

        assertSame(state2, result);
    }

    @Test
    public void findOutLastRetriedStateInstanceWhenNotFoundReturnNullTest() {
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        StateInstance state1 = mock(StateInstance.class);

        when(state1.getName()).thenReturn("state-loop-0");

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(state1);

        when(smInstance.getStateList()).thenReturn(stateList);

        StateInstance result = LoopTaskUtils.findOutLastRetriedStateInstance(smInstance, "nonexistent");

        assertNull(result);
    }

    @Test
    public void findOutLastRetriedStateInstanceWithEmptyListReturnNullTest() {
        StateMachineInstance smInstance = mock(StateMachineInstance.class);
        when(smInstance.getStateList()).thenReturn(new ArrayList<>());

        StateInstance result = LoopTaskUtils.findOutLastRetriedStateInstance(smInstance, "anyState");

        assertNull(result);
    }
}
