/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.xa;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.xa.ConnectionProxyXA;
import io.seata.rm.datasource.xa.StatementProxyXA;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

/**
 * Tests for ConnectionProxyXA
 *
 * @author sharajava
 */
public class ConnectionProxyXATest {

    @Test
    public void testInit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(false);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);

        Assertions.assertThrows(IllegalStateException.class,
                connectionProxyXA::init,
                "Connection[autocommit=false] as default is NOT supported");
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

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.setAutoCommit(false);

        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Mockito.verify(connection, times(0)).setAutoCommit(false);
        // Assert XA start was invoked
        Mockito.verify(xaResource).start(any(Xid.class), any(Integer.class));

        connectionProxyXA.commit();

        Mockito.verify(xaResource).end(any(Xid.class), any(Integer.class));
        Mockito.verify(xaResource).prepare(any(Xid.class));
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

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.setAutoCommit(false);

        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Mockito.verify(connection, times(0)).setAutoCommit(false);

        // Assert XA start was invoked
        Mockito.verify(xaResource).start(any(Xid.class), any(Integer.class));

        connectionProxyXA.rollback();

        Mockito.verify(xaResource).end(any(Xid.class), any(Integer.class));

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

        ConnectionProxyXA connectionProxyXA1 = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA1.init();
        // Kept
        connectionProxyXA1.setHeld(true);
        // call close on proxy
        connectionProxyXA1.close();
        // Assert the original connection was NOT closed
        Mockito.verify(connection, times(0)).close();

        ConnectionProxyXA connectionProxyXA2 = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
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

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaCommit("xxx", 123L, null);

        Mockito.verify(xaResource).commit(any(Xid.class), any(Boolean.class));
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

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaRollback("xxx", 123L, null);

        Mockito.verify(xaResource, times(0)).commit(any(Xid.class), any(Boolean.class));
        Mockito.verify(xaResource).rollback(any(Xid.class));
    }

    @Test
    public void testCreateStatement() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        Statement statement = connectionProxyXA.createStatement();
        Assertions.assertTrue(statement instanceof StatementProxyXA);
    }

    @AfterAll
    public static void tearDown(){
        RootContext.unbind();
    }
}
