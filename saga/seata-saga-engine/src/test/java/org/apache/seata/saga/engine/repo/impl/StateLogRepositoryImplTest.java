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
package org.apache.seata.saga.engine.repo.impl;

import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link StateLogRepositoryImpl}
 */
public class StateLogRepositoryImplTest {

    @Test
    public void getStateMachineInstanceWhenStoreIsNullReturnNullTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        assertNull(repo.getStateMachineInstance("any-id"));
    }

    @Test
    public void getStateMachineInstanceWhenStoreExistsDelegateToStoreTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        StateLogStore store = mock(StateLogStore.class);
        StateMachineInstance expected = mock(StateMachineInstance.class);
        when(store.getStateMachineInstance("test-id")).thenReturn(expected);

        repo.setStateLogStore(store);

        assertSame(expected, repo.getStateMachineInstance("test-id"));
        verify(store).getStateMachineInstance("test-id");
    }

    @Test
    public void getStateMachineInstanceByBusinessKeyWhenStoreIsNullReturnNullTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        assertNull(repo.getStateMachineInstanceByBusinessKey("key", "tenant"));
    }

    @Test
    public void getStateMachineInstanceByBusinessKeyWhenStoreExistsDelegateToStoreTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        StateLogStore store = mock(StateLogStore.class);
        StateMachineInstance expected = mock(StateMachineInstance.class);
        when(store.getStateMachineInstanceByBusinessKey("key", "tenant")).thenReturn(expected);

        repo.setStateLogStore(store);

        assertSame(expected, repo.getStateMachineInstanceByBusinessKey("key", "tenant"));
        verify(store).getStateMachineInstanceByBusinessKey("key", "tenant");
    }

    @Test
    public void queryStateMachineInstanceByParentIdWhenStoreIsNullReturnNullTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        assertNull(repo.queryStateMachineInstanceByParentId("parent-id"));
    }

    @Test
    public void queryStateMachineInstanceByParentIdWhenStoreExistsDelegateToStoreTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        StateLogStore store = mock(StateLogStore.class);
        List<StateMachineInstance> expected = Arrays.asList(mock(StateMachineInstance.class));
        when(store.queryStateMachineInstanceByParentId("parent-id")).thenReturn(expected);

        repo.setStateLogStore(store);

        assertSame(expected, repo.queryStateMachineInstanceByParentId("parent-id"));
        verify(store).queryStateMachineInstanceByParentId("parent-id");
    }

    @Test
    public void getStateInstanceWhenStoreIsNullReturnNullTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        assertNull(repo.getStateInstance("state-id", "machine-id"));
    }

    @Test
    public void getStateInstanceWhenStoreExistsDelegateToStoreTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        StateLogStore store = mock(StateLogStore.class);
        StateInstance expected = mock(StateInstance.class);
        when(store.getStateInstance("state-id", "machine-id")).thenReturn(expected);

        repo.setStateLogStore(store);

        assertSame(expected, repo.getStateInstance("state-id", "machine-id"));
        verify(store).getStateInstance("state-id", "machine-id");
    }

    @Test
    public void queryStateInstanceListByMachineInstanceIdWhenStoreIsNullReturnNullTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        assertNull(repo.queryStateInstanceListByMachineInstanceId("machine-id"));
    }

    @Test
    public void queryStateInstanceListByMachineInstanceIdWhenStoreExistsDelegateToStoreTest() {
        StateLogRepositoryImpl repo = new StateLogRepositoryImpl();
        StateLogStore store = mock(StateLogStore.class);
        List<StateInstance> expected = Arrays.asList(mock(StateInstance.class));
        when(store.queryStateInstanceListByMachineInstanceId("machine-id")).thenReturn(expected);

        repo.setStateLogStore(store);

        assertSame(expected, repo.queryStateInstanceListByMachineInstanceId("machine-id"));
        verify(store).queryStateInstanceListByMachineInstanceId("machine-id");
    }
}
