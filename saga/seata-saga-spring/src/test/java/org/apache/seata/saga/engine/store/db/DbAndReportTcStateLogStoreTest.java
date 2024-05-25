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
package org.apache.seata.saga.engine.store.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.seata.saga.engine.config.DbStateMachineConfig;
import org.apache.seata.saga.engine.sequence.UUIDSeqGenerator;
import org.apache.seata.saga.proctrl.impl.ProcessContextImpl;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl;
import org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

/**
 * DbAndReportTcStateLogStoreTest
 */
public class DbAndReportTcStateLogStoreTest {
    private DbAndReportTcStateLogStore dbAndReportTcStateLogStore;

    @BeforeEach
    public void setUp() {
        DbAndReportTcStateLogStore mock = Mockito.spy(DbAndReportTcStateLogStore.class);
        dbAndReportTcStateLogStore = mock;
        dbAndReportTcStateLogStore.setSeqGenerator(new UUIDSeqGenerator());
        dbAndReportTcStateLogStore.setSagaTransactionalTemplate(new MockSagaTransactionTemplate());
        dbAndReportTcStateLogStore.setTablePrefix("test_");
        Mockito.doReturn(new StateInstanceImpl()).when(mock).selectOne(any(), any(), any(), any());
        Mockito.doReturn(Collections.singletonList(new StateInstanceImpl())).when(mock).selectList(any(), any(), any());
        Mockito.doReturn(1).when(mock).executeUpdate(any(), any(), any());
        Mockito.doReturn(1).when(mock).executeUpdate(any(), any(), any());
    }

    @Test
    public void testRecordStateMachineStarted() {
        DbAndReportTcStateLogStore dbAndReportTcStateLogStore = new DbAndReportTcStateLogStore();
        StateMachineInstanceImpl stateMachineInstance = new StateMachineInstanceImpl();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG,  new DbStateMachineConfig());
        Assertions.assertThrows(NullPointerException.class,
                () -> dbAndReportTcStateLogStore.recordStateMachineStarted(stateMachineInstance, context));
    }

    @Test
    public void testRecordStateMachineFinished() {
        DbAndReportTcStateLogStore dbAndReportTcStateLogStore = new DbAndReportTcStateLogStore();
        StateMachineInstanceImpl stateMachineInstance = new StateMachineInstanceImpl();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG,  new DbStateMachineConfig());
        Assertions.assertThrows(NullPointerException.class,
                () -> dbAndReportTcStateLogStore.recordStateMachineFinished(stateMachineInstance, context));
    }

    @Test
    public void testRecordStateMachineRestarted() {
        DbAndReportTcStateLogStore dbAndReportTcStateLogStore = new DbAndReportTcStateLogStore();
        StateMachineInstanceImpl stateMachineInstance = new StateMachineInstanceImpl();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG,  new DbStateMachineConfig());
        Assertions.assertThrows(NullPointerException.class,
                () -> dbAndReportTcStateLogStore.recordStateMachineRestarted(stateMachineInstance, context));
    }

    @Test
    public void testRecordStateStarted() {
        DbAndReportTcStateLogStore dbAndReportTcStateLogStore = new DbAndReportTcStateLogStore();
        StateInstanceImpl stateMachineInstance = new StateInstanceImpl();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG,  new DbStateMachineConfig());
        Assertions.assertThrows(NullPointerException.class,
                () -> dbAndReportTcStateLogStore.recordStateStarted(stateMachineInstance, context));
    }

    @Test
    public void testRecordStateFinished() {
        DbAndReportTcStateLogStore dbAndReportTcStateLogStore = new DbAndReportTcStateLogStore();
        StateInstanceImpl stateMachineInstance = new StateInstanceImpl();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG,  new DbStateMachineConfig());
        Assertions.assertThrows(NullPointerException.class,
                () -> dbAndReportTcStateLogStore.recordStateFinished(stateMachineInstance, context));
    }

    @Test
    public void testGetStateMachineInstance() {
        Assertions.assertDoesNotThrow(() -> dbAndReportTcStateLogStore.getStateInstance("test", "test"));
    }

    @Test
    public void testGetStateMachineInstanceByBusinessKey() {
        StateMachineInstanceImpl stateMachineInstance = new StateMachineInstanceImpl();
        stateMachineInstance.setStateMap(new HashMap<>());
        stateMachineInstance.setStateList(new ArrayList<>());
        Mockito.doReturn(stateMachineInstance).when(dbAndReportTcStateLogStore).selectOne(any(), any(), any(), any());
        dbAndReportTcStateLogStore.getStateMachineInstanceByBusinessKey("test", "test");
    }

    @Test
    public void testQueryStateMachineInstanceByParentId() {
        Assertions.assertDoesNotThrow(() -> dbAndReportTcStateLogStore.queryStateMachineInstanceByParentId("test"));
    }

    @Test
    public void testGetStateInstance() {
        Assertions.assertDoesNotThrow(() -> dbAndReportTcStateLogStore.getStateInstance("test", "test"));
    }

    @Test
    public void testQueryStateInstanceListByMachineInstanceId() {
        Assertions.assertDoesNotThrow(() -> dbAndReportTcStateLogStore.queryStateInstanceListByMachineInstanceId("test"));
    }

    @Test
    public void testClearUp() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST, new StateMachineInstanceImpl());
        Assertions.assertDoesNotThrow(() -> dbAndReportTcStateLogStore.clearUp(context));
    }
}