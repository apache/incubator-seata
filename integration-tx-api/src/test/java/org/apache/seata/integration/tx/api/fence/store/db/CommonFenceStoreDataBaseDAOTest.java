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
package org.apache.seata.integration.tx.api.fence.store.db;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.exception.DataAccessException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommonFenceStoreDataBaseDAOTest {

    private CommonFenceStoreDataBaseDAO dao;

    @BeforeEach
    public void setUp() {
        dao = (CommonFenceStoreDataBaseDAO) CommonFenceStoreDataBaseDAO.getInstance();
        dao.setLogTableName(DefaultValues.DEFAULT_COMMON_FENCE_LOG_TABLE_NAME);
    }

    @Test
    public void testQueryCommonFenceDO() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("xid")).thenReturn("xid");
        when(resultSet.getLong("branch_id")).thenReturn(1L);
        when(resultSet.getInt("status")).thenReturn(2);

        CommonFenceDO fenceDO = dao.queryCommonFenceDO(connection, "xid", 1L);

        assertEquals("xid", fenceDO.getXid());
        assertEquals(1L, fenceDO.getBranchId());
        assertEquals(2, fenceDO.getStatus());
        verify(statement).setString(1, "xid");
        verify(statement).setLong(2, 1L);
    }

    @Test
    public void testQueryCommonFenceDOReturnsNullWhenNoRow() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(dao.queryCommonFenceDO(connection, "xid", 1L));
    }

    @Test
    public void testQueryCommonFenceDOWrapsSqlException() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("failed"));

        assertThrows(DataAccessException.class, () -> dao.queryCommonFenceDO(connection, "xid", 1L));
    }

    @Test
    public void testQueryEndStatusXidsByDate() throws SQLException {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:oracle:thin:@localhost:1521:xe");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("xid")).thenReturn("xid1", "xid2");

        Set<String> xids = dao.queryEndStatusXidsByDate(connection, new Date(1000L), 10);

        assertEquals(2, xids.size());
        assertTrue(xids.contains("xid1"));
        assertTrue(xids.contains("xid2"));
        verify(statement).setTimestamp(1, new Timestamp(1000L));
        verify(statement).setInt(2, 10);
    }

    @Test
    public void testInsertCommonFenceDO() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1, 0);

        assertTrue(dao.insertCommonFenceDO(connection, newFenceDO()));
        assertFalse(dao.insertCommonFenceDO(connection, newFenceDO()));
        verify(statement, times(2)).setString(1, "xid");
        verify(statement, times(2)).setLong(2, 1L);
        verify(statement, times(2)).setString(3, "prepare");
        verify(statement, times(2)).setInt(4, 1);
    }

    @Test
    public void testInsertCommonFenceDOWrapsDuplicateKey() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLIntegrityConstraintViolationException("duplicate"));

        assertThrows(CommonFenceException.class, () -> dao.insertCommonFenceDO(connection, newFenceDO()));
    }

    @Test
    public void testUpdateAndDeleteCommonFenceDO() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1, 0, 1);

        assertTrue(dao.updateCommonFenceDO(connection, "xid", 1L, 2, 1));
        assertFalse(dao.updateCommonFenceDO(connection, "xid", 1L, 2, 1));
        assertTrue(dao.deleteCommonFenceDO(connection, "xid", 1L));

        verify(statement, times(2)).setInt(1, 2);
        verify(statement, times(2)).setString(3, "xid");
        verify(statement, times(2)).setLong(4, 1L);
        verify(statement, times(2)).setInt(5, 1);
    }

    @Test
    public void testDeleteTCCFenceDO() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(2);

        assertEquals(2, dao.deleteTCCFenceDO(connection, Arrays.asList("xid1", "xid2")));
        verify(statement).setString(1, "xid1");
        verify(statement).setString(2, "xid2");
    }

    @Test
    public void testStoreMethodsWrapSqlException() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        doThrow(new SQLException("failed")).when(statement).executeUpdate();

        assertThrows(StoreException.class, () -> dao.insertCommonFenceDO(connection, newFenceDO()));
        assertThrows(StoreException.class, () -> dao.updateCommonFenceDO(connection, "xid", 1L, 2, 1));
        assertThrows(StoreException.class, () -> dao.deleteCommonFenceDO(connection, "xid", 1L));
        assertThrows(StoreException.class, () -> dao.deleteTCCFenceDO(connection, Arrays.asList("xid1", "xid2")));
    }

    private static CommonFenceDO newFenceDO() {
        CommonFenceDO fenceDO = new CommonFenceDO();
        fenceDO.setXid("xid");
        fenceDO.setBranchId(1L);
        fenceDO.setActionName("prepare");
        fenceDO.setStatus(1);
        return fenceDO;
    }
}
