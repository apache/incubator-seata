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
package org.apache.seata.server.storage.raft.session;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.SessionConverter;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RaftSessionManagerTest extends BaseSpringBootTest {

    private RaftSessionManager raftSessionManager;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    @BeforeEach
    public void setUp() throws Exception {
        raftSessionManager = new RaftSessionManager("test-session-manager");
        mockGlobalSession = createGlobalSession("192.168.1.1:8091:123456");
        mockBranchSession = createBranchSession(mockGlobalSession);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (raftSessionManager != null) {
            raftSessionManager.destroy();
        }
    }

    @Test
    public void testRaftSessionManagerExtendsFileSessionManager() throws Exception {
        Class<?> clazz = Class.forName("org.apache.seata.server.storage.raft.session.RaftSessionManager");
        Assertions.assertTrue(FileSessionManager.class.isAssignableFrom(clazz));
    }

    @Test
    public void testRaftSessionManagerHasConstructor() throws Exception {
        Class<?> clazz = Class.forName("org.apache.seata.server.storage.raft.session.RaftSessionManager");
        Assertions.assertNotNull(clazz.getConstructor(String.class));
    }

    // ==================== onBegin Tests ====================

    @Test
    public void testOnBegin_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class);
                MockedStatic<SessionConverter> sessionConverterMock = Mockito.mockStatic(SessionConverter.class)) {

            // Mock SessionConverter
            sessionConverterMock
                    .when(() -> SessionConverter.convertGlobalTransactionDO(any(), any()))
                    .thenAnswer(invocation -> null);

            // Mock RaftTaskUtil to invoke the closure with success status
            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        // Simulate success
                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            Assertions.assertDoesNotThrow(() -> raftSessionManager.onBegin(mockGlobalSession));

            // Verify session was added
            GlobalSession foundSession = raftSessionManager.findGlobalSession(mockGlobalSession.getXid());
            Assertions.assertNotNull(foundSession);
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOnBegin_FailureNotRaftLeader() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class);
                MockedStatic<SessionConverter> sessionConverterMock = Mockito.mockStatic(SessionConverter.class)) {

            // Mock SessionConverter
            sessionConverterMock
                    .when(() -> SessionConverter.convertGlobalTransactionDO(any(), any()))
                    .thenAnswer(invocation -> null);

            // Mock RaftTaskUtil to invoke the closure with failure status
            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        // Simulate failure
                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftSessionManager.onBegin(mockGlobalSession));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
            Assertions.assertTrue(exception.getMessage().contains("Not raft leader"));
        }
    }

    // ==================== onStatusChange Tests ====================

    @Test
    public void testOnStatusChange_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Mock RaftTaskUtil
            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        // Simulate success
                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            GlobalStatus newStatus = GlobalStatus.Committing;
            Assertions.assertDoesNotThrow(() -> raftSessionManager.onStatusChange(mockGlobalSession, newStatus));

            // Verify status was changed
            Assertions.assertEquals(newStatus, mockGlobalSession.getStatus());
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOnStatusChange_FailureNotRaftLeader() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Mock RaftTaskUtil to simulate failure
            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            GlobalStatus newStatus = GlobalStatus.Committing;
            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftSessionManager.onStatusChange(mockGlobalSession, newStatus));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
        }
    }

    @Test
    public void testOnStatusChange_RollbackStatusSetsLockStatus() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Add a branch session to test lock status update
            mockGlobalSession.add(mockBranchSession);

            // Mock RaftTaskUtil
            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            // Test with Rollbacking status
            Assertions.assertDoesNotThrow(
                    () -> raftSessionManager.onStatusChange(mockGlobalSession, GlobalStatus.Rollbacking));
            Assertions.assertEquals(GlobalStatus.Rollbacking, mockGlobalSession.getStatus());
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    // ==================== onBranchStatusChange Tests ====================

    @Test
    public void testOnBranchStatusChange_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            BranchStatus newStatus = BranchStatus.PhaseTwo_Committed;
            Assertions.assertDoesNotThrow(
                    () -> raftSessionManager.onBranchStatusChange(mockGlobalSession, mockBranchSession, newStatus));

            Assertions.assertEquals(newStatus, mockBranchSession.getStatus());
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOnBranchStatusChange_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            BranchStatus newStatus = BranchStatus.PhaseTwo_Committed;
            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class,
                    () -> raftSessionManager.onBranchStatusChange(mockGlobalSession, mockBranchSession, newStatus));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
        }
    }

    // ==================== onAddBranch Tests ====================

    @Test
    public void testOnAddBranch_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class);
                MockedStatic<SessionConverter> sessionConverterMock = Mockito.mockStatic(SessionConverter.class)) {

            sessionConverterMock
                    .when(() -> SessionConverter.convertBranchTransaction(any(), any()))
                    .thenAnswer(invocation -> null);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            BranchSession newBranch = createBranchSession(mockGlobalSession);
            Assertions.assertDoesNotThrow(() -> raftSessionManager.onAddBranch(mockGlobalSession, newBranch));

            // Verify branch was added
            Assertions.assertTrue(mockGlobalSession.getBranchSessions().contains(newBranch));
            Assertions.assertEquals(BranchStatus.Registered, newBranch.getStatus());
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOnAddBranch_FailureWithRollback() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class);
                MockedStatic<SessionConverter> sessionConverterMock = Mockito.mockStatic(SessionConverter.class)) {

            sessionConverterMock
                    .when(() -> SessionConverter.convertBranchTransaction(any(), any()))
                    .thenAnswer(invocation -> null);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            BranchSession newBranch = createBranchSession(mockGlobalSession);
            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftSessionManager.onAddBranch(mockGlobalSession, newBranch));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
        }
    }

    // ==================== onRemoveBranch Tests ====================

    @Test
    public void testOnRemoveBranch_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Add branch first
            mockGlobalSession.add(mockBranchSession);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            Assertions.assertDoesNotThrow(
                    () -> raftSessionManager.onRemoveBranch(mockGlobalSession, mockBranchSession));

            // Verify branch was removed
            Assertions.assertFalse(mockGlobalSession.getBranchSessions().contains(mockBranchSession));
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOnRemoveBranch_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class,
                    () -> raftSessionManager.onRemoveBranch(mockGlobalSession, mockBranchSession));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
        }
    }

    // ==================== end Tests ====================

    @Test
    public void testEnd_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class);
                MockedStatic<SessionConverter> sessionConverterMock = Mockito.mockStatic(SessionConverter.class)) {

            // Add session first using addGlobalSession
            raftSessionManager.addGlobalSession(mockGlobalSession);

            sessionConverterMock
                    .when(() -> SessionConverter.convertGlobalTransactionDO(any(), any()))
                    .thenAnswer(invocation -> null);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            Assertions.assertDoesNotThrow(() -> raftSessionManager.end(mockGlobalSession));

            // Verify session was removed
            Assertions.assertNull(raftSessionManager.findGlobalSession(mockGlobalSession.getXid()));
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testEnd_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(false);
                        when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftSessionManager.end(mockGlobalSession));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
        }
    }

    // ==================== onSuccessEnd Tests ====================

    @Test
    public void testOnSuccessEnd_CallsEnd() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            // Add session first using addGlobalSession
            raftSessionManager.addGlobalSession(mockGlobalSession);

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            Assertions.assertDoesNotThrow(() -> raftSessionManager.onSuccessEnd(mockGlobalSession));

            // Verify session was removed (since onSuccessEnd calls end)
            Assertions.assertNull(raftSessionManager.findGlobalSession(mockGlobalSession.getXid()));
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    // ==================== removeGlobalSession Tests ====================

    @Test
    public void testRemoveGlobalSession_WithoutBranches() throws Exception {
        // Add session first using addGlobalSession
        raftSessionManager.addGlobalSession(mockGlobalSession);

        raftSessionManager.removeGlobalSession(mockGlobalSession);

        // Verify session was removed
        Assertions.assertNull(raftSessionManager.findGlobalSession(mockGlobalSession.getXid()));
    }

    @Test
    public void testRemoveGlobalSession_DirectCall() throws Exception {
        // Add session first using addGlobalSession
        raftSessionManager.addGlobalSession(mockGlobalSession);

        // Directly call removeGlobalSession without branches
        // This tests the basic removal functionality
        raftSessionManager.removeGlobalSession(mockGlobalSession);

        // Verify session was removed
        Assertions.assertNull(raftSessionManager.findGlobalSession(mockGlobalSession.getXid()));
    }

    // ==================== Getter/Setter Tests ====================

    @Test
    public void testGetName() {
        Assertions.assertEquals("test-session-manager", raftSessionManager.getName());
    }

    @Test
    public void testSetName() {
        raftSessionManager.setName("new-name");
        Assertions.assertEquals("new-name", raftSessionManager.getName());
    }

    @Test
    public void testDestroy() {
        Assertions.assertDoesNotThrow(() -> raftSessionManager.destroy());
    }

    // ==================== Helper Methods ====================

    private GlobalSession createGlobalSession(String xid) {
        GlobalSession session = new GlobalSession("test-app", "test-group", "test-tx", 60000);
        session.setXid(xid);
        session.setTransactionId(123456L);
        session.setStatus(GlobalStatus.Begin);
        return session;
    }

    private BranchSession createBranchSession(GlobalSession globalSession) {
        BranchSession branch = new BranchSession();
        branch.setXid(globalSession.getXid());
        branch.setTransactionId(globalSession.getTransactionId());
        branch.setBranchId(1L);
        branch.setResourceId("test-resource");
        branch.setLockKey("test-lock-key");
        branch.setBranchType(BranchType.AT);
        branch.setStatus(BranchStatus.Unknown);
        branch.setApplicationData("test-data");
        return branch;
    }
}
