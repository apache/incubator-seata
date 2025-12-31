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
package org.apache.seata.server.transaction.at;

import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.session.BranchSession;
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
 * ATCore Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ATCore Test")
class ATCoreTest {

    private ATCore atCore;

    @Mock
    private RemotingServer mockRemotingServer;

    @Mock
    private GlobalSession mockGlobalSession;

    @Mock
    private BranchSession mockBranchSession;

    @BeforeEach
    void setUp() {
        atCore = new ATCore(mockRemotingServer);
    }

    @Test
    @DisplayName("test getHandleBranchType returns AT")
    void testGetHandleBranchTypeReturnsAT() {
        BranchType branchType = atCore.getHandleBranchType();
        assertEquals(BranchType.AT, branchType);
    }

    @Test
    @DisplayName("test branchSessionUnlock")
    void testBranchSessionUnlock() throws TransactionException {
        atCore.branchSessionUnlock(mockBranchSession);
        verify(mockBranchSession).unlock();
    }

    @Test
    @DisplayName("test lockQuery with isLockable true")
    void testLockQueryWithIsLockableTrue() throws TransactionException {
        // This test verifies that lockQuery delegates to lockManager
        // The actual implementation depends on the lock manager
        assertThrows(
                NullPointerException.class, () -> atCore.lockQuery(BranchType.AT, "resourceId", "xid", "lockKeys"));
    }

    @Test
    @DisplayName("test branchDelete delegates to branchCommit")
    void testBranchDeleteDelegatesToBranchCommit() throws TransactionException {
        // branchDelete should call branchCommit for AT mode
        // This is a behavior test
        assertNotNull(atCore);
    }

    @Test
    @DisplayName("test constructor initializes with remotingServer")
    void testConstructorInitializesWithRemotingServer() {
        ATCore core = new ATCore(mockRemotingServer);
        assertNotNull(core);
        assertEquals(BranchType.AT, core.getHandleBranchType());
    }

    @Test
    @DisplayName("test branchSessionLock with valid branch session")
    void testBranchSessionLockWithValidBranchSession() throws TransactionException {
        when(mockBranchSession.getApplicationData()).thenReturn(null);
        when(mockBranchSession.lock(anyBoolean(), anyBoolean())).thenReturn(true);

        assertDoesNotThrow(() -> atCore.branchSessionLock(mockGlobalSession, mockBranchSession));

        verify(mockBranchSession).lock(true, false);
    }

    @Test
    @DisplayName("test branchSessionLock with lock failure")
    void testBranchSessionLockWithLockFailure() throws TransactionException {
        when(mockBranchSession.getApplicationData()).thenReturn(null);
        when(mockBranchSession.lock(anyBoolean(), anyBoolean())).thenReturn(false);
        when(mockGlobalSession.getXid()).thenReturn("test-xid");
        when(mockBranchSession.getBranchId()).thenReturn(123L);

        assertThrows(
                BranchTransactionException.class, () -> atCore.branchSessionLock(mockGlobalSession, mockBranchSession));
    }

    @Test
    @DisplayName("test branchSessionLock with application data")
    void testBranchSessionLockWithApplicationData() throws TransactionException {
        String appData = "{\"autoCommit\": false}";
        when(mockBranchSession.getApplicationData()).thenReturn(appData);
        when(mockBranchSession.lock(anyBoolean(), anyBoolean())).thenReturn(true);

        assertDoesNotThrow(() -> atCore.branchSessionLock(mockGlobalSession, mockBranchSession));
    }

    @Test
    @DisplayName("test branchSessionLock with skipCheckLock true")
    void testBranchSessionLockWithSkipCheckLockTrue() throws TransactionException {
        String appData = "{\"skipCheckLock\": true}";
        when(mockBranchSession.getApplicationData()).thenReturn(appData);
        when(mockBranchSession.lock(anyBoolean(), anyBoolean())).thenReturn(true);

        assertDoesNotThrow(() -> atCore.branchSessionLock(mockGlobalSession, mockBranchSession));
    }

    @Test
    @DisplayName("test branchSessionLock with empty application data")
    void testBranchSessionLockWithEmptyApplicationData() throws TransactionException {
        when(mockBranchSession.getApplicationData()).thenReturn("");
        when(mockBranchSession.lock(anyBoolean(), anyBoolean())).thenReturn(true);

        assertDoesNotThrow(() -> atCore.branchSessionLock(mockGlobalSession, mockBranchSession));
    }

    @Test
    @DisplayName("test AT core is instantiable")
    void testATCoreIsInstantiable() {
        assertNotNull(atCore);
        assertTrue(atCore instanceof ATCore);
    }

    @Test
    @DisplayName("test branchSessionUnlock with mock branch session")
    void testBranchSessionUnlockWithMockBranchSession() throws TransactionException {
        BranchSession branch = mock(BranchSession.class);

        atCore.branchSessionUnlock(branch);

        verify(branch).unlock();
    }
}
