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
package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlExecutionTemplateTest {

    @Test
    void shouldExecuteSelectQueryWithStructuredResult() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        String sql = "select name from users where id = ?";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn("Alice");

        try (MockedStatic<DataSourceFactory> dataSourceFactory = mockStatic(DataSourceFactory.class)) {
            dataSourceFactory.when(() -> DataSourceFactory.getDataSource("biz")).thenReturn(dataSource);

            BusinessQueryResult result = new SqlExecutionTemplate(new SqlSafetyValidator()).query("biz", sql, 1);

            assertEquals("biz", result.getResourceId());
            assertEquals(1, result.getColumns().size());
            assertEquals("name", result.getColumns().get(0));
            assertEquals(1, result.getRowCount());
            assertEquals("Alice", result.getRows().get(0).get("name"));
        }
        verify(connection).setReadOnly(true);
        verify(preparedStatement).setQueryTimeout(30);
        verify(preparedStatement).setFetchSize(100);
        verify(preparedStatement).setMaxRows(501);
        verify(preparedStatement).setObject(1, 1);
    }

    @Test
    void shouldReturnTruncatedWhenRowsExceedMaxRows() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        String sql = "select name from users";
        SqlExecutionTemplate template = new SqlExecutionTemplate(new SqlSafetyValidator());
        ReflectionTestUtils.setField(template, "maxRows", 1);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject(1)).thenReturn("Alice");

        try (MockedStatic<DataSourceFactory> dataSourceFactory = mockStatic(DataSourceFactory.class)) {
            dataSourceFactory.when(() -> DataSourceFactory.getDataSource("biz")).thenReturn(dataSource);

            BusinessQueryResult result = template.query("biz", sql);

            assertEquals(1, result.getRowCount());
            assertTrue(result.isTruncated());
            assertEquals(1, result.getMaxRows());
        }
        verify(preparedStatement).setMaxRows(2);
    }

    @Test
    void shouldRejectDmlAndDdlSql() {
        SqlExecutionTemplate template = new SqlExecutionTemplate(new SqlSafetyValidator());

        assertThrows(StoreException.class, () -> template.query("biz", "delete from users where id = 1"));
        assertThrows(StoreException.class, () -> template.query("biz", "drop table users"));
    }

    @Test
    void shouldRejectMultipleStatements() {
        SqlExecutionTemplate template = new SqlExecutionTemplate(new SqlSafetyValidator());

        StoreException exception =
                assertThrows(StoreException.class, () -> template.query("biz", "select 1; select 2"));

        assertEquals("Only a single SELECT statement is allowed", exception.getMessage());
    }

    @Test
    void shouldRejectSelectForUpdate() {
        SqlExecutionTemplate template = new SqlExecutionTemplate(new SqlSafetyValidator());

        StoreException exception =
                assertThrows(StoreException.class, () -> template.query("biz", "select * from users for update"));

        assertEquals("SELECT locking clauses are not allowed", exception.getMessage());
    }

    @Test
    void shouldRejectUndoLogQueryCaseInsensitive() {
        SqlExecutionTemplate template = new SqlExecutionTemplate(new SqlSafetyValidator());

        StoreException exception =
                assertThrows(StoreException.class, () -> template.query("biz", "select * from `Undo_Log`"));

        assertEquals("Querying undo_log is not allowed", exception.getMessage());
    }
}
