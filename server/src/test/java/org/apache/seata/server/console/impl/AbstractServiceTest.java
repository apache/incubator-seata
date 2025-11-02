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
package org.apache.seata.server.console.impl;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class AbstractServiceTest extends BaseSpringBootTest {

    private TestAbstractService service;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    private static class TestAbstractService extends AbstractService {}

    @BeforeEach
    public void setUp() {
        service = new TestAbstractService();
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession = mock(BranchSession.class);
    }

    @Test
    public void testCommonCheck_WithValidParams() {
        Assertions.assertDoesNotThrow(() -> service.commonCheck("192.168.1.1:8091:123456", "123"));
    }

    @Test
    public void testCommonCheck_WithBlankXid() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.commonCheck("", "123"));
        Assertions.assertEquals("Wrong parameter for xid", exception.getMessage());
    }

    @Test
    public void testCommonCheck_WithNullXid() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.commonCheck(null, "123"));
        Assertions.assertEquals("Wrong parameter for xid", exception.getMessage());
    }

    @Test
    public void testCommonCheck_WithBlankBranchId() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class, () -> service.commonCheck("192.168.1.1:8091:123456", ""));
        Assertions.assertEquals("Wrong parameter for branchId", exception.getMessage());
    }

    @Test
    public void testCommonCheck_WithNullBranchId() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class, () -> service.commonCheck("192.168.1.1:8091:123456", null));
        Assertions.assertEquals("Wrong parameter for branchId", exception.getMessage());
    }

    @Test
    public void testCommonCheck_WithNonNumericBranchId() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class, () -> service.commonCheck("192.168.1.1:8091:123456", "abc"));
        Assertions.assertEquals("Wrong parameter for branchId, branch Id is not number", exception.getMessage());
    }

    @Test
    public void testCheckGlobalSession_WithValidXid() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);

            GlobalSession result = service.checkGlobalSession("192.168.1.1:8091:123456");
            Assertions.assertNotNull(result);
            Assertions.assertEquals(mockGlobalSession, result);
        }
    }

    @Test
    public void testCheckGlobalSession_WithBlankXid() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkGlobalSession(""));
        Assertions.assertEquals("Wrong parameter for xid", exception.getMessage());
    }

    @Test
    public void testCheckGlobalSession_WithNullSession() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(null);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.checkGlobalSession("192.168.1.1:8091:123456"));
            Assertions.assertEquals("Global session is not exist, may be finished", exception.getMessage());
        }
    }

    @Test
    public void testCommonCheckAndGetGlobalStatus_WithValidParams() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            List<BranchSession> branchSessions = new ArrayList<>();
            branchSessions.add(mockBranchSession);
            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);

            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);

            AbstractService.CheckResult result =
                    service.commonCheckAndGetGlobalStatus("192.168.1.1:8091:123456", "123");
            Assertions.assertNotNull(result);
            Assertions.assertEquals(mockGlobalSession, result.getGlobalSession());
            Assertions.assertEquals(mockBranchSession, result.getBranchSession());
        }
    }

    @Test
    public void testCommonCheckAndGetGlobalStatus_WithNullGlobalSession() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(null);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.commonCheckAndGetGlobalStatus("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals("global session is not exist, may be finished", exception.getMessage());
        }
    }

    @Test
    public void testCommonCheckAndGetGlobalStatus_WithBranchNotFound() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            List<BranchSession> branchSessions = new ArrayList<>();
            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);

            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.commonCheckAndGetGlobalStatus("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals("branch session is not exist, may be finished", exception.getMessage());
        }
    }

    @Test
    public void testDoDeleteBranch_WithPhaseOneFailedStatus() throws TransactionException {
        when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseOne_Failed);
        when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
        when(mockBranchSession.getBranchId()).thenReturn(123L);

        boolean result = service.doDeleteBranch(mockGlobalSession, mockBranchSession);

        Assertions.assertTrue(result);
        verify(mockGlobalSession).removeBranch(mockBranchSession);
        verify(mockBranchSession, never()).unlock();
    }

    @Test
    public void testDoDeleteBranch_SuccessfulDelete() throws TransactionException {
        try (MockedStatic<DefaultCoordinator> coordinatorMock = Mockito.mockStatic(DefaultCoordinator.class)) {
            DefaultCoordinator mockCoordinator = mock(DefaultCoordinator.class);
            coordinatorMock.when(DefaultCoordinator::getInstance).thenReturn(mockCoordinator);

            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseTwo_Committed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            when(mockCoordinator.doBranchDelete(mockGlobalSession, mockBranchSession))
                    .thenReturn(true);
            when(mockBranchSession.unlock()).thenReturn(true);

            boolean result = service.doDeleteBranch(mockGlobalSession, mockBranchSession);

            Assertions.assertTrue(result);
            verify(mockBranchSession).unlock();
            verify(mockGlobalSession).removeBranch(mockBranchSession);
        }
    }

    @Test
    public void testDoDeleteBranch_FailedDelete_CoordinatorReturnsFalse() throws TransactionException {
        try (MockedStatic<DefaultCoordinator> coordinatorMock = Mockito.mockStatic(DefaultCoordinator.class)) {
            DefaultCoordinator mockCoordinator = mock(DefaultCoordinator.class);
            coordinatorMock.when(DefaultCoordinator::getInstance).thenReturn(mockCoordinator);

            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseTwo_Committed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            when(mockCoordinator.doBranchDelete(mockGlobalSession, mockBranchSession))
                    .thenReturn(false);

            boolean result = service.doDeleteBranch(mockGlobalSession, mockBranchSession);

            Assertions.assertFalse(result);
            verify(mockBranchSession, never()).unlock();
            verify(mockGlobalSession, never()).removeBranch(mockBranchSession);
        }
    }

    @Test
    public void testDoDeleteBranch_FailedDelete_UnlockReturnsFalse() throws TransactionException {
        try (MockedStatic<DefaultCoordinator> coordinatorMock = Mockito.mockStatic(DefaultCoordinator.class)) {
            DefaultCoordinator mockCoordinator = mock(DefaultCoordinator.class);
            coordinatorMock.when(DefaultCoordinator::getInstance).thenReturn(mockCoordinator);

            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseTwo_Committed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            when(mockCoordinator.doBranchDelete(mockGlobalSession, mockBranchSession))
                    .thenReturn(true);
            when(mockBranchSession.unlock()).thenReturn(false);

            boolean result = service.doDeleteBranch(mockGlobalSession, mockBranchSession);

            Assertions.assertFalse(result);
            verify(mockBranchSession).unlock();
            verify(mockGlobalSession, never()).removeBranch(mockBranchSession);
        }
    }

    @Test
    public void testDoForceDeleteBranch_AlwaysSucceeds() throws TransactionException {
        when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
        when(mockBranchSession.getBranchId()).thenReturn(123L);

        boolean result = service.doForceDeleteBranch(mockGlobalSession, mockBranchSession);

        Assertions.assertTrue(result);
        verify(mockGlobalSession).removeBranch(mockBranchSession);
    }

    @Test
    public void testDoRetryCommitGlobal() throws TransactionException {
        try (MockedStatic<DefaultCoordinator> coordinatorMock = Mockito.mockStatic(DefaultCoordinator.class)) {
            DefaultCoordinator mockCoordinator = mock(DefaultCoordinator.class);
            coordinatorMock.when(DefaultCoordinator::getInstance).thenReturn(mockCoordinator);
            when(mockCoordinator.doGlobalCommit(mockGlobalSession, true)).thenReturn(true);

            boolean result = service.doRetryCommitGlobal(mockGlobalSession);

            Assertions.assertTrue(result);
            verify(mockCoordinator).doGlobalCommit(mockGlobalSession, true);
        }
    }

    @Test
    public void testDoRetryRollbackGlobal() throws TransactionException {
        try (MockedStatic<DefaultCoordinator> coordinatorMock = Mockito.mockStatic(DefaultCoordinator.class)) {
            DefaultCoordinator mockCoordinator = mock(DefaultCoordinator.class);
            coordinatorMock.when(DefaultCoordinator::getInstance).thenReturn(mockCoordinator);
            when(mockCoordinator.doGlobalRollback(mockGlobalSession, true)).thenReturn(true);

            boolean result = service.doRetryRollbackGlobal(mockGlobalSession);

            Assertions.assertTrue(result);
            verify(mockCoordinator).doGlobalRollback(mockGlobalSession, true);
        }
    }

    @Test
    public void testCheckResult_GettersAndSetters() {
        AbstractService.CheckResult checkResult = new AbstractService.CheckResult(mockGlobalSession, mockBranchSession);

        Assertions.assertEquals(mockGlobalSession, checkResult.getGlobalSession());
        Assertions.assertEquals(mockBranchSession, checkResult.getBranchSession());

        GlobalSession newGlobalSession = mock(GlobalSession.class);
        BranchSession newBranchSession = mock(BranchSession.class);

        checkResult.setGlobalSession(newGlobalSession);
        checkResult.setBranchSession(newBranchSession);

        Assertions.assertEquals(newGlobalSession, checkResult.getGlobalSession());
        Assertions.assertEquals(newBranchSession, checkResult.getBranchSession());
    }
}
