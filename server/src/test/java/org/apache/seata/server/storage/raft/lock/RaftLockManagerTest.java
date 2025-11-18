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
package org.apache.seata.server.storage.raft.lock;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import org.apache.seata.common.XID;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.util.RaftTaskUtil;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class RaftLockManagerTest extends BaseSpringBootTest {

    private RaftLockManager raftLockManager;

    @BeforeEach
    public void setUp() {
        raftLockManager = new RaftLockManager();
    }

    @Test
    public void testLocalReleaseGlobalSessionLock() throws TransactionException {
        GlobalSession globalSession = createGlobalSession();
        boolean result = raftLockManager.localReleaseGlobalSessionLock(globalSession);
        Assertions.assertTrue(result);
    }

    @Test
    public void testLocalReleaseLock() throws TransactionException {
        BranchSession branchSession = createBranchSession();
        boolean result = raftLockManager.localReleaseLock(branchSession);
        Assertions.assertTrue(result);
    }

    @Test
    public void testLocalReleaseGlobalSessionLockWithNull() throws TransactionException {
        GlobalSession globalSession = createGlobalSession();
        globalSession.setXid(null);
        boolean result = raftLockManager.localReleaseGlobalSessionLock(globalSession);
        Assertions.assertTrue(result);
    }

    @Test
    public void testLocalReleaseLockWithInvalidBranchSession() throws TransactionException {
        BranchSession branchSession = createBranchSession();
        branchSession.setXid(null);
        boolean result = raftLockManager.localReleaseLock(branchSession);
        Assertions.assertTrue(result);
    }

    private GlobalSession createGlobalSession() {
        GlobalSession session = GlobalSession.createGlobalSession("test-app", "test-group", "test-tx", 60000);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setStatus(GlobalStatus.Begin);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("test-data");
        return session;
    }

    private BranchSession createBranchSession() {
        BranchSession branchSession = new BranchSession();
        String xid = XID.generateXID(12345L);
        branchSession.setXid(xid);
        branchSession.setTransactionId(12345L);
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("test-group");
        branchSession.setResourceId("test-resource");
        branchSession.setLockKey("test:1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setClientId("test-client:127.0.0.1:8080");
        branchSession.setApplicationData("test-branch-data");
        return branchSession;
    }

    // ==================== Raft Consensus Methods Tests ====================

    @Test
    public void testReleaseGlobalSessionLock_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            GlobalSession globalSession = createGlobalSession();
            boolean result = raftLockManager.releaseGlobalSessionLock(globalSession);

            Assertions.assertTrue(result);
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testReleaseGlobalSessionLock_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(false);
                        Mockito.when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            GlobalSession globalSession = createGlobalSession();
            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftLockManager.releaseGlobalSessionLock(globalSession));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
            Assertions.assertTrue(exception.getMessage().contains("Not raft leader"));
        }
    }

    @Test
    public void testReleaseGlobalSessionLock_WithNullXid() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            GlobalSession globalSession = createGlobalSession();
            globalSession.setXid(null);
            boolean result = raftLockManager.releaseGlobalSessionLock(globalSession);

            Assertions.assertTrue(result);
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testReleaseLock_Success() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            BranchSession branchSession = createBranchSession();
            boolean result = raftLockManager.releaseLock(branchSession);

            Assertions.assertTrue(result);
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testReleaseLock_Failure() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(false);
                        Mockito.when(status.getErrorMsg()).thenReturn("Not raft leader");
                        closure.run(status);

                        CompletableFuture<Boolean> future = invocation.getArgument(2);
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw e.getCause();
                        }
                    });

            BranchSession branchSession = createBranchSession();
            TransactionException exception = Assertions.assertThrows(
                    TransactionException.class, () -> raftLockManager.releaseLock(branchSession));

            Assertions.assertEquals(TransactionExceptionCode.NotRaftLeader, exception.getCode());
            Assertions.assertTrue(exception.getMessage().contains("Not raft leader"));
        }
    }

    @Test
    public void testReleaseLock_WithNullXid() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenAnswer(invocation -> {
                        Closure closure = invocation.getArgument(0);
                        CompletableFuture<Boolean> future = invocation.getArgument(2);

                        Status status = mock(Status.class);
                        Mockito.when(status.isOk()).thenReturn(true);
                        closure.run(status);

                        return future.get();
                    });

            BranchSession branchSession = createBranchSession();
            branchSession.setXid(null);
            boolean result = raftLockManager.releaseLock(branchSession);

            Assertions.assertTrue(result);
        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testReleaseLock_Exception() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenThrow(new RuntimeException("Raft error"));

            BranchSession branchSession = createBranchSession();
            Assertions.assertThrows(RuntimeException.class, () -> raftLockManager.releaseLock(branchSession));
        }
    }

    @Test
    public void testReleaseGlobalSessionLock_Exception() {
        try (MockedStatic<RaftTaskUtil> raftTaskUtilMock = Mockito.mockStatic(RaftTaskUtil.class)) {

            raftTaskUtilMock
                    .when(() -> RaftTaskUtil.createTask(any(Closure.class), any(), any(CompletableFuture.class)))
                    .thenThrow(new RuntimeException("Raft error"));

            GlobalSession globalSession = createGlobalSession();
            Assertions.assertThrows(
                    RuntimeException.class, () -> raftLockManager.releaseGlobalSessionLock(globalSession));
        }
    }
}
