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

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.exception.ConsoleException;
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

public class AbstractGlobalServiceTest extends BaseSpringBootTest {

    private TestAbstractGlobalService service;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession1;
    private BranchSession mockBranchSession2;

    private static class TestAbstractGlobalService extends AbstractGlobalService {
        @Override
        public org.apache.seata.common.result.PageResult<org.apache.seata.server.console.entity.vo.GlobalSessionVO>
                query(org.apache.seata.server.console.entity.param.GlobalSessionParam param) {
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        service = new TestAbstractGlobalService();
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession1 = mock(BranchSession.class);
        mockBranchSession2 = mock(BranchSession.class);
    }

    @Test
    public void testDeleteGlobalSession_WithUnsupportedStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.deleteGlobalSession("192.168.1.1:8091:123456"));
            Assertions.assertEquals("current global transaction status is not support deleted", exception.getMessage());
        }
    }

    @Test
    public void testDeleteGlobalSession_WithCommitFailedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.CommitFailed, false);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.deleteGlobalSession("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testDeleteGlobalSession_WithRetryStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.CommitRetrying, false);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.deleteGlobalSession("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testDeleteGlobalSession_WithFinishedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.Finished, false);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.deleteGlobalSession("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testDeleteGlobalSession_WithException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.CommitFailed, false);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeGlobalStatus(GlobalStatus.Deleting);

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.deleteGlobalSession("192.168.1.1:8091:123456"));
            Assertions.assertTrue(exception.getLogMessage().contains("delete global session fail"));
        }
    }

    @Test
    public void testForceDeleteGlobalSession_WorksRegardlessOfStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.Begin, false);

            // Force delete should work regardless of status
            Assertions.assertDoesNotThrow(() -> service.forceDeleteGlobalSession("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testForceDeleteGlobalSession_WithException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupDeleteMocks(sessionHolderMock, GlobalStatus.Begin, false);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeGlobalStatus(GlobalStatus.Deleting);

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.forceDeleteGlobalSession("192.168.1.1:8091:123456"));
            Assertions.assertTrue(exception.getLogMessage().contains("force delete global session fail"));
        }
    }

    @Test
    public void testStopGlobalRetry_WithCommitRetryingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);

            SingleResult<Void> result = service.stopGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.StopCommitOrCommitRetry);
        }
    }

    @Test
    public void testStopGlobalRetry_WithCommittingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);

            SingleResult<Void> result = service.stopGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.StopCommitOrCommitRetry);
        }
    }

    @Test
    public void testStopGlobalRetry_WithRollbackRetryingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.RollbackRetrying);

            SingleResult<Void> result = service.stopGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.StopRollbackOrRollbackRetry);
        }
    }

    @Test
    public void testStopGlobalRetry_WithRollbackingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Rollbacking);

            SingleResult<Void> result = service.stopGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.StopRollbackOrRollbackRetry);
        }
    }

    @Test
    public void testStopGlobalRetry_WithInvalidStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.stopGlobalRetry("192.168.1.1:8091:123456"));
            Assertions.assertEquals("current global transaction status is not support stop", exception.getMessage());
        }
    }

    @Test
    public void testStopGlobalRetry_WithException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeGlobalStatus(GlobalStatus.StopCommitOrCommitRetry);

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.stopGlobalRetry("192.168.1.1:8091:123456"));
            Assertions.assertTrue(exception.getLogMessage().contains("Stop global session retry fail"));
        }
    }

    @Test
    public void testStartGlobalRetry_WithStopCommitOrCommitRetryStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.StopCommitOrCommitRetry);

            SingleResult<Void> result = service.startGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.CommitRetrying);
        }
    }

    @Test
    public void testStartGlobalRetry_WithStopRollbackOrRollbackRetryStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.StopRollbackOrRollbackRetry);

            SingleResult<Void> result = service.startGlobalRetry("192.168.1.1:8091:123456");

            Assertions.assertTrue(result.isSuccess());
            verify(mockGlobalSession).changeGlobalStatus(GlobalStatus.RollbackRetrying);
        }
    }

    @Test
    public void testStartGlobalRetry_WithInvalidStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.startGlobalRetry("192.168.1.1:8091:123456"));
            Assertions.assertEquals("current global transaction status is not support start", exception.getMessage());
        }
    }

    @Test
    public void testStartGlobalRetry_WithException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.StopCommitOrCommitRetry);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeGlobalStatus(GlobalStatus.CommitRetrying);

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.startGlobalRetry("192.168.1.1:8091:123456"));
            Assertions.assertTrue(exception.getLogMessage().contains("Start global session retry fail"));
        }
    }

    @Test
    public void testSendCommitOrRollback_WithCommitRetryingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.sendCommitOrRollback("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testSendCommitOrRollback_WithCommittingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.sendCommitOrRollback("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testSendCommitOrRollback_WithRollbackRetryingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.RollbackRetrying);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.sendCommitOrRollback("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testSendCommitOrRollback_WithRollbackingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Rollbacking);

            // Test that it doesn't throw IllegalArgumentException
            Assertions.assertDoesNotThrow(() -> service.sendCommitOrRollback("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testSendCommitOrRollback_WithUnsupportedStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            // Should throw IllegalArgumentException wrapped in ConsoleException
            Assertions.assertThrows(Exception.class, () -> service.sendCommitOrRollback("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithCommitFailedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitFailed);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithCommitRetryTimeoutStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetryTimeout);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithRollbackFailedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.RollbackFailed);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithTimeoutRollbackedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.TimeoutRollbacked);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithRollbackRetryTimeoutStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.RollbackRetryTimeout);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
        }
    }

    @Test
    public void testChangeGlobalStatus_WithUnsupportedStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.changeGlobalStatus("192.168.1.1:8091:123456"));
            Assertions.assertEquals(
                    "current global transaction status is not support to change", exception.getMessage());
        }
    }

    private void setupDeleteMocks(
            MockedStatic<SessionHolder> sessionHolderMock, GlobalStatus status, boolean isDeleting) {
        // Use empty branch sessions list to simplify testing - we're only testing status validation
        List<BranchSession> branchSessions = new ArrayList<>();

        sessionHolderMock
                .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                .thenReturn(mockGlobalSession);
        when(mockGlobalSession.getStatus()).thenReturn(status);
        when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);
    }
}
