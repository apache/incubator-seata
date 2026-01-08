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

import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class StateMachineInstanceImplTest {

    @Test
    public void testStateMachineInstanceImpl() {
        StateMachineInstanceImpl instance = new StateMachineInstanceImpl();
        Date now = new Date();
        String testId = "TEST_ID_123";
        ExecutionStatus status = ExecutionStatus.SU;

        instance.setId(testId);
        instance.setGmtStarted(now);
        instance.setStatus(status);
        instance.setRunning(true);

        assertEquals(testId, instance.getId());
        assertEquals(now, instance.getGmtStarted());
        assertEquals(status, instance.getStatus());
        Assertions.assertTrue(instance.isRunning());

        StateInstance mockState = mock(StateInstance.class);
        String stateId = "STATE_1";
        instance.putStateInstance(stateId, mockState);

        Map<String, StateInstance> stateMap = instance.getStateMap();
        assertEquals(1, stateMap.size());
        assertEquals(mockState, stateMap.get(stateId));
        Assertions.assertTrue(instance.getStateList().contains(mockState));
    }
}
