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
package org.apache.seata.saga.engine.impl;

import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.exception.ForwardInvalidException;
import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link ProcessCtrlStateMachineEngine}
 */
public class ProcessCtrlStateMachineEngineTest {

    private ProcessCtrlStateMachineEngine engine;
    private StateMachineConfig config;

    @BeforeEach
    public void setUp() {
        engine = new ProcessCtrlStateMachineEngine();
        config = mock(StateMachineConfig.class);
        engine.setStateMachineConfig(config);
    }

    @Test
    public void getStateMachineConfigTest() {
        assertSame(config, engine.getStateMachineConfig());
    }

    @Test
    public void setStateMachineConfigTest() {
        StateMachineConfig newConfig = mock(StateMachineConfig.class);
        engine.setStateMachineConfig(newConfig);
        assertSame(newConfig, engine.getStateMachineConfig());
    }

    @Test
    public void findOutLastForwardStateInstanceWithEmptyListReturnNullTest() {
        List<StateInstance> emptyList = Collections.emptyList();
        StateInstance result = engine.findOutLastForwardStateInstance(emptyList);
        assertNull(result);
    }

    @Test
    public void findOutLastForwardStateInstanceWithCompensationStateSkipTest() {
        StateInstance stateInstance = mock(StateInstance.class);
        when(stateInstance.isForCompensation()).thenReturn(true);

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(stateInstance);

        StateInstance result = engine.findOutLastForwardStateInstance(stateList);
        assertNull(result);
    }

    @Test
    public void findOutLastForwardStateInstanceWithSuccessfulCompensationSkipTest() {
        StateInstance stateInstance = mock(StateInstance.class);
        when(stateInstance.isForCompensation()).thenReturn(false);
        when(stateInstance.getCompensationStatus()).thenReturn(ExecutionStatus.SU);

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(stateInstance);

        StateInstance result = engine.findOutLastForwardStateInstance(stateList);
        assertNull(result);
    }

    @Test
    public void findOutLastForwardStateInstanceReturnLastNonCompensationStateTest() {
        StateInstance state1 = mock(StateInstance.class);
        StateInstance state2 = mock(StateInstance.class);

        when(state1.isForCompensation()).thenReturn(false);
        when(state1.getCompensationStatus()).thenReturn(null);
        when(state1.getType()).thenReturn(StateType.SERVICE_TASK);

        when(state2.isForCompensation()).thenReturn(false);
        when(state2.getCompensationStatus()).thenReturn(null);
        when(state2.getType()).thenReturn(StateType.SERVICE_TASK);

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(state1);
        stateList.add(state2);

        StateInstance result = engine.findOutLastForwardStateInstance(stateList);
        assertSame(state2, result);
    }

    @Test
    public void findOutLastForwardStateInstanceWithUNCompensationStatusThrowExceptionTest() {
        StateInstance stateInstance = mock(StateInstance.class);
        when(stateInstance.isForCompensation()).thenReturn(false);
        when(stateInstance.getCompensationStatus()).thenReturn(ExecutionStatus.UN);
        when(stateInstance.getType()).thenReturn(StateType.SERVICE_TASK);
        when(stateInstance.getId()).thenReturn("state-123");

        List<StateInstance> stateList = new ArrayList<>();
        stateList.add(stateInstance);

        assertThrows(ForwardInvalidException.class, () -> engine.findOutLastForwardStateInstance(stateList));
    }

    @Test
    public void reloadStateMachineInstanceWhenNullReturnNullTest() {
        StateLogStore stateLogStore = mock(StateLogStore.class);
        when(config.getStateLogStore()).thenReturn(stateLogStore);
        when(stateLogStore.getStateMachineInstance("non-existent")).thenReturn(null);

        StateMachineInstance result = engine.reloadStateMachineInstance("non-existent");
        assertNull(result);
    }

    @Test
    public void engineNotNullTest() {
        assertNotNull(engine);
    }
}
