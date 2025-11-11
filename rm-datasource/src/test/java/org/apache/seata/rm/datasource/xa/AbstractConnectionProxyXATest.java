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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.rm.BaseDataSourceResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.XAConnection;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AbstractConnectionProxyXA
 *
 */
public class AbstractConnectionProxyXATest {

    private Connection mockConnection;
    private XAConnection mockXAConnection;
    private BaseDataSourceResource mockResource;
    private String xid;
    private TestConnectionProxyXA connectionProxy;

    @BeforeEach
    public void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockXAConnection = mock(XAConnection.class);
        mockResource = mock(BaseDataSourceResource.class);
        xid = "testXid";

        connectionProxy = new TestConnectionProxyXA(mockConnection, mockXAConnection, mockResource, xid);
    }

    @Test
    public void testGetWrappedXAConnection() {
        XAConnection result = connectionProxy.getWrappedXAConnection();
        Assertions.assertEquals(mockXAConnection, result);
    }

    @Test
    public void testGetWrappedConnection() {
        Connection result = connectionProxy.getWrappedConnection();
        Assertions.assertEquals(mockConnection, result);
    }

    @Test
    public void testCreateStatement() throws SQLException {
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        Statement result = connectionProxy.createStatement();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof StatementProxyXA);
        verify(mockConnection).createStatement();
    }

    @Test
    public void testCreateStatementWithParameters() throws SQLException {
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement(anyInt(), anyInt())).thenReturn(mockStatement);

        Statement result = connectionProxy.createStatement(1, 2);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof StatementProxyXA);
        verify(mockConnection).createStatement(1, 2);
    }

    @Test
    public void testCreateStatementWithThreeParameters() throws SQLException {
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(mockStatement);

        Statement result = connectionProxy.createStatement(1, 2, 3);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof StatementProxyXA);
        verify(mockConnection).createStatement(1, 2, 3);
    }

    @Test
    public void testPrepareStatement() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        PreparedStatement result = connectionProxy.prepareStatement("SELECT * FROM test");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("SELECT * FROM test");
    }

    @Test
    public void testPrepareStatementWithParameters() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mockPreparedStatement);

        PreparedStatement result = connectionProxy.prepareStatement("SELECT * FROM test", 1, 2);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("SELECT * FROM test", 1, 2);
    }

    @Test
    public void testPrepareStatementWithThreeParameters() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockPreparedStatement);

        PreparedStatement result = connectionProxy.prepareStatement("SELECT * FROM test", 1, 2, 3);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("SELECT * FROM test", 1, 2, 3);
    }

    @Test
    public void testPrepareStatementWithAutoGeneratedKeys() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);

        PreparedStatement result =
                connectionProxy.prepareStatement("INSERT INTO test", Statement.RETURN_GENERATED_KEYS);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("INSERT INTO test", Statement.RETURN_GENERATED_KEYS);
    }

    @Test
    public void testPrepareStatementWithColumnIndexes() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        int[] columnIndexes = {1, 2};
        when(mockConnection.prepareStatement(anyString(), any(int[].class))).thenReturn(mockPreparedStatement);

        PreparedStatement result = connectionProxy.prepareStatement("INSERT INTO test", columnIndexes);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("INSERT INTO test", columnIndexes);
    }

    @Test
    public void testPrepareStatementWithColumnNames() throws SQLException {
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        String[] columnNames = {"id", "name"};
        when(mockConnection.prepareStatement(anyString(), any(String[].class))).thenReturn(mockPreparedStatement);

        PreparedStatement result = connectionProxy.prepareStatement("INSERT INTO test", columnNames);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof PreparedStatementProxyXA);
        verify(mockConnection).prepareStatement("INSERT INTO test", columnNames);
    }

    @Test
    public void testNativeSQL() throws SQLException {
        String sql = "SELECT * FROM test";
        String nativeSQL = "SELECT * FROM test_native";
        when(mockConnection.nativeSQL(sql)).thenReturn(nativeSQL);

        String result = connectionProxy.nativeSQL(sql);

        Assertions.assertEquals(nativeSQL, result);
        verify(mockConnection).nativeSQL(sql);
    }

    @Test
    public void testIsClosed() throws SQLException {
        when(mockConnection.isClosed()).thenReturn(false);

        boolean result = connectionProxy.isClosed();

        Assertions.assertFalse(result);
        verify(mockConnection).isClosed();
    }

    @Test
    public void testGetMetaData() throws SQLException {
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);

        DatabaseMetaData result = connectionProxy.getMetaData();

        Assertions.assertEquals(mockMetaData, result);
        verify(mockConnection).getMetaData();
    }

    @Test
    public void testSetReadOnly() throws SQLException {
        connectionProxy.setReadOnly(true);

        verify(mockConnection).setReadOnly(true);
    }

    @Test
    public void testIsReadOnly() throws SQLException {
        when(mockConnection.isReadOnly()).thenReturn(true);

        boolean result = connectionProxy.isReadOnly();

        Assertions.assertTrue(result);
        verify(mockConnection).isReadOnly();
    }

    @Test
    public void testSetCatalog() throws SQLException {
        String catalog = "testCatalog";
        connectionProxy.setCatalog(catalog);

        verify(mockConnection).setCatalog(catalog);
    }

    @Test
    public void testGetCatalog() throws SQLException {
        String catalog = "testCatalog";
        when(mockConnection.getCatalog()).thenReturn(catalog);

        String result = connectionProxy.getCatalog();

        Assertions.assertEquals(catalog, result);
        verify(mockConnection).getCatalog();
    }

    @Test
    public void testSetTransactionIsolation() throws SQLException {
        connectionProxy.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        verify(mockConnection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Test
    public void testGetTransactionIsolation() throws SQLException {
        when(mockConnection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);

        int result = connectionProxy.getTransactionIsolation();

        Assertions.assertEquals(Connection.TRANSACTION_READ_COMMITTED, result);
        verify(mockConnection).getTransactionIsolation();
    }

    @Test
    public void testGetWarnings() throws SQLException {
        SQLWarning mockWarning = mock(SQLWarning.class);
        when(mockConnection.getWarnings()).thenReturn(mockWarning);

        SQLWarning result = connectionProxy.getWarnings();

        Assertions.assertEquals(mockWarning, result);
        verify(mockConnection).getWarnings();
    }

    @Test
    public void testClearWarnings() throws SQLException {
        connectionProxy.clearWarnings();

        verify(mockConnection).clearWarnings();
    }

    @Test
    public void testGetTypeMap() throws SQLException {
        Map<String, Class<?>> typeMap = new HashMap<>();
        when(mockConnection.getTypeMap()).thenReturn(typeMap);

        Map<String, Class<?>> result = connectionProxy.getTypeMap();

        Assertions.assertEquals(typeMap, result);
        verify(mockConnection).getTypeMap();
    }

    @Test
    public void testSetTypeMap() throws SQLException {
        Map<String, Class<?>> typeMap = new HashMap<>();
        connectionProxy.setTypeMap(typeMap);

        verify(mockConnection).setTypeMap(typeMap);
    }

    @Test
    public void testSetHoldability() throws SQLException {
        connectionProxy.setHoldability(1);

        verify(mockConnection).setHoldability(1);
    }

    @Test
    public void testGetHoldability() throws SQLException {
        when(mockConnection.getHoldability()).thenReturn(1);

        int result = connectionProxy.getHoldability();

        Assertions.assertEquals(1, result);
        verify(mockConnection).getHoldability();
    }

    @Test
    public void testSetSavepoint() throws SQLException {
        Savepoint mockSavepoint = mock(Savepoint.class);
        when(mockConnection.setSavepoint()).thenReturn(mockSavepoint);

        Savepoint result = connectionProxy.setSavepoint();

        Assertions.assertEquals(mockSavepoint, result);
        verify(mockConnection).setSavepoint();
    }

    @Test
    public void testSetSavepointWithName() throws SQLException {
        Savepoint mockSavepoint = mock(Savepoint.class);
        when(mockConnection.setSavepoint("sp1")).thenReturn(mockSavepoint);

        Savepoint result = connectionProxy.setSavepoint("sp1");

        Assertions.assertEquals(mockSavepoint, result);
        verify(mockConnection).setSavepoint("sp1");
    }

    @Test
    public void testRollbackSavepoint() throws SQLException {
        Savepoint mockSavepoint = mock(Savepoint.class);
        connectionProxy.rollback(mockSavepoint);

        verify(mockConnection).rollback(mockSavepoint);
    }

    @Test
    public void testReleaseSavepoint() throws SQLException {
        Savepoint mockSavepoint = mock(Savepoint.class);
        connectionProxy.releaseSavepoint(mockSavepoint);

        verify(mockConnection).releaseSavepoint(mockSavepoint);
    }

    @Test
    public void testCreateClob() throws SQLException {
        Clob mockClob = mock(Clob.class);
        when(mockConnection.createClob()).thenReturn(mockClob);

        Clob result = connectionProxy.createClob();

        Assertions.assertEquals(mockClob, result);
        verify(mockConnection).createClob();
    }

    @Test
    public void testCreateBlob() throws SQLException {
        Blob mockBlob = mock(Blob.class);
        when(mockConnection.createBlob()).thenReturn(mockBlob);

        Blob result = connectionProxy.createBlob();

        Assertions.assertEquals(mockBlob, result);
        verify(mockConnection).createBlob();
    }

    @Test
    public void testCreateNClob() throws SQLException {
        NClob mockNClob = mock(NClob.class);
        when(mockConnection.createNClob()).thenReturn(mockNClob);

        NClob result = connectionProxy.createNClob();

        Assertions.assertEquals(mockNClob, result);
        verify(mockConnection).createNClob();
    }

    @Test
    public void testCreateSQLXML() throws SQLException {
        SQLXML mockSQLXML = mock(SQLXML.class);
        when(mockConnection.createSQLXML()).thenReturn(mockSQLXML);

        SQLXML result = connectionProxy.createSQLXML();

        Assertions.assertEquals(mockSQLXML, result);
        verify(mockConnection).createSQLXML();
    }

    @Test
    public void testIsValid() throws SQLException {
        when(mockConnection.isValid(10)).thenReturn(true);

        boolean result = connectionProxy.isValid(10);

        Assertions.assertTrue(result);
        verify(mockConnection).isValid(10);
    }

    @Test
    public void testSetClientInfo() throws SQLClientInfoException {
        connectionProxy.setClientInfo("name", "value");

        verify(mockConnection).setClientInfo("name", "value");
    }

    @Test
    public void testSetClientInfoWithProperties() throws SQLClientInfoException {
        Properties properties = new Properties();
        connectionProxy.setClientInfo(properties);

        verify(mockConnection).setClientInfo(properties);
    }

    @Test
    public void testGetClientInfo() throws SQLException {
        when(mockConnection.getClientInfo("name")).thenReturn("value");

        String result = connectionProxy.getClientInfo("name");

        Assertions.assertEquals("value", result);
        verify(mockConnection).getClientInfo("name");
    }

    @Test
    public void testGetClientInfoProperties() throws SQLException {
        Properties properties = new Properties();
        when(mockConnection.getClientInfo()).thenReturn(properties);

        Properties result = connectionProxy.getClientInfo();

        Assertions.assertEquals(properties, result);
        verify(mockConnection).getClientInfo();
    }

    @Test
    public void testCreateArrayOf() throws SQLException {
        Array mockArray = mock(Array.class);
        Object[] elements = {"a", "b"};
        when(mockConnection.createArrayOf("VARCHAR", elements)).thenReturn(mockArray);

        Array result = connectionProxy.createArrayOf("VARCHAR", elements);

        Assertions.assertEquals(mockArray, result);
        verify(mockConnection).createArrayOf("VARCHAR", elements);
    }

    @Test
    public void testCreateStruct() throws SQLException {
        Struct mockStruct = mock(Struct.class);
        Object[] attributes = {1, "test"};
        when(mockConnection.createStruct("TYPE", attributes)).thenReturn(mockStruct);

        Struct result = connectionProxy.createStruct("TYPE", attributes);

        Assertions.assertEquals(mockStruct, result);
        verify(mockConnection).createStruct("TYPE", attributes);
    }

    @Test
    public void testSetSchema() throws SQLException {
        connectionProxy.setSchema("testSchema");

        verify(mockConnection).setSchema("testSchema");
    }

    @Test
    public void testGetSchema() throws SQLException {
        when(mockConnection.getSchema()).thenReturn("testSchema");

        String result = connectionProxy.getSchema();

        Assertions.assertEquals("testSchema", result);
        verify(mockConnection).getSchema();
    }

    @Test
    public void testAbort() throws SQLException {
        Executor mockExecutor = mock(Executor.class);
        connectionProxy.abort(mockExecutor);

        verify(mockConnection).abort(mockExecutor);
    }

    @Test
    public void testSetNetworkTimeout() throws SQLException {
        Executor mockExecutor = mock(Executor.class);
        connectionProxy.setNetworkTimeout(mockExecutor, 1000);

        verify(mockConnection).setNetworkTimeout(mockExecutor, 1000);
    }

    @Test
    public void testGetNetworkTimeout() throws SQLException {
        when(mockConnection.getNetworkTimeout()).thenReturn(1000);

        int result = connectionProxy.getNetworkTimeout();

        Assertions.assertEquals(1000, result);
        verify(mockConnection).getNetworkTimeout();
    }

    @Test
    public void testUnwrap() throws SQLException {
        when(mockConnection.unwrap(Connection.class)).thenReturn(mockConnection);

        Connection result = connectionProxy.unwrap(Connection.class);

        Assertions.assertEquals(mockConnection, result);
        verify(mockConnection).unwrap(Connection.class);
    }

    @Test
    public void testIsWrapperFor() throws SQLException {
        when(mockConnection.isWrapperFor(Connection.class)).thenReturn(true);

        boolean result = connectionProxy.isWrapperFor(Connection.class);

        Assertions.assertTrue(result);
        verify(mockConnection).isWrapperFor(Connection.class);
    }

    /**
     * Test implementation of AbstractConnectionProxyXA for testing purposes
     */
    private static class TestConnectionProxyXA extends AbstractConnectionProxyXA {
        public TestConnectionProxyXA(
                Connection originalConnection, XAConnection xaConnection, BaseDataSourceResource resource, String xid) {
            super(originalConnection, xaConnection, resource, xid);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            // Test implementation
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return false;
        }

        @Override
        public void commit() throws SQLException {
            // Test implementation
        }

        @Override
        public void rollback() throws SQLException {
            // Test implementation
        }

        @Override
        public void close() throws SQLException {
            // Test implementation
        }
    }
}
