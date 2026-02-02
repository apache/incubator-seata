package org.apache.seata.rm.datasource.undo;

import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.rm.datasource.ConnectionContext;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UndoLogMetricsTest {

    @Test
    public void testFlushUndoLogsMetrics() throws Exception {
        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            Registry registry = mock(Registry.class);
            org.apache.seata.metrics.Summary summary = mock(org.apache.seata.metrics.Summary.class);
            
            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registry);
            when(registry.getSummary(any(Id.class))).thenReturn(summary);

            ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
            ConnectionContext context = mock(ConnectionContext.class);
            DataSourceProxy dataSourceProxy = mock(DataSourceProxy.class);
            
            when(connectionProxy.getContext()).thenReturn(context);
            when(connectionProxy.getDataSourceProxy()).thenReturn(dataSourceProxy);
            when(connectionProxy.getTargetConnection()).thenReturn(mock(Connection.class));
            when(dataSourceProxy.getResourceId()).thenReturn("jdbc:mysql://localhost:3306/test");
            
            when(context.hasUndoLog()).thenReturn(true);
            when(context.getXid()).thenReturn("xid");
            when(context.getBranchId()).thenReturn(123456L);
            when(context.getUndoItems()).thenReturn(Collections.emptyList());

            MySQLUndoLogManager manager = new MySQLUndoLogManager();
            manager.flushUndoLogs(connectionProxy);

            verify(registry).getSummary(eq(UndoLogConstants.SUMMARY_UNDO_LOG_SIZE));
            verify(summary).increase(anyLong());
        }
    }

    @Test
    public void testDeleteUndoLogMetrics() throws Exception {
        try (MockedStatic<RegistryFactory> registryFactoryMock = mockStatic(RegistryFactory.class)) {
            Registry registry = mock(Registry.class);
            org.apache.seata.metrics.Timer timer = mock(org.apache.seata.metrics.Timer.class);
            org.apache.seata.metrics.Counter counter = mock(org.apache.seata.metrics.Counter.class);

            registryFactoryMock.when(RegistryFactory::getInstance).thenReturn(registry);
            when(registry.getTimer(any(Id.class))).thenReturn(timer);
            when(registry.getCounter(any(Id.class))).thenReturn(counter);

            Connection connection = mock(Connection.class);
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);

            MySQLUndoLogManager manager = new MySQLUndoLogManager();
            manager.deleteUndoLog("xid", 123L, connection);

            verify(registry).getTimer(eq(UndoLogConstants.TIMER_UNDO_LOG_DELETE_LATENCY));
            verify(timer).record(anyLong(), any(TimeUnit.class));
            
            verify(registry).getCounter(eq(UndoLogConstants.COUNTER_UNDO_LOG_DELETE_COUNT));
            verify(counter).increase(1);
        }
    }
}
