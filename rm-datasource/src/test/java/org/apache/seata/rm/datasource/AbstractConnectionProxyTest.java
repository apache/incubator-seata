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
package org.apache.seata.rm.datasource;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractConnectionProxyTest {
    private DataSourceProxy mockedDataSourceProxy;
    private Connection mockedTargetConnection;

    private TestConnectionProxy testConnectionProxy;

    @BeforeEach
    public void beforeEach() {
        mockedTargetConnection = mock(Connection.class);
        mockedDataSourceProxy = mock(DataSourceProxy.class);

        testConnectionProxy = new TestConnectionProxy(mockedTargetConnection, mockedDataSourceProxy);
    }

    @AfterEach
    public void afterEach() {
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    public void testPrepareStatement() throws SQLException {
        PreparedStatement mockedPreparedStatement = mock(PreparedStatement.class);
        String mockedSql = "mocked sql";
        when(mockedTargetConnection.prepareStatement(mockedSql)).thenReturn(mockedPreparedStatement);

        PreparedStatement actual = testConnectionProxy.prepareStatement(mockedSql);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection).prepareStatement(mockedSql);
    }

    @Test
    public void testPrepareStatementWhenBranchTypeIsAT() throws SQLException {
        TableMetaCache mockedTableMetaCache = mock(TableMetaCache.class);
        TableMeta mockedTableMeta = mock(TableMeta.class);

        String mockedDBType = JdbcConstants.MYSQL;
        String mockedSql = "INSERT INTO t1 (id) VALUES (1)";
        String mockedResourceId = "mockedResourceId";

        when(mockedDataSourceProxy.getDbType()).thenReturn(mockedDBType);
        when(mockedDataSourceProxy.getResourceId()).thenReturn(mockedResourceId);
        when(mockedTableMetaCache.getTableMeta(mockedTargetConnection, "t1", mockedResourceId)).thenReturn(mockedTableMeta);
        when(mockedTableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));

        try (MockedStatic<TableMetaCacheFactory> tableMetaCacheFactoryMockedStatic = mockStatic(TableMetaCacheFactory.class)) {
            tableMetaCacheFactoryMockedStatic
                    .when(() -> TableMetaCacheFactory.getTableMetaCache(mockedDBType))
                    .thenReturn(mockedTableMetaCache);

            RootContext.bind("test-xid-for-branch");
            RootContext.bindBranchType(BranchType.AT);
            PreparedStatement actual = testConnectionProxy.prepareStatement(mockedSql);

            assertNotNull(actual);
            assertInstanceOf(PreparedStatementProxy.class, actual);
            verify(mockedTargetConnection).prepareStatement(mockedSql, new String[]{"id"});
            verify(mockedTableMetaCache).getTableMeta(mockedTargetConnection, "t1", mockedResourceId);
        }
    }

    @Test
    public void testPrepareCall() throws SQLException {
        String sql = "{call test()}";
        CallableStatement mockCallable = mock(CallableStatement.class);
        when(mockedTargetConnection.prepareCall(sql)).thenReturn(mockCallable);

        CallableStatement actualResult = testConnectionProxy.prepareCall(sql);

        assertNotNull(actualResult);
        assertInstanceOf(CallableStatement.class, actualResult);
        verify(mockedTargetConnection).prepareCall(sql);
    }

    @Test
    public void testPrepareCallWithTypeAndConcurrency() throws SQLException {
        String mockedSql = "mock sql";
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        CallableStatement mockCallable = mock(CallableStatement.class);
        when(mockedTargetConnection.prepareCall(mockedSql, mockedResultSetType, mockedResultSetConcurrency))
                .thenReturn(mockCallable);

        CallableStatement actual =
                testConnectionProxy.prepareCall(mockedSql, mockedResultSetType, mockedResultSetConcurrency);

        assertNotNull(actual);
        assertInstanceOf(CallableStatement.class, actual);
        verify(mockedTargetConnection).prepareCall(mockedSql, mockedResultSetType, mockedResultSetConcurrency);
    }

    @Test
    public void testPrepareCallWithTypeAndConcurrencyAndHoldability() throws SQLException {
        String mockedSql = "mock sql";
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        int mockedResultSetHoldability = ResultSet.HOLD_CURSORS_OVER_COMMIT;

        CallableStatement mockCallable = mock(CallableStatement.class);
        when(mockedTargetConnection.prepareCall(
                mockedSql, mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability))
                .thenReturn(mockCallable);

        CallableStatement actual = testConnectionProxy.prepareCall(
                mockedSql, mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);

        assertNotNull(actual);
        assertInstanceOf(CallableStatement.class, actual);
        verify(mockedTargetConnection)
                .prepareCall(mockedSql, mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);
    }

    @Test
    public void shouldCallNativeSQL() throws SQLException {
        String sql = "mock native sql";

        testConnectionProxy.nativeSQL(sql);

        verify(mockedTargetConnection).nativeSQL(sql);
    }

    @Test
    public void shouldCallGetAutoCommit() throws SQLException {
        when(mockedTargetConnection.getAutoCommit()).thenReturn(true);

        boolean autoCommit = testConnectionProxy.getAutoCommit();

        assertTrue(autoCommit);
        verify(mockedTargetConnection).getAutoCommit();
    }

    @Test
    public void shouldCloseConnection() throws SQLException {
        testConnectionProxy.close();

        verify(mockedTargetConnection).close();
    }

    @Test
    public void shouldCallIsClosed() throws SQLException {
        when(mockedTargetConnection.isClosed()).thenReturn(true);

        boolean isClosed = testConnectionProxy.isClosed();

        assertTrue(isClosed);
        verify(mockedTargetConnection).isClosed();
    }

    @Test
    public void shouldGetMetaData() throws SQLException {
        testConnectionProxy.getMetaData();

        verify(mockedTargetConnection).getMetaData();
    }

    @Test
    public void shouldSetReadOnly() throws SQLException {
        testConnectionProxy.setReadOnly(true);

        verify(mockedTargetConnection).setReadOnly(true);
    }

    @Test
    public void shouldIsReadOnly() throws SQLException {
        when(mockedTargetConnection.isReadOnly()).thenReturn(true);

        boolean isReadOnly = testConnectionProxy.isReadOnly();

        assertTrue(isReadOnly);
        verify(mockedTargetConnection).isReadOnly();
    }

    @Test
    public void shouldSetCatalog() throws SQLException {
        testConnectionProxy.setCatalog("mockCatalog");

        verify(mockedTargetConnection).setCatalog("mockCatalog");
    }

    @Test
    public void shouldGetCatalog() throws SQLException {
        when(mockedTargetConnection.getCatalog()).thenReturn("mockCatalog");

        String actual = testConnectionProxy.getCatalog();

        assertEquals("mockCatalog", actual);
        verify(mockedTargetConnection).getCatalog();
    }

    @Test
    public void shouldSetTransactionIsolation() throws SQLException {
        int level = Connection.TRANSACTION_READ_COMMITTED;

        testConnectionProxy.setTransactionIsolation(level);

        verify(mockedTargetConnection).setTransactionIsolation(level);
    }

    @Test
    public void shouldGetTransactionIsolation() throws SQLException {
        int expectedLevel = Connection.TRANSACTION_READ_COMMITTED;
        when(mockedTargetConnection.getTransactionIsolation()).thenReturn(expectedLevel);

        int actual = testConnectionProxy.getTransactionIsolation();

        assertEquals(expectedLevel, actual);
        verify(mockedTargetConnection).getTransactionIsolation();
    }

    @Test
    public void shouldGetWarnings() throws SQLException {
        testConnectionProxy.getWarnings();

        verify(mockedTargetConnection).getWarnings();
    }

    @Test
    public void shouldClearWarnings() throws SQLException {
        testConnectionProxy.clearWarnings();

        verify(mockedTargetConnection).clearWarnings();
    }

    @Test
    public void shouldCreateStatement() throws SQLException {
        Statement actual = testConnectionProxy.createStatement();

        verify(mockedTargetConnection).createStatement();
        assertInstanceOf(StatementProxy.class, actual);
    }

    @Test
    public void testCreateStatementWithTypeAndConcurrency() throws SQLException {
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

        Statement actual = testConnectionProxy.createStatement(mockedResultSetType, mockedResultSetConcurrency);

        assertNotNull(actual);
        assertInstanceOf(StatementProxy.class, actual);
        verify(mockedTargetConnection).createStatement(mockedResultSetType, mockedResultSetConcurrency);
    }

    @Test
    public void testCreateStatementWithTypeAndConcurrencyAndHoldability() throws SQLException {
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        int mockedResultSetHoldability = ResultSet.HOLD_CURSORS_OVER_COMMIT;

        Statement actual = testConnectionProxy.createStatement(
                mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);

        assertNotNull(actual);
        assertInstanceOf(StatementProxy.class, actual);
        verify(mockedTargetConnection)
                .createStatement(mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);
    }

    @Test
    public void testPrepareStatementWithTypeAndConcurrency() throws SQLException {
        String mockedSql = "mock sql";
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

        PreparedStatement actual =
                testConnectionProxy.prepareStatement(mockedSql, mockedResultSetType, mockedResultSetConcurrency);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection).prepareStatement(mockedSql, mockedResultSetType, mockedResultSetConcurrency);
    }

    @Test
    public void testPrepareStatementWithTypeAndConcurrencyAndHoldability() throws SQLException {
        String mockedSql = "mock sql";
        int mockedResultSetType = ResultSet.TYPE_FORWARD_ONLY;
        int mockedResultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        int mockedResultSetHoldability = ResultSet.HOLD_CURSORS_OVER_COMMIT;

        PreparedStatement actual = testConnectionProxy.prepareStatement(
                mockedSql, mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection)
                .prepareStatement(
                        mockedSql, mockedResultSetType, mockedResultSetConcurrency, mockedResultSetHoldability);
    }

    @Test
    public void testPrepareStatementWithAutoGeneratedKeys() throws SQLException {
        String mockedSql = "mock sql";
        int mockedAutoGeneratedKeys = Statement.RETURN_GENERATED_KEYS;

        PreparedStatement actual = testConnectionProxy.prepareStatement(mockedSql, mockedAutoGeneratedKeys);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection).prepareStatement(mockedSql, mockedAutoGeneratedKeys);
    }

    @Test
    public void testPrepareStatementWithColumnIndexes() throws SQLException {
        String mockedSql = "mock sql";
        int[] mockedColumnIndexes = {1, 2};

        PreparedStatement actual = testConnectionProxy.prepareStatement(mockedSql, mockedColumnIndexes);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection).prepareStatement(mockedSql, mockedColumnIndexes);
    }

    @Test
    public void testPrepareStatementWithColumnNames() throws SQLException {
        String mockedSql = "mock sql";
        String[] mockedColumnNames = {"col1", "col2"};

        PreparedStatement actual = testConnectionProxy.prepareStatement(mockedSql, mockedColumnNames);

        assertNotNull(actual);
        assertInstanceOf(PreparedStatementProxy.class, actual);
        verify(mockedTargetConnection).prepareStatement(mockedSql, mockedColumnNames);
    }

    @Test
    public void testGetTypeMap() throws SQLException {
        testConnectionProxy.getTypeMap();

        verify(mockedTargetConnection).getTypeMap();
    }

    @Test
    public void testSetTypeMap() throws SQLException {
        testConnectionProxy.setTypeMap(null);

        verify(mockedTargetConnection).setTypeMap(null);
    }

    @Test
    public void testSetHoldability() throws SQLException {
        int holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        testConnectionProxy.setHoldability(holdability);

        verify(mockedTargetConnection).setHoldability(holdability);
    }

    @Test
    public void testGetHoldability() throws SQLException {
        when(mockedTargetConnection.getHoldability()).thenReturn(ResultSet.HOLD_CURSORS_OVER_COMMIT);

        int actual = testConnectionProxy.getHoldability();

        verify(mockedTargetConnection).getHoldability();
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, actual);
    }

    @Test
    public void testCreateClob() throws SQLException {
        testConnectionProxy.createClob();

        verify(mockedTargetConnection).createClob();
    }

    @Test
    public void testCreateBlob() throws SQLException {
        testConnectionProxy.createBlob();

        verify(mockedTargetConnection).createBlob();
    }

    @Test
    public void testCreateNClob() throws SQLException {
        testConnectionProxy.createNClob();

        verify(mockedTargetConnection).createNClob();
    }

    @Test
    public void testCreateSQLXML() throws SQLException {
        testConnectionProxy.createSQLXML();

        verify(mockedTargetConnection).createSQLXML();
    }

    @Test
    public void testIsValid() throws SQLException {
        when(mockedTargetConnection.isValid(10)).thenReturn(true);

        boolean actual = testConnectionProxy.isValid(10);

        verify(mockedTargetConnection).isValid(10);
        assertTrue(actual);
    }

    @Test
    public void testSetClientInfo() throws SQLException {
        String name = "clientName";
        String value = "clientValue";
        testConnectionProxy.setClientInfo(name, value);

        verify(mockedTargetConnection).setClientInfo(name, value);
    }

    @Test
    public void testSetClientInfoWithProperties() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("clientName", "clientValue");

        testConnectionProxy.setClientInfo(properties);

        verify(mockedTargetConnection).setClientInfo(properties);
    }

    @Test
    public void testGetClientInfo() throws SQLException {
        Properties mockedClientInfo = new Properties();
        when(mockedTargetConnection.getClientInfo()).thenReturn(mockedClientInfo);

        Properties actual = testConnectionProxy.getClientInfo();

        assertEquals(mockedClientInfo, actual);
        verify(mockedTargetConnection).getClientInfo();
    }

    @Test
    public void testGetClientInfoWithName() throws SQLException {
        when(mockedTargetConnection.getClientInfo("clientName")).thenReturn("clientValue");

        String actual = testConnectionProxy.getClientInfo("clientName");
        assertEquals("clientValue", actual);
        verify(mockedTargetConnection).getClientInfo("clientName");
    }

    @Test
    public void testCreateArrayOf() throws SQLException {
        String typeName = "typeName";
        Object[] elements = new Object[]{"elem1", "elem2"};

        testConnectionProxy.createArrayOf(typeName, elements);

        verify(mockedTargetConnection).createArrayOf(typeName, elements);
    }

    @Test
    public void testCreateStruct() throws SQLException {
        String typeName = "typeName";
        Object[] attributes = new Object[]{"attr1", "attr2"};

        testConnectionProxy.createStruct(typeName, attributes);

        verify(mockedTargetConnection).createStruct(typeName, attributes);
    }

    @Test
    public void testSetSchema() throws SQLException {
        String schema = "testSchema";
        testConnectionProxy.setSchema(schema);

        verify(mockedTargetConnection).setSchema(schema);
    }

    @Test
    public void testGetSchema() throws SQLException {
        when(mockedTargetConnection.getSchema()).thenReturn("testSchema");

        String actual = testConnectionProxy.getSchema();

        assertEquals("testSchema", actual);
        verify(mockedTargetConnection).getSchema();
    }

    @Test
    public void testAbort() throws SQLException {
        Executor executor = Runnable::run;
        testConnectionProxy.abort(executor);

        verify(mockedTargetConnection).abort(executor);
    }

    @Test
    public void testSetNetworkTimeout() throws SQLException {
        Executor executor = Runnable::run;
        int milliseconds = 1000;

        testConnectionProxy.setNetworkTimeout(executor, milliseconds);

        verify(mockedTargetConnection).setNetworkTimeout(executor, milliseconds);
    }

    @Test
    public void testGetNetworkTimeout() throws SQLException {
        when(mockedTargetConnection.getNetworkTimeout()).thenReturn(1000);

        int actual = testConnectionProxy.getNetworkTimeout();
        assertEquals(1000, actual);
        verify(mockedTargetConnection).getNetworkTimeout();
    }

    @Test
    public void testUnwrap() throws SQLException {
        when(mockedTargetConnection.unwrap(CallableStatement.class)).thenReturn(mock(CallableStatement.class));

        CallableStatement actual = testConnectionProxy.unwrap(CallableStatement.class);
        assertNotNull(actual);
        verify(mockedTargetConnection).unwrap(CallableStatement.class);
    }

    @Test
    public void testIsWrapperFor() throws SQLException {
        when(mockedTargetConnection.isWrapperFor(CallableStatement.class)).thenReturn(true);

        boolean actual = testConnectionProxy.isWrapperFor(CallableStatement.class);

        assertTrue(actual);
        verify(mockedTargetConnection).isWrapperFor(CallableStatement.class);
    }

    /**
     * Minimal test implementation of AbstractConnectionProxy to allow testing delegation.
     */
    private static class TestConnectionProxy extends AbstractConnectionProxy {
        public TestConnectionProxy(Connection originalConnection, DataSourceProxy dataSourceProxy) {
            super(dataSourceProxy, originalConnection);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
        }

        @Override
        public void commit() throws SQLException {
        }

        @Override
        public void rollback() throws SQLException {
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        }
    }
}
