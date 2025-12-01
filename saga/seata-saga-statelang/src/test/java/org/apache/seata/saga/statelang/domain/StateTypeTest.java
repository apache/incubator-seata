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
package org.apache.seata.saga.statelang.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StateTypeTest {

    @Test
    public void testStateTypeValues() {
        assertEquals("ServiceTask", StateType.SERVICE_TASK.getValue());
        assertEquals("Choice", StateType.CHOICE.getValue());
        assertEquals("Fail", StateType.FAIL.getValue());
        assertEquals("Succeed", StateType.SUCCEED.getValue());
        assertEquals("CompensationTrigger", StateType.COMPENSATION_TRIGGER.getValue());
        assertEquals("SubStateMachine", StateType.SUB_STATE_MACHINE.getValue());
        assertEquals("CompensateSubMachine", StateType.SUB_MACHINE_COMPENSATION.getValue());
        assertEquals("ScriptTask", StateType.SCRIPT_TASK.getValue());
        assertEquals("LoopStart", StateType.LOOP_START.getValue());
    }

    @Test
    public void testGetStateType() {
        assertEquals(StateType.SERVICE_TASK, StateType.getStateType("ServiceTask"));
        assertEquals(StateType.CHOICE, StateType.getStateType("choice"));
        assertEquals(StateType.SCRIPT_TASK, StateType.getStateType("SCRIPTTASK"));
    }

    @Test
    public void testGetStateTypeWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            StateType.getStateType("UnknownType");
        });
    }
}
