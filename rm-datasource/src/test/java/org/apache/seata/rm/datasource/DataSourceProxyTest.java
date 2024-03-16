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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.mock.MockDataSource;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class DataSourceProxyTest {

    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();
        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxy dataSourceProxy2 = new DataSourceProxy(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void testNotSupportDb() {
        final MockDriver mockDriver = new MockDriver();
        final String username = "username";
        final String jdbcUrl = "jdbc:mock:xxx";

        // create data source
        final DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setDriver(mockDriver);
        dataSource.setUsername(username);
        dataSource.setPassword("password");

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> new DataSourceProxy(dataSource));
        assertThat(throwable).hasMessageContaining("AT mode don't support the dbtype");
    }


    @Test
    public void testUndologTableNotExist() {
        DataSource dataSource = new MockDataSource();

        MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = Mockito.mockStatic(UndoLogManagerFactory.class);

        MySQLUndoLogManager mysqlUndoLogManager = mock(MySQLUndoLogManager.class);
        undoLogManagerFactoryMockedStatic.when(()->UndoLogManagerFactory.getUndoLogManager(anyString()))
                .thenReturn(mysqlUndoLogManager);

        doReturn(false).when(mysqlUndoLogManager).hasUndoLogTable(any(Connection.class));

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> new DataSourceProxy(dataSource));
        undoLogManagerFactoryMockedStatic.close();

        assertThat(throwable).hasMessageContaining("table not exist");
    }

    @Test
    public void getResourceIdTest() throws SQLException, NoSuchFieldException, IllegalAccessException {
        // Disable 'DataSourceProxy.tableMetaExecutor' to prevent unit tests from being affected
        Field enableField = TableMetaCacheFactory.class.getDeclaredField("ENABLE_TABLE_META_CHECKER_ENABLE");
        enableField.setAccessible(true);
        enableField.set(null, false);


        final MockDriver mockDriver = new MockDriver();
        final String username = "username";
        final String jdbcUrl = "jdbc:mock:xxx";

        // create data source
        final DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setDriver(mockDriver);
        dataSource.setUsername(username);
        dataSource.setPassword("password");

        // create data source proxy
        final DataSourceProxy proxy = getDataSourceProxy(dataSource);

        // get fields
        Field resourceIdField = proxy.getClass().getDeclaredField("resourceId");
        resourceIdField.setAccessible(true);
        Field dbTypeField = proxy.getClass().getDeclaredField("dbType");
        dbTypeField.setAccessible(true);
        Field userNameField = proxy.getClass().getDeclaredField("userName");
        userNameField.setAccessible(true);
        Field jdbcUrlField = proxy.getClass().getDeclaredField("jdbcUrl");
        jdbcUrlField.setAccessible(true);


        // set userName
        String userNameFromMetaData = dataSource.getConnection().getMetaData().getUserName();
        Assertions.assertEquals(userNameFromMetaData, username);
        userNameField.set(proxy, username);


        // case: dbType = oracle
        {
            resourceIdField.set(proxy, null);
            dbTypeField.set(proxy, org.apache.seata.sqlparser.util.JdbcConstants.ORACLE);
            Assertions.assertEquals("jdbc:mock:xxx/username", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));
        }


        // case: dbType = postgresql
        {
            resourceIdField.set(proxy, null);
            dbTypeField.set(proxy, org.apache.seata.sqlparser.util.JdbcConstants.POSTGRESQL);
            Assertions.assertEquals(jdbcUrl, proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            resourceIdField.set(proxy, null);
            jdbcUrlField.set(proxy, "jdbc:postgresql://mock/postgresql?xxx=1111&currentSchema=schema1,schema2&yyy=1");
            Assertions.assertEquals("jdbc:postgresql://mock/postgresql?currentSchema=schema1!schema2", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            resourceIdField.set(proxy, null);
            jdbcUrlField.set(proxy, "jdbc:postgresql://192.168.1.123:30100,192.168.1.124:30100?xxx=1111&currentSchema=schema1,schema2&yyy=1");
            Assertions.assertEquals("jdbc:postgresql://192.168.1.123:30100|192.168.1.124:30100?currentSchema=schema1!schema2", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            jdbcUrlField.set(proxy, jdbcUrl);
        }

        // case: dbType = dm
        {
            resourceIdField.set(proxy, null);
            dbTypeField.set(proxy, org.apache.seata.sqlparser.util.JdbcConstants.DM);
            Assertions.assertEquals(jdbcUrl, proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            resourceIdField.set(proxy, null);
            jdbcUrlField.set(proxy, "jdbc:dm://mock/dm?xxx=1111&schema=schema1");
            Assertions.assertEquals("jdbc:dm://mock/dm?schema=schema1", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));
            jdbcUrlField.set(proxy, jdbcUrl);
        }

        // case: dbType = mysql
        {
            resourceIdField.set(proxy, null);
            dbTypeField.set(proxy, org.apache.seata.sqlparser.util.JdbcConstants.MYSQL);
            Assertions.assertEquals(jdbcUrl, proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            resourceIdField.set(proxy, null);
            jdbcUrlField.set(proxy, "jdbc:mysql:loadbalance://192.168.100.2:3306,192.168.100.3:3306,192.168.100.1:3306/seata");
            Assertions.assertEquals("jdbc:mysql:loadbalance://192.168.100.2:3306|192.168.100.3:3306|192.168.100.1:3306/seata", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));
            jdbcUrlField.set(proxy, jdbcUrl);
        }

        // case: dbType = sqlserver
        {
            resourceIdField.set(proxy, null);
            dbTypeField.set(proxy, org.apache.seata.sqlparser.util.JdbcConstants.SQLSERVER);
            Assertions.assertEquals(jdbcUrl, proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));

            resourceIdField.set(proxy, null);
            jdbcUrlField.set(proxy, "jdbc:mock:xxx;database=test");
            Assertions.assertEquals("jdbc:mock:xxx;database=test", proxy.getResourceId(), "dbType=" + dbTypeField.get(proxy));
            jdbcUrlField.set(proxy, jdbcUrl);
        }
    }

    // to skip the db & undolog table check
    public static DataSourceProxy getDataSourceProxy(DataSource dataSource) {
        try (MockedStatic<UndoLogManagerFactory> undoLogManagerFactoryMockedStatic = Mockito.mockStatic(UndoLogManagerFactory.class)) {
            MySQLUndoLogManager mysqlUndoLogManager = mock(MySQLUndoLogManager.class);
            undoLogManagerFactoryMockedStatic.when(() -> UndoLogManagerFactory.getUndoLogManager(anyString())).thenReturn(mysqlUndoLogManager);

            doReturn(true).when(mysqlUndoLogManager).hasUndoLogTable(any(Connection.class));

            // create data source proxy
            return new DataSourceProxy(dataSource);
        }
    }
}
