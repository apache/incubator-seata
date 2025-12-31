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
package org.apache.seata.server.session;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SessionHelper Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionHelper Test")
class SessionHelperTest {

    @Mock
    private GlobalSession mockGlobalSession;

    @Mock
    private BranchSession mockBranchSession;

    @Mock
    private GlobalSessionHandler mockGlobalSessionHandler;

    @Mock
    private BranchSessionHandler mockBranchSessionHandler;

    @BeforeEach
    void setUp() {
        when(mockBranchSession.getResourceId()).thenReturn("resource1");
    }

    @Test
    @DisplayName("test newBranchByGlobal with all parameters")
    void testNewBranchByGlobalWithAllParameters() {
        String xid = "test-xid-123";
        long transactionId = 12345L;

        when(mockGlobalSession.getXid()).thenReturn(xid);
        when(mockGlobalSession.getTransactionId()).thenReturn(transactionId);

        BranchSession result = SessionHelper.newBranchByGlobal(
                mockGlobalSession, BranchType.AT, "resource1", "appData", "lockKeys", "clientId");

        assertNotNull(result);
        assertEquals(xid, result.getXid());
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(BranchType.AT, result.getBranchType());
        assertEquals("resource1", result.getResourceId());
        assertEquals("lockKeys", result.getLockKey());
        assertEquals("clientId", result.getClientId());
        assertEquals("appData", result.getApplicationData());
        assertEquals(BranchStatus.Registered, result.getStatus());
    }

    @Test
    @DisplayName("test newBranchByGlobal without application data")
    void testNewBranchByGlobalWithoutApplicationData() {
        String xid = "test-xid-456";
        long transactionId = 456789L;

        when(mockGlobalSession.getXid()).thenReturn(xid);
        when(mockGlobalSession.getTransactionId()).thenReturn(transactionId);

        BranchSession result = SessionHelper.newBranchByGlobal(
                mockGlobalSession, BranchType.TCC, "resource2", "lockKeys2", "clientId2");

        assertNotNull(result);
        assertEquals(xid, result.getXid());
        assertEquals(transactionId, result.getTransactionId());
    }

    @Test
    @DisplayName("test newBranch")
    void testNewBranch() {
        String xid = "test-xid-789";
        long branchId = 111111L;

        BranchSession result = SessionHelper.newBranch(BranchType.XA, xid, branchId, "resource3", "appData3");

        assertNotNull(result);
        assertEquals(xid, result.getXid());
        assertEquals(branchId, result.getBranchId());
        assertEquals(BranchType.XA, result.getBranchType());
        assertEquals("resource3", result.getResourceId());
        assertEquals("appData3", result.getApplicationData());
    }

    @Test
    @DisplayName("test endCommitted with commit not retry")
    void testEndCommittedWithCommitNotRetry() throws TransactionException {
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);

        assertDoesNotThrow(() -> SessionHelper.endCommitted(mockGlobalSession, true));

        verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.Committed);
        verify(mockGlobalSession).end();
    }

    @Test
    @DisplayName("test endCommitFailed")
    void testEndCommitFailed() throws TransactionException {
        assertDoesNotThrow(() -> SessionHelper.endCommitFailed(mockGlobalSession, false));

        verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.CommitFailed);
        verify(mockGlobalSession).end();
    }

    @Test
    @DisplayName("test endCommitFailed with timeout")
    void testEndCommitFailedWithTimeout() throws TransactionException {
        assertDoesNotThrow(() -> SessionHelper.endCommitFailed(mockGlobalSession, false, true));

        verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.CommitRetryTimeout);
        verify(mockGlobalSession).end();
    }

    @Test
    @DisplayName("test endRollbacked with retry")
    void testEndRollbackedWithRetry() throws TransactionException {
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Rollbacking);
        when(mockGlobalSession.isSaga()).thenReturn(false);

        assertDoesNotThrow(() -> SessionHelper.endRollbacked(mockGlobalSession, true));

        verify(mockGlobalSession).end();
    }

    @Test
    @DisplayName("test endRollbackFailed")
    void testEndRollbackFailed() throws TransactionException {
        assertDoesNotThrow(() -> SessionHelper.endRollbackFailed(mockGlobalSession, false));

        verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.RollbackFailed);
        verify(mockGlobalSession).end();
    }

    @Test
    @DisplayName("test parallelForEach with empty collection")
    void testParallelForEachWithEmptyCollection() {
        assertDoesNotThrow(() -> SessionHelper.parallelForEach(Collections.emptyList(), mockGlobalSessionHandler));
    }

    @Test
    @DisplayName("test singleForEach with empty collection")
    void testSingleForEachWithEmptyCollection() {
        assertDoesNotThrow(() -> SessionHelper.singleForEach(Collections.emptyList(), mockGlobalSessionHandler));
    }

    @Test
    @DisplayName("test forEach with null collection")
    void testForEachWithNullCollection() {
        assertDoesNotThrow(() -> SessionHelper.forEach(null, mockGlobalSessionHandler));
    }

    @Test
    @DisplayName("test forEach with branch sessions and empty collection")
    void testForEachWithBranchSessionsAndEmptyCollection() throws TransactionException {
        Boolean result = SessionHelper.forEach(Collections.emptyList(), mockBranchSessionHandler);
        assertNull(result);
    }

    @Test
    @DisplayName("test singleForEach with branch sessions")
    void testSingleForEachWithBranchSessions() throws TransactionException {
        Collection<BranchSession> branches = new ArrayList<>();
        branches.add(mockBranchSession);

        when(mockBranchSessionHandler.handle(mockBranchSession)).thenReturn(null);

        Boolean result = SessionHelper.singleForEach(branches, mockBranchSessionHandler);
        assertNull(result);
        verify(mockBranchSessionHandler).handle(mockBranchSession);
    }

    @Test
    @DisplayName("test processEndState with Committed status")
    void testProcessEndStateWithCommittedStatus() throws TransactionException {
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committed);

        assertDoesNotThrow(() -> SessionHelper.processEndState(mockGlobalSession));

        verify(mockGlobalSession, atLeastOnce()).end();
    }

    @Test
    @DisplayName("test processEndState with Rollbacked status")
    void testProcessEndStateWithRollbackedStatus() throws TransactionException {
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Rollbacked);
        when(mockGlobalSession.isSaga()).thenReturn(false);

        assertDoesNotThrow(() -> SessionHelper.processEndState(mockGlobalSession));

        verify(mockGlobalSession, atLeastOnce()).end();
    }

    @Test
    @DisplayName("test processEndState with invalid status")
    void testProcessEndStateWithInvalidStatus() {
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);

        assertThrows(TransactionException.class, () -> SessionHelper.processEndState(mockGlobalSession));
    }

    @Test
    @DisplayName("test newBranch with all required parameters")
    void testNewBranchWithAllRequiredParameters() {
        BranchSession branch = SessionHelper.newBranch(BranchType.SAGA, "xid123", 999L, "resourceId", "appData");

        assertEquals("xid123", branch.getXid());
        assertEquals(999L, branch.getBranchId());
        assertEquals(BranchType.SAGA, branch.getBranchType());
        assertEquals("resourceId", branch.getResourceId());
        assertEquals("appData", branch.getApplicationData());
    }
}
