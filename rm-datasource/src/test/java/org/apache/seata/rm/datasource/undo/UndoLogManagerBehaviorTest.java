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

import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.constants.ClientTableColumnsName;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.rpc.processor.Pair;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UndoLogManagerBehaviorTest {
    private AbstractUndoLogManager manager;
    private DataSourceProxy source;
    private ConnectionProxy proxy;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private UndoLogParser parser;

    @BeforeEach
    void setUp() throws Exception {
        manager = mock(AbstractUndoLogManager.class, CALLS_REAL_METHODS);
        source = mock(DataSourceProxy.class);
        proxy = mock(ConnectionProxy.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        parser = mock(UndoLogParser.class);
        when(source.getConnection()).thenReturn(proxy);
        when(source.getDbType()).thenReturn("mysql");
        when(source.getResourceId()).thenReturn("resource");
        when(proxy.getTargetConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(parser.getName()).thenReturn("test");
    }

    @Test
    void contextRoundTripAndOptionalPairs() {
        Map<String, String> context = manager.parseContext(
                manager.buildContext("jackson", CompressorType.NONE, "extra", null, null, "ignored"));
        assertEquals("jackson", context.get(UndoLogConstants.SERIALIZER_KEY));
        assertEquals("NONE", context.get(UndoLogConstants.COMPRESSOR_TYPE_KEY));
        assertNull(context.get("extra"));
        assertEquals(2, context.size());
        assertTrue(
                manager.buildContext("test", CompressorType.NONE, "extra", null).contains("extra="));
        assertEquals(
                2,
                manager.parseContext(manager.buildContext("test", CompressorType.NONE, "odd"))
                        .size());
        assertEquals(
                2,
                manager.parseContext(manager.buildContext("test", CompressorType.NONE, (String[]) null))
                        .size());
        assertTrue(AbstractUndoLogManager.canUndo(0));
        assertFalse(AbstractUndoLogManager.canUndo(1));
        assertFalse(AbstractUndoLogManager.canUndo(-1));
    }

    @Test
    void rollbackInfoCombinesFragmentsInOrderBeforeDecoding() throws Exception {
        when(result.getBytes(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO)).thenReturn(new byte[] {1, 2});
        when(result.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT)).thenReturn("subId=7,8");
        when(result.getString(ClientTableColumnsName.UNDO_LOG_XID)).thenReturn("xid");
        when(result.getLong(ClientTableColumnsName.UNDO_LOG_BRANCH_XID)).thenReturn(42L);
        when(result.getStatement()).thenReturn(statement);
        when(statement.getConnection()).thenReturn(connection);
        doReturn(new Pair<>(3, Arrays.asList(new byte[] {3}, new byte[] {4, 5})))
                .when(manager)
                .getSubRollbackInfo(connection, "7,8", 42L, "xid");
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, manager.getRollbackInfo(result));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void undoExecutesReverseOrderAndClearsSerializer(boolean autoCommit) throws Exception {
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        when(result.next()).thenReturn(true, false);
        when(result.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT)).thenReturn("serializer=test");
        byte[] bytes = {1};
        doReturn(bytes).when(manager).getRollbackInfo(result);
        SQLUndoLog first = mock(SQLUndoLog.class);
        SQLUndoLog second = mock(SQLUndoLog.class);
        when(first.getTableName()).thenReturn("orders");
        when(second.getTableName()).thenReturn("orders");
        BranchUndoLog log = new BranchUndoLog();
        log.setSqlUndoLogs(new ArrayList<>(Arrays.asList(first, second)));
        when(parser.decode(bytes)).thenReturn(log);
        TableMetaCache cache = mock(TableMetaCache.class);
        TableMeta meta = new TableMeta();
        when(cache.getTableMeta(connection, "orders", "resource")).thenReturn(meta);
        AbstractUndoExecutor executor1 = mock(AbstractUndoExecutor.class);
        AbstractUndoExecutor executor2 = mock(AbstractUndoExecutor.class);
        doAnswer(invocation -> {
                    assertEquals("test", AbstractUndoLogManager.getCurrentSerializer());
                    return null;
                })
                .when(executor2)
                .executeOn(proxy);
        try (MockedStatic<UndoLogParserFactory> parsers = mockStatic(UndoLogParserFactory.class);
                MockedStatic<TableMetaCacheFactory> caches = mockStatic(TableMetaCacheFactory.class);
                MockedStatic<UndoExecutorFactory> executors = mockStatic(UndoExecutorFactory.class)) {
            parsers.when(() -> UndoLogParserFactory.getInstance("test")).thenReturn(parser);
            caches.when(() -> TableMetaCacheFactory.getTableMetaCache("mysql")).thenReturn(cache);
            executors
                    .when(() -> UndoExecutorFactory.getUndoExecutor("mysql", first))
                    .thenReturn(executor1);
            executors
                    .when(() -> UndoExecutorFactory.getUndoExecutor("mysql", second))
                    .thenReturn(executor2);
            manager.undo(source, "xid", 42);
            org.mockito.InOrder order = inOrder(executor1, executor2, connection);
            order.verify(executor2).executeOn(proxy);
            order.verify(executor1).executeOn(proxy);
            order.verify(connection).commit();
        }
        verify(first).setTableMeta(meta);
        verify(second).setTableMeta(meta);
        verify(manager).deleteUndoLog("xid", 42, connection);
        verify(connection, times(autoCommit ? 1 : 0)).setAutoCommit(false);
        verify(connection, times(autoCommit ? 1 : 0)).setAutoCommit(true);
        verify(proxy).close();
        assertNull(AbstractUndoLogManager.getCurrentSerializer());
    }

    @Test
    void missingLogInsertsFenceAndRetriesConcurrentInsertion() throws Exception {
        doThrow(new SQLIntegrityConstraintViolationException("concurrent fence"))
                .doNothing()
                .when(manager)
                .insertUndoLogWithGlobalFinished("xid", 42, parser, connection);
        try (MockedStatic<UndoLogParserFactory> parsers = mockStatic(UndoLogParserFactory.class)) {
            parsers.when(UndoLogParserFactory::getInstance).thenReturn(parser);
            manager.undo(source, "xid", 42);
        }
        verify(manager, times(2)).insertUndoLogWithGlobalFinished("xid", 42, parser, connection);
        verify(connection).commit();
        verify(proxy, times(2)).close();
        verify(connection, never()).rollback();
    }

    @Test
    void finishedLogIsIgnoredAndResourcesAreClosed() throws Exception {
        when(result.next()).thenReturn(true);
        when(result.getInt(ClientTableColumnsName.UNDO_LOG_LOG_STATUS)).thenReturn(1);
        manager.undo(source, "xid", 42);
        verify(connection, never()).commit();
        verify(result).close();
        verify(statement).close();
        verify(proxy).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rollbackFailureDoesNotHideOriginalError(boolean dirty) throws Exception {
        SQLException failure = dirty ? new SQLUndoDirtyException("dirty row") : new SQLException("read failed");
        when(statement.executeQuery()).thenThrow(failure);
        doThrow(new SQLException("rollback failed")).when(connection).rollback();
        doThrow(new SQLException("close failed")).when(proxy).close();
        BranchTransactionException error =
                assertThrows(BranchTransactionException.class, () -> manager.undo(source, "xid", 42));
        assertSame(failure, error.getCause());
        assertEquals(
                dirty
                        ? TransactionExceptionCode.BranchRollbackFailed_Unretriable
                        : TransactionExceptionCode.BranchRollbackFailed_Retriable,
                error.getCode());
        verify(connection).rollback();
        verify(statement).close();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void connectionAcquisitionFailureIsRetriable() throws Exception {
        SQLException failure = new SQLException("unavailable");
        when(source.getConnection()).thenThrow(failure);
        BranchTransactionException error =
                assertThrows(BranchTransactionException.class, () -> manager.undo(source, "xid", 42));
        assertSame(failure, error.getCause());
        assertEquals(TransactionExceptionCode.BranchRollbackFailed_Retriable, error.getCode());
        verifyNoInteractions(connection);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deleteWrapsUnexpectedFailureAndPreservesSqlFailure(boolean batch) throws Exception {
        RuntimeException failure = new IllegalStateException("driver failure");
        when(statement.executeUpdate()).thenThrow(failure);
        SQLException error = assertThrows(SQLException.class, () -> {
            if (batch) {
                manager.batchDeleteUndoLog(Collections.singleton("xid"), Collections.singleton(42L), connection);
            } else {
                manager.deleteUndoLog("xid", 42, connection);
            }
        });
        assertSame(failure, error.getCause());
        verify(statement, times(2)).close();
        SQLException sql = new SQLException("SQL failure");
        when(connection.prepareStatement(anyString())).thenThrow(sql);
        assertSame(sql, assertThrows(SQLException.class, () -> manager.deleteUndoLog("xid", 42, connection)));
    }

    @Test
    void defaultsAndTableExistenceHandleUnsupportedOperations() throws Exception {
        assertThrows(
                UnsupportedOperationException.class, () -> manager.getSubRollbackInfo(connection, "1", 42L, "xid"));
        assertEquals(0, manager.deleteUndoLogByLogCreated(new Date(), 100, connection));
        assertEquals("", manager.getMaxAllowedPacket(source));
        assertFalse(manager.needCompress(new byte[0]));
        assertTrue(manager.hasUndoLogTable(connection));
        verify(statement).close();
        when(statement.executeQuery()).thenThrow(new SQLException("missing table"));
        assertFalse(manager.hasUndoLogTable(connection));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void flushEncodesBranchAndRecordsCompressionContext(boolean compress) throws Exception {
        org.apache.seata.rm.datasource.ConnectionContext context =
                mock(org.apache.seata.rm.datasource.ConnectionContext.class);
        when(proxy.getContext()).thenReturn(context);
        manager.flushUndoLogs(proxy);
        verify(manager, never()).insertUndoLogWithNormal(anyString(), anyLong(), anyString(), any(), any());
        when(context.hasUndoLog()).thenReturn(true);
        when(context.getXid()).thenReturn("xid");
        when(context.getBranchId()).thenReturn(42L);
        SQLUndoLog item = new SQLUndoLog();
        when(context.getUndoItems()).thenReturn(Collections.singletonList(item));
        when(proxy.getDataSourceProxy()).thenReturn(source);
        byte[] encoded = new byte[] {1, 2, 3, 4, 5};
        when(parser.encode(any())).thenReturn(encoded);
        doReturn(compress).when(manager).needCompress(any());
        try (MockedStatic<UndoLogParserFactory> parsers = mockStatic(UndoLogParserFactory.class)) {
            parsers.when(UndoLogParserFactory::getInstance).thenReturn(parser);
            manager.flushUndoLogs(proxy);
        }
        org.mockito.ArgumentCaptor<BranchUndoLog> branch = org.mockito.ArgumentCaptor.forClass(BranchUndoLog.class);
        verify(parser).encode(branch.capture());
        assertEquals("xid", branch.getValue().getXid());
        assertEquals(42L, branch.getValue().getBranchId());
        assertEquals(Collections.singletonList(item), branch.getValue().getSqlUndoLogs());
        org.mockito.ArgumentCaptor<String> metadata = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<byte[]> payload = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(manager)
                .insertUndoLogWithNormal(eq("xid"), eq(42L), metadata.capture(), payload.capture(), same(connection));
        Map<String, String> values = manager.parseContext(metadata.getValue());
        CompressorType type = compress ? AbstractUndoLogManager.ROLLBACK_INFO_COMPRESS_TYPE : CompressorType.NONE;
        assertEquals(type.name(), values.get(UndoLogConstants.COMPRESSOR_TYPE_KEY));
        assertEquals("test", values.get(UndoLogConstants.SERIALIZER_KEY));
        assertArrayEquals(
                encoded,
                org.apache.seata.core.compressor.CompressorFactory.getCompressor(type.getCode())
                        .decompress(payload.getValue()));
    }

    @Test
    void defaultParserHandlesLegacyContextAndCleansUpOnDecodeFailure() throws Exception {
        when(result.next()).thenReturn(true);
        when(result.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT)).thenReturn("");
        byte[] bytes = {1};
        doReturn(bytes).when(manager).getRollbackInfo(result);
        RuntimeException failure = new IllegalArgumentException("invalid undo log");
        when(parser.decode(bytes)).thenThrow(failure);
        try (MockedStatic<UndoLogParserFactory> parsers = mockStatic(UndoLogParserFactory.class)) {
            parsers.when(UndoLogParserFactory::getInstance).thenReturn(parser);
            assertSame(
                    failure,
                    assertThrows(BranchTransactionException.class, () -> manager.undo(source, "xid", 42))
                            .getCause());
        }
        verify(connection).rollback();
        verify(result).close();
        verify(statement).close();
        verify(proxy).close();
        assertNull(AbstractUndoLogManager.getCurrentSerializer());
    }
}
