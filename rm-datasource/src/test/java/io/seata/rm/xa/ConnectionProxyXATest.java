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

import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.xa.ConnectionProxyXA;
import io.seata.rm.datasource.xa.StatementProxyXA;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for ConnectionProxyXA
 *
 * @author sharajava
 */
public class ConnectionProxyXATest {

    private class ValueHolder<T> {
        public T value;

        public ValueHolder(T value) {
            this.value = value;
        }

        public ValueHolder() {

        }
    }

    @Test
    public void testInit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(false);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        Assertions.assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                try {
                    connectionProxyXA.init();
                } catch (Throwable ex) {
                    Assertions.assertTrue(ex.getMessage().startsWith("Connection[autocommit=false]"));
                    throw ex;
                }
            }
        });
    }

    @Test
    public void testXABranchCommit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        ValueHolder<Boolean> autoCommitOnConnection = new ValueHolder<>();
        autoCommitOnConnection.value = true;
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                autoCommitOnConnection.value = (boolean)invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(connection).setAutoCommit(any(Boolean.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return autoCommitOnConnection.value;
            }
        }).when(connection).getAutoCommit();

        XAResource xaResource = Mockito.mock(XAResource.class);
        ValueHolder<Boolean> xaStarted = new ValueHolder<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaStarted.value = true;
                return null;
            }
        }).when(xaResource).start(any(Xid.class), any(Integer.class));

        ValueHolder<Boolean> xaEnded = new ValueHolder<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaEnded.value = true;
                return null;
            }
        }).when(xaResource).end(any(Xid.class), any(Integer.class));

        ValueHolder<Boolean> xaPrepared = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaPrepared.value = true;
                return null;
            }
        }).when(xaResource).prepare(any(Xid.class));

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

        Assertions.assertFalse(connectionProxyXA.getAutoCommit());
        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Assertions.assertTrue(connection.getAutoCommit());
        // Assert XA start was invoked
        Assertions.assertTrue(xaStarted.value);

        connectionProxyXA.commit();
        Assertions.assertTrue(xaEnded.value);
        Assertions.assertTrue(xaPrepared.value);
    }

    @Test
    public void testXABranchRollback() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        ValueHolder<Boolean> autoCommitOnConnection = new ValueHolder<>();
        autoCommitOnConnection.value = true;
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                autoCommitOnConnection.value = (boolean)invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(connection).setAutoCommit(any(Boolean.class));

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return autoCommitOnConnection.value;
            }
        }).when(connection).getAutoCommit();

        XAResource xaResource = Mockito.mock(XAResource.class);
        ValueHolder<Boolean> xaStarted = new ValueHolder<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaStarted.value = true;
                return null;
            }
        }).when(xaResource).start(any(Xid.class), any(Integer.class));

        ValueHolder<Boolean> xaEnded = new ValueHolder<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaEnded.value = true;
                return null;
            }
        }).when(xaResource).end(any(Xid.class), any(Integer.class));

        ValueHolder<Boolean> xaPrepared = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaPrepared.value = true;
                return null;
            }
        }).when(xaResource).prepare(any(Xid.class));

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

        Assertions.assertFalse(connectionProxyXA.getAutoCommit());
        // Assert setAutoCommit = false was NEVER invoked on the wrapped connection
        Assertions.assertTrue(connection.getAutoCommit());
        // Assert XA start was invoked
        Assertions.assertTrue(xaStarted.value);

        connectionProxyXA.rollback();
        Assertions.assertTrue(xaEnded.value);
        // Not prepared
        Assertions.assertFalse(xaPrepared.value);
    }

    @Test
    public void testClose() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        ValueHolder<Boolean> closed = new ValueHolder<>();
        closed.value = false;
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                closed.value = true;
                return null;
            }
        }).when(connection).close();
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
        Assertions.assertFalse(closed.value);

        ConnectionProxyXA connectionProxyXA2 = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA2.init();
        // Kept
        connectionProxyXA2.setHeld(false);
        // call close on proxy
        connectionProxyXA2.close();
        // Assert the original connection was ALSO closed
        Assertions.assertTrue(closed.value);
    }

    @Test
    public void testXACommit() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        ValueHolder<Boolean> xaCommitted = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaCommitted.value = true;
                return null;
            }
        }).when(xaResource).commit(any(Xid.class), any(Boolean.class));

        ValueHolder<Boolean> xaRolledback = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaRolledback.value = true;
                return null;
            }
        }).when(xaResource).rollback(any(Xid.class));

        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaCommit("xxx", 123L, null);
        Assertions.assertTrue(xaCommitted.value);
        Assertions.assertFalse(xaRolledback.value);
    }

    @Test
    public void testXARollback() throws Throwable {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);

        XAResource xaResource = Mockito.mock(XAResource.class);
        ValueHolder<Boolean> xaCommitted = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaCommitted.value = true;
                return null;
            }
        }).when(xaResource).commit(any(Xid.class), any(Boolean.class));

        ValueHolder<Boolean> xaRolledback = new ValueHolder<>(false);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                xaRolledback.value = true;
                return null;
            }
        }).when(xaResource).rollback(any(Xid.class));

        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        BaseDataSourceResource<ConnectionProxyXA> baseDataSourceResource = Mockito.mock(BaseDataSourceResource.class);
        String xid = "xxx";

        ConnectionProxyXA connectionProxyXA = new ConnectionProxyXA(connection, xaConnection, baseDataSourceResource, xid);
        connectionProxyXA.init();

        connectionProxyXA.xaRollback("xxx", 123L, null);
        Assertions.assertFalse(xaCommitted.value);
        Assertions.assertTrue(xaRolledback.value);
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
}
