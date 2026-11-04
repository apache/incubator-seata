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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.util.SeataXAResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConnectionProxyXABehaviorTest {
    private Connection original;
    private XAResource xa;
    private BaseDataSourceResource<ConnectionProxyXA> resource;
    private DefaultResourceManager manager;
    private ConnectionProxyXA proxy;
    private final String xid = "xa-behavior";

    @BeforeEach
    void setUp() throws Exception {
        original = mock(Connection.class);
        when(original.getAutoCommit()).thenReturn(true);
        XAConnection connection = mock(XAConnection.class);
        xa = mock(XAResource.class);
        when(connection.getXAResource()).thenReturn(xa);
        resource = mock(BaseDataSourceResource.class);
        when(resource.getResourceId()).thenReturn("resource");
        when(resource.getDbType()).thenReturn("mysql");
        manager = mock(DefaultResourceManager.class);
        when(manager.branchRegister(BranchType.XA, "resource", null, xid, null, null))
                .thenReturn(42L);
        proxy = new ConnectionProxyXA(original, connection, resource, xid);
        proxy.init();
    }

    @Test
    void registrationAndStartFailuresPreserveCauseAndAllowRetry() throws Exception {
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            TransactionException failure = new TransactionException(TransactionExceptionCode.IO);
            doThrow(failure).when(manager).branchRegister(BranchType.XA, "resource", null, xid, null, null);
            assertSame(
                    failure,
                    assertThrows(SQLException.class, () -> proxy.setAutoCommit(false))
                            .getCause());
            verifyNoInteractions(xa);
            doReturn(42L).when(manager).branchRegister(BranchType.XA, "resource", null, xid, null, null);
            XAException start = new XAException("start failed");
            doThrow(start).doNothing().when(xa).start(any(), eq(XAResource.TMNOFLAGS));
            assertSame(
                    start,
                    assertThrows(SQLException.class, () -> proxy.setAutoCommit(false))
                            .getCause());
            proxy.setAutoCommit(false);
            assertFalse(proxy.getAutoCommit());
        }
    }

    @Test
    void completedGlobalTransactionRejectsStartAndReportsRollback() throws Exception {
        String key = XAXidBuilder.build(xid, 42).toString();
        BaseDataSourceResource.setBranchStatus(key, BranchStatus.PhaseTwo_Committed);
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            assertThrows(SQLException.class, () -> proxy.setAutoCommit(false));
            verify(xa).end(any(), eq(XAResource.TMFAIL));
            verify(xa).rollback(any());
            verify(manager).branchReport(BranchType.XA, xid, 42, BranchStatus.PhaseOne_Failed, null);
        } finally {
            BaseDataSourceResource.remove(key);
        }
    }

    @Test
    void prepareFailureRollsBackAndClosesPhysicalConnection() throws Exception {
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            XAException failure = new XAException("prepare failed");
            when(xa.prepare(any())).thenThrow(failure);
            assertSame(failure, assertThrows(SQLException.class, proxy::close).getCause());
            verify(xa).rollback(any());
            verify(manager).branchReport(BranchType.XA, xid, 42, BranchStatus.PhaseOne_PrepareFailed, null);
            verify(original).close();
            assertNull(proxy.getPrepareTime());
        }
    }

    @Test
    void completionBetweenStartAndEndTriggersRollbackOnClose() throws Exception {
        String key = XAXidBuilder.build(xid, 42).toString();
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            BaseDataSourceResource.setBranchStatus(key, BranchStatus.PhaseTwo_Rollbacked);
            SQLException error = assertThrows(SQLException.class, proxy::close);
            assertTrue(error.getMessage().contains("rollbacked on committing"));
            verify(xa).rollback(any());
            verify(original).close();
        } finally {
            BaseDataSourceResource.remove(key);
        }
    }

    @Test
    void rollbackFailureIsWrappedAndContextIsCleaned() throws Exception {
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            XAException failure = new XAException("rollback failed");
            doThrow(failure).when(xa).rollback(any());
            assertSame(
                    failure, assertThrows(SQLException.class, proxy::rollback).getCause());
            assertThrows(SQLException.class, proxy::commit);
            proxy.close();
            verify(original).close();
        }
    }

    @Test
    void oracleUsesLooseCouplingAndIgnoresFailedStatusReport() throws Exception {
        when(resource.getDbType()).thenReturn("oracle");
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            doThrow(new TransactionException(TransactionExceptionCode.IO))
                    .when(manager)
                    .branchReport(BranchType.XA, xid, 42, BranchStatus.PhaseOne_Failed, null);
            proxy.setAutoCommit(false);
            verify(xa).start(any(), eq(SeataXAResource.ORATRANSLOOSE));
            assertDoesNotThrow(() -> proxy.rollback());
            proxy.closeForce();
            verify(original, times(2)).close();
        }
    }

    @Test
    void dmPhaseTwoCommitRetriesLegacyIdentityWhenPerBranchXidIsMissing() throws Exception {
        when(resource.getDbType()).thenReturn("dm");
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            proxy.close();
            // a branch prepared by an RM before the per-branch-gtrid format keeps the legacy
            // physical identity: the rebuilt per-branch Xid misses it (XAER_NOTA) and the legacy
            // identity must be retried once, so an upgraded RM can still resolve the branch
            doThrow(new XAException(XAException.XAER_NOTA)).doNothing().when(xa).commit(any(), eq(false));
            assertDoesNotThrow(() -> proxy.xaCommit(xid, 42, null));

            ArgumentCaptor<Xid> committed = ArgumentCaptor.forClass(Xid.class);
            verify(xa, times(2)).commit(committed.capture(), eq(false));
            Xid perBranch = committed.getAllValues().get(0);
            Xid legacy = committed.getAllValues().get(1);
            assertEquals(XABranchXid.SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID, perBranch.getFormatId());
            assertEquals(XABranchXid.SEATA_XA_XID_FORMAT_ID, legacy.getFormatId());
            assertArrayEquals((xid + "-" + 42).getBytes(StandardCharsets.UTF_8), perBranch.getGlobalTransactionId());
            assertArrayEquals(xid.getBytes(StandardCharsets.UTF_8), legacy.getGlobalTransactionId());
        }
    }

    @Test
    void dmPhaseTwoRollbackRetriesLegacyIdentityWhenPerBranchXidIsMissing() throws Exception {
        when(resource.getDbType()).thenReturn("dm");
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            proxy.close();
            doThrow(new XAException(XAException.XAER_NOTA)).doNothing().when(xa).rollback(any());
            assertDoesNotThrow(() -> proxy.xaRollback(xid, 42, null));

            ArgumentCaptor<Xid> rolledBack = ArgumentCaptor.forClass(Xid.class);
            verify(xa, times(2)).rollback(rolledBack.capture());
            Xid legacy = rolledBack.getAllValues().get(1);
            assertEquals(XABranchXid.SEATA_XA_XID_FORMAT_ID, legacy.getFormatId());
            assertArrayEquals(xid.getBytes(StandardCharsets.UTF_8), legacy.getGlobalTransactionId());
        }
    }

    @Test
    void xaerNotaStaysPropagatingForDatabasesWithoutTheLegacyIdentity() throws Exception {
        // non-DM databases keep the legacy encoding in phase one: XAER_NOTA must not trigger any
        // retry and propagates to the TC retryable handling exactly as before
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            proxy.setAutoCommit(false);
            proxy.close();
            doThrow(new XAException(XAException.XAER_NOTA)).when(xa).commit(any(), eq(false));
            assertThrows(XAException.class, () -> proxy.xaCommit(xid, 42, null));
            verify(xa, times(1)).commit(any(), eq(false));
        }
    }

    @Test
    void longXidDmBranchSurvivesRegistrationPhaseOneAndPhaseTwoRollback() throws Exception {
        // the overflow case from the review: the readable per-branch gtrid would exceed the X/Open
        // limit, yet registration, xa start, prepare and the TC driven rollback must all work
        String longXid = "2001:db8:1234:5678:90ab:cdef:1234:5678:8091:812345678901234567";
        when(resource.getDbType()).thenReturn("dm");
        when(manager.branchRegister(BranchType.XA, "resource", null, longXid, null, null))
                .thenReturn(43L);
        try (MockedStatic<DefaultResourceManager> managers = mockStatic(DefaultResourceManager.class)) {
            managers.when(DefaultResourceManager::get).thenReturn(manager);
            XAConnection xaConnection = mock(XAConnection.class);
            when(xaConnection.getXAResource()).thenReturn(xa);
            ConnectionProxyXA longXidProxy = new ConnectionProxyXA(original, xaConnection, resource, longXid);
            longXidProxy.init();

            // phase one: registration succeeds and the physical xa branch starts with a bounded gtrid
            assertDoesNotThrow(() -> longXidProxy.setAutoCommit(false));
            ArgumentCaptor<Xid> started = ArgumentCaptor.forClass(Xid.class);
            verify(xa).start(started.capture(), eq(XAResource.TMNOFLAGS));
            assertTrue(started.getValue().getGlobalTransactionId().length <= XABranchXid.MAX_GTRID_LENGTH);

            longXidProxy.close();
            // phase two: the TC driven rollback resolves without any encoder failure
            doThrow(new XAException(XAException.XAER_NOTA)).doNothing().when(xa).rollback(any());
            assertDoesNotThrow(() -> longXidProxy.xaRollback(longXid, 43L, null));
            verify(xa, times(2)).rollback(any());
        }
    }
}
