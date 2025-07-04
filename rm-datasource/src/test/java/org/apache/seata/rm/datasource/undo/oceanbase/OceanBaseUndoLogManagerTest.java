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
package org.apache.seata.rm.datasource.undo.oceanbase;

import org.apache.seata.common.util.DateUtil;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OceanBaseUndoLogManagerTest {

    private OceanBaseUndoLogManager undoLogManager;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;

    @BeforeEach
    void setUp() {
        undoLogManager = new OceanBaseUndoLogManager();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
    }

    @Test
    void testDeleteUndoLogByLogCreated_Success() throws SQLException {
        // Arrange
        Date logCreated = new Date();
        int limitRows = 100;
        String dateStr = DateUtil.formatDate(logCreated, "yyyy-MM-dd HH:mm:ss");

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(5);

        // Act
        int result = undoLogManager.deleteUndoLogByLogCreated(logCreated, limitRows, mockConnection);

        // Assert
        verify(mockPreparedStatement).setString(1, dateStr);
        verify(mockPreparedStatement).setInt(2, limitRows);
        verify(mockPreparedStatement).executeUpdate();
        assertEquals(5, result);
    }

    @Test
    void testDeleteUndoLogByLogCreated_ExceptionHandling() throws SQLException {
        // Arrange
        Date logCreated = new Date();
        int limitRows = 100;
        RuntimeException runtimeException = new RuntimeException("Test exception");

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        doThrow(runtimeException).when(mockPreparedStatement).executeUpdate();

        // Act & Assert
        assertThrows(SQLException.class, () -> {
            undoLogManager.deleteUndoLogByLogCreated(logCreated, limitRows, mockConnection);
        });
    }

    @Test
    void testInsertUndoLogWithNormal_Success() throws SQLException {
        // Arrange
        String xid = "xid-123";
        long branchId = 1001L;
        String rollbackCtx = "rollbackCtx";
        byte[] undoLogContent = "undoLogContent".getBytes();
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        undoLogManager.insertUndoLogWithNormal(xid, branchId, rollbackCtx, undoLogContent, mockConnection);

        // Assert
        verify(mockPreparedStatement).setLong(1, branchId);
        verify(mockPreparedStatement).setString(2, xid);
        verify(mockPreparedStatement).setString(3, rollbackCtx);
        verify(mockPreparedStatement).setBytes(4, undoLogContent);
        verify(mockPreparedStatement).setInt(5, 0); // State.Normal
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testInsertUndoLogWithGlobalFinished_Success() throws SQLException {
        // Arrange
        String xid = "xid-123";
        long branchId = 1001L;
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        UndoLogParser parser = mock(UndoLogParser.class);

        when(parser.getName()).thenReturn("");

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        undoLogManager.insertUndoLogWithGlobalFinished(xid, branchId, parser, mockConnection);

        // Assert
        verify(mockPreparedStatement).setLong(1, branchId);
        verify(mockPreparedStatement).setString(2, xid);
        verify(mockPreparedStatement).setString(3, "serializer=&compressorType=NONE");
        verify(mockPreparedStatement).setBytes(4, null);
        verify(mockPreparedStatement).setInt(5, 1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testGetCheckUndoLogTableExistSql() {
        // Act
        String sql = undoLogManager.getCheckUndoLogTableExistSql();

        // Assert
        assertEquals("SELECT 1 FROM undo_log WHERE ROWNUM = 1", sql);
    }
}
