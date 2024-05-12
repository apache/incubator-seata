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
package io.seata.rm.datasource.xa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;

import com.alibaba.druid.pool.DruidDataSource;

import com.mysql.jdbc.JDBC4MySQLConnection;
import com.mysql.jdbc.jdbc2.optional.JDBC4ConnectionWrapper;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.mock.MockDataSource;
import org.apache.seata.rm.datasource.xa.ConnectionProxyXA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for DataSourceProxyXA
 */
public class DataSourceProxyXATest {


    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();

        DataSourceProxyXA dataSourceProxy = new DataSourceProxyXA(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxyXA dataSourceProxy2 = new DataSourceProxyXA(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void testGetConnection() throws SQLException {
        // Mock
        Driver driver = Mockito.mock(Driver.class);
        JDBC4MySQLConnection connection = Mockito.mock(JDBC4MySQLConnection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);
        RootContext.unbind();
        Connection connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
        Assertions.assertFalse(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        RootContext.bind("test");
        connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
        Assertions.assertTrue(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA)dataSourceProxyXA.getConnection();

        Connection wrappedConnection = connectionProxyXA.getWrappedConnection();
        Assertions.assertTrue(wrappedConnection instanceof PooledConnection);

        Connection wrappedPhysicalConn = ((PooledConnection)wrappedConnection).getConnection();
        Assertions.assertSame(wrappedPhysicalConn, connection);

        XAConnection xaConnection = connectionProxyXA.getWrappedXAConnection();
        Connection connectionInXA = xaConnection.getConnection();
        Assertions.assertTrue(connectionInXA instanceof JDBC4ConnectionWrapper);
        tearDown();
    }

    @Test
    public void testGetMariaXaConnection() throws SQLException, ClassNotFoundException {
        // Mock
        Driver driver = Mockito.mock(Driver.class);
        Class clazz = Class.forName("org.mariadb.jdbc.MariaDbConnection");
        Connection connection = (Connection)(Mockito.mock(clazz));
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mariadb:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);
        RootContext.unbind();
        Connection connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
        Assertions.assertFalse(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        RootContext.bind("test");
        connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();

        Assertions.assertTrue(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA)dataSourceProxyXA.getConnection();

        Connection wrappedConnection = connectionProxyXA.getWrappedConnection();
        Assertions.assertTrue(wrappedConnection instanceof PooledConnection);

        Connection wrappedPhysicalConn = ((PooledConnection)wrappedConnection).getConnection();
        Assertions.assertSame(wrappedPhysicalConn, connection);

        XAConnection xaConnection = connectionProxyXA.getWrappedXAConnection();
        Connection connectionInXA = xaConnection.getConnection();
        Assertions.assertEquals("org.mariadb.jdbc.MariaDbConnection", connectionInXA.getClass().getName());
    }

    @AfterEach
    public void tearDown() {
        RootContext.unbind();
    }
}
