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

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.MySqlUtils;
import com.kingbase8.xa.KBXAConnection;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.jdbc.MysqlXAConnection;
import com.oscar.xa.Jdbc3XAConnection;
import org.apache.seata.core.constants.DBType;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.combine.CombineConnectionHolder;
import org.apache.seata.rm.datasource.mock.MockDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.mariadb.jdbc.MariaXaConnection;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for DataSourceProxyXA
 */
public class DataSourceProxyXATest {
    @BeforeEach
    public void setUp() {
        // Clean up context before each test
        RootContext.unbind();
        RootContext.unbindBranchType();
        RootContext.unbindCombineTransaction();
    }

    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();

        DataSourceProxyXA dataSourceProxy = new DataSourceProxyXA(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxyXA dataSourceProxy2 = new DataSourceProxyXA(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void testGetConnection() throws SQLException, ClassNotFoundException {
        Driver driver = mock(Driver.class);
        JdbcConnection connection = mock(JdbcConnection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        Mockito.when(connection.unwrap(Connection.class)).thenReturn(connection);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        druidDataSource.setUrl("jdbc:mysql:xxx");
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);
        Assertions.assertTrue(dataSourceProxyXA.isShouldBeHeld());

        MysqlXAConnection xaConnection = mock(MysqlXAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaConnection.getXAResource()).thenReturn(mock(XAResource.class));

        try (MockedStatic<MySqlUtils> mySqlUtilsMock = Mockito.mockStatic(MySqlUtils.class)) {
            mySqlUtilsMock
                    .when(() -> MySqlUtils.createXAConnection(any(), any()))
                    .thenReturn(xaConnection);
            Connection connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
            Assertions.assertFalse(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
            RootContext.bind("test");
            connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
            Assertions.assertTrue(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
            ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA) dataSourceProxyXA.getConnection();
            Assertions.assertSame(
                    connection, connectionProxyXA.getWrappedConnection().unwrap(Connection.class));
            Assertions.assertSame(xaConnection, connectionProxyXA.getWrappedXAConnection());
            Assertions.assertSame(connection, xaConnection.getConnection());
        }
    }

    @Test
    public void testGetMariaXaConnection() throws SQLException, ClassNotFoundException {
        XAConnection xaConnection =
                testGetXaConnection(MariaXaConnection.class, "jdbc:mariadb:xxx", "org.mariadb.jdbc.MariaDbConnection");
        Connection connectionInXA = xaConnection.getConnection();
        Assertions.assertEquals(
                "org.mariadb.jdbc.MariaDbConnection", connectionInXA.getClass().getName());
        tearDown();
    }

    @Test
    @DisabledIfSystemProperty(
            named = "druid.version",
            matches = "[0-1].[1-2].[0-7]",
            disabledReason = "druid 1.2.8 correct support kingbase")
    public void testGetKingbaseXaConnection() throws SQLException, ClassNotFoundException {
        testGetXaConnection(KBXAConnection.class, "jdbc:kingbase8:xxx", "com.kingbase8.jdbc.KbConnection");
        tearDown();
    }

    @Test
    @DisabledIfSystemProperty(
            named = "druid.version",
            matches = "[0-1].[1-2].[0-24]",
            disabledReason = "druid 1.2.24 correct support oscar")
    public void testGetOscarXaConnection() throws SQLException, ClassNotFoundException {
        testGetXaConnection(Jdbc3XAConnection.class, "jdbc:oscar:xxx", "com.oscar.jdbc.OscarJdbc2Connection");
        tearDown();
    }

    private XAConnection testGetXaConnection(
            Class<? extends XAConnection> xaConnectionClass, String mockJdbcUrl, String connectionClassName)
            throws SQLException, ClassNotFoundException {
        // Mock
        Driver driver = mock(Driver.class);
        Class clazz = Class.forName(connectionClassName);
        Connection connection = (Connection) (mock(clazz));
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn(mockJdbcUrl);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        druidDataSource.setUrl(mockJdbcUrl);
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);
        // Test isShouldBeHeld
        String dbType = dataSourceProxyXA.getDbType();
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
                || DBType.MARIADB.name().equalsIgnoreCase(dbType)
                || DBType.OSCAR.name().equalsIgnoreCase(dbType)) {
            Assertions.assertTrue(dataSourceProxyXA.isShouldBeHeld());
        }
        Connection connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
        Assertions.assertFalse(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        RootContext.bind("test");
        connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();

        Assertions.assertTrue(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA) dataSourceProxyXA.getConnection();

        Connection wrappedConnection = connectionProxyXA.getWrappedConnection();
        Assertions.assertTrue(wrappedConnection instanceof PooledConnection);

        Connection wrappedPhysicalConn = ((PooledConnection) wrappedConnection).getConnection();
        wrappedPhysicalConn = wrappedConnection.unwrap(Connection.class);
        Assertions.assertSame(wrappedPhysicalConn, connection);

        XAConnection xaConnection = connectionProxyXA.getWrappedXAConnection();
        Assertions.assertEquals(xaConnection.getClass(), xaConnectionClass);
        return xaConnection;
    }

    @Test
    public void testGetConnectionInCombineMode() throws SQLException {
        RootContext.bind("testXID");
        RootContext.bindCombineTransaction();

        ConnectionProxyXA combineConn = mock(ConnectionProxyXA.class);
        when(combineConn.isClosed()).thenReturn(false);

        try (MockedStatic<CombineConnectionHolder> holderMock = Mockito.mockStatic(CombineConnectionHolder.class)) {
            holderMock
                    .when(() -> CombineConnectionHolder.get(any(DataSource.class)))
                    .thenReturn(combineConn);
            Driver driver = mock(Driver.class);
            JdbcConnection connection = mock(JdbcConnection.class);
            Mockito.when(connection.getAutoCommit()).thenReturn(true);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
            Mockito.when(connection.getMetaData()).thenReturn(metaData);
            Mockito.when(driver.connect(any(), any())).thenReturn(connection);

            DruidDataSource realDataSource = new DruidDataSource();
            realDataSource.setDriver(driver);
            realDataSource.setUrl("jdbc:mysql:xxx");
            DataSourceProxyXA proxyDataSource = new DataSourceProxyXA(realDataSource);

            Connection result = proxyDataSource.getConnection();

            Assertions.assertEquals(combineConn, result);

            holderMock.verify(() -> CombineConnectionHolder.get(realDataSource));
        }
    }

    @AfterAll
    public static void tearDown() {
        RootContext.unbind();
    }
}
