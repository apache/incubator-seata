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
package org.apache.seata.server.console.impl.db;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalLockParam;
import org.apache.seata.server.console.entity.vo.GlobalLockVO;
import org.apache.seata.server.console.exception.ConsoleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for GlobalLockDBServiceImpl
 */
@TestPropertySource(properties = {"lockMode=db", "sessionMode=file"})
class GlobalLockDBServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private GlobalLockDBServiceImpl globalLockDBService;

    @MockBean
    private DataSource dataSource;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private PreparedStatement countPreparedStatement;
    private ResultSet resultSet;
    private ResultSet countResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        // Inject mock DataSource to service
        ReflectionTestUtils.setField(globalLockDBService, "dataSource", dataSource);

        connection = org.mockito.Mockito.mock(Connection.class);
        preparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        countPreparedStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        resultSet = org.mockito.Mockito.mock(ResultSet.class);
        countResultSet = org.mockito.Mockito.mock(ResultSet.class);
    }

    @Test
    void querySuccessTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("xid")).thenReturn("test-xid-001");
        when(resultSet.getLong("transaction_id")).thenReturn(1001L);
        when(resultSet.getLong("branch_id")).thenReturn(2001L);
        when(resultSet.getString("resource_id")).thenReturn("jdbc:mysql://localhost:3306/test");
        when(resultSet.getString("table_name")).thenReturn("tb_order");
        when(resultSet.getString("pk")).thenReturn("1");
        when(resultSet.getString("row_key")).thenReturn("jdbc:mysql://localhost:3306/test^^^tb_order^^^1");
        when(resultSet.getLong("gmt_create")).thenReturn(System.currentTimeMillis());
        when(resultSet.getLong("gmt_modified")).thenReturn(System.currentTimeMillis());

        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(2);

        PageResult<GlobalLockVO> result = globalLockDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(1, result.getPageNum());
        Assertions.assertEquals(10, result.getPageSize());

        verify(connection, times(1)).close();
    }

    @Test
    void queryWithMultipleParamsTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");
        param.setTableName("tb_order");
        param.setTransactionId("1001");
        param.setBranchId("2001");
        param.setTimeStart(System.currentTimeMillis() - 86400000L);
        param.setTimeEnd(System.currentTimeMillis());

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(false);
        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(0);

        PageResult<GlobalLockVO> result = globalLockDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());

        verify(connection, times(1)).close();
    }

    @Test
    void queryEmptyResultTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(1);
        param.setPageSize(10);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(false);
        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(0);

        PageResult<GlobalLockVO> result = globalLockDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryInvalidPageNumTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(0);
        param.setPageSize(10);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void queryInvalidPageSizeTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(1);
        param.setPageSize(0);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void querySqlExceptionTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> globalLockDBService.query(param));
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof SQLException);

        verify(connection, times(1)).close();
    }

    @Test
    void deleteLockSuccessTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setBranchId("2001");
        param.setTableName("tb_order");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        SingleResult<Void> result = globalLockDBService.deleteLock(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());

        verify(preparedStatement, times(1)).setString(eq(1), anyString());
        verify(preparedStatement, times(1)).setString(eq(2), eq("test-xid-001"));
        verify(preparedStatement, times(1)).executeUpdate();
        verify(connection, times(1)).close();
    }

    @Test
    void deleteLockMissingXidTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setBranchId("2001");
        param.setTableName("tb_order");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void deleteLockMissingBranchIdTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setTableName("tb_order");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void deleteLockMissingTableNameTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setBranchId("2001");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void deleteLockMissingPkTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setBranchId("2001");
        param.setTableName("tb_order");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void deleteLockMissingResourceIdTest() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setBranchId("2001");
        param.setTableName("tb_order");
        param.setPk("1");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void deleteLockSqlExceptionTest() throws SQLException {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("test-xid-001");
        param.setBranchId("2001");
        param.setTableName("tb_order");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        ConsoleException exception =
                Assertions.assertThrows(ConsoleException.class, () -> globalLockDBService.deleteLock(param));
        Assertions.assertNotNull(exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("delete global lock"));

        verify(connection, times(1)).close();
    }
}
