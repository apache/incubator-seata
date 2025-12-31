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
package org.apache.seata.server.transaction.saga;

import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SagaCore Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaCore Test")
class SagaCoreTest {

    private SagaCore sagaCore;

    @Mock
    private RemotingServer mockRemotingServer;

    @Mock
    private GlobalSession mockGlobalSession;

    @BeforeEach
    void setUp() {
        sagaCore = new SagaCore(mockRemotingServer);
    }

    @Test
    @DisplayName("test getHandleBranchType returns SAGA")
    void testGetHandleBranchTypeReturnsSAGA() {
        BranchType branchType = sagaCore.getHandleBranchType();
        assertEquals(BranchType.SAGA, branchType);
    }

    @Test
    @DisplayName("test globalSessionStatusCheck does not throw")
    void testGlobalSessionStatusCheckDoesNotThrow() {
        assertDoesNotThrow(() -> sagaCore.globalSessionStatusCheck(mockGlobalSession));
    }

    @Test
    @DisplayName("test constructor initializes with remotingServer")
    void testConstructorInitializesWithRemotingServer() {
        SagaCore core = new SagaCore(mockRemotingServer);
        assertNotNull(core);
        assertEquals(BranchType.SAGA, core.getHandleBranchType());
    }

    @Test
    @DisplayName("test SagaCore extends AbstractCore")
    void testSagaCoreExtendsAbstractCore() {
        assertTrue(sagaCore instanceof org.apache.seata.server.coordinator.AbstractCore);
    }

    @Test
    @DisplayName("test SagaCore branch type is consistent")
    void testSagaCoreBranchTypeIsConsistent() {
        BranchType type1 = sagaCore.getHandleBranchType();
        BranchType type2 = sagaCore.getHandleBranchType();

        assertEquals(type1, type2);
        assertEquals(BranchType.SAGA, type1);
    }

    @Test
    @DisplayName("test SagaCore with different remoting servers")
    void testSagaCoreWithDifferentRemotingServers() {
        RemotingServer server1 = mock(RemotingServer.class);
        RemotingServer server2 = mock(RemotingServer.class);

        SagaCore core1 = new SagaCore(server1);
        SagaCore core2 = new SagaCore(server2);

        assertEquals(core1.getHandleBranchType(), core2.getHandleBranchType());
        assertEquals(BranchType.SAGA, core1.getHandleBranchType());
    }

    @Test
    @DisplayName("test SagaCore is instantiable")
    void testSagaCoreIsInstantiable() {
        assertNotNull(sagaCore);
        assertTrue(sagaCore instanceof SagaCore);
    }

    @Test
    @DisplayName("test multiple SagaCore instances")
    void testMultipleSagaCoreInstances() {
        RemotingServer server1 = mock(RemotingServer.class);
        RemotingServer server2 = mock(RemotingServer.class);

        SagaCore core1 = new SagaCore(server1);
        SagaCore core2 = new SagaCore(server2);

        assertNotNull(core1);
        assertNotNull(core2);
        assertNotSame(core1, core2);
        assertEquals(core1.getHandleBranchType(), core2.getHandleBranchType());
    }

    @Test
    @DisplayName("test SagaCore handles SAGA branch type only")
    void testSagaCoreHandlesSAGABranchTypeOnly() {
        BranchType handledType = sagaCore.getHandleBranchType();

        // Should only handle SAGA type
        assertEquals(BranchType.SAGA, handledType);
        assertNotEquals(BranchType.AT, handledType);
        assertNotEquals(BranchType.TCC, handledType);
        assertNotEquals(BranchType.XA, handledType);
    }

    @Test
    @DisplayName("test globalSessionStatusCheck with null session")
    void testGlobalSessionStatusCheckWithNullSession() {
        assertThrows(NullPointerException.class, () -> sagaCore.globalSessionStatusCheck(null));
    }

    @Test
    @DisplayName("test branchDelete throws ShouldNeverHappenException")
    void testBranchDeleteThrowsShouldNeverHappenException() {
        assertThrows(
                org.apache.seata.common.exception.ShouldNeverHappenException.class,
                () -> sagaCore.branchDelete(mockGlobalSession, null));
    }
}
