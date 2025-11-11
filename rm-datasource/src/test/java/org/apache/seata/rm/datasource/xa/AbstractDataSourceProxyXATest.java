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

import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AbstractDataSourceProxyXA
 *
 */
public class AbstractDataSourceProxyXATest {

    private TestDataSourceProxyXA dataSourceProxy;
    private XAXid xaXid;

    @BeforeEach
    public void setUp() {
        dataSourceProxy = new TestDataSourceProxyXA();
        xaXid = XAXidBuilder.build("testXid", 123L);
    }

    @Test
    public void testGetConnectionForXAFinish_WithExistingConnection() throws SQLException {
        // Mock existing connection
        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(mockWrappedConnection.isClosed()).thenReturn(false);

        // Put connection in the lookup
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Get connection for XA finish
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        Assertions.assertEquals(mockConnection, result);
        verify(mockConnection).getWrappedConnection();
        verify(mockWrappedConnection).isClosed();
    }

    @Test
    public void testGetConnectionForXAFinish_WithClosedConnection() throws SQLException {
        // Mock closed connection
        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(mockWrappedConnection.isClosed()).thenReturn(true);

        // Put connection in the lookup
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Get connection for XA finish
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        // Should return a new connection from getConnectionProxyXA
        Assertions.assertNotNull(result);
        verify(mockConnection).getWrappedConnection();
        verify(mockWrappedConnection).isClosed();
    }

    @Test
    public void testGetConnectionForXAFinish_NoExistingConnection() throws SQLException {
        // Get connection for XA finish without existing connection
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        // Should return a new connection from getConnectionProxyXA
        Assertions.assertNotNull(result);
    }

    @Test
    public void testForceClosePhysicalConnection_WithExistingConnection() throws SQLException {
        // Mock connection
        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);

        // Setup the mock behavior
        Mockito.doNothing().when(mockConnection).close();
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        Mockito.doNothing().when(mockWrappedConnection).close();

        // Put connection in the lookup
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Force close physical connection
        dataSourceProxy.forceClosePhysicalConnection(xaXid);

        verify(mockConnection).close();
        verify(mockConnection, times(1)).getWrappedConnection();
        verify(mockWrappedConnection).close();
    }

    @Test
    public void testForceClosePhysicalConnection_WithPooledConnection() throws SQLException {
        // Create a mock that implements both Connection and PooledConnection
        Connection mockWrappedConnection =
                mock(Connection.class, Mockito.withSettings().extraInterfaces(PooledConnection.class));
        Connection mockPhysicalConnection = mock(Connection.class);

        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);

        // Setup the mock behavior
        Mockito.doNothing().when(mockConnection).close();
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(((PooledConnection) mockWrappedConnection).getConnection()).thenReturn(mockPhysicalConnection);
        Mockito.doNothing().when(mockPhysicalConnection).close();

        // Put connection in the lookup
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Force close physical connection
        dataSourceProxy.forceClosePhysicalConnection(xaXid);

        verify(mockConnection).close();
        verify(((PooledConnection) mockWrappedConnection)).getConnection();
        verify(mockPhysicalConnection).close();
    }

    @Test
    public void testForceClosePhysicalConnection_NoExistingConnection() throws SQLException {
        // Force close physical connection without existing connection
        // Should not throw exception
        Assertions.assertDoesNotThrow(() -> dataSourceProxy.forceClosePhysicalConnection(xaXid));
    }

    @Test
    public void testDefaultResourceGroupId() {
        Assertions.assertEquals("DEFAULT_XA", TestDataSourceProxyXA.DEFAULT_RESOURCE_GROUP_ID);
    }

    /**
     * Test implementation of AbstractDataSourceProxyXA for testing purposes
     */
    private static class TestDataSourceProxyXA extends AbstractDataSourceProxyXA {

        private ConnectionProxyXA mockConnectionProxy;

        public TestDataSourceProxyXA() {
            this.branchType = BranchType.XA;
            this.resourceId = "test-resource-id";
        }

        @Override
        protected Connection getConnectionProxyXA() throws SQLException {
            // Return a mock connection for testing
            if (mockConnectionProxy == null) {
                mockConnectionProxy = mock(ConnectionProxyXA.class);
                Connection mockWrappedConnection = mock(Connection.class);
                when(mockConnectionProxy.getWrappedConnection()).thenReturn(mockWrappedConnection);
                when(mockWrappedConnection.isClosed()).thenReturn(false);
            }
            return mockConnectionProxy;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return getConnectionProxyXA();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnectionProxyXA();
        }
    }
}
