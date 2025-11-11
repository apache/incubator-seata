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
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.vo.BranchSessionVO;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for BranchSessionDBServiceImpl
 */
@TestPropertySource(properties = {"sessionMode=db", "lockMode=file"})
class BranchSessionDBServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private BranchSessionDBServiceImpl branchSessionDBService;

    @MockBean
    private DataSource dataSource;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws SQLException {
        // Inject mock DataSource to service
        ReflectionTestUtils.setField(branchSessionDBService, "dataSource", dataSource);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
    }

    @Test
    void queryByXidSuccessTest() throws SQLException {
        String xid = "test-xid-001";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("xid")).thenReturn(xid);
        when(resultSet.getLong("branch_id")).thenReturn(123456L);
        when(resultSet.getString("application_id")).thenReturn("test-app");
        when(resultSet.getString("transaction_id")).thenReturn("1001");
        when(resultSet.getString("resource_id")).thenReturn("jdbc:mysql://localhost:3306/test");
        when(resultSet.getString("lock_key")).thenReturn("tb_test:1");
        when(resultSet.getString("branch_type")).thenReturn("AT");
        when(resultSet.getInt("status")).thenReturn(1);
        when(resultSet.getString("client_id")).thenReturn("192.168.1.1:8091");
        when(resultSet.getString("application_data")).thenReturn("{}");
        when(resultSet.getLong("gmt_create")).thenReturn(System.currentTimeMillis());
        when(resultSet.getLong("gmt_modified")).thenReturn(System.currentTimeMillis());

        PageResult<BranchSessionVO> result = branchSessionDBService.queryByXid(xid);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(2, result.getTotal());

        verify(preparedStatement, times(1)).setObject(1, xid);
        verify(resultSet, times(3)).next();
        verify(connection, times(1)).close();
    }

    @Test
    void queryByXidEmptyResultTest() throws SQLException {
        String xid = "test-xid-002";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        PageResult<BranchSessionVO> result = branchSessionDBService.queryByXid(xid);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());

        verify(connection, times(1)).close();
    }

    @Test
    void queryByXidBlankXidTest() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> branchSessionDBService.queryByXid(""));
        Assertions.assertEquals("xid should not be blank", exception.getMessage());
    }

    @Test
    void queryByXidNullXidTest() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> branchSessionDBService.queryByXid(null));
        Assertions.assertEquals("xid should not be blank", exception.getMessage());
    }

    @Test
    void queryByXidSqlExceptionTest() throws SQLException {
        String xid = "test-xid-003";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database connection error"));

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> branchSessionDBService.queryByXid(xid));
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof SQLException);

        verify(connection, times(1)).close();
    }

    @Test
    void queryByXidConnectionExceptionTest() throws SQLException {
        String xid = "test-xid-004";

        when(dataSource.getConnection()).thenThrow(new SQLException("Cannot get connection"));

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> branchSessionDBService.queryByXid(xid));
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof SQLException);
    }
}
