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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.common.exception.*;
import org.apache.seata.core.store.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogStoreDaoUnitTest extends BaseSpringBootTest {
    private DataSource source;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private LogStoreDataBaseDAO dao;

    @BeforeEach
    void open() throws Exception {
        source = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables(any(), any(), any(), any())).thenReturn(mock(ResultSet.class));
        dao = new LogStoreDataBaseDAO(source);
        dao.setDbType("mysql");
        clearInvocations(source, connection, statement, result);
    }

    @Test
    void maxIdScansGlobalAndBranchTablesWithinBounds() throws Exception {
        when(result.next()).thenReturn(true, false, true, false);
        when(result.getLong(1)).thenReturn(12L, 20L);
        assertEquals(20L, dao.getCurrentMaxSessionId(100, 1));
        verify(statement, times(2)).setLong(1, 100);
        verify(statement, times(2)).setLong(2, 1);
        verify(connection, times(2)).close();
        verify(result, times(2)).close();
    }

    @Test
    void missingQueriesReturnEmptyResultsAndSqlFailuresPropagate() throws Exception {
        assertNull(dao.queryGlobalTransactionDO("xid"));
        assertNull(dao.queryGlobalTransactionDO(1L));
        assertTrue(dao.queryGlobalTransactionDO(new int[] {1}, 10).isEmpty());
        assertTrue(dao.queryBranchTransactionDO("xid").isEmpty());
        when(source.getConnection()).thenThrow(new SQLException("offline"));
        assertThrows(DataAccessException.class, () -> dao.queryGlobalTransactionDO("xid"));
        assertThrows(DataAccessException.class, () -> dao.queryGlobalTransactionDO(1L));
        assertThrows(DataAccessException.class, () -> dao.queryGlobalTransactionDO(new int[] {1}, 10));
        assertThrows(DataAccessException.class, () -> dao.queryBranchTransactionDO("xid"));
        assertThrows(DataAccessException.class, () -> dao.queryBranchTransactionDO(Collections.singletonList("xid")));
        assertThrows(DataAccessException.class, () -> dao.getCurrentMaxSessionId(100, 1));
        GlobalTransactionDO global = new GlobalTransactionDO();
        global.setStatus(1);
        global.setTransactionId(1L);
        global.setTimeout(1000);
        global.setBeginTime(1L);
        BranchTransactionDO branch = new BranchTransactionDO();
        branch.setBranchId(7L);
        branch.setStatus(1);
        branch.setTransactionId(1L);
        assertThrows(StoreException.class, () -> dao.insertGlobalTransactionDO(global));
        assertThrows(StoreException.class, () -> dao.updateGlobalTransactionDO(global));
        assertThrows(StoreException.class, () -> dao.updateGlobalTransactionDO(global, 1));
        assertThrows(StoreException.class, () -> dao.deleteGlobalTransactionDO(global));
        assertThrows(StoreException.class, () -> dao.insertBranchTransactionDO(branch));
        assertThrows(StoreException.class, () -> dao.updateBranchTransactionDO(branch));
        assertThrows(StoreException.class, () -> dao.deleteBranchTransactionDO(branch));
    }

    @Test
    void schemasFollowDatabaseDialectAndClosePostgresResources() throws Exception {
        dao.setDbType("h2");
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(dao, "getSchema", connection));
        dao.setDbType("sqlserver");
        when(connection.getSchema()).thenReturn("dbo");
        assertEquals(
                "dbo", org.springframework.test.util.ReflectionTestUtils.invokeMethod(dao, "getSchema", connection));
        dao.setDbType("postgresql");
        when(result.next()).thenReturn(true);
        when(result.getString(1)).thenReturn("transactions");
        assertEquals(
                "transactions",
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(dao, "getSchema", connection));
        verify(connection).prepareStatement("select current_schema");
        verify(statement).close();
        verify(result).close();
        when(statement.executeQuery()).thenThrow(new SQLException("schema unavailable"));
        assertThrows(
                StoreException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(dao, "getSchema", connection));
        verify(statement, times(2)).close();
    }
}
