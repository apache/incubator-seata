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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.State;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class StateMachineImplTest {

    @Test
    public void testStateMachineImpl() {
        StateMachineImpl machine = new StateMachineImpl();
        String testId = "MACHINE_ID_456";
        String testName = "TestMachine";
        Date createTime = new Date();

        machine.setId(testId);
        machine.setName(testName);
        machine.setGmtCreate(createTime);

        assertEquals(testId, machine.getId());
        assertEquals(testName, machine.getName());
        assertEquals(createTime, machine.getGmtCreate());

        State mockState = mock(State.class);
        String stateName = "State1";
        machine.putState(stateName, mockState);

        Map<String, State> states = machine.getStates();
        assertEquals(1, states.size());
        assertEquals(mockState, machine.getState(stateName));
    }
}
