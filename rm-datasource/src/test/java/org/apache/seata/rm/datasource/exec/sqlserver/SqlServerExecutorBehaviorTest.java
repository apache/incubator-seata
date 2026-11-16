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
package org.apache.seata.rm.datasource.exec.sqlserver;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.*;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SqlServerExecutorBehaviorTest {
    private StatementProxy<Statement> statement;
    private TableMeta meta;
    private ConnectionProxy connection;

    @BeforeEach
    void setUp() {
        connection = mock(ConnectionProxy.class);
        when(connection.getDbType()).thenReturn("sqlserver");
        statement = mock(StatementProxy.class);
        when(statement.getConnectionProxy()).thenReturn(connection);
        meta = mock(TableMeta.class);
        when(meta.getTableName()).thenReturn("orders");
        when(meta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));
        when(meta.getEscapePkNameList("sqlserver")).thenReturn(Collections.singletonList("id"));
        Map<String, ColumnMeta> columns = new LinkedHashMap<>();
        columns.put("id", new ColumnMeta());
        columns.put("amount", new ColumnMeta());
        when(meta.getAllColumns()).thenReturn(columns);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateAndSelectImagesUseUpdateLocks(boolean withWhere) {
        SQLUpdateRecognizer update = mock(SQLUpdateRecognizer.class);
        when(update.getTableName()).thenReturn("orders");
        when(update.getWhereCondition()).thenReturn(withWhere ? "id = 1" : "");
        SqlServerUpdateExecutor<Integer, Statement> updater =
                new SqlServerUpdateExecutor<Integer, Statement>(statement, (s, args) -> 1, update) {
                    @Override
                    protected TableMeta getTableMeta(String table) {
                        return meta;
                    }
                };
        String updateSql = updater.buildBeforeImageSQL(meta, new ArrayList<>());
        assertEquals(
                "SELECT amount, id FROM orders WITH(UPDLOCK)" + (withWhere ? " WHERE id = 1" : ""),
                normalize(updateSql));
        SQLSelectRecognizer select = mock(SQLSelectRecognizer.class);
        when(select.getTableName()).thenReturn("orders");
        when(select.getWhereCondition()).thenReturn(withWhere ? "id = 1" : "");
        when(select.getOrderByCondition()).thenReturn(withWhere ? "ORDER BY id" : "");
        when(select.getLimitCondition()).thenReturn(withWhere ? "OFFSET 0 ROWS" : "");
        SqlServerSelectForUpdateExecutor<Integer, Statement> selector =
                new SqlServerSelectForUpdateExecutor<Integer, Statement>(statement, (s, args) -> 1, select) {
                    @Override
                    protected TableMeta getTableMeta() {
                        return meta;
                    }
                };
        assertEquals(
                "SELECT id FROM orders WITH(UPDLOCK)" + (withWhere ? " WHERE id = 1 ORDER BY id OFFSET 0 ROWS" : ""),
                normalize(selector.buildSelectSQL(new ArrayList<>())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deleteImageUsesUpdateLockAndOptionalPredicate(boolean withWhere) {
        SQLDeleteRecognizer recognizer = delete(withWhere ? "id = 1" : "");
        SqlServerDeleteExecutor<Integer, Statement> executor =
                new SqlServerDeleteExecutor<>(statement, (s, args) -> 1, recognizer);
        assertEquals(
                "SELECT id, amount FROM orders WITH(UPDLOCK)" + (withWhere ? " WHERE id = 1" : ""),
                normalize(executor.buildBeforeImageSQL(recognizer, meta, new ArrayList<>())));
    }

    @Test
    void multiDeleteCombinesPredicatesButFullTableDeleteRemovesFilter() throws Exception {
        SQLDeleteRecognizer first = delete("id = 1");
        SQLDeleteRecognizer second = delete("id = 2");
        CapturingDelete executor = new CapturingDelete(Arrays.asList(first, second));
        assertSame(executor.records, executor.beforeImage());
        assertEquals("SELECT id, amount FROM orders WITH(UPDLOCK) WHERE id = 1 OR id = 2", normalize(executor.sql));
        when(second.getWhereCondition()).thenReturn("");
        executor.beforeImage();
        assertEquals("SELECT id, amount FROM orders WITH(UPDLOCK)", normalize(executor.sql));
        when(first.getLimitCondition()).thenReturn("TOP 1");
        assertThrows(NotSupportYetException.class, executor::beforeImage);
        when(first.getLimitCondition()).thenReturn("");
        when(first.getOrderByCondition()).thenReturn("ORDER BY id");
        assertThrows(NotSupportYetException.class, executor::beforeImage);
    }

    private SQLDeleteRecognizer delete(String where) {
        SQLDeleteRecognizer recognizer = mock(SQLDeleteRecognizer.class);
        when(recognizer.getTableName()).thenReturn("orders");
        when(recognizer.getWhereCondition()).thenReturn(where);
        return recognizer;
    }

    private class CapturingDelete extends SqlServerMultiDeleteExecutor<Integer, Statement> {
        String sql;
        TableRecords records = new TableRecords();

        CapturingDelete(List<SQLRecognizer> recognizers) {
            super(statement, (s, args) -> 1, recognizers);
        }

        @Override
        protected TableMeta getTableMeta(String table) {
            return meta;
        }

        @Override
        protected TableRecords buildTableRecords(TableMeta table, String query, ArrayList<List<Object>> params) {
            sql = query;
            return records;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reconstructsBatchIdentityKeysUsingDatabaseIncrement(boolean decimal) throws Exception {
        SQLInsertRecognizer recognizer = mock(SQLInsertRecognizer.class);
        SqlServerInsertExecutor executor = new SqlServerInsertExecutor(statement, (s, args) -> 1, recognizer) {
            @Override
            protected TableMeta getTableMeta() {
                return meta;
            }
        };
        ResultSet keys = mock(ResultSet.class);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true, false);
        when(keys.getObject(1)).thenReturn(decimal ? BigDecimal.valueOf(14) : Long.valueOf(14));
        doThrow(new SQLException("forward only")).when(keys).beforeFirst();
        when(statement.getUpdateCount()).thenReturn(3);
        ColumnMeta pk = mock(ColumnMeta.class);
        when(pk.isAutoincrement()).thenReturn(true);
        when(meta.getPrimaryKeyMap()).thenReturn(Collections.singletonMap("id", pk));
        Statement query = mock(Statement.class);
        ResultSet increment = mock(ResultSet.class);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(query);
        when(query.executeQuery("SELECT IDENT_INCR('orders') As INCR")).thenReturn(increment);
        when(increment.next()).thenReturn(true);
        when(increment.getInt("INCR")).thenReturn(2);
        assertEquals(Arrays.asList(10L, 12L, 14L), executor.getGeneratedKeys());
        verify(query).close();
        verify(increment).close();
        when(keys.next()).thenReturn(true, false);
        when(increment.getInt("INCR")).thenReturn(0);
        assertThrows(SQLException.class, executor::getGeneratedKeys);
        when(keys.next()).thenReturn(true, false);
        when(pk.isAutoincrement()).thenReturn(false);
        assertThrows(SQLException.class, executor::getGeneratedKeys);
        when(keys.next()).thenReturn(false);
        assertThrows(NotSupportYetException.class, executor::getGeneratedKeys);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
