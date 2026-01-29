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

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.RmTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

class DataSourceManagerTest {

    private static final String MOCKED_RESOURCE_ID = "mockedResourceId";
    private static final String MOCKED_XID = "mockedXid";
    private static final String MOCKED_LOCK_KEYS = "mockedLockKeys";
    private static final long MOCKED_BRANCH_ID = 12345L;

    private DataSourceManager dataSourceManager;

    @BeforeEach
    public void beforeEach() {
        dataSourceManager = new DataSourceManager();
    }

    @AfterEach
    public void afterEach() {
        // Clean up the global transaction context after each test
        RootContext.unbind();
    }

    @Test
    public void shouldThrowExceptionWhenLockQueryNotInGlobalTransaction() {
        Throwable actualException = assertThrows(RmTransactionException.class, () -> {
            dataSourceManager.lockQuery(BranchType.AT, MOCKED_RESOURCE_ID, MOCKED_XID, MOCKED_LOCK_KEYS);
        });

        assertEquals("Runtime", actualException.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenLockQueryRequireGlobalLockIsFalse() {
        // Simulate being in a global transaction
        RootContext.bind(MOCKED_XID);

        Throwable actualException = assertThrows(RmTransactionException.class, () -> {
            dataSourceManager.lockQuery(BranchType.AT, MOCKED_RESOURCE_ID, MOCKED_XID, MOCKED_LOCK_KEYS);
        });

        assertEquals("Runtime", actualException.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenLockQueryTimeout() throws TimeoutException {
        try (MockedStatic<RmNettyRemotingClient> rmNettyRemotingClientMockedStatic = mockStatic(RmNettyRemotingClient.class)) {
            // Simulate being in a global transaction
            RootContext.bind(MOCKED_XID);
            RmNettyRemotingClient mockedRmNettyRemotingClient = mock(RmNettyRemotingClient.class);
            rmNettyRemotingClientMockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockedRmNettyRemotingClient);
            when(mockedRmNettyRemotingClient.sendSyncRequest(any())).thenThrow(TimeoutException.class);

            Throwable actualException = assertThrows(RmTransactionException.class, () -> {
                dataSourceManager.lockQuery(BranchType.AT, MOCKED_RESOURCE_ID, MOCKED_XID, MOCKED_LOCK_KEYS);
            });

            assertEquals("RPC Timeout", actualException.getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenLockQueryWithFailedResponseCode() throws TimeoutException {
        try (MockedStatic<RmNettyRemotingClient> rmNettyRemotingClientMockedStatic = mockStatic(RmNettyRemotingClient.class)) {
            // Simulate being in a global transaction
            RootContext.bind(MOCKED_XID);
            RmNettyRemotingClient mockedRmNettyRemotingClient = mock(RmNettyRemotingClient.class);
            rmNettyRemotingClientMockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockedRmNettyRemotingClient);
            when(mockedRmNettyRemotingClient.sendSyncRequest(any())).thenReturn(new GlobalLockQueryResponse() {
                {
                    setResultCode(ResultCode.Failed);
                    setMsg("Mocked failure");
                }
            });

            Throwable actualException = assertThrows(TransactionException.class, () -> {
                dataSourceManager.lockQuery(BranchType.AT, MOCKED_RESOURCE_ID, MOCKED_XID, MOCKED_LOCK_KEYS);
            });

            assertEquals("Response[Mocked failure]", actualException.getMessage());
        }
    }

    @Test
    public void shouldReturnLockableWhenLockQuerySuccessful() throws TimeoutException, TransactionException {
        try (MockedStatic<RmNettyRemotingClient> rmNettyRemotingClientMockedStatic = mockStatic(RmNettyRemotingClient.class)) {
            // Simulate being in a global transaction
            RootContext.bind(MOCKED_XID);
            RmNettyRemotingClient mockedRmNettyRemotingClient = mock(RmNettyRemotingClient.class);
            rmNettyRemotingClientMockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockedRmNettyRemotingClient);
            when(mockedRmNettyRemotingClient.sendSyncRequest(any())).thenReturn(new GlobalLockQueryResponse() {
                {
                    setResultCode(ResultCode.Success);
                    setLockable(true);
                }
            });

            boolean actualLockable = dataSourceManager.lockQuery(BranchType.AT, MOCKED_RESOURCE_ID, MOCKED_XID, MOCKED_LOCK_KEYS);

            assertTrue(actualLockable);
        }
    }

    @Test
    public void shouldThrowExceptionWhenUnregisterResource() {
        Throwable actualException = assertThrows(NotSupportYetException.class, () -> {
            dataSourceManager.unregisterResource(null);
        });

        assertEquals("unregister a resource", actualException.getMessage());
    }

    @Test
    public void shouldRegisterAndGetDataSourceProxy() {
        DataSourceProxy mockedDataSourceProxy = mock(DataSourceProxy.class);
        when(mockedDataSourceProxy.getResourceId()).thenReturn(MOCKED_RESOURCE_ID);

        dataSourceManager.registerResource(mockedDataSourceProxy);

        DataSourceProxy actualDataSourceProxy = dataSourceManager.get(MOCKED_RESOURCE_ID);

        assertEquals(mockedDataSourceProxy, actualDataSourceProxy);
    }

    @Test
    public void shouldThrowExceptionWhenBranchRollbackWithoutDataSourceProxy() {
        Throwable actualException = assertThrows(ShouldNeverHappenException.class, () -> {
            dataSourceManager.branchRollback(BranchType.AT, MOCKED_XID, MOCKED_BRANCH_ID, MOCKED_RESOURCE_ID, "whatever");
        });

        assertEquals("resource: mockedResourceId not found", actualException.getMessage());
    }

    @Test
    public void shouldReturnUnretriableWhenBranchCommit() throws TransactionException {
        try (MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = mockStatic(UndoLogManagerFactory.class)) {
            UndoLogManager mockedUndoLogManager = mock(UndoLogManager.class);
            DataSourceProxy mockedDataSourceProxy = mock(DataSourceProxy.class);
            TransactionException mockedTransactionException = new TransactionException(TransactionExceptionCode.BranchRollbackFailed_Unretriable, "Mocked undo failure");

            when(mockedDataSourceProxy.getResourceId()).thenReturn(MOCKED_RESOURCE_ID);
            undoLogManagerFactoryMockedStatic.when(() -> UndoLogManagerFactory.getUndoLogManager(any())).thenReturn(mockedUndoLogManager);
            doThrow(mockedTransactionException).when(mockedUndoLogManager).undo(any(), anyString(), anyLong());

            dataSourceManager.registerResource(mockedDataSourceProxy);
            BranchStatus actualBranchStatus = dataSourceManager.branchRollback(BranchType.AT, MOCKED_XID, MOCKED_BRANCH_ID, MOCKED_RESOURCE_ID, null);

            assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Unretryable, actualBranchStatus);
        }
    }

    @Test
    public void shouldReturnRetryableWhenBranchCommit() throws TransactionException {
        try (MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = mockStatic(UndoLogManagerFactory.class)) {
            UndoLogManager mockedUndoLogManager = mock(UndoLogManager.class);
            DataSourceProxy mockedDataSourceProxy = mock(DataSourceProxy.class);

            when(mockedDataSourceProxy.getResourceId()).thenReturn(MOCKED_RESOURCE_ID);
            undoLogManagerFactoryMockedStatic.when(() -> UndoLogManagerFactory.getUndoLogManager(any())).thenReturn(mockedUndoLogManager);
            doThrow(TransactionException.class).when(mockedUndoLogManager).undo(any(), anyString(), anyLong());

            dataSourceManager.registerResource(mockedDataSourceProxy);
            BranchStatus actualBranchStatus = dataSourceManager.branchRollback(BranchType.AT, MOCKED_XID, MOCKED_BRANCH_ID, MOCKED_RESOURCE_ID, null);

            assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, actualBranchStatus);
        }
    }

    @Test
    public void shouldReturnRollbackWhenBranchCommit() throws TransactionException {
        try (MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = mockStatic(UndoLogManagerFactory.class)) {
            UndoLogManager mockedUndoLogManager = mock(UndoLogManager.class);
            DataSourceProxy mockedDataSourceProxy = mock(DataSourceProxy.class);

            when(mockedDataSourceProxy.getResourceId()).thenReturn(MOCKED_RESOURCE_ID);
            undoLogManagerFactoryMockedStatic.when(() -> UndoLogManagerFactory.getUndoLogManager(any())).thenReturn(mockedUndoLogManager);

            dataSourceManager.registerResource(mockedDataSourceProxy);
            BranchStatus actualBranchStatus = dataSourceManager.branchRollback(BranchType.AT, MOCKED_XID, MOCKED_BRANCH_ID, MOCKED_RESOURCE_ID, null);

            assertEquals(BranchStatus.PhaseTwo_Rollbacked, actualBranchStatus);
        }
    }

    @Test
    public void shouldGetDataSourceCacheViaReflection() throws NoSuchFieldException, IllegalAccessException {
        Map<String, Resource> expectedDataSourceCache = ReflectionUtil.getFieldValue(dataSourceManager, "dataSourceCache");

        Map<String, Resource> actualDataSourceCache = dataSourceManager.getManagedResources();

        assertSame(expectedDataSourceCache, actualDataSourceCache);
    }
    
    @Test
    public void shouldGetBranchType() {
        BranchType branchType = dataSourceManager.getBranchType();
        
        assertEquals(BranchType.AT, branchType);
    }
}