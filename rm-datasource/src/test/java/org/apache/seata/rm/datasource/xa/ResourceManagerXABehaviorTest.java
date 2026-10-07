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

import org.apache.seata.common.lock.ResourceLock;
import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.BaseDataSourceResource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.transaction.xa.XAException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceManagerXABehaviorTest {
    @Test
    void timeoutCheckerClosesOnlyExpiredPreparedConnectionsAndContinuesAfterFailure() throws Exception {
        ResourceManagerXA manager = new ResourceManagerXA();
        DataSourceProxyXA source = mock(DataSourceProxyXA.class);
        when(source.isShouldBeHeld()).thenReturn(true);
        Map<String, ConnectionProxyXA> connections = new LinkedHashMap<>();
        ConnectionProxyXA expired = mock(ConnectionProxyXA.class);
        ConnectionProxyXA failed = mock(ConnectionProxyXA.class);
        ConnectionProxyXA active = mock(ConnectionProxyXA.class);
        for (ConnectionProxyXA connection : new ConnectionProxyXA[] {expired, failed, active}) {
            when(connection.getResourceLock()).thenReturn(new ResourceLock());
        }
        when(active.getPrepareTime()).thenReturn(null);
        when(expired.getPrepareTime()).thenReturn(1L);
        when(failed.getPrepareTime()).thenReturn(1L);
        doThrow(new SQLException("close failed")).when(failed).closeForce();
        connections.put("failed", failed);
        connections.put("expired", expired);
        connections.put("active", active);
        when(source.getKeeper()).thenReturn(connections);
        manager.getManagedResources().put("resource", source);
        ScheduledThreadPoolExecutor scheduler = mock(ScheduledThreadPoolExecutor.class);
        try (MockedStatic<ThreadPoolExecutorFactory> threads = mockStatic(ThreadPoolExecutorFactory.class)) {
            threads.when(() -> ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor(
                            "xaTwoPhaseTimeoutChecker", 1, true))
                    .thenReturn(scheduler);
            manager.initXaTwoPhaseTimeoutChecker();
            manager.initXaTwoPhaseTimeoutChecker();
            org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).scheduleAtFixedRate(task.capture(), eq(60000L), eq(1000L), eq(TimeUnit.MILLISECONDS));
            task.getValue().run();
            verify(failed).closeForce();
            verify(expired).closeForce();
            verify(active, never()).closeForce();
        }
    }

    @Test
    void missingXaBranchRemainsRetryableAndCachesCompletionStatus() throws Exception {
        ResourceManagerXA manager = new ResourceManagerXA();
        DataSourceProxyXA source = mock(DataSourceProxyXA.class);
        ConnectionProxyXA connection = mock(ConnectionProxyXA.class);
        when(source.getConnectionForXAFinish(any())).thenReturn(connection);
        manager.getManagedResources().put("resource", source);
        XAException failure = new XAException(XAException.XAER_NOTA);
        doThrow(failure).when(connection).xaCommit("xid", 42, null);
        doThrow(failure).when(connection).xaRollback("xid", 42, null);
        String key = XAXidBuilder.build("xid", 42).toString();
        try {
            assertEquals(
                    BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable,
                    manager.branchCommit(BranchType.XA, "xid", 42, "resource", null));
            assertEquals(BranchStatus.PhaseTwo_Committed, BaseDataSourceResource.getBranchStatus(key));
            assertEquals(
                    BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable,
                    manager.branchRollback(BranchType.XA, "xid", 42, "resource", null));
            assertEquals(BranchStatus.PhaseTwo_Rollbacked, BaseDataSourceResource.getBranchStatus(key));
            verify(connection, times(2)).close();
        } finally {
            BaseDataSourceResource.remove(key);
        }
    }
}
