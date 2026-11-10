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

import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.sql.serial.SerialArray;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.sqlserver.SqlServerUndoDeleteExecutor;
import org.apache.seata.sqlparser.struct.SqlServerTableMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UndoExecutorBehaviorTest {
    @Test
    void bindsNullableJdbcSpecialTypesAndKeepsPrimaryKeyLast() throws Exception {
        AbstractUndoExecutor executor = mock(AbstractUndoExecutor.class, CALLS_REAL_METHODS);
        PreparedStatement statement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        when(statement.getConnection()).thenReturn(connection);
        Array array = mock(Array.class);
        when(connection.createArrayOf(eq("INTEGER"), any())).thenReturn(array);
        SerialArray serialized = new SerialArray();
        serialized.setBaseTypeName("INTEGER");
        serialized.setElements(new Object[] {1, 2});
        URL url = new URL("https://example.com/value");
        ArrayList<Field> values = new ArrayList<>(Arrays.asList(
                new Field("blob", Types.BLOB, new SerialBlob(new byte[] {1, 2})),
                new Field("binary", Types.LONGVARBINARY, new byte[] {3, 4}),
                new Field("clob", Types.CLOB, new SerialClob("text".toCharArray())),
                new Field("link", Types.DATALINK, new SerialDatalink(url)),
                new Field("array", Types.ARRAY, serialized),
                new Field("other", Types.OTHER, "custom"),
                new Field("bit", Types.BIT, true)));
        for (int type :
                new int[] {Types.BLOB, Types.LONGVARBINARY, Types.CLOB, Types.NCLOB, Types.DATALINK, Types.ARRAY}) {
            values.add(new Field("nullable", type, null));
        }
        executor.undoPrepare(statement, values, Collections.singletonList(new Field("id", Types.INTEGER, 42)));
        org.mockito.ArgumentCaptor<Object> blob = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(statement).setObject(eq(1), blob.capture());
        assertEquals(1, ((InputStream) blob.getValue()).read());
        verify(statement).setObject(eq(2), isA(InputStream.class));
        verify(statement).setClob(eq(3), isA(Reader.class));
        verify(statement).setURL(4, url);
        verify(statement).setArray(5, array);
        verify(statement).setObject(6, "custom");
        verify(statement).setObject(7, true);
        for (int i = 8; i <= 13; i++) {
            verify(statement).setObject(i, null);
        }
        verify(statement).setObject(14, 42, Types.INTEGER);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sqlServerDeleteUndoRestoresIdentityAndClosesStatement(boolean identity) throws Exception {
        SqlServerTableMeta meta = mock(SqlServerTableMeta.class);
        when(meta.getTableName()).thenReturn("orders");
        when(meta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));
        when(meta.isTableIdentifyExistence()).thenReturn(identity);
        TableRecords before = records(meta, "before");
        SQLUndoLog log = new SQLUndoLog();
        log.setTableName("orders");
        log.setBeforeImage(before);
        SqlServerUndoDeleteExecutor executor = new SqlServerUndoDeleteExecutor(log) {
            @Override
            protected boolean dataValidationAndGoOn(ConnectionProxy connection) {
                return true;
            }
        };
        ConnectionProxy proxy = mock(ConnectionProxy.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(proxy.getTargetConnection()).thenReturn(connection);
        when(proxy.getDbType()).thenReturn("sqlserver");
        when(proxy.getDataSourceProxy()).thenReturn(source);
        when(source.getResourceId()).thenReturn("resource");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        TableMetaCache cache = mock(TableMetaCache.class);
        when(cache.getTableMeta(connection, "orders", "resource")).thenReturn(meta);
        try (MockedStatic<TableMetaCacheFactory> caches = mockStatic(TableMetaCacheFactory.class)) {
            caches.when(() -> TableMetaCacheFactory.getTableMetaCache("sqlserver"))
                    .thenReturn(cache);
            executor.executeOn(proxy);
            org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sql.capture());
            assertEquals(identity, sql.getValue().contains("SET IDENTITY_INSERT orders ON"));
            assertTrue(sql.getValue().contains("INSERT INTO orders(value, id) VALUES (?, ?);"));
            verify(statement).setObject(1, "before", Types.VARCHAR);
            verify(statement).setObject(2, 42, Types.INTEGER);
            verify(statement).executeUpdate();
            verify(statement).close();
            RuntimeException failure = new IllegalStateException("driver");
            doThrow(failure).when(statement).executeUpdate();
            assertSame(
                    failure,
                    assertThrows(SQLException.class, () -> executor.executeOn(proxy))
                            .getCause());
            SQLException sqlFailure = new SQLException("SQL");
            doThrow(sqlFailure).when(statement).executeUpdate();
            assertSame(sqlFailure, assertThrows(SQLException.class, () -> executor.executeOn(proxy)));
        }
    }

    @Test
    void validationDistinguishesAlreadyUndoneChangedAndDirtyRows() throws Exception {
        TableMeta meta = mock(TableMeta.class);
        when(meta.getTableName()).thenReturn("orders");
        when(meta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));
        SQLUndoLog log = new SQLUndoLog();
        log.setTableName("orders");
        TableRecords before = records(meta, "before");
        TableRecords after = records(meta, "after");
        log.setBeforeImage(before);
        log.setAfterImage(after);
        AbstractUndoExecutor executor = mock(
                AbstractUndoExecutor.class, withSettings().useConstructor(log).defaultAnswer(CALLS_REAL_METHODS));
        ConnectionProxy proxy = mock(ConnectionProxy.class);
        doReturn(after).when(executor).queryCurrentRecords(proxy);
        assertTrue(executor.dataValidationAndGoOn(proxy));
        doReturn(before).when(executor).queryCurrentRecords(proxy);
        assertFalse(executor.dataValidationAndGoOn(proxy));
        doReturn(records(meta, "dirty")).when(executor).queryCurrentRecords(proxy);
        assertThrows(SQLUndoDirtyException.class, () -> executor.dataValidationAndGoOn(proxy));
        log.setAfterImage(before);
        assertFalse(executor.dataValidationAndGoOn(proxy));
    }

    private TableRecords records(TableMeta meta, String value) {
        TableRecords records = new TableRecords(meta);
        Row row = new Row();
        Field pk = new Field("id", Types.INTEGER, 42);
        pk.setKeyType(KeyType.PRIMARY_KEY);
        row.add(pk);
        row.add(new Field("value", Types.VARCHAR, value));
        records.add(row);
        return records;
    }
}
