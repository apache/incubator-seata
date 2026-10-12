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
package org.apache.seata.rm;

import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.rm.datasource.DataSourceManager;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RMHandlerATBehaviorTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, 7})
    void deletesExpiredLogsInBatchesAndClosesConnection(int saveDays) throws Exception {
        RMHandlerAT handler = spy(new RMHandlerAT());
        DataSourceManager resources = mock(DataSourceManager.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        Connection connection = mock(Connection.class);
        UndoLogManager logs = mock(UndoLogManager.class);
        doReturn(resources).when(handler).getResourceManager();
        when(resources.get("resource")).thenReturn(source);
        when(source.getPlainConnection()).thenReturn(connection);
        when(source.getDbType()).thenReturn("mysql");
        when(logs.deleteUndoLogByLogCreated(any(Date.class), eq(3000), same(connection)))
                .thenReturn(3000, 5);
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId("resource");
        request.setSaveDays((short) saveDays);
        try (MockedStatic<UndoLogManagerFactory> factory = mockStatic(UndoLogManagerFactory.class)) {
            factory.when(() -> UndoLogManagerFactory.getUndoLogManager("mysql")).thenReturn(logs);
            handler.handle(request);
        }
        org.mockito.ArgumentCaptor<Date> cutoff = org.mockito.ArgumentCaptor.forClass(Date.class);
        verify(logs, times(2)).deleteUndoLogByLogCreated(cutoff.capture(), eq(3000), same(connection));
        assertTrue(cutoff.getValue().before(new Date()));
        assertEquals(cutoff.getAllValues().get(0), cutoff.getAllValues().get(1));
        verify(connection, times(2)).commit();
        verify(connection).close();
        assertEquals(BranchType.AT, handler.getBranchType());
    }

    @Test
    void missingResourceOrConnectionStopsCleanup() throws Exception {
        RMHandlerAT handler = spy(new RMHandlerAT());
        DataSourceManager resources = mock(DataSourceManager.class);
        doReturn(resources).when(handler).getResourceManager();
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId("missing");
        handler.handle(request);
        DataSourceProxy source = mock(DataSourceProxy.class);
        when(resources.get("missing")).thenReturn(source);
        when(source.getPlainConnection()).thenThrow(new SQLException("unavailable"));
        doReturn(mock(UndoLogManager.class)).when(handler).getUndoLogManager(source);
        handler.handle(request);
        verify(source).getPlainConnection();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedDeleteRollsBackOnlyManualTransactions(boolean autoCommit) throws Exception {
        RMHandlerAT handler = new RMHandlerAT();
        Connection connection = mock(Connection.class);
        UndoLogManager logs = mock(UndoLogManager.class);
        Date cutoff = new Date();
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        when(logs.deleteUndoLogByLogCreated(cutoff, 3000, connection)).thenThrow(new SQLException("delete failed"));
        doThrow(new SQLException("rollback failed")).when(connection).rollback();
        assertEquals(0, handler.deleteUndoLog(logs, connection, cutoff));
        verify(connection, times(autoCommit ? 0 : 1)).rollback();
    }

    @Test
    void getsATResourceManager() {
        DefaultResourceManager resources = mock(DefaultResourceManager.class);
        DataSourceManager manager = mock(DataSourceManager.class);
        when(resources.getResourceManager(BranchType.AT)).thenReturn(manager);
        try (MockedStatic<DefaultResourceManager> factory = mockStatic(DefaultResourceManager.class)) {
            factory.when(DefaultResourceManager::get).thenReturn(resources);
            assertSame(manager, new RMHandlerAT().getResourceManager());
        }
    }
}
