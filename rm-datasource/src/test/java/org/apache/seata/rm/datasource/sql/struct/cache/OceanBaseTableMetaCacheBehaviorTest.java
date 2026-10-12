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
package org.apache.seata.rm.datasource.sql.struct.cache;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OceanBaseTableMetaCacheBehaviorTest {
    private final OceanBaseTableMetaCache cache = new OceanBaseTableMetaCache();

    @ParameterizedTest
    @CsvSource({"orders,APP,ORDERS,false", "sales.orders,SALES,ORDERS,false", "'\"Sales\".\"Orders\"',Sales,Orders,true"
    })
    void resolvesSchemaCaseAndMetadata(String input, String schema, String table, boolean sensitive) throws Exception {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(metadata.getUserName()).thenReturn("app@tenant#cluster");
        ResultSet columns = rows(
                row("COLUMN_NAME", "ID", "DATA_TYPE", Types.INTEGER, "TYPE_NAME", "INTEGER", "COLUMN_SIZE", 11),
                row("COLUMN_NAME", "NAME", "DATA_TYPE", Types.VARCHAR, "TYPE_NAME", "VARCHAR", "COLUMN_SIZE", 64));
        ResultSet indexes = rows(
                row("INDEX_NAME", ""),
                row("INDEX_NAME", "PK_ORDER", "COLUMN_NAME", "ID", "NON_UNIQUE", false),
                row("INDEX_NAME", "IX_NAME", "COLUMN_NAME", "NAME", "NON_UNIQUE", true));
        ResultSet primary = rows(row("PK_NAME", "PK_ORDER", "COLUMN_NAME", "ID"));
        when(metadata.getColumns("", schema, table, "%")).thenReturn(columns);
        when(metadata.getIndexInfo(null, schema, table, false, true)).thenReturn(indexes);
        when(metadata.getPrimaryKeys(null, schema, table)).thenReturn(primary);
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metadata);
        TableMeta result = cache.fetchSchema(connection, input);
        assertEquals(input, result.getOriginalTableName());
        assertEquals(table, result.getTableName());
        assertEquals(sensitive, result.isCaseSensitive());
        assertEquals(2, result.getAllColumns().size());
        assertEquals(Types.INTEGER, result.getAllColumns().get("ID").getDataType());
        assertEquals(64, result.getAllColumns().get("NAME").getColumnSize());
        assertEquals(IndexType.PRIMARY, result.getAllIndexes().get("PK_ORDER").getIndextype());
        assertEquals(IndexType.NORMAL, result.getAllIndexes().get("IX_NAME").getIndextype());
        assertEquals(
                "jdbc:unit." + (sensitive ? "Sales.Orders" : input.toUpperCase()),
                cache.getCacheKey(connection, input, "jdbc:unit"));
        verify(columns).close();
        verify(indexes).close();
        verify(primary).close();
    }

    @Test
    void matchesCompositePrimaryKeyWhenConstraintAndIndexNamesDiffer() throws Exception {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet columns = rows(row("COLUMN_NAME", "ID"), row("COLUMN_NAME", "TENANT"), row("COLUMN_NAME", "OTHER"));
        ResultSet indexes = rows(
                row("INDEX_NAME", "UNIQUE_OTHER", "COLUMN_NAME", "OTHER", "NON_UNIQUE", false),
                row("INDEX_NAME", "UNIQUE_ORDER", "COLUMN_NAME", "ID", "NON_UNIQUE", false),
                row("INDEX_NAME", "UNIQUE_ORDER", "COLUMN_NAME", "TENANT", "NON_UNIQUE", false));
        ResultSet primary =
                rows(row("PK_NAME", "SYS_PK", "COLUMN_NAME", "ID"), row("PK_NAME", "SYS_PK", "COLUMN_NAME", "TENANT"));
        when(metadata.getColumns(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(columns);
        when(metadata.getIndexInfo(isNull(), anyString(), anyString(), eq(false), eq(true)))
                .thenReturn(indexes);
        when(metadata.getPrimaryKeys(isNull(), anyString(), anyString())).thenReturn(primary);
        TableMeta result = cache.resultSetMetaToSchema(metadata, "app.orders");
        assertEquals(
                IndexType.PRIMARY, result.getAllIndexes().get("UNIQUE_ORDER").getIndextype());
        assertEquals(2, result.getAllIndexes().get("UNIQUE_ORDER").getValues().size());
        assertEquals(
                IndexType.UNIQUE, result.getAllIndexes().get("UNIQUE_OTHER").getIndextype());
    }

    @Test
    void duplicateColumnsAndMissingIndexesCloseAllResultSets() throws Exception {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet columns = rows(row("COLUMN_NAME", "ID"), row("COLUMN_NAME", "ID"));
        ResultSet indexes = rows();
        ResultSet primary = rows();
        when(metadata.getColumns(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(columns);
        when(metadata.getIndexInfo(isNull(), anyString(), anyString(), eq(false), eq(true)))
                .thenReturn(indexes);
        when(metadata.getPrimaryKeys(isNull(), anyString(), anyString())).thenReturn(primary);
        assertThrows(NotSupportYetException.class, () -> cache.resultSetMetaToSchema(metadata, "app.orders"));
        verify(columns).close();
        verify(indexes).close();
        verify(primary).close();
        ResultSet singleColumn = rows(row("COLUMN_NAME", "ID"));
        when(metadata.getColumns(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(singleColumn);
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metadata);
        SQLException error = assertThrows(SQLException.class, () -> cache.fetchSchema(connection, "app.orders"));
        assertInstanceOf(ShouldNeverHappenException.class, error.getCause());
        assertTrue(error.getMessage().contains("app.orders"));
    }

    @Test
    void preservesSqlExceptionsFromMetadataProvider() throws Exception {
        Connection connection = mock(Connection.class);
        SQLException failure = new SQLException("metadata unavailable");
        when(connection.getMetaData()).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> cache.fetchSchema(connection, "app.orders")));
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], values[i + 1]);
        }
        return result;
    }

    @SafeVarargs
    private static ResultSet rows(Map<String, Object>... rows) throws SQLException {
        AtomicInteger cursor = new AtomicInteger(-1);
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenAnswer(invocation -> cursor.incrementAndGet() < rows.length);
        when(result.getString(anyString())).thenAnswer(invocation -> {
            Object value = rows[cursor.get()].get(invocation.getArgument(0));
            return value == null ? null : value.toString();
        });
        when(result.getInt(anyString()))
                .thenAnswer(invocation ->
                        ((Number) rows[cursor.get()].getOrDefault(invocation.getArgument(0), 0)).intValue());
        when(result.getShort(anyString()))
                .thenAnswer(invocation ->
                        ((Number) rows[cursor.get()].getOrDefault(invocation.getArgument(0), 0)).shortValue());
        when(result.getLong(anyString()))
                .thenAnswer(invocation ->
                        ((Number) rows[cursor.get()].getOrDefault(invocation.getArgument(0), 0)).longValue());
        when(result.getBoolean(anyString()))
                .thenAnswer(invocation -> rows[cursor.get()].getOrDefault(invocation.getArgument(0), false));
        return result;
    }
}
