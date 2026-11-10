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

import org.apache.seata.core.context.GlobalLockConfigHolder;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.exec.LockConflictException;
import org.apache.seata.rm.datasource.exec.LockRetryController;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConnectionProxyBehaviorTest {
    private Connection target;
    private ConnectionProxy proxy;
    private DefaultResourceManager resourceManager;
    private UndoLogManager undoManager;
    private MockedStatic<DefaultResourceManager> resources;
    private MockedStatic<UndoLogManagerFactory> undoFactory;
    private GlobalLockConfig previousLockConfig;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProxy dataSource = mock(DataSourceProxy.class);
        when(dataSource.getResourceId()).thenReturn("jdbc:unit");
        when(dataSource.getDbType()).thenReturn("mysql");
        target = mock(Connection.class);
        resourceManager = mock(DefaultResourceManager.class);
        undoManager = mock(UndoLogManager.class);
        resources = mockStatic(DefaultResourceManager.class);
        resources.when(DefaultResourceManager::get).thenReturn(resourceManager);
        undoFactory = mockStatic(UndoLogManagerFactory.class);
        undoFactory.when(() -> UndoLogManagerFactory.getUndoLogManager("mysql")).thenReturn(undoManager);
        when(resourceManager.branchRegister(
                        eq(BranchType.AT),
                        eq("jdbc:unit"),
                        isNull(),
                        eq("xid"),
                        nullable(String.class),
                        eq("orders:1")))
                .thenReturn(42L);
        when(resourceManager.lockQuery(any(), anyString(), nullable(String.class), anyString()))
                .thenReturn(true);
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockRetryTimes(0);
        config.setLockRetryInterval(1);
        previousLockConfig = GlobalLockConfigHolder.setAndReturnPrevious(config);
        proxy = new ConnectionProxy(dataSource, target);
    }

    @AfterEach
    void tearDown() {
        undoFactory.close();
        resources.close();
        if (previousLockConfig == null) {
            GlobalLockConfigHolder.remove();
        } else {
            GlobalLockConfigHolder.setAndReturnPrevious(previousLockConfig);
        }
    }

    @Test
    void globalCommitRegistersFlushesCommitsAndClearsContext() throws Exception {
        globalChanges();
        proxy.commit();
        InOrder order = inOrder(resourceManager, undoManager, target);
        order.verify(resourceManager)
                .branchRegister(
                        eq(BranchType.AT),
                        eq("jdbc:unit"),
                        isNull(),
                        eq("xid"),
                        nullable(String.class),
                        eq("orders:1"));
        order.verify(undoManager).flushUndoLogs(proxy);
        order.verify(target).commit();
        if (ConnectionProxy.IS_REPORT_SUCCESS_ENABLE) {
            verify(resourceManager).branchReport(BranchType.AT, "xid", 42L, BranchStatus.PhaseOne_Done, null);
        }
        assertNull(proxy.getContext().getXid());
        assertNull(proxy.getContext().getBranchId());
        assertFalse(proxy.getContext().hasUndoLog());
        assertFalse(proxy.getContext().hasLockKey());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readOnlyOrUnlockedGlobalCommitDoesNotRegister(boolean undoOnly) throws Exception {
        proxy.bind("xid");
        if (undoOnly) {
            proxy.appendUndoLog(new SQLUndoLog());
        }
        proxy.commit();
        verifyNoInteractions(resourceManager);
        verify(undoManager).flushUndoLogs(proxy);
        verify(target).commit();
    }

    @Test
    void localCommitChecksGlobalLocksBeforeCommitting() throws Exception {
        proxy.setGlobalLockRequire(true);
        proxy.appendLockKey("orders:1");
        assertTrue(proxy.isGlobalLockRequire());
        proxy.commit();
        InOrder order = inOrder(resourceManager, target);
        order.verify(resourceManager).lockQuery(BranchType.AT, "jdbc:unit", null, "orders:1");
        order.verify(target).commit();
        assertFalse(proxy.isGlobalLockRequire());
        assertFalse(proxy.getContext().hasLockKey());
    }

    @Test
    void plainCommitDelegatesWithoutRegistryOrUndoWork() throws Exception {
        proxy.commit();
        verify(target).commit();
        verifyNoInteractions(resourceManager, undoManager);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedFlushOrCommitReportsFailureAndRollsBack(boolean flushFailure) throws Exception {
        globalChanges();
        SQLException failure = new SQLException("write failed");
        if (flushFailure) {
            doThrow(failure).when(undoManager).flushUndoLogs(proxy);
        } else {
            doThrow(failure).when(target).commit();
        }
        SQLException error = assertThrows(SQLException.class, proxy::commit);
        assertSame(failure, error.getCause());
        verify(target).rollback();
        verify(resourceManager, times(2)).branchReport(BranchType.AT, "xid", 42L, BranchStatus.PhaseOne_Failed, null);
        assertNull(proxy.getContext().getXid());
    }

    @Test
    void automaticCommitChangeLeavesRollbackToOuterExecutor() throws Exception {
        proxy.changeAutoCommit();
        assertTrue(proxy.getContext().isAutoCommitChanged());
        verify(target).setAutoCommit(false);
        doThrow(new SQLException("commit failed")).when(target).commit();
        assertThrows(SQLException.class, proxy::commit);
        verify(target, never()).rollback();
    }

    @Test
    void wrapsUnexpectedCommitFailureAndDoesNotRollbackAutocommitConnection() throws Exception {
        when(target.getAutoCommit()).thenReturn(true);
        doThrow(new IllegalStateException("unexpected")).when(target).commit();
        SQLException error = assertThrows(SQLException.class, proxy::commit);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        verify(target, never()).rollback();
        doThrow(new SQLException("sql failure")).when(target).commit();
        assertThrows(SQLException.class, proxy::commit);
        verify(target, never()).rollback();
    }

    @Test
    void localLockedCommitWrapsFailureAndRollsBack() throws Exception {
        proxy.setGlobalLockRequire(true);
        doThrow(new SQLException("local failed")).when(target).commit();
        assertThrows(SQLException.class, proxy::commit);
        verify(target).rollback();
        assertFalse(proxy.isGlobalLockRequire());
    }

    @ParameterizedTest
    @EnumSource(
            value = TransactionExceptionCode.class,
            names = {"LockKeyConflict", "LockKeyConflictFailFast", "Unknown"})
    void translatesLockQueryErrors(TransactionExceptionCode code) throws Exception {
        proxy.bind("xid");
        TransactionException failure = new TransactionException(code);
        when(resourceManager.lockQuery(any(), anyString(), anyString(), anyString()))
                .thenThrow(failure);
        SQLException error = assertThrows(SQLException.class, () -> proxy.checkLock("orders:1"));
        if (code == TransactionExceptionCode.Unknown) {
            assertSame(failure, error.getCause());
        } else {
            assertEquals(code, ((LockConflictException) error).getCode());
            assertTrue(error.getMessage().contains("orders:1"));
        }
        assertThrows(SQLException.class, () -> proxy.lockQuery("orders:1"));
    }

    @Test
    void blankLockKeysAvoidRemoteQueryAndFalseResultConflicts() throws Exception {
        proxy.checkLock(" ");
        verifyNoInteractions(resourceManager);
        assertTrue(proxy.lockQuery("orders:1"));
        when(resourceManager.lockQuery(any(), anyString(), nullable(String.class), anyString()))
                .thenReturn(false);
        assertFalse(proxy.lockQuery("orders:1"));
        assertThrows(LockConflictException.class, () -> proxy.checkLock("orders:1"));
    }

    @Test
    void registrationFailureDoesNotFlushOrCommit() throws Exception {
        globalChanges();
        TransactionException failure = new TransactionException(TransactionExceptionCode.Unknown);
        when(resourceManager.branchRegister(
                        any(), anyString(), isNull(), anyString(), nullable(String.class), anyString()))
                .thenThrow(failure);
        SQLException error = assertThrows(SQLException.class, proxy::commit);
        assertSame(failure, error.getCause());
        verifyNoInteractions(undoManager);
        verify(target, never()).commit();
        verify(target).rollback();
    }

    @Test
    void rollbackReportsRegisteredBranchAndRetriesTransientReportFailure() throws Exception {
        proxy.bind("xid");
        proxy.getContext().setBranchId(42L);
        doThrow(new TransactionException(TransactionExceptionCode.Unknown))
                .doNothing()
                .when(resourceManager)
                .branchReport(any(), anyString(), anyLong(), any(), isNull());
        proxy.rollback();
        verify(resourceManager, times(2)).branchReport(BranchType.AT, "xid", 42L, BranchStatus.PhaseOne_Failed, null);
        verify(target).rollback();
        assertNull(proxy.getContext().getBranchId());
    }

    @Test
    void exhaustedReportRetriesPreserveFailureCause() throws Exception {
        proxy.bind("xid");
        proxy.getContext().setBranchId(42L);
        TransactionException failure = new TransactionException(TransactionExceptionCode.Unknown);
        doThrow(failure).when(resourceManager).branchReport(any(), anyString(), anyLong(), any(), isNull());
        SQLException error = assertThrows(SQLException.class, proxy::rollback);
        assertSame(failure, error.getCause());
        assertTrue(error.getMessage().contains("Failed to report branch status false"));
        verify(target).rollback();
    }

    @Test
    void rollbackWithoutRegisteredBranchSkipsReport() throws Exception {
        proxy.bind("xid");
        proxy.rollback();
        verifyNoInteractions(resourceManager);
        assertNull(proxy.getContext().getXid());
    }

    @Test
    void changingAutocommitCommitsGlobalWorkBeforeDelegation() throws Exception {
        proxy.bind("xid");
        proxy.setAutoCommit(true);
        InOrder order = inOrder(undoManager, target);
        order.verify(undoManager).flushUndoLogs(proxy);
        order.verify(target).commit();
        order.verify(target).setAutoCommit(true);
        assertNull(proxy.getContext().getXid());
    }

    @Test
    void savepointRollbackAndReleaseDelegateAndUpdateBufferedWork() throws Exception {
        Savepoint first = mock(Savepoint.class);
        Savepoint second = mock(Savepoint.class);
        when(target.setSavepoint()).thenReturn(first);
        when(target.setSavepoint("second")).thenReturn(second);
        assertSame(first, proxy.setSavepoint());
        proxy.appendLockKey("orders:1");
        assertSame(second, proxy.setSavepoint("second"));
        proxy.appendLockKey("orders:2");
        proxy.rollback(second);
        verify(target).rollback(second);
        assertEquals("orders:1", proxy.getContext().buildLockKeys());
        proxy.releaseSavepoint(first);
        verify(target).releaseSavepoint(first);
    }

    @Test
    void retryPolicyRetriesConflictsAndPropagatesOtherFailures() throws Exception {
        Callable<String> action = mock(Callable.class);
        LockConflictException conflict =
                new LockConflictException("busy", TransactionExceptionCode.LockKeyConflictFailFast);
        when(action.call()).thenThrow(conflict).thenReturn("done");
        proxy.getContext().setAutoCommitChanged(true);
        try (MockedConstruction<LockRetryController> controllers = mockConstruction(LockRetryController.class)) {
            assertEquals("done", new ConnectionProxy.LockRetryPolicy(proxy).doRetryOnLockConflict(action));
            assertEquals(TransactionExceptionCode.LockKeyConflict, conflict.getCode());
            verify(controllers.constructed().get(0)).sleep(conflict);
        }
        when(action.call()).thenThrow(new SQLException("not a conflict"));
        assertThrows(SQLException.class, () -> new ConnectionProxy.LockRetryPolicy(proxy).execute(action));
    }

    private void globalChanges() {
        proxy.bind("xid");
        proxy.appendUndoLog(new SQLUndoLog());
        proxy.appendLockKey("orders:1");
    }
}
