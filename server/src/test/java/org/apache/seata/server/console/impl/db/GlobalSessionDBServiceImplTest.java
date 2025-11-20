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
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.console.entity.vo.BranchSessionVO;
import org.apache.seata.server.console.entity.vo.GlobalSessionVO;
import org.apache.seata.server.console.service.BranchSessionService;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"sessionMode=db", "lockMode=file"})
class GlobalSessionDBServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private GlobalSessionDBServiceImpl globalSessionDBService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private BranchSessionService branchSessionService;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private PreparedStatement countPreparedStatement;
    private ResultSet resultSet;
    private ResultSet countResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        // Inject mock DataSource to service
        ReflectionTestUtils.setField(globalSessionDBService, "dataSource", dataSource);

        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        countPreparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        countResultSet = mock(ResultSet.class);
    }

    @Test
    void querySuccessTest() throws SQLException {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");
        param.setWithBranch(false);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("xid")).thenReturn("test-xid-001");
        when(resultSet.getLong("transaction_id")).thenReturn(1001L);
        when(resultSet.getInt("status")).thenReturn(1);
        when(resultSet.getString("application_id")).thenReturn("test-app");
        when(resultSet.getString("transaction_service_group")).thenReturn("default_tx_group");
        when(resultSet.getString("transaction_name")).thenReturn("test-transaction");
        when(resultSet.getInt("timeout")).thenReturn(60000);
        when(resultSet.getLong("begin_time")).thenReturn(System.currentTimeMillis());
        when(resultSet.getString("application_data")).thenReturn("{}");
        when(resultSet.getLong("gmt_create")).thenReturn(System.currentTimeMillis());
        when(resultSet.getLong("gmt_modified")).thenReturn(System.currentTimeMillis());

        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(2);

        PageResult<GlobalSessionVO> result = globalSessionDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(1, result.getPageNum());
        Assertions.assertEquals(10, result.getPageSize());

        verify(connection, times(1)).close();
    }

    @Test
    void queryWithBranchTest() throws SQLException {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");
        param.setWithBranch(true);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("xid")).thenReturn("test-xid-001");
        when(resultSet.getLong("transaction_id")).thenReturn(1001L);
        when(resultSet.getInt("status")).thenReturn(1);
        when(resultSet.getString("application_id")).thenReturn("test-app");
        when(resultSet.getString("transaction_service_group")).thenReturn("default_tx_group");
        when(resultSet.getString("transaction_name")).thenReturn("test-transaction");
        when(resultSet.getInt("timeout")).thenReturn(60000);
        when(resultSet.getLong("begin_time")).thenReturn(System.currentTimeMillis());
        when(resultSet.getString("application_data")).thenReturn("{}");
        when(resultSet.getLong("gmt_create")).thenReturn(System.currentTimeMillis());
        when(resultSet.getLong("gmt_modified")).thenReturn(System.currentTimeMillis());

        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(1);

        List<BranchSessionVO> branchSessions = new ArrayList<>();
        BranchSessionVO branchVO = new BranchSessionVO();
        branchVO.setXid("test-xid-001");
        branchVO.setBranchId(2001L);
        branchSessions.add(branchVO);

        PageResult<BranchSessionVO> branchResult = PageResult.success(branchSessions, 1, 1, 10);
        when(branchSessionService.queryByXid("test-xid-001")).thenReturn(branchResult);

        PageResult<GlobalSessionVO> result = globalSessionDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertNotNull(result.getData().get(0).getBranchSessionVOs());
        Assertions.assertEquals(1, result.getData().get(0).getBranchSessionVOs().size());

        verify(branchSessionService, times(1)).queryByXid("test-xid-001");
        verify(connection, times(1)).close();
    }

    @Test
    void queryWithMultipleParamsTest() throws SQLException {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");
        param.setApplicationId("test-app");
        param.setStatus(1);
        param.setTransactionName("test-transaction");
        param.setTimeStart(System.currentTimeMillis() - 86400000L);
        param.setTimeEnd(System.currentTimeMillis());
        param.setWithBranch(false);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(false);
        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(0);

        PageResult<GlobalSessionVO> result = globalSessionDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());

        verify(connection, times(1)).close();
    }

    @Test
    void queryEmptyResultTest() throws SQLException {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setWithBranch(false);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement)
                .thenReturn(countPreparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(countPreparedStatement.executeQuery()).thenReturn(countResultSet);

        when(resultSet.next()).thenReturn(false);
        when(countResultSet.next()).thenReturn(true);
        when(countResultSet.getInt(1)).thenReturn(0);

        PageResult<GlobalSessionVO> result = globalSessionDBService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryInvalidPageNumTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(0);
        param.setPageSize(10);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalSessionDBService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void queryInvalidPageSizeTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(0);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalSessionDBService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void querySqlExceptionTest() throws SQLException {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("test-xid-001");
        param.setWithBranch(false);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        StoreException exception =
                Assertions.assertThrows(StoreException.class, () -> globalSessionDBService.query(param));
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof SQLException);

        verify(connection, times(1)).close();
    }
}
