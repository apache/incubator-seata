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
import org.mockito.MockedStatic;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
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
}
