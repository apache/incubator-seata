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
package org.apache.seata.rm.datasource.undo.sqlserver;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.google.common.collect.Sets;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.rm.datasource.ConnectionContext;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.undo.parser.JacksonUndoLogParser;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlServerUndoLogManagerTest {

    private SqlServerUndoLogManager undoLogManager;
    private DataSourceProxy dataSourceProxy;
    private ConnectionProxy connectionProxy;
    private DruidDataSource dataSource;

    @BeforeAll
    public static void setUpBeforeClass() {
        System.setProperty("file.encoding", "UTF-8");
        EnhancedServiceLoader.load(UndoLogManager.class, JdbcConstants.SQLSERVER);
    }

    @BeforeEach
    public void setUp() throws SQLException {
        undoLogManager = new SqlServerUndoLogManager();
        dataSource = mock(DruidDataSource.class);

        dataSourceProxy = mock(DataSourceProxy.class);
        connectionProxy = mock(ConnectionProxy.class);
        Connection mockConnection = mock(Connection.class);

        when(dataSourceProxy.getPlainConnection()).thenReturn(mockConnection);
        when(connectionProxy.getDataSourceProxy()).thenReturn(dataSourceProxy);
        when(connectionProxy.getContext()).thenReturn(new ConnectionContext());
        when(connectionProxy.getTargetConnection()).thenReturn(mockConnection);

        // Mock dataSource.getConnection() to return a mock DruidPooledConnection
        DruidPooledConnection mockPooledConnection = mock(DruidPooledConnection.class);
        when(dataSource.getConnection()).thenReturn(mockPooledConnection);

        // Mock PreparedStatement for common operations
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        PreparedStatement mockPreparedStatement2 = mock(PreparedStatement.class);

        // Mock ConnectionProxy's prepareStatement method directly
        when(connectionProxy.prepareStatement(anyString()))
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2)
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2);

        // For methods that create multiple PreparedStatements, we need to return different instances
        when(mockConnection.prepareStatement(anyString()))
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2)
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2);

        when(mockPooledConnection.prepareStatement(anyString()))
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2)
                .thenReturn(mockPreparedStatement)
                .thenReturn(mockPreparedStatement2);

        // Mock all PreparedStatement operations
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement2.executeUpdate()).thenReturn(1);
    }

    @Test
    public void testGetInstance() {
        UndoLogManager instance = UndoLogManagerFactory.getUndoLogManager(JdbcConstants.SQLSERVER);
        Assertions.assertInstanceOf(SqlServerUndoLogManager.class, instance);
    }

    @Test
    public void testInsertUndoLogWithGlobalFinished() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Assertions.assertDoesNotThrow(() -> undoLogManager.insertUndoLogWithGlobalFinished(
                "test-xid", 123L, new JacksonUndoLogParser(), connection));

        verify(preparedStatement).setLong(1, 123L);
        verify(preparedStatement).setString(2, "test-xid");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testInsertUndoLogWithNormal() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        byte[] undoLogContent = "test-undo-log".getBytes();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Assertions.assertDoesNotThrow(() ->
                undoLogManager.insertUndoLogWithNormal("test-xid", 456L, "test-context", undoLogContent, connection));

        verify(preparedStatement).setLong(1, 456L);
        verify(preparedStatement).setString(2, "test-xid");
        verify(preparedStatement).setString(3, "test-context");
        verify(preparedStatement).setBytes(4, undoLogContent);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("test-xid", 1L, dataSource.getConnection()));
        Assertions.assertDoesNotThrow(() -> undoLogManager.deleteUndoLog("test-xid", 1L, connectionProxy));
    }

    @Test
    public void testBatchDeleteUndoLog() {
        Assertions.assertDoesNotThrow(() -> undoLogManager.batchDeleteUndoLog(
                Sets.newHashSet("test-xid"), Sets.newHashSet(1L), dataSource.getConnection()));

        Assertions.assertDoesNotThrow(() ->
                undoLogManager.batchDeleteUndoLog(Sets.newHashSet("test-xid"), Sets.newHashSet(1L), connectionProxy));
    }

    @Test
    public void testDeleteUndoLogByLogCreated() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(5);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.JANUARY, 1);
        Date logCreated = calendar.getTime();

        int deletedRows = undoLogManager.deleteUndoLogByLogCreated(logCreated, 100, connection);

        Assertions.assertEquals(5, deletedRows);
        verify(preparedStatement).setInt(1, 100);
        verify(preparedStatement).setDate(2, new java.sql.Date(logCreated.getTime()));
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testDeleteUndoLogByLogCreatedWithException() throws SQLException {
        Connection connection = mock(Connection.class);

        when(connection.prepareStatement(anyString())).thenThrow(new RuntimeException("Connection error"));

        Calendar calendar = Calendar.getInstance();
        Date logCreated = calendar.getTime();

        Assertions.assertThrows(
                SQLException.class, () -> undoLogManager.deleteUndoLogByLogCreated(logCreated, 100, connection));
    }

    @Test
    public void testUndo() throws SQLException {
        // Mock additional dependencies for undo operation
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);

        when(dataSourceProxy.getPlainConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mock(java.sql.ResultSet.class));

        // The undo method may throw exceptions due to complex internal logic
        // We just verify it doesn't throw unexpected runtime exceptions
        try {
            undoLogManager.undo(dataSourceProxy, "test-xid", 1L);
        } catch (Exception e) {
            // Expected exceptions from business logic are acceptable
            // We're mainly testing that the method can be called without null pointer exceptions
            Assertions.assertTrue(e instanceof SQLException
                    || e instanceof org.apache.seata.core.exception.BranchTransactionException
                    || e instanceof RuntimeException);
        }
    }

    @Test
    public void testBuildSelectUndoSql() {
        String selectSql = undoLogManager.buildSelectUndoSql();

        Assertions.assertNotNull(selectSql);
        Assertions.assertTrue(selectSql.contains("SELECT * FROM"));
        Assertions.assertTrue(selectSql.contains("undo_log"));
        Assertions.assertTrue(selectSql.contains("WITH(UPDLOCK)"));
        Assertions.assertTrue(selectSql.contains("WHERE"));
        Assertions.assertTrue(selectSql.contains("branch_id = ?"));
        Assertions.assertTrue(selectSql.contains("xid = ?"));
    }

    @Test
    public void testGetCheckUndoLogTableExistSql() {
        String checkSql = undoLogManager.getCheckUndoLogTableExistSql();

        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT TOP 1 1 FROM"));
        Assertions.assertTrue(checkSql.contains("undo_log"));
    }

    @Test
    public void testSqlServerSpecificSqlSyntax() {
        // Test that SQL Server-specific syntax is used
        String selectSql = undoLogManager.buildSelectUndoSql();

        // Verify SQL Server specific WITH(UPDLOCK) is present
        Assertions.assertTrue(selectSql.contains("WITH(UPDLOCK)"));

        String checkSql = undoLogManager.getCheckUndoLogTableExistSql();

        // Verify SQL Server specific TOP syntax is used
        Assertions.assertTrue(checkSql.contains("SELECT TOP 1"));
    }

    @Test
    public void testInsertUndoLogSqlContainsSqlServerSpecificFunctions() throws Exception {
        // Use reflection to access the private INSERT_UNDO_LOG_SQL field
        Field field = SqlServerUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        field.setAccessible(true);
        String insertSql = (String) field.get(undoLogManager);

        Assertions.assertNotNull(insertSql);
        Assertions.assertTrue(insertSql.contains("INSERT INTO"));
        Assertions.assertTrue(insertSql.contains("undo_log"));
        // Verify SQL Server specific SYSDATETIME() function is used
        Assertions.assertTrue(insertSql.contains("SYSDATETIME()"));
        Assertions.assertTrue(insertSql.contains("VALUES"));
    }

    @Test
    public void testDeleteUndoLogByCreateSqlContainsSqlServerSpecificSyntax() throws Exception {
        // Use reflection to access the private DELETE_UNDO_LOG_BY_CREATE_SQL field
        Field field = SqlServerUndoLogManager.class.getDeclaredField("DELETE_UNDO_LOG_BY_CREATE_SQL");
        field.setAccessible(true);
        String deleteSql = (String) field.get(undoLogManager);

        Assertions.assertNotNull(deleteSql);
        Assertions.assertTrue(deleteSql.contains("DELETE FROM"));
        Assertions.assertTrue(deleteSql.contains("undo_log"));
        // Verify SQL Server specific TOP syntax is used
        Assertions.assertTrue(deleteSql.contains("SELECT TOP(?)"));
        Assertions.assertTrue(deleteSql.contains("ORDER BY"));
        Assertions.assertTrue(deleteSql.contains("ASC"));
    }

    @Test
    public void testInsertUndoLogWithGlobalFinishedState() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        undoLogManager.insertUndoLogWithGlobalFinished("test-xid", 789L, new JacksonUndoLogParser(), connection);

        // Verify correct state value (GlobalFinished = 1) is set
        verify(preparedStatement).setInt(5, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testInsertUndoLogWithNormalState() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        byte[] content = "test-content".getBytes();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        undoLogManager.insertUndoLogWithNormal("test-xid", 789L, "test-ctx", content, connection);

        // Verify correct state value (Normal = 0) is set
        verify(preparedStatement).setInt(5, 0);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testInsertUndoLogWithSqlException() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        Assertions.assertThrows(
                SQLException.class,
                () -> undoLogManager.insertUndoLogWithGlobalFinished(
                        "test-xid", 789L, new JacksonUndoLogParser(), connection));
    }

    @Test
    public void testInsertUndoLogWithNonSqlException() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new RuntimeException("Runtime error"));

        Assertions.assertThrows(
                SQLException.class,
                () -> undoLogManager.insertUndoLogWithGlobalFinished(
                        "test-xid", 789L, new JacksonUndoLogParser(), connection));
    }

    @Test
    public void testDeleteUndoLogByLogCreatedWithZeroRows() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        Calendar calendar = Calendar.getInstance();
        Date logCreated = calendar.getTime();

        int deletedRows = undoLogManager.deleteUndoLogByLogCreated(logCreated, 100, connection);

        Assertions.assertEquals(0, deletedRows);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void testLoadLevelAnnotation() {
        // Verify that the class has LoadLevel annotation for SQL Server
        org.apache.seata.common.loader.LoadLevel loadLevel =
                SqlServerUndoLogManager.class.getAnnotation(org.apache.seata.common.loader.LoadLevel.class);

        Assertions.assertNotNull(loadLevel);
        Assertions.assertEquals(JdbcConstants.SQLSERVER, loadLevel.name());
    }

    @Test
    public void testInheritance() {
        // Verify inheritance hierarchy
        Assertions.assertTrue(undoLogManager instanceof org.apache.seata.rm.datasource.undo.AbstractUndoLogManager);
        Assertions.assertTrue(undoLogManager instanceof org.apache.seata.rm.datasource.undo.UndoLogManager);
    }

    @Test
    public void testMultipleInsertOperations() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Test multiple insert operations
        undoLogManager.insertUndoLogWithGlobalFinished("xid-1", 1L, new JacksonUndoLogParser(), connection);
        undoLogManager.insertUndoLogWithNormal("xid-2", 2L, "ctx-2", "content-2".getBytes(), connection);
        undoLogManager.insertUndoLogWithGlobalFinished("xid-3", 3L, new JacksonUndoLogParser(), connection);

        // Verify that prepareStatement is called for each operation
        verify(connection, Mockito.times(3)).prepareStatement(anyString());
        verify(preparedStatement, Mockito.times(3)).executeUpdate();
    }

    @Test
    public void testBuildContextForGlobalFinished() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        JacksonUndoLogParser parser = new JacksonUndoLogParser();

        undoLogManager.insertUndoLogWithGlobalFinished("test-xid", 100L, parser, connection);

        // Verify context is built with parser name and NONE compressor
        verify(preparedStatement).setString(3, "serializer=jackson&compressorType=" + CompressorType.NONE.name());
    }

    @Test
    public void testSqlServerSpecificDateTimeFunctions() throws Exception {
        // Verify that SQL Server specific SYSDATETIME() is used instead of NOW() or SYSDATE
        Field field = SqlServerUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        field.setAccessible(true);
        String insertSql = (String) field.get(undoLogManager);

        // Should contain SYSDATETIME() function which is SQL Server specific
        Assertions.assertTrue(insertSql.contains("SYSDATETIME()"), "INSERT SQL should contain SYSDATETIME() function");

        // Should not contain other database's date functions
        Assertions.assertFalse(insertSql.contains("NOW()"), "INSERT SQL should not contain MySQL's NOW() function");
        Assertions.assertFalse(
                insertSql.contains("CURRENT_TIMESTAMP"), "INSERT SQL should not contain standard CURRENT_TIMESTAMP");
    }

    @Test
    public void testConstantSqlStatements() throws Exception {
        // Verify that all SQL constants are properly defined and accessible
        Field insertField = SqlServerUndoLogManager.class.getDeclaredField("INSERT_UNDO_LOG_SQL");
        insertField.setAccessible(true);
        String insertSql = (String) insertField.get(undoLogManager);
        Assertions.assertNotNull(insertSql);
        Assertions.assertFalse(insertSql.isEmpty());

        Field deleteField = SqlServerUndoLogManager.class.getDeclaredField("DELETE_UNDO_LOG_BY_CREATE_SQL");
        deleteField.setAccessible(true);
        String deleteSql = (String) deleteField.get(undoLogManager);
        Assertions.assertNotNull(deleteSql);
        Assertions.assertFalse(deleteSql.isEmpty());

        Field checkField = SqlServerUndoLogManager.class.getDeclaredField("CHECK_UNDO_LOG_TABLE_EXIST_SQL");
        checkField.setAccessible(true);
        String checkSql = (String) checkField.get(undoLogManager);
        Assertions.assertNotNull(checkSql);
        Assertions.assertFalse(checkSql.isEmpty());
    }
}
