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
package org.apache.seata.rm.datasource;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataSourceManagerBehaviorTest {
    @Test
    void lockQueryMapsResponsesAndTransportErrors() throws Exception {
        RmNettyRemotingClient client = mock(RmNettyRemotingClient.class);
        GlobalLockQueryResponse response = new GlobalLockQueryResponse();
        response.setResultCode(ResultCode.Success);
        response.setLockable(true);
        when(client.sendSyncRequest(any(GlobalLockQueryRequest.class))).thenReturn(response);
        try (MockedConstruction<AsyncWorker> workers = mockConstruction(AsyncWorker.class);
                MockedStatic<RootContext> context = mockStatic(RootContext.class);
                MockedStatic<RmNettyRemotingClient> clients = mockStatic(RmNettyRemotingClient.class)) {
            clients.when(RmNettyRemotingClient::getInstance).thenReturn(client);
            context.when(RootContext::requireGlobalLock).thenReturn(true);
            DataSourceManager manager = new DataSourceManager();
            assertTrue(manager.lockQuery(BranchType.AT, "resource", "xid", "orders:1"));
            org.mockito.ArgumentCaptor<GlobalLockQueryRequest> request =
                    org.mockito.ArgumentCaptor.forClass(GlobalLockQueryRequest.class);
            verify(client).sendSyncRequest(request.capture());
            assertEquals("xid", request.getValue().getXid());
            assertEquals("resource", request.getValue().getResourceId());
            assertEquals("orders:1", request.getValue().getLockKey());
            response.setLockable(false);
            assertFalse(manager.lockQuery(BranchType.AT, "resource", "xid", "orders:1"));
            response.setResultCode(ResultCode.Failed);
            response.setTransactionExceptionCode(TransactionExceptionCode.LockKeyConflict);
            assertEquals(
                    TransactionExceptionCode.LockKeyConflict,
                    assertThrows(
                                    TransactionException.class,
                                    () -> manager.lockQuery(BranchType.AT, "resource", "xid", "orders:1"))
                            .getCode());
            doThrow(new TimeoutException("timeout")).when(client).sendSyncRequest(any(GlobalLockQueryRequest.class));
            assertEquals(
                    TransactionExceptionCode.IO,
                    assertThrows(
                                    TransactionException.class,
                                    () -> manager.lockQuery(BranchType.AT, "resource", "xid", "orders:1"))
                            .getCode());
            context.when(RootContext::requireGlobalLock).thenReturn(false);
            assertEquals(
                    TransactionExceptionCode.LockableCheckFailed,
                    assertThrows(
                                    TransactionException.class,
                                    () -> manager.lockQuery(BranchType.AT, "resource", "xid", "orders:1"))
                            .getCode());
        }
    }

    @Test
    void rollbackReportsRetryabilityAndUnregistersResources() throws Exception {
        RmNettyRemotingClient client = mock(RmNettyRemotingClient.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        when(source.getResourceId()).thenReturn("resource");
        when(source.getResourceGroupId()).thenReturn("group");
        when(source.getDbType()).thenReturn("mysql");
        UndoLogManager undo = mock(UndoLogManager.class);
        try (MockedConstruction<AsyncWorker> workers = mockConstruction(AsyncWorker.class);
                MockedStatic<UndoLogManagerFactory> logs = mockStatic(UndoLogManagerFactory.class);
                MockedStatic<RmNettyRemotingClient> clients = mockStatic(RmNettyRemotingClient.class)) {
            logs.when(() -> UndoLogManagerFactory.getUndoLogManager("mysql")).thenReturn(undo);
            clients.when(RmNettyRemotingClient::getInstance).thenReturn(client);
            DataSourceManager manager = new DataSourceManager();
            assertThrows(
                    ShouldNeverHappenException.class,
                    () -> manager.branchRollback(BranchType.AT, "xid", 42, "missing", null));
            manager.getManagedResources().put("resource", source);
            assertEquals(
                    BranchStatus.PhaseTwo_Rollbacked,
                    manager.branchRollback(BranchType.AT, "xid", 42, "resource", null));
            doThrow(new TransactionException(TransactionExceptionCode.BranchRollbackFailed_Unretriable))
                    .when(undo)
                    .undo(source, "xid", 42);
            assertEquals(
                    BranchStatus.PhaseTwo_RollbackFailed_Unretryable,
                    manager.branchRollback(BranchType.AT, "xid", 42, "resource", null));
            doThrow(new TransactionException(TransactionExceptionCode.BranchRollbackFailed_Retriable))
                    .when(undo)
                    .undo(source, "xid", 42);
            assertEquals(
                    BranchStatus.PhaseTwo_RollbackFailed_Retryable,
                    manager.branchRollback(BranchType.AT, "xid", 42, "resource", null));
            manager.unregisterResource(source);
            assertNull(manager.get("resource"));
            verify(client).unregisterResource("group", "resource");
            when(workers.constructed().get(0).branchCommit("xid", 42, "resource"))
                    .thenReturn(BranchStatus.PhaseTwo_Committed);
            assertEquals(
                    BranchStatus.PhaseTwo_Committed, manager.branchCommit(BranchType.AT, "xid", 42, "resource", null));
        }
    }
}
