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

import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AsyncWorkerBehaviorTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void queuedCommitsArePartitionedAndAcknowledged(boolean autoCommit) throws Exception {
        DataSourceManager resources = mock(DataSourceManager.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        Connection connection = mock(Connection.class);
        UndoLogManager logs = mock(UndoLogManager.class);
        when(resources.get("resource")).thenReturn(source);
        when(source.getPlainConnection()).thenReturn(connection);
        when(source.getDbType()).thenReturn("mysql");
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        try (MockedStatic<ThreadPoolExecutorFactory> threads = mockStatic(ThreadPoolExecutorFactory.class);
                MockedStatic<UndoLogManagerFactory> factory = mockStatic(UndoLogManagerFactory.class)) {
            threads.when(() -> ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("AsyncWorker", 2, true))
                    .thenReturn(mock(ScheduledThreadPoolExecutor.class));
            factory.when(() -> UndoLogManagerFactory.getUndoLogManager("mysql")).thenReturn(logs);
            AsyncWorker worker = new AsyncWorker(resources);
            for (int i = 0; i < 1001; i++) {
                assertEquals(BranchStatus.PhaseTwo_Committed, worker.branchCommit("xid", i, "resource"));
            }
            worker.doBranchCommitSafely();
            org.mockito.ArgumentCaptor<Set<Long>> ids = org.mockito.ArgumentCaptor.forClass(Set.class);
            verify(logs, times(2))
                    .batchDeleteUndoLog(eq(Collections.singleton("xid")), ids.capture(), same(connection));
            assertEquals(1000, ids.getAllValues().get(0).size());
            assertEquals(Collections.singleton(1000L), ids.getAllValues().get(1));
            verify(connection, times(autoCommit ? 0 : 2)).commit();
            verify(connection).close();
            worker.doBranchCommitSafely();
            verifyNoMoreInteractions(logs);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"resource", "connection", "delete", "rollback"})
    void transientFailuresRequeueUntilResourceRecovers(String failure) throws Exception {
        DataSourceManager resources = mock(DataSourceManager.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        Connection connection = mock(Connection.class);
        UndoLogManager logs = mock(UndoLogManager.class);
        when(resources.get("resource")).thenReturn(source);
        when(source.getPlainConnection()).thenReturn(connection);
        when(source.getDbType()).thenReturn("mysql");
        if ("resource".equals(failure)) {
            when(resources.get("resource")).thenReturn(null, source);
        } else if ("connection".equals(failure)) {
            when(source.getPlainConnection())
                    .thenThrow(new SQLException("unavailable"))
                    .thenReturn(connection);
        } else {
            doThrow(new SQLException("delete failed"))
                    .doNothing()
                    .when(logs)
                    .batchDeleteUndoLog(anySet(), anySet(), same(connection));
            if ("rollback".equals(failure)) {
                doThrow(new SQLException("rollback failed")).when(connection).rollback();
            }
        }
        try (MockedStatic<ThreadPoolExecutorFactory> threads = mockStatic(ThreadPoolExecutorFactory.class);
                MockedStatic<UndoLogManagerFactory> factory = mockStatic(UndoLogManagerFactory.class)) {
            threads.when(() -> ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("AsyncWorker", 2, true))
                    .thenReturn(mock(ScheduledThreadPoolExecutor.class));
            factory.when(() -> UndoLogManagerFactory.getUndoLogManager("mysql")).thenReturn(logs);
            AsyncWorker worker = new AsyncWorker(resources);
            worker.branchCommit("xid", 42, "resource");
            worker.doBranchCommitSafely();
            worker.doBranchCommitSafely();
            verify(logs, times("delete".equals(failure) ? 2 : 1))
                    .batchDeleteUndoLog(Collections.singleton("xid"), Collections.singleton(42L), connection);
            verify(connection, times("rollback".equals(failure) ? 0 : 1)).commit();
        }
    }

    @Test
    void unexpectedResourceFailureIsContained() {
        DataSourceManager resources = mock(DataSourceManager.class);
        when(resources.get("resource")).thenThrow(new IllegalStateException("shutdown"));
        try (MockedStatic<ThreadPoolExecutorFactory> threads = mockStatic(ThreadPoolExecutorFactory.class)) {
            threads.when(() -> ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("AsyncWorker", 2, true))
                    .thenReturn(mock(ScheduledThreadPoolExecutor.class));
            AsyncWorker worker = new AsyncWorker(resources);
            worker.branchCommit("xid", 42, "resource");
            assertDoesNotThrow(worker::doBranchCommitSafely);
            assertTrue(new AsyncWorker.Phase2Context("xid", 42, "resource")
                    .toString()
                    .contains("42"));
        }
    }
}
