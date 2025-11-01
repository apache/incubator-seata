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
import org.apache.seata.core.model.BranchStatus;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AbstractBranchServiceTest extends BaseSpringBootTest {

    private TestAbstractBranchService service;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    private static class TestAbstractBranchService extends AbstractBranchService {
        @Override
        public org.apache.seata.common.result.PageResult<org.apache.seata.server.console.entity.vo.BranchSessionVO>
                queryByXid(String xid) {
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        service = new TestAbstractBranchService();
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession = mock(BranchSession.class);
    }

    @Test
    public void testStopBranchRetry_WithSagaTransaction() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(true);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.stopBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "saga can not operate branch transactions because it have no determinative role",
                    exception.getMessage());
        }
    }

    @Test
    public void testStopBranchRetry_WithAlreadyStoppedStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.STOP_RETRY);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.stopBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals("current branch session is already stop", exception.getMessage());
        }
    }

    @Test
    public void testStopBranchRetry_WithUnsupportedStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseTwo_Committed);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.stopBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals("current branch session is not support to stop", exception.getMessage());
        }
    }

    @Test
    public void testStopBranchRetry_WithCommitRetryingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);

            SingleResult<Void> result = service.stopBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            verify(mockBranchSession).setStatus(BranchStatus.STOP_RETRY);
            verify(mockGlobalSession).changeBranchStatus(mockBranchSession, BranchStatus.STOP_RETRY);
        }
    }

    @Test
    public void testStopBranchRetry_WithRollbackingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Rollbacking);

            SingleResult<Void> result = service.stopBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            verify(mockBranchSession).setStatus(BranchStatus.STOP_RETRY);
        }
    }

    @Test
    public void testStopBranchRetry_WithCommittingStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);

            SingleResult<Void> result = service.stopBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testStopBranchRetry_WithStopRollbackOrRollbackRetryStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.StopRollbackOrRollbackRetry);

            SingleResult<Void> result = service.stopBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testStopBranchRetry_WithStopCommitOrCommitRetryStatus() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.StopCommitOrCommitRetry);

            SingleResult<Void> result = service.stopBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testStopBranchRetry_WithInvalidGlobalStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.stopBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals("wrong status for global status", exception.getMessage());
        }
    }

    @Test
    public void testStopBranchRetry_WithChangeBranchStatusException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeBranchStatus(eq(mockBranchSession), eq(BranchStatus.STOP_RETRY));

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.stopBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertTrue(exception.getLogMessage().contains("stop branch session retry fail"));
        }
    }

    @Test
    public void testStartBranchRetry_WithSagaTransaction() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(true);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.startBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "saga can not operate branch transactions because it have no determinative role",
                    exception.getMessage());
        }
    }

    @Test
    public void testStartBranchRetry_WithNonStopRetryStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.startBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "current branch transactions status is not support to start retry", exception.getMessage());
        }
    }

    @Test
    public void testStartBranchRetry_Success() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.STOP_RETRY);

            SingleResult<Void> result = service.startBranchRetry("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            verify(mockBranchSession).setStatus(BranchStatus.Registered);
            verify(mockGlobalSession).changeBranchStatus(mockBranchSession, BranchStatus.Registered);
        }
    }

    @Test
    public void testStartBranchRetry_WithException() throws Exception {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.STOP_RETRY);
            doThrow(new TransactionException("Test exception"))
                    .when(mockGlobalSession)
                    .changeBranchStatus(eq(mockBranchSession), eq(BranchStatus.Registered));

            ConsoleException exception = Assertions.assertThrows(
                    ConsoleException.class, () -> service.startBranchRetry("192.168.1.1:8091:123456", "123"));
            Assertions.assertTrue(exception.getLogMessage().contains("start branch session retry fail"));
        }
    }

    @Test
    public void testDeleteBranchSession_WithSagaTransaction() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(true);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "saga can not operate branch transactions because it have no determinative role",
                    exception.getMessage());
        }
    }

    @Test
    public void testDeleteBranchSession_WithUnsupportedGlobalStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "current global transaction is not support delete branch transaction", exception.getMessage());
        }
    }

    @Test
    public void testDeleteBranchSession_WithCommitFailedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseOne_Failed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitFailed);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
        }
    }

    @Test
    public void testDeleteBranchSession_WithCommitRetryingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseOne_Failed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.CommitRetrying);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
        }
    }

    @Test
    public void testDeleteBranchSession_WithFinishedStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseOne_Failed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Finished);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
        }
    }

    @Test
    public void testDeleteBranchSession_WithDeletingStatus_ValidatesStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockBranchSession.getStatus()).thenReturn(BranchStatus.PhaseOne_Failed);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.isSaga()).thenReturn(false);
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Deleting);

            // Test that it doesn't throw IllegalArgumentException for valid status
            Assertions.assertDoesNotThrow(() -> service.deleteBranchSession("192.168.1.1:8091:123456", "123"));
        }
    }

    @Test
    public void testForceDeleteBranchSession_WithSagaTransaction() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(true);

            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> service.forceDeleteBranchSession("192.168.1.1:8091:123456", "123"));
            Assertions.assertEquals(
                    "saga can not operate branch transactions because it have no determinative role",
                    exception.getMessage());
        }
    }

    @Test
    public void testForceDeleteBranchSession_DoesNotValidateStatus() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            setupMockForCommonCheck(sessionHolderMock);
            when(mockGlobalSession.isSaga()).thenReturn(false);

            // Force delete should work regardless of status (as opposed to regular delete)
            Assertions.assertDoesNotThrow(() -> service.forceDeleteBranchSession("192.168.1.1:8091:123456", "123"));
        }
    }

    private void setupMockForCommonCheck(MockedStatic<SessionHolder> sessionHolderMock) {
        when(mockBranchSession.getBranchId()).thenReturn(123L);
        List<BranchSession> branchSessions = new ArrayList<>();
        branchSessions.add(mockBranchSession);
        when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);
        sessionHolderMock
                .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                .thenReturn(mockGlobalSession);
    }
}
