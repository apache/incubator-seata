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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AbstractDataSourceProxyXA
 * Focus on verifying actual results and business logic
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
    public void testGetConnectionForXAFinish_ReturnsExistingOpenConnection() throws SQLException {
        // Verify that an existing open connection is returned (not a new one)
        ConnectionProxyXA existingConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);
        when(existingConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(mockWrappedConnection.isClosed()).thenReturn(false);

        // Store the existing connection
        dataSourceProxy.hold(xaXid.toString(), existingConnection);

        // Get connection for XA finish
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        // Verify it returns the EXACT same connection instance (not a new one)
        Assertions.assertSame(
                existingConnection, result, "Should return the exact same connection instance that was held");

        // Verify that getConnectionProxyXA was NOT called (didn't create a new connection)
        Assertions.assertEquals(
                0,
                dataSourceProxy.getConnectionProxyXACallCount,
                "Should not create a new connection when an open one exists");
    }

    @Test
    public void testGetConnectionForXAFinish_CreatesNewConnectionWhenExistingIsClosed() throws SQLException {
        // Verify that a new connection is created when the existing one is closed
        ConnectionProxyXA closedConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);
        when(closedConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(mockWrappedConnection.isClosed()).thenReturn(true);

        // Store the closed connection
        dataSourceProxy.hold(xaXid.toString(), closedConnection);

        // Get connection for XA finish
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        // Verify it returns the NEW connection (from getConnectionProxyXA)
        Assertions.assertSame(
                dataSourceProxy.getNewConnection(),
                result,
                "Should return a new connection when existing one is closed");

        // Verify that a new connection was actually created
        Assertions.assertEquals(
                1, dataSourceProxy.getConnectionProxyXACallCount, "Should create exactly one new connection");
    }

    @Test
    public void testGetConnectionForXAFinish_CreatesNewConnectionWhenNoneExists() throws SQLException {
        // Verify that a new connection is created when no existing connection is found

        // Don't hold any connection, so lookup returns null
        ConnectionProxyXA result = dataSourceProxy.getConnectionForXAFinish(xaXid);

        // Verify it returns the new connection from getConnectionProxyXA
        Assertions.assertSame(
                dataSourceProxy.getNewConnection(),
                result,
                "Should return a new connection when no existing connection is found");

        // Verify that a new connection was actually created
        Assertions.assertEquals(
                1, dataSourceProxy.getConnectionProxyXACallCount, "Should create exactly one new connection");
    }

    @Test
    public void testForceClosePhysicalConnection_ClosesConnectionAndWrappedConnection() throws SQLException {
        // Verify that both the connection proxy and its wrapped connection are closed
        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);
        Connection mockWrappedConnection = mock(Connection.class);

        Mockito.doNothing().when(mockConnection).close();
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        Mockito.doNothing().when(mockWrappedConnection).close();

        // Store the connection
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Force close
        dataSourceProxy.forceClosePhysicalConnection(xaXid);

        // Verify both connections were closed
        verify(mockConnection).close();
        verify(mockWrappedConnection).close();
    }

    @Test
    public void testForceClosePhysicalConnection_ClosesPooledConnectionCorrectly() throws SQLException {
        // Verify that for PooledConnection, the physical connection is closed
        Connection mockWrappedConnection =
                mock(Connection.class, Mockito.withSettings().extraInterfaces(PooledConnection.class));
        Connection mockPhysicalConnection = mock(Connection.class);

        ConnectionProxyXA mockConnection = mock(ConnectionProxyXA.class);

        Mockito.doNothing().when(mockConnection).close();
        when(mockConnection.getWrappedConnection()).thenReturn(mockWrappedConnection);
        when(((PooledConnection) mockWrappedConnection).getConnection()).thenReturn(mockPhysicalConnection);
        Mockito.doNothing().when(mockPhysicalConnection).close();

        // Store the connection
        dataSourceProxy.hold(xaXid.toString(), mockConnection);

        // Force close
        dataSourceProxy.forceClosePhysicalConnection(xaXid);

        // Verify the proxy connection was closed
        verify(mockConnection).close();

        // Verify the physical connection (from PooledConnection) was closed, not the wrapper
        verify(mockPhysicalConnection).close();
        verify(mockWrappedConnection, never()).close();
    }

    @Test
    public void testForceClosePhysicalConnection_DoesNothingWhenNoConnection() throws SQLException {
        // Verify that no exception is thrown when there's no connection to close

        // Don't hold any connection
        Assertions.assertDoesNotThrow(
                () -> dataSourceProxy.forceClosePhysicalConnection(xaXid),
                "Should not throw exception when no connection exists");

        // Verify no connections were created or closed
        Assertions.assertEquals(0, dataSourceProxy.getConnectionProxyXACallCount, "Should not create any connections");
    }

    @Test
    public void testDefaultResourceGroupId() {
        // Verify the default resource group ID constant value
        Assertions.assertEquals(
                "DEFAULT_XA",
                TestDataSourceProxyXA.DEFAULT_RESOURCE_GROUP_ID,
                "DEFAULT_RESOURCE_GROUP_ID should be 'DEFAULT_XA'");
    }

    @Test
    public void testGetBranchType() {
        // Verify getBranchType returns the correct branch type
        Assertions.assertEquals(BranchType.XA, dataSourceProxy.getBranchType(), "Branch type should be XA");
    }

    @Test
    public void testGetResourceId() {
        // Verify getResourceId returns the configured resource ID
        Assertions.assertEquals(
                "test-resource-id", dataSourceProxy.getResourceId(), "Resource ID should match the configured value");
    }

    /**
     * Test implementation of AbstractDataSourceProxyXA for testing purposes
     */
    private static class TestDataSourceProxyXA extends AbstractDataSourceProxyXA {

        private ConnectionProxyXA mockConnectionProxy;
        int getConnectionProxyXACallCount = 0;

        public TestDataSourceProxyXA() {
            this.branchType = BranchType.XA;
            this.resourceId = "test-resource-id";
        }

        @Override
        protected Connection getConnectionProxyXA() throws SQLException {
            getConnectionProxyXACallCount++;
            // Create a new mock connection each time
            mockConnectionProxy = mock(ConnectionProxyXA.class);
            Connection mockWrappedConnection = mock(Connection.class);
            when(mockConnectionProxy.getWrappedConnection()).thenReturn(mockWrappedConnection);
            when(mockWrappedConnection.isClosed()).thenReturn(false);
            return mockConnectionProxy;
        }

        public ConnectionProxyXA getNewConnection() {
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
