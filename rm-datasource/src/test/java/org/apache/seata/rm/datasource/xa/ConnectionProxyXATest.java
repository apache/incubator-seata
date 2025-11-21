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

import org.apache.seata.common.lock.ResourceLock;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Tests for ConnectionProxyXA
 *
 */
public class ConnectionProxyXATest {

    private Connection mockConnection;
    private XAConnection mockXAConnection;
    private XAResource mockXAResource;
    private BaseDataSourceResource<ConnectionProxyXA> mockDataSourceResource;
    private ResourceManager mockResourceManager;

    @BeforeEach
    public void setUp() throws SQLException, XAException {
        mockConnection = Mockito.mock(Connection.class);
        mockXAConnection = Mockito.mock(XAConnection.class);
        mockXAResource = Mockito.mock(XAResource.class);
        mockDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        mockResourceManager = Mockito.mock(ResourceManager.class);

        // Default setup
        when(mockConnection.getAutoCommit()).thenReturn(true);
        when(mockXAConnection.getXAResource()).thenReturn(mockXAResource);
        Mockito.doNothing().when(mockResourceManager).registerResource(any(Resource.class));

        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.XA, mockResourceManager);
    }

    @Test
    public void testConstructor() {
        // Test constructor properly initializes the proxy
        String xid = "test-xid-123";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        Assertions.assertNotNull(connectionProxyXA, "Constructor should create a valid proxy instance");

        // Verify that the proxy correctly wraps the provided connections
        Assertions.assertSame(
                mockConnection,
                connectionProxyXA.getWrappedConnection(),
                "Should correctly wrap the original connection");
        Assertions.assertSame(
                mockXAConnection,
                connectionProxyXA.getWrappedXAConnection(),
                "Should correctly wrap the XA connection");
    }

    @Test
    public void testInitSuccess() throws SQLException {
        // Test successful initialization
        String xid = "test-xid-success";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        // This should succeed without throwing an exception
        Assertions.assertDoesNotThrow(() -> connectionProxyXA.init(), "Init should succeed when autocommit=true");

        // Verify the connection's autocommit status was checked
        Mockito.verify(mockConnection).getAutoCommit();
        Mockito.verify(mockXAConnection).getXAResource();
    }

    @Test
    public void testInitFailsWithAutoCommitFalse() throws SQLException {
        // Test initialization failure when autocommit is false
        when(mockConnection.getAutoCommit()).thenReturn(false);
        String xid = "test-xid-fail";

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        Assertions.assertThrows(
                IllegalStateException.class,
                connectionProxyXA::init,
                "Connection[autocommit=false] as default is NOT supported");
    }

    @Test
    public void testInitFailsWithSQLException() throws SQLException {
        // Test initialization failure when SQLException is thrown
        when(mockConnection.getAutoCommit()).thenThrow(new SQLException("Connection error"));
        String xid = "test-xid-sql-error";

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        Assertions.assertThrows(
                RuntimeException.class,
                connectionProxyXA::init,
                "Init should throw RuntimeException when SQLException occurs");
    }

    @Test
    public void testXABranchCommit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.XA, resourceManager);

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.setAutoCommit(false);

        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Mockito.verify(connection, times(0)).setAutoCommit(false);
        // Assert XA start was invoked
        Mockito.verify(xaResource).start(any(Xid.class), anyInt());

        connectionProxyXA.commit();

        Mockito.verify(xaResource, times(0)).end(any(Xid.class), anyInt());
        Mockito.verify(xaResource, times(0)).prepare(any(Xid.class));
    }

    @Test
    public void testXABranchRollback() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.XA, resourceManager);

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.setAutoCommit(false);

        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Mockito.verify(connection, times(0)).setAutoCommit(false);

        // Assert XA start was invoked
        Mockito.verify(xaResource).start(any(Xid.class), anyInt());

        connectionProxyXA.rollback();

        Mockito.verify(xaResource).end(any(Xid.class), anyInt());

        // Not prepared
        Mockito.verify(xaResource, times(0)).prepare(any(Xid.class));
    }

    @Test
    public void testClose() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA1 =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA1.init();
        // Kept
        connectionProxyXA1.setHeld(true);
        // call close on proxy
        connectionProxyXA1.close();
        // Assert the original connection was NOT closed
        Mockito.verify(connection, times(0)).close();

        ConnectionProxyXA connectionProxyXA2 =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA2.init();
        // Kept
        connectionProxyXA2.setHeld(false);
        // call close on proxy
        connectionProxyXA2.close();
        // Assert the original connection was ALSO closed
        Mockito.verify(connection).close();
    }

    @Test
    public void testXACommit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaCommit("xxx", 123L, null);

        Mockito.verify(xaResource).commit(any(Xid.class), anyBoolean());
        Mockito.verify(xaResource, times(0)).rollback(any(Xid.class));
    }

    @Test
    public void testXARollback() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);

        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaRollback("xxx", 123L, null);

        Mockito.verify(xaResource, times(0)).commit(any(Xid.class), anyBoolean());
        Mockito.verify(xaResource).rollback(any(Xid.class));
    }

    @Test
    public void testCreateStatement() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        Statement statement = connectionProxyXA.createStatement();
        Assertions.assertTrue(statement instanceof StatementProxyXA);
    }

    @Test
    public void testXAReadOnly() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        Mockito.when(connection.isReadOnly()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.XA, resourceManager);

        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();
        connectionProxyXA.setAutoCommit(false);

        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Mockito.verify(connection, times(0)).setAutoCommit(false);
        // Assert XA start was invoked
        Mockito.verify(xaResource, times(0)).start(any(Xid.class), anyInt());

        connectionProxyXA.commit();

        Mockito.verify(xaResource, times(0)).end(any(Xid.class), anyInt());
        Mockito.verify(xaResource, times(0)).prepare(any(Xid.class));

        connectionProxyXA.rollback();
        Mockito.verify(xaResource, times(0)).rollback(any(Xid.class));
    }

    @Test
    public void testGetAutoCommit() throws SQLException, TransactionException {
        // Test getAutoCommit returns current status
        String xid = "test-autocommit";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Initial state should be true (from setUp)
        Assertions.assertTrue(connectionProxyXA.getAutoCommit(), "getAutoCommit should return true initially");

        // After setting autocommit to false, it should return false
        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-autocommit"), eq(null), eq(null)))
                .thenReturn(123L);
        connectionProxyXA.setAutoCommit(false);
        Assertions.assertFalse(
                connectionProxyXA.getAutoCommit(), "getAutoCommit should return false after setAutoCommit(false)");
    }

    @Test
    public void testSetAutoCommitFromTrueToFalse() throws Exception {
        // Test setting autocommit from true to false (starting XA transaction)
        String xid = "test-xa-start";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-xa-start"), eq(null), eq(null)))
                .thenReturn(123L);

        connectionProxyXA.setAutoCommit(false);

        // Verify XA start was called
        Mockito.verify(mockXAResource).start(any(Xid.class), eq(XAResource.TMNOFLAGS));
        // Parameters: BranchType.XA, resource.getResourceId(), null, xid, null, null
        Mockito.verify(mockResourceManager)
                .branchRegister(eq(BranchType.XA), eq(null), eq(null), eq("test-xa-start"), eq(null), eq(null));

        Assertions.assertFalse(connectionProxyXA.getAutoCommit(), "AutoCommit should be false");
    }

    @Test
    public void testSetAutoCommitFromFalseToTrue() throws Exception {
        // Test setting autocommit from false to true (committing XA transaction)
        String xid = "test-xa-commit";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-xa-commit"), eq(null), eq(null)))
                .thenReturn(123L);

        // First set to false to start XA
        connectionProxyXA.setAutoCommit(false);
        // Then set back to true to commit
        connectionProxyXA.setAutoCommit(true);

        Assertions.assertTrue(connectionProxyXA.getAutoCommit(), "AutoCommit should be true");
    }

    @Test
    public void testSetAutoCommitSameValue() throws SQLException, XAException, TransactionException {
        // Test setting autocommit to the same value (should be no-op)
        String xid = "test-same-value";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Set to true (already true, should be no-op)
        connectionProxyXA.setAutoCommit(true);

        // Verify no XA operations were performed
        Mockito.verify(mockXAResource, times(0)).start(any(Xid.class), anyInt());
        Mockito.verify(mockResourceManager, times(0))
                .branchRegister(eq(BranchType.XA), anyString(), eq(null), anyString(), eq(null), eq(null));
    }

    @Test
    public void testSetAutoCommitOnReadOnlyTransaction() throws SQLException, XAException {
        // Test setAutoCommit on read-only transaction
        when(mockConnection.isReadOnly()).thenReturn(true);
        String xid = "test-readonly";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.setAutoCommit(false);

        // Verify no XA operations were performed for read-only transaction
        Mockito.verify(mockXAResource, times(0)).start(any(Xid.class), anyInt());
        Assertions.assertFalse(connectionProxyXA.getAutoCommit(), "AutoCommit should be false");
    }

    @Test
    public void testSetAutoCommitXAStartFails() throws Exception {
        // Test setAutoCommit when XA start fails
        String xid = "test-xa-start-fail";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-xa-start-fail"), eq(null), eq(null)))
                .thenReturn(123L);
        doThrow(new XAException("XA start failed")).when(mockXAResource).start(any(Xid.class), anyInt());

        SQLException exception = Assertions.assertThrows(
                SQLException.class,
                () -> connectionProxyXA.setAutoCommit(false),
                "Should throw SQLException when XA start fails");

        Assertions.assertTrue(
                exception.getMessage().contains("failed to start xa branch"),
                "Exception message should indicate XA start failure");
    }

    @Test
    public void testCommitOnAutoCommitSession() throws SQLException, XAException {
        // Test commit on autocommit session (should be ignored)
        String xid = "test-commit-autocommit";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Should not throw exception and should be ignored
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.commit(), "Commit on autocommit session should be ignored");

        // Verify no XA operations
        Mockito.verify(mockXAResource, times(0)).end(any(Xid.class), anyInt());
        Mockito.verify(mockXAResource, times(0)).prepare(any(Xid.class));
    }

    @Test
    public void testCommitOnReadOnlyTransaction() throws Exception {
        // Test commit on read-only transaction
        when(mockConnection.isReadOnly()).thenReturn(true);
        String xid = "test-commit-readonly";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-commit-readonly"), eq(null), eq(null)))
                .thenReturn(123L);
        connectionProxyXA.setAutoCommit(false);

        // Should not throw exception and should be ignored
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.commit(), "Commit on read-only transaction should be ignored");
    }

    @Test
    public void testRollbackOnAutoCommitSession() throws SQLException, XAException {
        // Test rollback on autocommit session (should be ignored)
        String xid = "test-rollback-autocommit";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Should not throw exception and should be ignored
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.rollback(), "Rollback on autocommit session should be ignored");

        // Verify no XA operations
        Mockito.verify(mockXAResource, times(0)).end(any(Xid.class), anyInt());
        Mockito.verify(mockXAResource, times(0)).rollback(any(Xid.class));
    }

    @Test
    public void testHoldableMethods() throws SQLException {
        // Test Holdable interface methods
        String xid = "test-holdable";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        // Initially not held
        Assertions.assertFalse(connectionProxyXA.isHeld(), "Connection should not be held initially");

        // Set held
        connectionProxyXA.setHeld(true);
        Assertions.assertTrue(connectionProxyXA.isHeld(), "Connection should be held after setHeld(true)");

        // Set not held
        connectionProxyXA.setHeld(false);
        Assertions.assertFalse(connectionProxyXA.isHeld(), "Connection should not be held after setHeld(false)");
    }

    @Test
    public void testShouldBeHeld() throws SQLException {
        // Test shouldBeHeld method with different scenarios
        String xid = "test-should-be-held";

        // When resource says it should be held
        when(mockDataSourceResource.isShouldBeHeld()).thenReturn(true);
        ConnectionProxyXA connectionProxyXA1 =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        Assertions.assertTrue(connectionProxyXA1.shouldBeHeld(), "Should be held when resource indicates so");

        // When resource says it should not be held, but DB type is blank
        when(mockDataSourceResource.isShouldBeHeld()).thenReturn(false);
        when(mockDataSourceResource.getDbType()).thenReturn("");
        ConnectionProxyXA connectionProxyXA2 =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        Assertions.assertTrue(connectionProxyXA2.shouldBeHeld(), "Should be held when DB type is blank");

        // When resource says it should not be held and DB type is not blank
        when(mockDataSourceResource.getDbType()).thenReturn("mysql");
        ConnectionProxyXA connectionProxyXA3 =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        Assertions.assertFalse(
                connectionProxyXA3.shouldBeHeld(),
                "Should not be held when resource indicates so and DB type is not blank");
    }

    @Test
    public void testGetResourceLock() throws SQLException {
        // Test getResourceLock method returns a valid lock
        String xid = "test-resource-lock";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        ResourceLock resourceLock = connectionProxyXA.getResourceLock();
        Assertions.assertNotNull(resourceLock, "Resource lock should not be null");
    }

    @Test
    public void testGetAndSetPrepareTime() throws SQLException {
        // Test getPrepareTime method (initially null, then set during close)
        String xid = "test-prepare-time";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        Assertions.assertNull(connectionProxyXA.getPrepareTime(), "Prepare time should be null initially");
    }

    @Test
    public void testSetCombine() throws SQLException {
        // Test setCombine method
        String xid = "test-combine";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Set combine mode
        connectionProxyXA.setCombine(true);

        // Test that commit returns early in combine mode
        Assertions.assertDoesNotThrow(() -> connectionProxyXA.commit(), "Commit should return early in combine mode");

        // Test that rollback returns early in combine mode
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.rollback(), "Rollback should return early in combine mode");

        // Test that close returns early in combine mode
        Assertions.assertDoesNotThrow(() -> connectionProxyXA.close(), "Close should return early in combine mode");
    }

    @Test
    public void testCreatePreparedStatement() throws SQLException {
        // Test createPreparedStatement returns PreparedStatementProxyXA
        String xid = "test-prepared-statement";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);

        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        when(mockConnection.prepareStatement("SELECT * FROM test")).thenReturn(mockPreparedStatement);

        PreparedStatement statement = connectionProxyXA.prepareStatement("SELECT * FROM test");
        Assertions.assertTrue(
                statement instanceof PreparedStatementProxyXA, "Should return PreparedStatementProxyXA instance");
    }

    @Test
    public void testXACommitWithResourceLock() throws Exception {
        // Test xaCommit method with resource lock
        String xid = "test-xa-commit-lock";
        long branchId = 12345L;
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        // Should not throw exception
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.xaCommit(xid, branchId, null), "xaCommit should not throw exception");

        // Verify XA commit was called
        Mockito.verify(mockXAResource).commit(any(Xid.class), eq(false));
    }

    @Test
    public void testXARollbackWithResourceLock() throws Exception {
        // Test xaRollback method with resource lock
        String xid = "test-xa-rollback-lock";
        long branchId = 12345L;
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();
        // Set up mock for branch registration to enable XA transaction
        when(mockResourceManager.branchRegister(eq(BranchType.XA), anyString(), eq(null), eq(xid), eq(null), eq(null)))
                .thenReturn(branchId);

        // Start XA transaction by setting autoCommit to false
        connectionProxyXA.setAutoCommit(false);
        // Should not throw exception
        Assertions.assertDoesNotThrow(
                () -> connectionProxyXA.xaRollback(xid, branchId, null), "xaRollback should not throw exception");

        // Verify XA end and rollback were called
        Mockito.verify(mockXAResource).end(any(Xid.class), eq(XAResource.TMFAIL));
        Mockito.verify(mockXAResource).rollback(any(Xid.class));
    }

    @Test
    public void testCloseWithXAReadOnly() throws Exception {
        // Test close when XA prepare returns XA_RDONLY
        String xid = "test-close-readonly";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-close-readonly"), eq(null), eq(null)))
                .thenReturn(123L);
        when(mockXAResource.prepare(any(Xid.class))).thenReturn(XAResource.XA_RDONLY);

        connectionProxyXA.setAutoCommit(false);

        // Should not throw exception and should report RDONLY status
        Assertions.assertDoesNotThrow(() -> connectionProxyXA.close(), "Close should handle XA_RDONLY properly");

        // Verify prepare was called
        Mockito.verify(mockXAResource).prepare(any(Xid.class));
        // Verify branch report was called with RDONLY status
        Mockito.verify(mockResourceManager)
                .branchReport(eq(BranchType.XA), eq(xid), anyLong(), eq(BranchStatus.PhaseOne_RDONLY), any());
    }

    @Test
    public void testCloseWithXAException() throws Exception {
        // Test close when XA operations throw exception
        String xid = "test-close-xa-exception";
        ConnectionProxyXA connectionProxyXA =
                new ConnectionProxyXA(mockConnection, mockXAConnection, mockDataSourceResource, xid);
        connectionProxyXA.init();

        when(mockResourceManager.branchRegister(
                        eq(BranchType.XA), anyString(), eq(null), eq("test-close-xa-exception"), eq(null), eq(null)))
                .thenReturn(123L);
        when(mockXAResource.prepare(any(Xid.class))).thenThrow(new XAException("Prepare failed"));

        connectionProxyXA.setAutoCommit(false);

        SQLException exception = Assertions.assertThrows(
                SQLException.class,
                () -> connectionProxyXA.close(),
                "Close should throw SQLException when XA operations fail");

        Assertions.assertTrue(
                exception.getMessage().contains("Failed to end(TMSUCCESS)/prepare xa branch"),
                "Exception message should indicate XA failure");

        // Verify branch report was called with Failed status
        Mockito.verify(mockResourceManager)
                .branchReport(eq(BranchType.XA), eq(xid), anyLong(), eq(BranchStatus.PhaseOne_Failed), any());
    }

    @AfterAll
    public static void tearDown() {
        RootContext.unbind();
    }
}
