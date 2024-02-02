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

import org.apache.seata.rm.RMHandlerAT;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.rm.datasource.DataSourceManager;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class RMHandlerATTest {

    @Test
    void testNormalDeleteUndoLogTable() throws SQLException {
        RMHandlerAT handler = buildHandler(false);
        UndoLogDeleteRequest request = buildRequest();
        int testTimes = 5;
        for (int i = 0; i < testTimes; i++) {
            handler.handle(request);
        }
        verify(handler, times(testTimes)).deleteUndoLog(any(), any(), any());
    }

    @Test
    void testErrorDeleteUndoLogTable() throws SQLException {
        RMHandlerAT handler = buildHandler(true);
        UndoLogDeleteRequest request = buildRequest();
        request.setSaveDays((short) -1);
        handler.handle(request);
        verify(handler, times(1)).deleteUndoLog(any(), any(), any());
    }

    private RMHandlerAT buildHandler(boolean errorDeleteUndologTable) throws SQLException {
        RMHandlerAT handler = spy(new RMHandlerAT());
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        doReturn(dataSourceManager).when(handler).getResourceManager();

        DataSourceProxy dataSourceProxy = mock(DataSourceProxy.class);
        when(dataSourceManager.get(anyString())).thenReturn(dataSourceProxy);

        Connection connection = mock(Connection.class);
        assertDoesNotThrow(() -> {
            when(dataSourceProxy.getPlainConnection()).thenReturn(connection);
        });

        UndoLogManager manager = mock(UndoLogManager.class);
        when(manager.hasUndoLogTable(any())).thenReturn(true);
        doReturn(manager).when(handler).getUndoLogManager(any());

        if (errorDeleteUndologTable) {
            when(manager.deleteUndoLogByLogCreated(any(Date.class), anyInt(), any(Connection.class)))
                    .thenThrow(new SQLException());
        }

        return handler;
    }

    private UndoLogDeleteRequest buildRequest() {
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId("test");
        request.setSaveDays((short) 1);
        return request;
    }
}
