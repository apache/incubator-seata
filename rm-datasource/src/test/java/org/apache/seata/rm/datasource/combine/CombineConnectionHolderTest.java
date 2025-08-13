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
package org.apache.seata.rm.datasource.combine;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.xa.ConnectionProxyXA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class CombineConnectionHolderTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private ConnectionProxyXA connectionProxyXA;

    private AutoCloseable closeable;
    private MockedStatic<RootContext> mockedRootContext;

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
        mockedRootContext = Mockito.mockStatic(RootContext.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
        mockedRootContext.close();
    }

    @Test
    public void testGet() throws SQLException {
        String xid = "test-xid";
        mockedRootContext.when(RootContext::getXID).thenReturn(xid);

        assertNull(CombineConnectionHolder.get(dataSource));

        RootContext.bind(xid);
        CombineConnectionHolder.putConnection(dataSource, connectionProxyXA);

        assertSame(connectionProxyXA, CombineConnectionHolder.get(dataSource));
        CombineConnectionHolder.clear();
    }

    @Test
    public void testPutConnection() throws SQLException {
        String xid = "test-xid";
        mockedRootContext.when(RootContext::getXID).thenReturn(xid);
        RootContext.bind(xid);
        CombineConnectionHolder.putConnection(dataSource, connectionProxyXA);

        ConnectionProxyXA getConnectionProxyXA = CombineConnectionHolder.get(dataSource);
        assertNotNull(getConnectionProxyXA);
        assertSame(connectionProxyXA, getConnectionProxyXA);

        verify(connectionProxyXA).setAutoCommit(false);
        verify(connectionProxyXA).setCombine(true);
        CombineConnectionHolder.clear();
    }

    @Test
    public void testGetDsConn() throws SQLException {
        String xid = "test-xid";
        mockedRootContext.when(RootContext::getXID).thenReturn(xid);

        Collection<ConnectionProxyXA> connections = CombineConnectionHolder.getDsConn();
        assertTrue(connections.isEmpty());

        RootContext.bind(xid);
        CombineConnectionHolder.putConnection(dataSource, connectionProxyXA);

        connections = CombineConnectionHolder.getDsConn();
        assertEquals(1, connections.size());
        assertSame(connectionProxyXA, connections.iterator().next());
        CombineConnectionHolder.clear();
    }

    @Test
    public void testClear() throws SQLException {
        String xid = "test-xid";
        mockedRootContext.when(RootContext::getXID).thenReturn(xid);

        RootContext.bind(xid);
        CombineConnectionHolder.putConnection(dataSource, connectionProxyXA);

        CombineConnectionHolder.clear();

        assertNull(CombineConnectionHolder.get(dataSource));
    }
}
