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
package org.apache.seata.rm.datasource.undo;

import org.apache.seata.metrics.Counter;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Summary;
import org.apache.seata.metrics.Timer;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.rm.datasource.ConnectionContext;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UndoLogMetricsTest {

    @Test
    public void testFlushUndoLogsMetrics() throws Exception {
        Registry registry = mock(Registry.class);
        Summary summary = mock(Summary.class);
        when(registry.getSummary(any(Id.class))).thenReturn(summary);

        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        ConnectionContext context = mock(ConnectionContext.class);
        DataSourceProxy dataSourceProxy = mock(DataSourceProxy.class);

        when(connectionProxy.getContext()).thenReturn(context);
        when(connectionProxy.getDataSourceProxy()).thenReturn(dataSourceProxy);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(connectionProxy.getTargetConnection()).thenReturn(connection);
        when(dataSourceProxy.getResourceId()).thenReturn("jdbc:mysql://localhost:3306/test");

        when(context.hasUndoLog()).thenReturn(true);
        when(context.getXid()).thenReturn("xid");
        when(context.getBranchId()).thenReturn(123456L);
        when(context.getUndoItems()).thenReturn(Collections.emptyList());

        // Spy on the manager to mock getRegistry()
        MySQLUndoLogManager manager = spy(new MySQLUndoLogManager());
        doReturn(registry).when(manager).getRegistry();

        manager.flushUndoLogs(connectionProxy);

        verify(registry).getSummary(eq(UndoLogConstants.SUMMARY_UNDO_LOG_SIZE));
        verify(summary).increase(anyLong());
    }

    @Test
    public void testDeleteUndoLogMetrics() throws Exception {
        Registry registry = mock(Registry.class);
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        when(registry.getTimer(any(Id.class))).thenReturn(timer);
        when(registry.getCounter(any(Id.class))).thenReturn(counter);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);

        // Spy on the manager to mock getRegistry()
        MySQLUndoLogManager manager = spy(new MySQLUndoLogManager());
        doReturn(registry).when(manager).getRegistry();

        manager.deleteUndoLog("xid", 123L, connection);

        verify(registry).getTimer(eq(UndoLogConstants.TIMER_UNDO_LOG_DELETE_LATENCY));
        verify(timer).record(anyLong(), any(TimeUnit.class));
        verify(registry).getCounter(eq(UndoLogConstants.COUNTER_UNDO_LOG_DELETE_COUNT));
        verify(counter).increase(1);
    }

    @Test
    public void testBatchDeleteUndoLogMetrics() throws Exception {
        Registry registry = mock(Registry.class);
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        when(registry.getTimer(any(Id.class))).thenReturn(timer);
        when(registry.getCounter(any(Id.class))).thenReturn(counter);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1); // Simulate rows affected

        // Spy on the manager to mock getRegistry()
        MySQLUndoLogManager manager = spy(new MySQLUndoLogManager());
        doReturn(registry).when(manager).getRegistry();

        manager.batchDeleteUndoLog(
                new HashSet<>(Arrays.asList("xid1", "xid2")), new HashSet<>(Arrays.asList(1L, 2L)), connection);

        verify(registry).getTimer(eq(UndoLogConstants.TIMER_UNDO_LOG_DELETE_LATENCY));
        verify(timer).record(anyLong(), any(TimeUnit.class));
        verify(registry).getCounter(eq(UndoLogConstants.COUNTER_UNDO_LOG_DELETE_COUNT));
        verify(counter).increase(1); // Aligned to 1 operation count
    }

    @Test
    public void testBatchDeleteUndoLogMetricsWithNoRows() throws Exception {
        Registry registry = mock(Registry.class);
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        when(registry.getTimer(any(Id.class))).thenReturn(timer);
        when(registry.getCounter(any(Id.class))).thenReturn(counter);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // No rows affected

        // Spy on the manager to mock getRegistry()
        MySQLUndoLogManager manager = spy(new MySQLUndoLogManager());
        doReturn(registry).when(manager).getRegistry();

        manager.batchDeleteUndoLog(
                new HashSet<>(Collections.singletonList("xid")),
                new HashSet<>(Collections.singletonList(1L)),
                connection);

        verify(registry).getTimer(eq(UndoLogConstants.TIMER_UNDO_LOG_DELETE_LATENCY));
        verify(timer).record(anyLong(), any(TimeUnit.class));
        // Counter should NOT be increased if rows deleted is 0
        verify(registry, never()).getCounter(any(Id.class));
    }
}
