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
package io.seata.saga.statelang.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for StateType enum compatibility wrapper.
 */
public class StateTypeTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(StateType.class.isAnnotationPresent(Deprecated.class), "StateType should be marked as @Deprecated");
    }

    @Test
    public void testEnumValues() {
        StateType[] values = StateType.values();
        assertEquals(9, values.length, "Should have 9 enum values");
    }

    @Test
    public void testServiceTask() {
        assertEquals("ServiceTask", StateType.SERVICE_TASK.getValue());
    }

    @Test
    public void testChoice() {
        assertEquals("Choice", StateType.CHOICE.getValue());
    }

    @Test
    public void testFail() {
        assertEquals("Fail", StateType.FAIL.getValue());
    }

    @Test
    public void testSucceed() {
        assertEquals("Succeed", StateType.SUCCEED.getValue());
    }

    @Test
    public void testCompensationTrigger() {
        assertEquals("CompensationTrigger", StateType.COMPENSATION_TRIGGER.getValue());
    }

    @Test
    public void testSubStateMachine() {
        assertEquals("SubStateMachine", StateType.SUB_STATE_MACHINE.getValue());
    }

    @Test
    public void testSubMachineCompensation() {
        assertEquals("CompensateSubMachine", StateType.SUB_MACHINE_COMPENSATION.getValue());
    }

    @Test
    public void testScriptTask() {
        assertEquals("ScriptTask", StateType.SCRIPT_TASK.getValue());
    }

    @Test
    public void testLoopStart() {
        assertEquals("LoopStart", StateType.LOOP_START.getValue());
    }

    @Test
    public void testGetStateTypeByValue() {
        assertEquals(StateType.SERVICE_TASK, StateType.getStateType("ServiceTask"));
        assertEquals(StateType.CHOICE, StateType.getStateType("Choice"));
        assertEquals(StateType.FAIL, StateType.getStateType("Fail"));
        assertEquals(StateType.SUCCEED, StateType.getStateType("Succeed"));
    }

    @Test
    public void testGetStateTypeCaseInsensitive() {
        assertEquals(StateType.SERVICE_TASK, StateType.getStateType("servicetask"));
        assertEquals(StateType.CHOICE, StateType.getStateType("CHOICE"));
    }

    @Test
    public void testGetStateTypeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> StateType.getStateType("InvalidType"));
    }

    @Test
    public void testWrapNull() {
        assertNull(StateType.wrap(null), "Wrapping null should return null");
    }

    @Test
    public void testWrapServiceTask() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK);
        assertEquals(StateType.SERVICE_TASK, wrapped);
    }

    @Test
    public void testWrapChoice() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.CHOICE);
        assertEquals(StateType.CHOICE, wrapped);
    }

    @Test
    public void testWrapFail() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.FAIL);
        assertEquals(StateType.FAIL, wrapped);
    }

    @Test
    public void testWrapSucceed() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.SUCCEED);
        assertEquals(StateType.SUCCEED, wrapped);
    }

    @Test
    public void testWrapCompensationTrigger() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.COMPENSATION_TRIGGER);
        assertEquals(StateType.COMPENSATION_TRIGGER, wrapped);
    }

    @Test
    public void testWrapSubStateMachine() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.SUB_STATE_MACHINE);
        assertEquals(StateType.SUB_STATE_MACHINE, wrapped);
    }

    @Test
    public void testWrapSubMachineCompensation() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.SUB_MACHINE_COMPENSATION);
        assertEquals(StateType.SUB_MACHINE_COMPENSATION, wrapped);
    }

    @Test
    public void testWrapScriptTask() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.SCRIPT_TASK);
        assertEquals(StateType.SCRIPT_TASK, wrapped);
    }

    @Test
    public void testWrapLoopStart() {
        StateType wrapped = StateType.wrap(org.apache.seata.saga.statelang.domain.StateType.LOOP_START);
        assertEquals(StateType.LOOP_START, wrapped);
    }

    @Test
    public void testUnwrapAllValues() {
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.SERVICE_TASK, StateType.SERVICE_TASK.unwrap());
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.CHOICE, StateType.CHOICE.unwrap());
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.FAIL, StateType.FAIL.unwrap());
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.SUCCEED, StateType.SUCCEED.unwrap());
        assertEquals(
                org.apache.seata.saga.statelang.domain.StateType.COMPENSATION_TRIGGER,
                StateType.COMPENSATION_TRIGGER.unwrap());
        assertEquals(
                org.apache.seata.saga.statelang.domain.StateType.SUB_STATE_MACHINE,
                StateType.SUB_STATE_MACHINE.unwrap());
        assertEquals(
                org.apache.seata.saga.statelang.domain.StateType.SUB_MACHINE_COMPENSATION,
                StateType.SUB_MACHINE_COMPENSATION.unwrap());
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.SCRIPT_TASK, StateType.SCRIPT_TASK.unwrap());
        assertEquals(org.apache.seata.saga.statelang.domain.StateType.LOOP_START, StateType.LOOP_START.unwrap());
    }

    @Test
    public void testWrapAndUnwrapRoundTrip() {
        for (org.apache.seata.saga.statelang.domain.StateType apache :
                org.apache.seata.saga.statelang.domain.StateType.values()) {
            StateType wrapped = StateType.wrap(apache);
            assertNotNull(wrapped);
            org.apache.seata.saga.statelang.domain.StateType unwrapped = wrapped.unwrap();
            assertEquals(apache, unwrapped, "Round trip should preserve value");
        }
    }
}
